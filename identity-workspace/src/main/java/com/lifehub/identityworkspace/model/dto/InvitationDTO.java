package com.lifehub.identityworkspace.model.dto;

import java.time.Instant;
import java.util.UUID;

import com.lifehub.starter.membership.WorkspaceRole;

/** {@code token} is returned once, on creation. The PWA turns it into the invite link {@code /invitations/<token>}. */
public record InvitationDTO(UUID id, String email, WorkspaceRole role, Instant expiresAt, String token) {
}
