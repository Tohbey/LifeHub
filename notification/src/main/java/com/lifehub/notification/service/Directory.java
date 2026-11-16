package com.lifehub.notification.service;

import java.util.Map;
import java.util.UUID;

/**
 * What notification knows about workspaces and people, built from identity events. Rules use it to find
 * recipients and names without calling identity-workspace.
 */
public interface Directory {

    /** The workspace's name, or a neutral fallback when it is not known yet. */
    String workspaceName(UUID workspaceId);

    /** The active members of the workspace with their roles (OWNER, ADMIN, MEMBER). */
    Map<UUID, String> activeMembers(UUID workspaceId);

    /** The user's display name, or "Someone" when they have not set one. */
    String displayName(UUID userId);
}
