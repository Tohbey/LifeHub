package com.lifehub.shoppingmeals.model.dto;

import java.time.Instant;
import java.util.UUID;

public record ShoppingItemDTO(UUID id, UUID shoppingListId, String name, String quantity, String note,
        boolean purchased, Instant purchasedAt, UUID purchasedByUserId, UUID addedByUserId, Instant createdAt) {
}
