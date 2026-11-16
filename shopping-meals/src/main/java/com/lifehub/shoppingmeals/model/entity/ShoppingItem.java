package com.lifehub.shoppingmeals.model.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.lifehub.starter.persistence.AssignedIdEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One thing to buy on a list. Ticking it off records who bought it and when. */
@Entity
@Table(name = "shopping_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShoppingItem extends AssignedIdEntity {

    /** The list it is on. */
    @Column(name = "shopping_list_id", nullable = false, updatable = false)
    private UUID shoppingListId;

    /** The list's workspace, copied so the membership check needs no join. */
    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    /** What to buy. */
    @Column(name = "name", nullable = false)
    private String name;

    /** How much, as free text, or null. */
    @Column(name = "quantity")
    private String quantity;

    /** A note, for example a brand, or null. */
    @Column(name = "note")
    private String note;

    /** When it was bought, or null while still needed. */
    @Column(name = "purchased_at")
    private Instant purchasedAt;

    /** Who bought it, or null. */
    @Column(name = "purchased_by_user_id")
    private UUID purchasedByUserId;

    /** Who added it. */
    @Column(name = "added_by_user_id", nullable = false, updatable = false)
    private UUID addedByUserId;

    /** When it was added. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** When it last changed. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Adds an item to a list. */
    public ShoppingItem(UUID id, ShoppingList list, String name, String quantity, String note, UUID addedByUserId,
            Instant now) {
        super(id);
        this.shoppingListId = list.getId();
        this.workspaceId = list.getWorkspaceId();
        this.name = name;
        this.quantity = quantity;
        this.note = note;
        this.addedByUserId = addedByUserId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Changes the text fields. */
    public void edit(String name, String quantity, String note, Instant now) {
        this.name = name;
        this.quantity = quantity;
        this.note = note;
        this.updatedAt = now;
    }

    /** Ticks the item off as bought by this member. */
    public void purchase(UUID userId, Instant now) {
        this.purchasedAt = now;
        this.purchasedByUserId = userId;
        this.updatedAt = now;
    }

    /** Puts a bought item back on the list. */
    public void unpurchase(Instant now) {
        this.purchasedAt = null;
        this.purchasedByUserId = null;
        this.updatedAt = now;
    }

    /** True once bought. */
    public boolean isPurchased() {
        return purchasedAt != null;
    }
}
