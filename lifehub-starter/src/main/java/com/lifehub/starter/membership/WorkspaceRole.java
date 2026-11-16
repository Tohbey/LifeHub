package com.lifehub.starter.membership;

/** A member's role in a workspace, lowest to highest. Personal workspaces have a single OWNER. */
public enum WorkspaceRole {
    MEMBER,
    ADMIN,
    OWNER;

    /** True when this role is the required role or higher. */
    public boolean isAtLeast(WorkspaceRole required) {
        return compareTo(required) >= 0;
    }
}
