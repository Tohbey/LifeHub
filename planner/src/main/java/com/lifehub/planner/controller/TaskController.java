package com.lifehub.planner.controller;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.planner.model.dto.CreateTaskRequest;
import com.lifehub.planner.model.dto.TaskDTO;
import com.lifehub.planner.model.dto.UpdateTaskRequest;
import com.lifehub.planner.model.entity.TaskStatus;
import com.lifehub.planner.service.TaskService;
import com.lifehub.starter.web.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Tasks", description = "A workspace's shared to-do list.")
@RequestMapping("/api/v1/workspaces/{workspaceId}/tasks")
@RequiredArgsConstructor
class TaskController {

    private final TaskService taskService;

    /** GET /api/v1/workspaces/{id}/tasks: by due time; {@code ?status=OPEN}, {@code ?mine=true}. */
    @Operation(summary = "List tasks",
            description = "By due time. Filter by status, and use mine=true for tasks assigned to you.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "One page of tasks"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Workspace not found, or you are not a member of it",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    PageResponse<TaskDTO> getTasks(@PathVariable UUID workspaceId, @RequestParam(required = false) TaskStatus status,
            @RequestParam(defaultValue = "false") boolean mine,
            @PageableDefault(sort = "dueAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return taskService.getTasks(workspaceId, status, mine, pageable);
    }

    /** POST /api/v1/workspaces/{id}/tasks: creates a task. Returns 201. */
    @Operation(summary = "Create a task",
            description = "The assignee, if given, must be a member. Notifies the assignee.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Task created",
                    content = @Content(schema = @Schema(implementation = TaskDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request, or the assignee is not a member",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Workspace not found, or you are not a member of it",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    ResponseEntity<TaskDTO> createTask(@PathVariable UUID workspaceId, @Valid @RequestBody CreateTaskRequest request) {
        TaskDTO task = taskService.createTask(workspaceId, request);
        return ResponseEntity.created(URI.create("/api/v1/workspaces/" + workspaceId + "/tasks/" + task.id()))
                .body(task);
    }

    /** GET /api/v1/workspaces/{id}/tasks/{taskId}: one task. */
    @Operation(summary = "Get a task", description = "One task.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The task",
                    content = @Content(schema = @Schema(implementation = TaskDTO.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Task or workspace not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{taskId}")
    TaskDTO getTask(@PathVariable UUID workspaceId, @PathVariable UUID taskId) {
        return taskService.getTask(workspaceId, taskId);
    }

    /** PATCH /api/v1/workspaces/{id}/tasks/{taskId}: edits, assigns, completes or reopens a task. */
    @Operation(summary = "Update a task",
            description = "Edits, assigns, completes (status DONE) or reopens (status OPEN) a task. Any member can do "
                    + "this.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task updated",
                    content = @Content(schema = @Schema(implementation = TaskDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request, or the assignee is not a member",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Task or workspace not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{taskId}")
    TaskDTO updateTask(@PathVariable UUID workspaceId, @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.updateTask(workspaceId, taskId, request);
    }

    /** DELETE /api/v1/workspaces/{id}/tasks/{taskId}: deletes a task. Returns 204. */
    @Operation(summary = "Delete a task", description = "Only the person who created it, or an owner or admin.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Task deleted", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "You did not create this task and are not an admin",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Task or workspace not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{taskId}")
    ResponseEntity<Void> deleteTask(@PathVariable UUID workspaceId, @PathVariable UUID taskId) {
        taskService.deleteTask(workspaceId, taskId);
        return ResponseEntity.noContent().build();
    }
}
