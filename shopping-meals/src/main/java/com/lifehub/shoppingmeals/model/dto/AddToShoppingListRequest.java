package com.lifehub.shoppingmeals.model.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** The list of this workspace that a recipe's ingredients are added to. */
public record AddToShoppingListRequest(@NotNull UUID shoppingListId) {
}
