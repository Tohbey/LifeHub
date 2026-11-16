package com.lifehub.starter.membership;

import java.util.Collection;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.lifehub.starter.web.ApiException;

import lombok.RequiredArgsConstructor;

/**
 * The check every workspace-scoped use case starts with. The user is always the subject of the validated
 * token in the security context, never an ID from the request.
 *
 * <p>A non-member gets 404, the same as for a workspace that does not exist, so workspace IDs cannot be
 * probed. A member whose role is too low gets 403.
 */
@RequiredArgsConstructor
public class WorkspaceAccess {

    private final MembershipClient membershipClient;

    /** Requires the current user to be any active member of the workspace. */
    public Membership require(UUID workspaceId) {
        return require(workspaceId, WorkspaceRole.MEMBER);
    }

    /**
     * Requires the current user to be an active member with at least this role. Throws 404 for a
     * non-member and 403 for a role that is too low.
     */
    public Membership require(UUID workspaceId, WorkspaceRole minimumRole) {
        Membership membership = membershipClient.findBySubject(workspaceId, currentSubject())
                .orElseThrow(() -> ApiException.notFound("Workspace not found."));
        if (!membership.role().isAtLeast(minimumRole)) {
            throw ApiException.forbidden("This needs the " + minimumRole.name().toLowerCase() + " role or higher.");
        }
        return membership;
    }

    /** Fails unless every one of these LifeHub user IDs is an active member, for example split participants. */
    public void requireAllMembers(UUID workspaceId, Collection<UUID> userIds) {
        var members = membershipClient.findMembers(workspaceId, userIds);
        if (!members.keySet().containsAll(userIds)) {
            throw ApiException.badRequest("Every person involved must be a member of this workspace.");
        }
    }

    /**
     * The LifeHub user ID of the caller, for endpoints about the caller rather than a workspace. Fails with
     * 404 if the user has never signed in to LifeHub (identity-workspace creates users on first sign-in).
     */
    public UUID currentUserId() {
        return membershipClient.findUserIdBySubject(currentSubject())
                .orElseThrow(() -> ApiException.notFound("User not found. Sign in to LifeHub first."));
    }

    /** The Logto subject of the user making the current request. */
    public static String currentSubject() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getSubject();
        }
        throw new IllegalStateException("No user token in the security context");
    }
}
