package com.lifehub.identityworkspace.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.lifehub.starter.membership.WorkspaceRole;

public record CreateInvitationRequest(@NotBlank @Email @Size(max = 254) String email, @NotNull WorkspaceRole role) {
}
