package com.lifehub.identityworkspace.model.dto;

import java.time.Instant;
import java.util.UUID;

import com.lifehub.starter.membership.WorkspaceRole;

public record HouseholdDTO(UUID id, String name, String defaultCurrency, String timezone, Instant createdAt,
        WorkspaceRole yourRole) {
}
