package com.lifehub.shoppingmeals.model.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import com.lifehub.starter.persistence.AssignedIdEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One meal on the plan: a day, a slot and either a recipe or a free-text title ("Takeaway"). The title is copied
 * from the recipe, so the plan still reads correctly after the recipe is renamed or deleted.
 */
@Entity
@Table(name = "meal_plan_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealPlanEntry extends AssignedIdEntity {

    /** The workspace it belongs to. */
    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    /** The day, in the household's calendar. */
    @Column(name = "plan_date", nullable = false, updatable = false)
    private LocalDate planDate;

    /** Breakfast, lunch, dinner or snack. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "slot", nullable = false, updatable = false)
    private MealSlot slot;

    /** The recipe, or null for a free-text meal or a deleted recipe. */
    @Column(name = "recipe_id", updatable = false)
    private UUID recipeId;

    /** What is being eaten. */
    @Column(name = "title", nullable = false, updatable = false)
    private String title;

    /** An optional note, such as "Bob cooks". */
    @Column(name = "note", updatable = false)
    private String note;

    /** The member who planned it. */
    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private UUID createdByUserId;

    /** When it was planned. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Plans a meal. */
    public MealPlanEntry(UUID id, UUID workspaceId, LocalDate planDate, MealSlot slot, UUID recipeId, String title,
            String note, UUID createdByUserId, Instant now) {
        super(id);
        this.workspaceId = workspaceId;
        this.planDate = planDate;
        this.slot = slot;
        this.recipeId = recipeId;
        this.title = title;
        this.note = note;
        this.createdByUserId = createdByUserId;
        this.createdAt = now;
    }
}
