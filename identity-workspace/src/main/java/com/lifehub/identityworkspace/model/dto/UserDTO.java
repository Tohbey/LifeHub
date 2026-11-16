package com.lifehub.identityworkspace.model.dto;

import java.time.Instant;
import java.util.UUID;

public record UserDTO(UUID id, String displayName, String email, Instant createdAt) {
}
