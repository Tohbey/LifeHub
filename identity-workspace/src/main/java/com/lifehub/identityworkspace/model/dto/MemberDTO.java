package com.lifehub.identityworkspace.model.dto;

import java.time.Instant;
import java.util.UUID;

import com.lifehub.starter.membership.WorkspaceRole;

public record MemberDTO(UUID userId, String displayName, WorkspaceRole role, Instant joinedAt) {
}
