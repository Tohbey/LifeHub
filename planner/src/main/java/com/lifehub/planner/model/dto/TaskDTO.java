package com.lifehub.planner.model.dto;

import java.time.Instant;
import java.util.UUID;

import com.lifehub.planner.model.entity.TaskStatus;

public record TaskDTO(UUID id, String title, String description, UUID assigneeUserId, Instant dueAt,
        TaskStatus status, Instant completedAt, UUID completedByUserId, UUID createdByUserId, Instant createdAt) {
}
