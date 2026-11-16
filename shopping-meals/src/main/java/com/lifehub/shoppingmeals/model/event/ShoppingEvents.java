package com.lifehub.shoppingmeals.model.event;

import java.util.UUID;

import com.lifehub.starter.events.EventType;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Events this service publishes. Must match deploy/docs/events.md (shopping.events). */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShoppingEvents {

    public static final String TOPIC = "shopping.events";

    public static final EventType ITEM_ADDED = new EventType(TOPIC, "shopping.item.added", 1);
    public static final EventType ITEM_PURCHASED = new EventType(TOPIC, "shopping.item.purchased", 1);

    public record ItemAdded(UUID shoppingListId, String listName, UUID itemId, String name, String quantity) {
    }

    public record ItemPurchased(UUID shoppingListId, String listName, UUID itemId, String name) {
    }
}
