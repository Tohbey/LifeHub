package com.lifehub.shoppingmeals.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.lifehub.shoppingmeals.model.dto.CreateMealPlanEntryRequest;
import com.lifehub.shoppingmeals.model.dto.MealPlanEntryDTO;

/** A workspace's meal plan: any member plans or removes a meal. */
public interface MealPlanService {

    /** The longest range one request may read, so a request can never read years of plan. */
    int MAX_DAYS = 62;

    /** The plan from {@code from} to {@code to}, both included, by day and then breakfast to snack. */
    List<MealPlanEntryDTO> getPlan(UUID workspaceId, LocalDate from, LocalDate to);

    /** Plans a meal. */
    MealPlanEntryDTO addEntry(UUID workspaceId, CreateMealPlanEntryRequest request);

    /** Removes a planned meal. */
    void deleteEntry(UUID workspaceId, UUID entryId);
}
