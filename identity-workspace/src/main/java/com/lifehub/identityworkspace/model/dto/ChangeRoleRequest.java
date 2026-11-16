package com.lifehub.identityworkspace.model.dto;

import jakarta.validation.constraints.NotNull;

import com.lifehub.starter.membership.WorkspaceRole;

/** Setting {@code OWNER} hands over ownership; the current owner becomes an admin. */
public record ChangeRoleRequest(@NotNull WorkspaceRole role) {
}
