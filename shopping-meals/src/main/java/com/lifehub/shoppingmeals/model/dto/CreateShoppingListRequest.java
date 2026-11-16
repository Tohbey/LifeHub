package com.lifehub.shoppingmeals.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateShoppingListRequest(@NotBlank @Size(max = 100) String name) {
}
