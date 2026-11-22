package com.lifehub.shoppingmeals.model.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.lifehub.shoppingmeals.model.entity.MealSlot;

/** Plans a meal: a recipe of this workspace, or a free-text {@code title} when {@code recipeId} is null. */
public record CreateMealPlanEntryRequest(@NotNull LocalDate date, @NotNull MealSlot slot, UUID recipeId,
        @Size(max = 120) String title, @Size(max = 300) String note) {
}
