package com.lifehub.finance.model.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.springframework.data.domain.Persistable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Records that a budget threshold was announced for a month, so it is announced once. Implements
 * {@link Persistable} so a new alert is inserted directly instead of being looked up first.
 */
@Entity
@Table(name = "budget_alerts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BudgetAlert implements Persistable<BudgetAlertId> {

    /** Budget, period and threshold. */
    @EmbeddedId
    private BudgetAlertId id;

    /** When the alert was raised. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Transient
    private boolean isNew = true;

    /** Creates a new alert. */
    public BudgetAlert(BudgetAlertId id, Instant now) {
        this.id = id;
        this.createdAt = now;
    }

    /** True until saved or loaded, so Spring Data inserts instead of merging. */
    @Override
    public boolean isNew() {
        return isNew;
    }

    /** Called by JPA after loading or saving the alert. */
    @PostLoad
    @PostPersist
    void markNotNew() {
        isNew = false;
    }
}
