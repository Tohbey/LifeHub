package com.lifehub.starter.membership;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Answers "is this person a member of this workspace, and with which role". identity-workspace is the
 * source of truth; other services get {@link HttpMembershipClient}. See ADR 0005.
 *
 * <p>Implementations throw {@link MembershipUnavailableException} when they cannot answer. Callers must
 * treat that as a denial.
 */
public interface MembershipClient {

    /** The active membership of the user with this Logto subject, if any. */
    Optional<Membership> findBySubject(UUID workspaceId, String logtoSubject);

    /** Which of these LifeHub user IDs are active members, with their roles. Non-members are left out. */
    Map<UUID, WorkspaceRole> findMembers(UUID workspaceId, Collection<UUID> userIds);

    /**
     * The LifeHub user ID of this Logto subject, for endpoints that are about the caller rather than a
     * workspace (such as {@code /api/v1/me/notifications}). Empty if the user has never signed in.
     */
    Optional<UUID> findUserIdBySubject(String logtoSubject);

    /**
     * The display names of these LifeHub users, for showing people by name (for example in the audit log).
     * Unknown IDs are left out; a user who never set a name maps to null.
     */
    Map<UUID, String> findDisplayNames(Collection<UUID> userIds);

    /** The workspace's name, settings and active members; empty if it does not exist. */
    Optional<InternalMembershipApi.WorkspaceInfoResponse> findWorkspace(UUID workspaceId);
}
