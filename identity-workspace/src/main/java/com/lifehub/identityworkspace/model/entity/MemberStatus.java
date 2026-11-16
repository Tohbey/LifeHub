package com.lifehub.identityworkspace.model.entity;

public enum MemberStatus {
    ACTIVE,
    /** Removed or left. Kept so rejoining reuses the row and history stays intact. */
    REMOVED
}
