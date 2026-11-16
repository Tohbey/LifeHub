package com.lifehub.starter.membership;

import java.util.Collection;
import java.util.LinkedHashMap;
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
