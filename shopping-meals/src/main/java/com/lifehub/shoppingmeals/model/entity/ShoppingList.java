package com.lifehub.shoppingmeals.model.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.lifehub.starter.persistence.AssignedIdEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A named shopping list in a workspace, for example "Weekly shop". */
@Entity
@Table(name = "shopping_lists")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShoppingList extends AssignedIdEntity {

    /** The workspace it belongs to. */
    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    /** The list's name. */
    @Column(name = "name", nullable = false)
    private String name;

    /** When it was archived, or null while in use. */
    @Column(name = "archived_at")
    private Instant archivedAt;

    /** The member who created it. */
    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private UUID createdByUserId;

    /** When it was created. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** When it last changed. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Creates a list. */
    public ShoppingList(UUID id, UUID workspaceId, String name, UUID createdByUserId, Instant now) {
        super(id);
        this.workspaceId = workspaceId;
        this.name = name;
        this.createdByUserId = createdByUserId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Renames the list. */
    public void rename(String name, Instant now) {
        this.name = name;
        this.updatedAt = now;
    }

    /** Archives or restores the list. */
    public void archive(boolean archived, Instant now) {
        this.archivedAt = archived ? (archivedAt == null ? now : archivedAt) : null;
        this.updatedAt = now;
    }
}
