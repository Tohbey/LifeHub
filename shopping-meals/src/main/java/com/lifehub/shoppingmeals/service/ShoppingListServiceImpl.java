package com.lifehub.shoppingmeals.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.shoppingmeals.mapper.ShoppingMapper;
import com.lifehub.shoppingmeals.model.dto.CreateShoppingListRequest;
import com.lifehub.shoppingmeals.model.dto.ShoppingListDTO;
import com.lifehub.shoppingmeals.model.dto.UpdateShoppingListRequest;
import com.lifehub.shoppingmeals.model.entity.ShoppingList;
import com.lifehub.shoppingmeals.repository.OpenItemCount;
import com.lifehub.shoppingmeals.repository.ShoppingItemRepository;
import com.lifehub.shoppingmeals.repository.ShoppingListRepository;
import com.lifehub.starter.id.Ids;
import com.lifehub.starter.membership.Membership;
import com.lifehub.starter.membership.WorkspaceAccess;
import com.lifehub.starter.web.ApiException;
import com.lifehub.starter.web.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class ShoppingListServiceImpl implements ShoppingListService {

    private final WorkspaceAccess access;
    private final ShoppingListRepository lists;
    private final ShoppingItemRepository items;
    private final Clock clock;

    /** Checks membership, reads one page and counts each list's open items in one query. */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ShoppingListDTO> getLists(UUID workspaceId, boolean includeArchived, Pageable pageable) {
        access.require(workspaceId);
        Page<ShoppingList> page = includeArchived
                ? lists.findByWorkspaceId(workspaceId, pageable)
                : lists.findByWorkspaceIdAndArchivedAtIsNull(workspaceId, pageable);
        Map<UUID, Long> open = countOpen(page.map(ShoppingList::getId).getContent());
        return PageResponse.of(page, l -> ShoppingMapper.toDto(l, open.getOrDefault(l.getId(), 0L)));
    }

    /** Checks membership, then loads the list from this workspace only. */
    @Override
    @Transactional(readOnly = true)
    public ShoppingListDTO getList(UUID workspaceId, UUID listId) {
        access.require(workspaceId);
        ShoppingList list = load(workspaceId, listId);
        return ShoppingMapper.toDto(list, countOpen(List.of(listId)).getOrDefault(listId, 0L));
    }

    /** Checks membership, then saves. */
    @Override
    @Transactional
    public ShoppingListDTO createList(UUID workspaceId, CreateShoppingListRequest request) {
        Membership caller = access.require(workspaceId);
        ShoppingList list = lists.save(
                new ShoppingList(Ids.newId(), workspaceId, request.name().strip(), caller.userId(), clock.instant()));
        return ShoppingMapper.toDto(list, 0);
    }

    /** Checks membership, then applies the given changes. */
    @Override
    @Transactional
    public ShoppingListDTO updateList(UUID workspaceId, UUID listId, UpdateShoppingListRequest request) {
        access.require(workspaceId);
        ShoppingList list = load(workspaceId, listId);
        Instant now = clock.instant();
        if (request.name() != null) {
            list.rename(request.name().strip(), now);
        }
        if (request.archived() != null) {
            list.archive(request.archived(), now);
        }
        return ShoppingMapper.toDto(list, countOpen(List.of(listId)).getOrDefault(listId, 0L));
    }

    /** Loads a list of this workspace, or 404. */
    private ShoppingList load(UUID workspaceId, UUID listId) {
        return lists.findByIdAndWorkspaceId(listId, workspaceId)
                .orElseThrow(() -> ApiException.notFound("Shopping list not found."));
    }

    /** Open items per list. */
    private Map<UUID, Long> countOpen(List<UUID> listIds) {
        if (listIds.isEmpty()) {
            return Map.of();
        }
        return items.countOpen(listIds).stream()
                .collect(Collectors.toMap(OpenItemCount::shoppingListId,
                        OpenItemCount::count));
    }
}
