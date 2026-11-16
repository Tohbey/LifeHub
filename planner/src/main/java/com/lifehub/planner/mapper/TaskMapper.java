package com.lifehub.planner.mapper;

import com.lifehub.planner.model.dto.TaskDTO;
import com.lifehub.planner.model.entity.Task;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TaskMapper {

    /** Maps a task entity to its DTO. */
    public static TaskDTO toDto(Task t) {
        return new TaskDTO(t.getId(), t.getTitle(), t.getDescription(), t.getAssigneeUserId(), t.getDueAt(),
                t.getStatus(), t.getCompletedAt(), t.getCompletedByUserId(), t.getCreatedByUserId(), t.getCreatedAt());
    }
}
