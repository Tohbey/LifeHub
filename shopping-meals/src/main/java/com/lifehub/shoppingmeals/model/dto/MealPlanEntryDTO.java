package com.lifehub.shoppingmeals.model.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.lifehub.shoppingmeals.model.entity.MealSlot;

public record MealPlanEntryDTO(UUID id, UUID workspaceId, LocalDate date, MealSlot slot, UUID recipeId, String title,
        String note, UUID createdByUserId, Instant createdAt) {
}
