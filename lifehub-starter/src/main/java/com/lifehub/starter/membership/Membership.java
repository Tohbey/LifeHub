package com.lifehub.starter.membership;

import java.util.UUID;

/** An active member of a workspace, as reported by identity-workspace. */
public record Membership(UUID workspaceId, UUID userId, WorkspaceRole role) {
}
