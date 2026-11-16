package com.lifehub.shoppingmeals.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateShoppingItemRequest(@NotBlank @Size(max = 120) String name, @Size(max = 40) String quantity,
        @Size(max = 300) String note) {
}
