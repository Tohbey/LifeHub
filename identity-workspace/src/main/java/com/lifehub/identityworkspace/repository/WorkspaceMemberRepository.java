package com.lifehub.identityworkspace.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.lifehub.identityworkspace.model.entity.MemberStatus;
import com.lifehub.identityworkspace.model.entity.WorkspaceMember;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    /** Finds a user's membership row of a workspace, active or removed. */
    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    /** Pages through a workspace's members with the given status. */
    Page<WorkspaceMember> findByWorkspaceIdAndStatus(UUID workspaceId, MemberStatus status, Pageable pageable);

    /** Pages through a user's memberships with the given status. */
    Page<WorkspaceMember> findByUserIdAndStatus(UUID userId, MemberStatus status, Pageable pageable);

    /** Returns the memberships of the given users in a workspace with the given status. */
    List<WorkspaceMember> findByWorkspaceIdAndStatusAndUserIdIn(UUID workspaceId, MemberStatus status,
            Collection<UUID> userIds);

    @Query("""
            select m from WorkspaceMember m, User u
            where m.userId = u.id and m.workspaceId = :workspaceId and u.logtoSubject = :subject
              and m.status = com.lifehub.identityworkspace.model.entity.MemberStatus.ACTIVE
            """)
    /** Finds the active membership of the user with this Logto subject. */
    Optional<WorkspaceMember> findActiveBySubject(UUID workspaceId, String subject);
}
