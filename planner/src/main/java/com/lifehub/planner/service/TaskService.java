package com.lifehub.planner.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.lifehub.planner.model.dto.CreateTaskRequest;
import com.lifehub.planner.model.dto.TaskDTO;
import com.lifehub.planner.model.dto.UpdateTaskRequest;
import com.lifehub.planner.model.entity.TaskStatus;
import com.lifehub.starter.web.PageResponse;

/** A workspace's tasks. Every member can read, create, assign and complete them: it is a shared to-do list. */
public interface TaskService {

    /** Pages through tasks, optionally by status; {@code mine} limits it to tasks assigned to the caller. */
    PageResponse<TaskDTO> getTasks(UUID workspaceId, TaskStatus status, boolean mine, Pageable pageable);

    /** Returns one task. */
    TaskDTO getTask(UUID workspaceId, UUID taskId);

    /** Creates a task; publishes task.created, and task.assigned when it has an assignee. */
    TaskDTO createTask(UUID workspaceId, CreateTaskRequest request);

    /** Changes a task; publishes task.assigned and task.completed when those happen. */
    TaskDTO updateTask(UUID workspaceId, UUID taskId, UpdateTaskRequest request);
}
