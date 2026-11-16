package com.lifehub.starter.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.lifehub.starter.id.Ids;
import com.lifehub.starter.web.ApiException;

class WorkspaceAccessTest {

    private final UUID workspaceId = Ids.newId();
    private final UUID userId = Ids.newId();
    private final Map<String, Membership> memberships = new HashMap<>();

    private final WorkspaceAccess access = new WorkspaceAccess(new MembershipClient() {
        @Override
        public Optional<Membership> findBySubject(UUID ws, String subject) {
            return Optional.ofNullable(memberships.get(ws + "/" + subject));
        }

        @Override
        public Map<UUID, WorkspaceRole> findMembers(UUID ws, Collection<UUID> userIds) {
            Map<UUID, WorkspaceRole> result = new HashMap<>();
            memberships.values().stream()
                    .filter(m -> m.workspaceId().equals(ws) && userIds.contains(m.userId()))
                    .forEach(m -> result.put(m.userId(), m.role()));
            return result;
        }
    });

    @BeforeEach
    void signIn() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "ES384").subject("logto-sub")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void nonMemberGetsNotFound() {
        assertThatThrownBy(() -> access.require(workspaceId))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.status()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void memberBelowRequiredRoleGetsForbidden() {
        memberships.put(workspaceId + "/logto-sub", new Membership(workspaceId, userId, WorkspaceRole.MEMBER));

        assertThatThrownBy(() -> access.require(workspaceId, WorkspaceRole.ADMIN))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.status()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void memberWithEnoughRoleIsAllowed() {
        memberships.put(workspaceId + "/logto-sub", new Membership(workspaceId, userId, WorkspaceRole.OWNER));

        assertThat(access.require(workspaceId, WorkspaceRole.ADMIN).userId()).isEqualTo(userId);
    }

    @Test
    void requireAllMembersRejectsAnOutsider() {
        memberships.put(workspaceId + "/logto-sub", new Membership(workspaceId, userId, WorkspaceRole.MEMBER));

        access.requireAllMembers(workspaceId, java.util.List.of(userId));
        assertThatThrownBy(() -> access.requireAllMembers(workspaceId, java.util.List.of(userId, Ids.newId())))
                .isInstanceOf(ApiException.class);
    }
}
