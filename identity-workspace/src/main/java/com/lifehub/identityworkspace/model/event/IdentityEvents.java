package com.lifehub.identityworkspace.model.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.lifehub.starter.events.EventType;
import com.lifehub.starter.membership.WorkspaceRole;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Events this service publishes. Must match deploy/docs/events.md (identity.events). */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IdentityEvents {

    public static final String TOPIC = "identity.events";

    public static final EventType USER_CREATED = new EventType(TOPIC, "user.created", 1);
    public static final EventType USER_PROFILE_UPDATED = new EventType(TOPIC, "user.profile.updated", 1);
    public static final EventType HOUSEHOLD_CREATED = new EventType(TOPIC, "household.created", 1);
    public static final EventType HOUSEHOLD_UPDATED = new EventType(TOPIC, "household.updated", 1);
    public static final EventType MEMBER_INVITED = new EventType(TOPIC, "member.invited", 1);
    public static final EventType MEMBER_ADDED = new EventType(TOPIC, "member.added", 1);
    public static final EventType MEMBER_REMOVED = new EventType(TOPIC, "member.removed", 1);
    public static final EventType MEMBER_ROLE_CHANGED = new EventType(TOPIC, "member.role.changed", 1);

    public record UserCreated(UUID userId, UUID personalWorkspaceId, String displayName) {
    }

    public record UserProfileUpdated(UUID userId, String displayName, String email, List<String> changedFields) {
    }

    public record HouseholdCreated(UUID householdId, String name, UUID ownerUserId) {
    }

    public record HouseholdUpdated(UUID householdId, String name, List<String> changedFields) {
    }

    public record MemberInvited(UUID householdId, UUID invitationId, String invitedEmail, UUID invitedUserId,
            WorkspaceRole role, Instant expiresAt) {
    }

    public record MemberAdded(UUID householdId, UUID userId, WorkspaceRole role, UUID invitationId) {
    }

    public record MemberRemoved(UUID householdId, UUID userId, String reason) {

        public static final String REMOVED = "REMOVED";
        public static final String LEFT = "LEFT";
    }

    public record MemberRoleChanged(UUID householdId, UUID userId, WorkspaceRole previousRole, WorkspaceRole newRole) {
    }
}
