package com.lifehub.shoppingmeals.model.dto;

import java.time.Instant;
import java.util.UUID;

/** A list with how many items are still to buy. */
public record ShoppingListDTO(UUID id, UUID workspaceId, String name, boolean archived, long openItems,
        Instant createdAt) {
}
