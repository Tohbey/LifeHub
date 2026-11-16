package com.lifehub.shoppingmeals.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.lifehub.shoppingmeals.model.dto.CreateShoppingListRequest;
import com.lifehub.shoppingmeals.model.dto.ShoppingListDTO;
import com.lifehub.shoppingmeals.model.dto.UpdateShoppingListRequest;
import com.lifehub.starter.web.PageResponse;

/** A workspace's shopping lists. Every member can read, create, rename and archive them. */
public interface ShoppingListService {

    /** Pages through lists, archived ones only when asked. */
    PageResponse<ShoppingListDTO> getLists(UUID workspaceId, boolean includeArchived, Pageable pageable);

    /** Returns one list. */
    ShoppingListDTO getList(UUID workspaceId, UUID listId);

    /** Creates a list. */
    ShoppingListDTO createList(UUID workspaceId, CreateShoppingListRequest request);

    /** Renames, archives or restores a list. */
    ShoppingListDTO updateList(UUID workspaceId, UUID listId, UpdateShoppingListRequest request);
}
