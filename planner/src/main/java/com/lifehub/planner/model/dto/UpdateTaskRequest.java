package com.lifehub.planner.model.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Size;

import com.lifehub.planner.model.entity.TaskStatus;

/**
 * Fields left out are unchanged. {@code unassign} and {@code clearDueAt} remove the assignee and due time.
 * Setting {@code status} to DONE completes the task; OPEN reopens it.
 */
public record UpdateTaskRequest(
        @Size(min = 1, max = 200) String title,
        @Size(max = 2000) String description,
        UUID assigneeUserId,
        Boolean unassign,
        Instant dueAt,
        Boolean clearDueAt,
        TaskStatus status) {
}
