package com.lifehub.shoppingmeals.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lifehub.shoppingmeals.model.entity.ShoppingList;

@Repository
public interface ShoppingListRepository extends JpaRepository<ShoppingList, UUID> {

    /** Finds a list only if it belongs to this workspace. */
    Optional<ShoppingList> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    /** Pages through a workspace's lists in use. */
    Page<ShoppingList> findByWorkspaceIdAndArchivedAtIsNull(UUID workspaceId, Pageable pageable);

    /** Pages through all of a workspace's lists, archived ones included. */
    Page<ShoppingList> findByWorkspaceId(UUID workspaceId, Pageable pageable);
}
