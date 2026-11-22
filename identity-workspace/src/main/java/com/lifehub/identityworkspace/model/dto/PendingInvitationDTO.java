package com.lifehub.identityworkspace.model.dto;

import java.time.Instant;
import java.util.UUID;

import com.lifehub.starter.membership.WorkspaceRole;

/** An invitation still waiting to be accepted. The token is never shown again after creation. */
public record PendingInvitationDTO(UUID id, String email, WorkspaceRole role, UUID invitedByUserId, Instant createdAt,
        Instant expiresAt) {
}
