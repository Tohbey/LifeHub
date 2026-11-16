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

import com.lifehub.starter.persistence.AssignedIdEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** The unit every business record in LifeHub belongs to. */
@Entity
@Table(name = "workspaces")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Workspace extends AssignedIdEntity {

    /** Whether this is a personal or household workspace. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "type", nullable = false, updatable = false)
    private WorkspaceType type;

    /** The workspace's display name. */
    @Column(name = "name", nullable = false)
    private String name;

    /** When the workspace was created. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** When the workspace last changed. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Creates a personal or household workspace. */
    public Workspace(UUID id, WorkspaceType type, String name, Instant now) {
        super(id);
        this.type = type;
        this.name = name;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Changes the workspace's name. */
    public void rename(String name, Instant now) {
        this.name = name;
        this.updatedAt = now;
    }
}
