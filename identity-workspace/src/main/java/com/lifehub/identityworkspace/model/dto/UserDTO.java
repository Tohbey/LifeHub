package com.lifehub.identityworkspace.model.dto;

import java.time.Instant;
import java.util.UUID;

/** {@code emailVerified} is true once the user has opened the link sent to {@code email}. */
public record UserDTO(UUID id, String displayName, String email, boolean emailVerified, Instant createdAt) {
}
