package com.lifehub.shoppingmeals.repository;

import java.util.UUID;

/**
 * One row of the open-items count: a list and how many items are still to buy. A top-level record, because
 * Hibernate's {@code select new} cannot name a record nested in another type.
 */
public record OpenItemCount(UUID shoppingListId, long count) {
}
