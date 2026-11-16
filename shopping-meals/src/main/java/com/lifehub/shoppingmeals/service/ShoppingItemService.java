package com.lifehub.shoppingmeals.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.lifehub.shoppingmeals.model.dto.CreateShoppingItemRequest;
import com.lifehub.shoppingmeals.model.dto.ShoppingItemDTO;
import com.lifehub.shoppingmeals.model.dto.UpdateShoppingItemRequest;
import com.lifehub.starter.web.PageResponse;

/**
 * Items on a list. These endpoints carry a list or item ID, not a workspace ID, so each call loads the
 * resource's workspace first and checks the caller belongs to it.
 */
public interface ShoppingItemService {

    /** Pages through a list's items; {@code openOnly} hides bought ones. */
    PageResponse<ShoppingItemDTO> getItems(UUID listId, boolean openOnly, Pageable pageable);

    /** Adds an item; publishes shopping.item.added. 409 on an archived list. */
    ShoppingItemDTO addItem(UUID listId, CreateShoppingItemRequest request);

    /** Edits an item or ticks it off; ticking off publishes shopping.item.purchased. */
    ShoppingItemDTO updateItem(UUID itemId, UpdateShoppingItemRequest request);

    /** Removes an item. */
    void removeItem(UUID itemId);
}
