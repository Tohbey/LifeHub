package com.lifehub.starter.membership;

import java.util.List;
import java.util.Map;
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
 * <li>{@code GET /internal/v1/users?userId=...&userId=...} returns {@link UserNamesResponse}: the display name of
 * each of those users (at most {@link #MAX_BULK}). Unknown IDs are left out; a user without a name maps to null.</li>
 * <li>{@code GET /internal/v1/workspaces/{workspaceId}} returns {@link WorkspaceInfoResponse}: the workspace's
 * name, settings and active members, or 404 when it does not exist.</li>
 * </ul>
 * All require a service token with the {@link #SCOPE} scope.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InternalMembershipApi {

    public static final String PATH = "/internal/v1/workspaces/{workspaceId}/members";
    public static final String USERS_PATH = "/internal/v1/users";
    public static final String WORKSPACE_PATH = "/internal/v1/workspaces/{workspaceId}";
    public static final String SCOPE = "membership:read";
    public static final int MAX_BULK = 100;

    public record MemberLookupResponse(UUID workspaceId, UUID userId, WorkspaceRole role) {
    }

    public record UserLookupResponse(UUID userId) {
    }

    public record UserNamesResponse(Map<UUID, String> names) {
    }

    /**
     * A workspace as other services need it.
     *
     * @param type            PERSONAL or HOUSEHOLD
     * @param timezone        the household's IANA timezone, or null for a personal workspace (no settings)
     * @param defaultCurrency the household's ISO 4217 currency, or null for a personal workspace
     */
    public record WorkspaceInfoResponse(UUID workspaceId, String type, String name, String timezone,
            String defaultCurrency, List<MemberListResponse.Member> members) {
    }

    public record MemberListResponse(UUID workspaceId, List<Member> members) {

        public record Member(UUID userId, WorkspaceRole role) {
        }
    }
}
