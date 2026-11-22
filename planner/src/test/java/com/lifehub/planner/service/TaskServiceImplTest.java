package com.lifehub.planner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.lifehub.planner.model.dto.CreateTaskRequest;
import com.lifehub.planner.model.dto.UpdateTaskRequest;
import com.lifehub.planner.model.entity.Task;
import com.lifehub.planner.repository.TaskRepository;
import com.lifehub.starter.events.DomainEventPublisher;
import com.lifehub.starter.membership.Membership;
import com.lifehub.starter.membership.WorkspaceAccess;
import com.lifehub.starter.membership.WorkspaceRole;
import com.lifehub.starter.web.ApiException;

class TaskServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-18T10:00:00Z");

    private final WorkspaceAccess access = mock(WorkspaceAccess.class);
    private final TaskRepository tasks = mock(TaskRepository.class);
    private final TaskServiceImpl service = new TaskServiceImpl(access, tasks, mock(DomainEventPublisher.class),
            Clock.fixed(NOW, ZoneOffset.UTC));
    private final UUID workspace = UUID.randomUUID();
    private final UUID me = UUID.randomUUID();

    @BeforeEach
    void member() {
        when(access.require(workspace)).thenReturn(new Membership(workspace, me, WorkspaceRole.MEMBER));
        when(tasks.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void aNewTaskCannotBeDueInThePast() {
        assertThatThrownBy(() -> service.createTask(workspace, new CreateTaskRequest("Bins", null, null, NOW.minusSeconds(3600))))
                .isInstanceOf(ApiException.class)
                .hasMessage("The due date cannot be in the past.");
        verify(tasks, never()).save(any());
    }

    @Test
    void dueNowOrLaterIsFineAndAMinuteOfGraceIsAllowed() {
        assertThat(service.createTask(workspace, new CreateTaskRequest("Bins", null, null, NOW.plusSeconds(3600))).dueAt())
                .isEqualTo(NOW.plusSeconds(3600));
        assertThat(service.createTask(workspace, new CreateTaskRequest("Bins", null, null, NOW.minusSeconds(30))).dueAt())
                .isEqualTo(NOW.minusSeconds(30));
        assertThat(service.createTask(workspace, new CreateTaskRequest("Bins", null, null, null)).dueAt()).isNull();
    }

    @Test
    void anOverdueTaskCanStillBeEditedButNotMovedIntoThePast() {
        Task overdue = new Task(UUID.randomUUID(), workspace, "Bins", null, null, NOW.minusSeconds(86400), me, NOW.minusSeconds(90000));
        when(tasks.findByIdAndWorkspaceId(overdue.getId(), workspace)).thenReturn(Optional.of(overdue));

        assertThat(service.updateTask(workspace, overdue.getId(), new UpdateTaskRequest("Bins out", null, null, null,
                overdue.getDueAt(), null, null)).title()).isEqualTo("Bins out");
        assertThatThrownBy(() -> service.updateTask(workspace, overdue.getId(), new UpdateTaskRequest(null, null, null, null,
                NOW.minusSeconds(7200), null, null))).hasMessage("The due date cannot be in the past.");
    }
}
