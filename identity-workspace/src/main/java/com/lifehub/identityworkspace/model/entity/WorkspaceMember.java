package com.lifehub.identityworkspace.model.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.lifehub.starter.membership.WorkspaceRole;
import com.lifehub.starter.persistence.AssignedIdEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "workspace_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkspaceMember extends AssignedIdEntity {

    /** The workspace this membership belongs to. */
    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    /** The member's LifeHub user ID. */
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** The member's role in the workspace. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "role", nullable = false)
    private WorkspaceRole role;

    /** Whether the membership is active or removed. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false)
    private MemberStatus status;

    /** When the member joined, or last rejoined. */
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    /** When the member was removed or left, or null while active. */
    @Column(name = "removed_at")
    private Instant removedAt;

    /** Creates an active membership with the given role. */
    public WorkspaceMember(UUID id, UUID workspaceId, UUID userId, WorkspaceRole role, Instant now) {
        super(id);
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.role = role;
        this.status = MemberStatus.ACTIVE;
        this.joinedAt = now;
    }

    /** True unless the member has been removed or has left. */
    public boolean isActive() {
        return status == MemberStatus.ACTIVE;
    }

    /** Sets the member's role. */
    public void changeRole(WorkspaceRole role) {
        this.role = role;
    }

    /** Marks the member as removed. The row is kept for history and possible rejoining. */
    public void remove(Instant now) {
        this.status = MemberStatus.REMOVED;
        this.removedAt = now;
    }

    /** Reactivates a former member with a new role and join time. */
    public void rejoin(WorkspaceRole role, Instant now) {
        this.role = role;
        this.status = MemberStatus.ACTIVE;
        this.joinedAt = now;
        this.removedAt = null;
    }
}
