package com.lifehub.planner.model.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code assigneeUserId} must be a member of the workspace. */
public record CreateTaskRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        UUID assigneeUserId,
        Instant dueAt) {
}
