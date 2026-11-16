package com.lifehub.starter.membership;

import java.util.List;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * The contract of identity-workspace's internal membership endpoint, shared by the server and
 * {@link HttpMembershipClient} so they cannot drift apart.
 *
 * <ul>
 * <li>{@code GET /internal/v1/workspaces/{workspaceId}/members?sub=...} returns {@link MemberLookupResponse},
 * or 404 when the subject is not an active member.</li>
 * <li>{@code GET /internal/v1/workspaces/{workspaceId}/members?userId=...&userId=...} returns
 * {@link MemberListResponse} with the active members among those IDs.</li>
 * <li>{@code GET /internal/v1/users?sub=...} returns {@link UserLookupResponse}, or 404 when no user has
 * signed in with that subject.</li>
 * </ul>
 * All require a service token with the {@link #SCOPE} scope.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InternalMembershipApi {

    public static final String PATH = "/internal/v1/workspaces/{workspaceId}/members";
    public static final String USERS_PATH = "/internal/v1/users";
    public static final String SCOPE = "membership:read";

    public record MemberLookupResponse(UUID workspaceId, UUID userId, WorkspaceRole role) {
    }

    public record UserLookupResponse(UUID userId) {
    }

    public record MemberListResponse(UUID workspaceId, List<Member> members) {

        public record Member(UUID userId, WorkspaceRole role) {
        }
    }
}
