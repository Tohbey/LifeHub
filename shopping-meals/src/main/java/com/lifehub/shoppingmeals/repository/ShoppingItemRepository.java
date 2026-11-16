package com.lifehub.shoppingmeals.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.lifehub.shoppingmeals.model.entity.ShoppingItem;

@Repository
public interface ShoppingItemRepository extends JpaRepository<ShoppingItem, UUID> {

    /** Pages through a list's items. */
    Page<ShoppingItem> findByShoppingListId(UUID shoppingListId, Pageable pageable);

    /** Pages through a list's items that are still to buy. */
    Page<ShoppingItem> findByShoppingListIdAndPurchasedAtIsNull(UUID shoppingListId, Pageable pageable);

    @Query("""
            select new com.lifehub.shoppingmeals.repository.OpenItemCount(i.shoppingListId, count(i))
            from ShoppingItem i where i.shoppingListId in :listIds and i.purchasedAt is null
            group by i.shoppingListId
            """)
    /** How many items are still to buy on each of these lists. */
    List<OpenItemCount> countOpen(Collection<UUID> listIds);
}
