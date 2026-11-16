package com.lifehub.finance.model.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.lifehub.starter.persistence.AssignedIdEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One income or expense. Shared expenses have splits that add up to the amount. */
@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction extends AssignedIdEntity {

    /** The workspace it belongs to. Personal and household money are never combined. */
    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    /** Income or expense. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "type", nullable = false, updatable = false)
    private EntryType type;

    /** What it was, for example "Electricity bill". */
    @Column(name = "description", nullable = false)
    private String description;

    /** The amount, always positive, with two decimals. */
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    /** ISO 4217 currency code. */
    @Column(name = "currency", nullable = false)
    private String currency;

    /** The category, or null when uncategorised. */
    @Column(name = "category_id")
    private UUID categoryId;

    /** The member who paid (expense) or received (income) the money. */
    @Column(name = "payer_user_id", nullable = false)
    private UUID payerUserId;

    /** When the money moved. */
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /** The member who recorded it. */
    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private UUID createdByUserId;

    /** When it was recorded. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** When it last changed. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** When it was deleted, or null. Deleted transactions are kept for history but never shown or counted. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** Records a transaction. */
    public Transaction(UUID id, UUID workspaceId, EntryType type, String description, BigDecimal amount, String currency,
            UUID categoryId, UUID payerUserId, Instant occurredAt, UUID createdByUserId, Instant now) {
        super(id);
        this.workspaceId = workspaceId;
        this.type = type;
        this.description = description;
        this.amount = amount;
        this.currency = currency;
        this.categoryId = categoryId;
        this.payerUserId = payerUserId;
        this.occurredAt = occurredAt;
        this.createdByUserId = createdByUserId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Replaces the editable fields. */
    public void update(String description, BigDecimal amount, String currency, UUID categoryId, UUID payerUserId,
            Instant occurredAt, Instant now) {
        this.description = description;
        this.amount = amount;
        this.currency = currency;
        this.categoryId = categoryId;
        this.payerUserId = payerUserId;
        this.occurredAt = occurredAt;
        this.updatedAt = now;
    }

    /** Soft deletes the transaction. */
    public void delete(Instant now) {
        this.deletedAt = now;
        this.updatedAt = now;
    }

    /** True once deleted. */
    public boolean isDeleted() {
        return deletedAt != null;
    }
}
