package com.lifehub.identityworkspace.service;

import static com.lifehub.starter.membership.WorkspaceRole.ADMIN;
import static com.lifehub.starter.membership.WorkspaceRole.MEMBER;
import static com.lifehub.starter.membership.WorkspaceRole.OWNER;

import com.lifehub.starter.membership.WorkspaceRole;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Who may do what to whom in a household. Every household has exactly one owner at all times:
 * ownership moves only by the owner handing it over, and the owner cannot leave or be removed.
 *
 * <ul>
 * <li>Invite: owners invite admins or members; admins invite members.</li>
 * <li>Change roles: only the owner. Making someone owner hands over ownership.</li>
 * <li>Remove: owners remove anyone else; admins remove members; anyone except the owner may leave.</li>
 * </ul>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MemberPolicy {

    /** Decides whether a member with this role may invite someone at the given role. */
    public static Decision invite(WorkspaceRole actor, WorkspaceRole invitedRole) {
        if (invitedRole == OWNER) {
            return Decision.deny("An invitation cannot make someone the owner.");
        }
        if (actor == OWNER || (actor == ADMIN && invitedRole == MEMBER)) {
            return Decision.ALLOW;
        }
        return Decision.deny(actor == ADMIN ? "Admins can only invite members." : "Only owners and admins can invite.");
    }

    /** Decides whether a member with this role may change a role; {@code self} means their own. */
    public static Decision changeRole(WorkspaceRole actor, boolean self, WorkspaceRole newRole) {
        if (actor != OWNER) {
            return Decision.deny("Only the owner can change roles.");
        }
        if (self) {
            return Decision.deny("Hand ownership to another member instead of changing your own role.");
        }
        return Decision.ALLOW;
    }

    /** Decides whether a member with this role may remove the target; {@code self} means leaving. */
    public static Decision remove(WorkspaceRole actor, boolean self, WorkspaceRole target) {
        if (self) {
            return actor == OWNER
                    ? Decision.deny("The owner cannot leave. Hand ownership to another member first.")
                    : Decision.ALLOW;
        }
        if (actor == OWNER || (actor == ADMIN && target == MEMBER)) {
            return Decision.ALLOW;
        }
        return Decision.deny("You cannot remove this member.");
    }

    public record Decision(boolean allowed, String reason) {

        static final Decision ALLOW = new Decision(true, null);

        /** A refusal with the reason shown to the user. */
        static Decision deny(String reason) {
            return new Decision(false, reason);
        }
    }
}
