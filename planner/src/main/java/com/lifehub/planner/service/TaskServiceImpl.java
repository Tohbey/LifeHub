package com.lifehub.planner.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.planner.mapper.TaskMapper;
import com.lifehub.planner.model.dto.CreateTaskRequest;
import com.lifehub.planner.model.dto.TaskDTO;
import com.lifehub.planner.model.dto.UpdateTaskRequest;
import com.lifehub.planner.model.entity.Task;
import com.lifehub.planner.model.entity.TaskStatus;
import com.lifehub.planner.model.event.PlannerEvents;
import com.lifehub.planner.repository.TaskRepository;
import com.lifehub.starter.events.DomainEventPublisher;
import com.lifehub.starter.id.Ids;
import com.lifehub.starter.membership.Membership;
import com.lifehub.starter.membership.WorkspaceAccess;
import com.lifehub.starter.web.ApiException;
import com.lifehub.starter.web.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class TaskServiceImpl implements TaskService {

    private final WorkspaceAccess access;
    private final TaskRepository tasks;
    private final DomainEventPublisher events;
    private final Clock clock;

    /** Checks membership, then reads one page. */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<TaskDTO> getTasks(UUID workspaceId, TaskStatus status, boolean mine, Pageable pageable) {
        Membership caller = access.require(workspaceId);
        return PageResponse.of(tasks.search(workspaceId, status, mine ? caller.userId() : null, pageable),
                TaskMapper::toDto);
    }

    /** Checks membership, then loads the task from this workspace only. */
    @Override
    @Transactional(readOnly = true)
    public TaskDTO getTask(UUID workspaceId, UUID taskId) {
        access.require(workspaceId);
        return TaskMapper.toDto(load(workspaceId, taskId));
    }

    /** Checks the caller and the assignee are members, saves, and publishes the events. */
    @Override
    @Transactional
    public TaskDTO createTask(UUID workspaceId, CreateTaskRequest request) {
        Membership caller = access.require(workspaceId);
        requireMember(workspaceId, request.assigneeUserId());
        Task task = tasks.save(new Task(Ids.newId(), workspaceId, request.title().strip(), blankToNull(request.description()),
                request.assigneeUserId(), request.dueAt(), caller.userId(), clock.instant()));

        events.publish(PlannerEvents.TASK_CREATED, workspaceId, caller.userId(),
                new PlannerEvents.TaskCreated(task.getId(), task.getTitle(), task.getAssigneeUserId(), task.getDueAt()));
        if (task.getAssigneeUserId() != null) {
            events.publish(PlannerEvents.TASK_ASSIGNED, workspaceId, caller.userId(), new PlannerEvents.TaskAssigned(
                    task.getId(), task.getTitle(), task.getAssigneeUserId(), null, task.getDueAt()));
        }
        return TaskMapper.toDto(task);
    }

    /** Applies the given changes; a new assignee must be a member. */
    @Override
    @Transactional
    public TaskDTO updateTask(UUID workspaceId, UUID taskId, UpdateTaskRequest request) {
        Membership caller = access.require(workspaceId);
        Task task = load(workspaceId, taskId);
        Instant now = clock.instant();

        if (request.title() != null || request.description() != null) {
            task.describe(request.title() != null ? request.title().strip() : task.getTitle(),
                    request.description() != null ? blankToNull(request.description()) : task.getDescription(), now);
        }
        if (Boolean.TRUE.equals(request.clearDueAt()) || request.dueAt() != null) {
            task.reschedule(Boolean.TRUE.equals(request.clearDueAt()) ? null : request.dueAt(), now);
        }
        UUID previousAssignee = task.getAssigneeUserId();
        UUID newAssignee = Boolean.TRUE.equals(request.unassign()) ? null
                : request.assigneeUserId() != null ? request.assigneeUserId() : previousAssignee;
        if (!Objects.equals(previousAssignee, newAssignee)) {
            requireMember(workspaceId, newAssignee);
            task.assign(newAssignee, now);
            if (newAssignee != null) {
                events.publish(PlannerEvents.TASK_ASSIGNED, workspaceId, caller.userId(), new PlannerEvents.TaskAssigned(
                        task.getId(), task.getTitle(), newAssignee, previousAssignee, task.getDueAt()));
            }
        }
        if (request.status() == TaskStatus.DONE && task.getStatus() == TaskStatus.OPEN) {
            task.complete(caller.userId(), now);
            events.publish(PlannerEvents.TASK_COMPLETED, workspaceId, caller.userId(),
                    new PlannerEvents.TaskCompleted(task.getId(), task.getTitle(), caller.userId()));
        } else if (request.status() == TaskStatus.OPEN && task.getStatus() == TaskStatus.DONE) {
            task.reopen(now);
        }
        return TaskMapper.toDto(task);
    }

    /** Loads a task of this workspace, or 404. */
    private Task load(UUID workspaceId, UUID taskId) {
        return tasks.findByIdAndWorkspaceId(taskId, workspaceId)
                .orElseThrow(() -> ApiException.notFound("Task not found."));
    }

    /** An assignee, when given, must be an active member. */
    private void requireMember(UUID workspaceId, UUID userId) {
        if (userId != null) {
            access.requireAllMembers(workspaceId, List.of(userId));
        }
    }

    /** Null for a blank string, otherwise the stripped string. */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
