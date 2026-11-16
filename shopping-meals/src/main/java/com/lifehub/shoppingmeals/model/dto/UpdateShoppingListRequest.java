package com.lifehub.shoppingmeals.model.dto;

import jakarta.validation.constraints.Size;

/** Fields left out are unchanged. */
public record UpdateShoppingListRequest(@Size(min = 1, max = 100) String name, Boolean archived) {
}
