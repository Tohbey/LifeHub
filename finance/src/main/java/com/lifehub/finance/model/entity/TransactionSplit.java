package com.lifehub.finance.model.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.lifehub.starter.persistence.AssignedIdEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One member's share of a shared expense. */
@Entity
@Table(name = "transaction_splits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransactionSplit extends AssignedIdEntity {

    /** The expense this share belongs to. */
    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;

    /** The member who owes this share. */
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** The share, with two decimals. */
    @Column(name = "amount", nullable = false, updatable = false)
    private BigDecimal amount;

    /** Creates a share. */
    public TransactionSplit(UUID id, UUID transactionId, UUID userId, BigDecimal amount) {
        super(id);
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
    }
}
