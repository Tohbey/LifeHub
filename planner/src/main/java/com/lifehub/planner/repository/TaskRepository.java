package com.lifehub.planner.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import com.lifehub.planner.model.entity.Task;
import com.lifehub.planner.model.entity.TaskStatus;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    /** Finds a task only if it belongs to this workspace. */
    Optional<Task> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    @Query("""
            select t from Task t
            where t.workspaceId = :workspaceId
              and (:status is null or t.status = :status)
              and (:assigneeUserId is null or t.assigneeUserId = :assigneeUserId)
            """)
    /** Pages through a workspace's tasks, optionally by status and assignee (null means any). */
    Page<Task> search(UUID workspaceId, TaskStatus status, UUID assigneeUserId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    // -2 is Hibernate's SKIP LOCKED: a second planner instance takes other rows instead of waiting.
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
            select t from Task t
            where t.status = com.lifehub.planner.model.entity.TaskStatus.OPEN
              and t.overdueNotifiedAt is null and t.dueAt <= :now
            order by t.dueAt
            limit 100
            """)
    /** Open tasks past their due time that have not been announced yet, locked for this transaction. */
    List<Task> lockNewlyOverdue(Instant now);
}
