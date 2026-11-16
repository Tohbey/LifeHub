package com.lifehub.shoppingmeals.mapper;

import com.lifehub.shoppingmeals.model.dto.ShoppingItemDTO;
import com.lifehub.shoppingmeals.model.dto.ShoppingListDTO;
import com.lifehub.shoppingmeals.model.entity.ShoppingItem;
import com.lifehub.shoppingmeals.model.entity.ShoppingList;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShoppingMapper {

    /** Maps a list and its open-item count to a DTO. */
    public static ShoppingListDTO toDto(ShoppingList list, long openItems) {
        return new ShoppingListDTO(list.getId(), list.getWorkspaceId(), list.getName(), list.getArchivedAt() != null,
                openItems, list.getCreatedAt());
    }

    /** Maps an item to a DTO. */
    public static ShoppingItemDTO toDto(ShoppingItem i) {
        return new ShoppingItemDTO(i.getId(), i.getShoppingListId(), i.getName(), i.getQuantity(), i.getNote(),
                i.isPurchased(), i.getPurchasedAt(), i.getPurchasedByUserId(), i.getAddedByUserId(), i.getCreatedAt());
    }
}
