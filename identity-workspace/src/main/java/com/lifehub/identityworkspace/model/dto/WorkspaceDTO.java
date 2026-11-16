package com.lifehub.identityworkspace.model.dto;

import java.util.UUID;

import com.lifehub.identityworkspace.model.entity.WorkspaceType;
import com.lifehub.starter.membership.WorkspaceRole;

/** A workspace the current user belongs to, with their role in it. */
public record WorkspaceDTO(UUID id, WorkspaceType type, String name, WorkspaceRole role) {
}
