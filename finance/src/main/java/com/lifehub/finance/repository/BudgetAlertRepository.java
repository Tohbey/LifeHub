package com.lifehub.finance.repository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.lifehub.starter.id.Ids;
import com.lifehub.starter.persistence.Dates;

import lombok.RequiredArgsConstructor;

/** Which budget thresholds have already been announced, so each is announced once per period. */
@Repository
@RequiredArgsConstructor
public class BudgetAlertRepository {

    private final JdbcClient jdbc;
    private final Clock clock;

    /** Records the alert; returns true only the first time, even when two requests race. */
    public boolean recordOnce(UUID budgetId, LocalDate periodStart, int thresholdPercent) {
        return jdbc.sql("""
                INSERT IGNORE INTO budget_alerts (budget_id, period_start, threshold_percent, created_at)
                VALUES (:budget, :period, :threshold, :now)
                """)
                .param("budget", Ids.toBytes(budgetId))
                .param("period", periodStart)
                .param("threshold", thresholdPercent)
                .param("now", Dates.utc(clock.instant()))
                .update() == 1;
    }
}
