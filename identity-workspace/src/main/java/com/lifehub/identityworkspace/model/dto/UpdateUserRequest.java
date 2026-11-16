package com.lifehub.identityworkspace.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** Fields left out are unchanged; an empty string clears a field. */
public record UpdateUserRequest(@Size(max = 100) String displayName, @Email @Size(max = 254) String email) {
}
