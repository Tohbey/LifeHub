package com.lifehub.finance.model.entity;

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

/** A spending or income category of one workspace, for example "Utilities". */
@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends AssignedIdEntity {

    /** The workspace the category belongs to. */
    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    /** Whether it is for income or expenses. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "type", nullable = false, updatable = false)
    private EntryType type;

    /** The category's name, unique per workspace and type. */
    @Column(name = "name", nullable = false)
    private String name;

    /** When it was created. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Renames the category. */
    public void rename(String name) {
        this.name = name;
    }

    /** Creates a category. */
    public Category(UUID id, UUID workspaceId, EntryType type, String name, Instant now) {
        super(id);
        this.workspaceId = workspaceId;
        this.type = type;
        this.name = name;
        this.createdAt = now;
    }
}
