package com.lifehub.starter.membership;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lifehub.starter.membership.InternalMembershipApi.MemberListResponse;
import com.lifehub.starter.membership.InternalMembershipApi.MemberLookupResponse;
import com.lifehub.starter.membership.InternalMembershipApi.UserLookupResponse;
import com.lifehub.starter.membership.InternalMembershipApi.UserNamesResponse;
import com.lifehub.starter.membership.InternalMembershipApi.WorkspaceInfoResponse;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;

/**
 * Asks identity-workspace's internal API, with a short cache of positive answers and a circuit breaker.
 * Any failure becomes {@link MembershipUnavailableException}, which denies the request.
 */
public class HttpMembershipClient implements MembershipClient {

    private record CacheKey(UUID workspaceId, String subject) {
    }

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final Cache<CacheKey, Membership> cache;
    /** Subject to user ID never changes once the user exists, so it is kept much longer than memberships. */
    private final Cache<String, UUID> userIds = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .maximumSize(10_000)
            .build();
    /** Workspace settings and members, for timezones and directories; kept as long as membership answers. */
    private final Cache<UUID, WorkspaceInfoResponse> workspaces;
    /** Names change rarely, so they are kept for five minutes; an empty Optional means the user has no name. */
    private final Cache<UUID, Optional<String>> displayNames = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(10_000)
            .build();

    /**
     * @param restClient already configured with the base URL, timeouts and the service token
     */
    public HttpMembershipClient(RestClient restClient, CircuitBreaker circuitBreaker, MembershipProperties properties) {
        this.restClient = restClient;
        this.circuitBreaker = circuitBreaker;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(properties.cacheTtl())
                .maximumSize(10_000)
                .build();
        this.workspaces = Caffeine.newBuilder()
                .expireAfterWrite(properties.cacheTtl())
                .maximumSize(1_000)
                .build();
    }

    /** Returns the membership from the cache, or asks identity-workspace and caches a positive answer. */
    @Override
    public Optional<Membership> findBySubject(UUID workspaceId, String logtoSubject) {
        CacheKey key = new CacheKey(workspaceId, logtoSubject);
        Membership cached = cache.getIfPresent(key);
        if (cached != null) {
            return Optional.of(cached);
        }
        // Only positive answers are cached, so a newly added member gets access straight away.
        Optional<Membership> membership = call(() -> lookup(workspaceId, logtoSubject));
        membership.ifPresent(m -> cache.put(key, m));
        return membership;
    }

    /** Asks identity-workspace which of these users are active members, in one call. Not cached. */
    @Override
    public Map<UUID, WorkspaceRole> findMembers(UUID workspaceId, Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        MemberListResponse response = call(() -> restClient.get()
                .uri(uri -> {
                    uri.path(InternalMembershipApi.PATH);
                    userIds.stream().distinct().forEach(id -> uri.queryParam("userId", id));
                    return uri.build(workspaceId);
                })
                .retrieve()
                .body(MemberListResponse.class));
        Map<UUID, WorkspaceRole> members = new LinkedHashMap<>();
        if (response != null) {
            response.members().forEach(m -> members.put(m.userId(), m.role()));
        }
        return members;
    }

    /** Returns the user ID from the cache, or asks identity-workspace and caches a positive answer. */
    @Override
    public Optional<UUID> findUserIdBySubject(String logtoSubject) {
        UUID cached = userIds.getIfPresent(logtoSubject);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<UUID> userId = call(() -> lookupUser(logtoSubject));
        userId.ifPresent(id -> userIds.put(logtoSubject, id));
        return userId;
    }

    /** Returns cached names and asks identity-workspace for the rest, at most 100 IDs per call. */
    @Override
    public Map<UUID, String> findDisplayNames(Collection<UUID> ids) {
        Map<UUID, String> result = new HashMap<>();
        List<UUID> missing = new ArrayList<>();
        for (UUID id : ids.stream().distinct().toList()) {
            Optional<String> cached = displayNames.getIfPresent(id);
            if (cached != null) {
                result.put(id, cached.orElse(null));
            } else {
                missing.add(id);
            }
        }
        for (int from = 0; from < missing.size(); from += InternalMembershipApi.MAX_BULK) {
            List<UUID> chunk = missing.subList(from, Math.min(from + InternalMembershipApi.MAX_BULK, missing.size()));
            UserNamesResponse response = call(() -> restClient.get()
                    .uri(uri -> {
                        uri.path(InternalMembershipApi.USERS_PATH);
                        chunk.forEach(id -> uri.queryParam("userId", id));
                        return uri.build();
                    })
                    .retrieve()
                    .body(UserNamesResponse.class));
            if (response != null && response.names() != null) {
                response.names().forEach((id, name) -> {
                    displayNames.put(id, Optional.ofNullable(name));
                    result.put(id, name);
                });
            }
        }
        return result;
    }

    /** Returns the cached workspace, or asks identity-workspace; a 404 means it does not exist. */
    @Override
    public Optional<WorkspaceInfoResponse> findWorkspace(UUID workspaceId) {
        WorkspaceInfoResponse cached = workspaces.getIfPresent(workspaceId);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<WorkspaceInfoResponse> found = call(() -> {
            try {
                return Optional.ofNullable(restClient.get()
                        .uri(uri -> uri.path(InternalMembershipApi.WORKSPACE_PATH).build(workspaceId))
                        .retrieve()
                        .body(WorkspaceInfoResponse.class));
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                    return Optional.empty();
                }
                throw ex;
            }
        });
        found.ifPresent(info -> workspaces.put(workspaceId, info));
        return found;
    }

    /** Calls the internal user lookup endpoint; a 404 means nobody has signed in with this subject. */
    private Optional<UUID> lookupUser(String logtoSubject) {
        try {
            UserLookupResponse response = restClient.get()
                    .uri(uri -> uri.path(InternalMembershipApi.USERS_PATH).queryParam("sub", logtoSubject).build())
                    .retrieve()
                    .body(UserLookupResponse.class);
            return Optional.ofNullable(response).map(UserLookupResponse::userId);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw ex;
        }
    }

    /** Calls the internal lookup endpoint; a 404 means not a member. */
    private Optional<Membership> lookup(UUID workspaceId, String logtoSubject) {
        try {
            MemberLookupResponse response = restClient.get()
                    .uri(uri -> uri.path(InternalMembershipApi.PATH).queryParam("sub", logtoSubject).build(workspaceId))
                    .retrieve()
                    .body(MemberLookupResponse.class);
            return Optional.ofNullable(response)
                    .map(r -> new Membership(r.workspaceId(), r.userId(), r.role()));
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw ex;
        }
    }

    /** Runs a call through the circuit breaker and turns any failure into MembershipUnavailableException. */
    private <T> T call(Supplier<T> supplier) {
        try {
            return circuitBreaker.executeSupplier(supplier);
        } catch (RuntimeException ex) {
            throw new MembershipUnavailableException("Membership lookup failed", ex);
        }
    }
}
