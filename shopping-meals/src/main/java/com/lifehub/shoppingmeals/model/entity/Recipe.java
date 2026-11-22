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

/** A recipe a workspace keeps. Its ingredients are {@link RecipeIngredient} rows, replaced as a whole on edit. */
@Entity
@Table(name = "recipes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recipe extends AssignedIdEntity {

    /** The workspace it belongs to. */
    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    /** The recipe's name, for example "Jollof rice". */
    @Column(name = "name", nullable = false)
    private String name;

    /** How many people it serves, if known. */
    @Column(name = "servings")
    private Integer servings;

    /** The method, as plain text. */
    @Column(name = "instructions")
    private String instructions;

    /** Where it came from, an http(s) link. */
    @Column(name = "source_url")
    private String sourceUrl;

    /** The member who added it. */
    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private UUID createdByUserId;

    /** When it was added. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** When it last changed. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Creates a recipe. */
    public Recipe(UUID id, UUID workspaceId, String name, Integer servings, String instructions, String sourceUrl,
            UUID createdByUserId, Instant now) {
        super(id);
        this.workspaceId = workspaceId;
        this.createdByUserId = createdByUserId;
        this.createdAt = now;
        edit(name, servings, instructions, sourceUrl, now);
    }

    /** Replaces the recipe's details. */
    public void edit(String name, Integer servings, String instructions, String sourceUrl, Instant now) {
        this.name = name;
        this.servings = servings;
        this.instructions = instructions;
        this.sourceUrl = sourceUrl;
        this.updatedAt = now;
    }
}
