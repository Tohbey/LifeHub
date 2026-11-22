package com.lifehub.finance.model.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.lifehub.starter.persistence.AssignedIdEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A monthly spending limit for one expense category, in one currency. Months are calendar months in UTC. */
@Entity
@Table(name = "budgets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Budget extends AssignedIdEntity {

    /** The workspace it belongs to. */
    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    /** The expense category it limits. */
    @Column(name = "category_id", nullable = false, updatable = false)
    private UUID categoryId;

    /** The monthly limit. */
    @Column(name = "amount_limit", nullable = false)
    private BigDecimal amountLimit;

    /** ISO 4217 currency of the limit; only expenses in this currency count. */
    @Column(name = "currency", nullable = false, updatable = false)
    private String currency;

    /** When it was created. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** When it last changed. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Changes the monthly limit. */
    public void changeLimit(BigDecimal amountLimit, Instant now) {
        this.amountLimit = amountLimit;
        this.updatedAt = now;
    }

    /** Creates a budget. */
    public Budget(UUID id, UUID workspaceId, UUID categoryId, BigDecimal amountLimit, String currency, Instant now) {
        super(id);
        this.workspaceId = workspaceId;
        this.categoryId = categoryId;
        this.amountLimit = amountLimit;
        this.currency = currency;
        this.createdAt = now;
        this.updatedAt = now;
    }
}
