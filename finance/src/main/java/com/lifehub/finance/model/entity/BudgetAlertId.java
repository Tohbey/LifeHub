package com.lifehub.finance.model.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** The key of a budget alert: one per budget, period and threshold. */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BudgetAlertId implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The budget whose threshold was crossed. */
    @Column(name = "budget_id", nullable = false, updatable = false)
    private UUID budgetId;

    /** The first day of the budget month (in the workspace's timezone). */
    @Column(name = "period_start", nullable = false, updatable = false)
    private LocalDate periodStart;

    /** The threshold crossed, for example 80 or 100. */
    @Column(name = "threshold_percent", nullable = false, updatable = false)
    private int thresholdPercent;
}
