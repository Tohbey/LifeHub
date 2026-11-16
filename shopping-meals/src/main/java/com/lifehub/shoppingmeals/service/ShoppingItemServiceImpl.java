package com.lifehub.shoppingmeals.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.shoppingmeals.mapper.ShoppingMapper;
import com.lifehub.shoppingmeals.model.dto.CreateShoppingItemRequest;
import com.lifehub.shoppingmeals.model.dto.ShoppingItemDTO;
import com.lifehub.shoppingmeals.model.dto.UpdateShoppingItemRequest;
import com.lifehub.shoppingmeals.model.entity.ShoppingItem;
import com.lifehub.shoppingmeals.model.entity.ShoppingList;
import com.lifehub.shoppingmeals.model.event.ShoppingEvents;
import com.lifehub.shoppingmeals.repository.ShoppingItemRepository;
import com.lifehub.shoppingmeals.repository.ShoppingListRepository;
import com.lifehub.starter.events.DomainEventPublisher;
import com.lifehub.starter.id.Ids;
import com.lifehub.starter.membership.Membership;
import com.lifehub.starter.web.ApiException;
import com.lifehub.starter.web.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class ShoppingItemServiceImpl implements ShoppingItemService {

    static final String LIST_NOT_FOUND = "Shopping list not found.";
    static final String ITEM_NOT_FOUND = "Shopping item not found.";

    private final ResourceAccess access;
    private final ShoppingListRepository lists;
    private final ShoppingItemRepository items;
    private final DomainEventPublisher events;
    private final Clock clock;

    /** Loads the list, checks membership of its workspace, then reads one page. */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ShoppingItemDTO> getItems(UUID listId, boolean openOnly, Pageable pageable) {
        ShoppingList list = loadList(listId);
        access.require(list.getWorkspaceId(), LIST_NOT_FOUND);
        var page = openOnly ? items.findByShoppingListIdAndPurchasedAtIsNull(listId, pageable)
                : items.findByShoppingListId(listId, pageable);
        return PageResponse.of(page, ShoppingMapper::toDto);
    }

    /** Loads the list, checks membership, saves the item and publishes shopping.item.added. */
    @Override
    @Transactional
    public ShoppingItemDTO addItem(UUID listId, CreateShoppingItemRequest request) {
        ShoppingList list = loadList(listId);
        Membership caller = access.require(list.getWorkspaceId(), LIST_NOT_FOUND);
        if (list.getArchivedAt() != null) {
            throw ApiException.conflict("This list is archived. Restore it to add items.");
        }
        ShoppingItem item = items.save(new ShoppingItem(Ids.newId(), list, request.name().strip(),
                blankToNull(request.quantity()), blankToNull(request.note()), caller.userId(), clock.instant()));
        events.publish(ShoppingEvents.ITEM_ADDED, list.getWorkspaceId(), caller.userId(), new ShoppingEvents.ItemAdded(
                list.getId(), list.getName(), item.getId(), item.getName(), item.getQuantity()));
        return ShoppingMapper.toDto(item);
    }

    /** Loads the item, checks membership, applies the changes; ticking it off publishes shopping.item.purchased. */
    @Override
    @Transactional
    public ShoppingItemDTO updateItem(UUID itemId, UpdateShoppingItemRequest request) {
        ShoppingItem item = loadItem(itemId);
        Membership caller = access.require(item.getWorkspaceId(), ITEM_NOT_FOUND);
        Instant now = clock.instant();
        if (request.name() != null || request.quantity() != null || request.note() != null) {
            item.edit(request.name() != null ? request.name().strip() : item.getName(),
                    request.quantity() != null ? blankToNull(request.quantity()) : item.getQuantity(),
                    request.note() != null ? blankToNull(request.note()) : item.getNote(), now);
        }
        if (Boolean.TRUE.equals(request.purchased()) && !item.isPurchased()) {
            item.purchase(caller.userId(), now);
            ShoppingList list = loadList(item.getShoppingListId());
            events.publish(ShoppingEvents.ITEM_PURCHASED, item.getWorkspaceId(), caller.userId(),
                    new ShoppingEvents.ItemPurchased(list.getId(), list.getName(), item.getId(), item.getName()));
        } else if (Boolean.FALSE.equals(request.purchased()) && item.isPurchased()) {
            item.unpurchase(now);
        }
        return ShoppingMapper.toDto(item);
    }

    /** Loads the item, checks membership and deletes it. */
    @Override
    @Transactional
    public void removeItem(UUID itemId) {
        ShoppingItem item = loadItem(itemId);
        access.require(item.getWorkspaceId(), ITEM_NOT_FOUND);
        items.delete(item);
    }

    /** Loads a list by ID, or 404. */
    private ShoppingList loadList(UUID listId) {
        return lists.findById(listId).orElseThrow(() -> ApiException.notFound(LIST_NOT_FOUND));
    }

    /** Loads an item by ID, or 404. */
    private ShoppingItem loadItem(UUID itemId) {
        return items.findById(itemId).orElseThrow(() -> ApiException.notFound(ITEM_NOT_FOUND));
    }

    /** Null for a blank string, otherwise the stripped string. */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
