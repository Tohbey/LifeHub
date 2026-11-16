package com.lifehub.shoppingmeals.model.dto;

import jakarta.validation.constraints.Size;

/** Fields left out are unchanged. {@code purchased: true} ticks the item off; {@code false} puts it back. */
public record UpdateShoppingItemRequest(@Size(min = 1, max = 120) String name, @Size(max = 40) String quantity,
        @Size(max = 300) String note, Boolean purchased) {
}
