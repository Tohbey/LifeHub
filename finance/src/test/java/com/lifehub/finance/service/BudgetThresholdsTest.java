package com.lifehub.finance.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;

class BudgetThresholdsTest {

    @Test
    void reportsEveryThresholdReached() {
        BigDecimal limit = new BigDecimal("300.00");
        assertThat(BudgetThresholds.reached(limit, new BigDecimal("239.99"))).isEmpty();
        assertThat(BudgetThresholds.reached(limit, new BigDecimal("240.00"))).containsExactly(80);
        assertThat(BudgetThresholds.reached(limit, new BigDecimal("312.40"))).containsExactly(80, 100);
    }

    @Test
    void periodsAreCalendarMonthsInUtc() {
        YearMonth november = BudgetThresholds.period(Instant.parse("2026-11-30T23:59:59Z"));
        assertThat(november).isEqualTo(YearMonth.of(2026, 11));
        assertThat(BudgetThresholds.start(november)).isEqualTo(Instant.parse("2026-11-01T00:00:00Z"));
        assertThat(BudgetThresholds.end(november)).isEqualTo(Instant.parse("2026-12-01T00:00:00Z"));
    }

    @Test
    void moneyIsFormattedWithTwoDecimals() {
        assertThat(Money.format(new BigDecimal("120"))).isEqualTo("120.00");
        assertThat(Money.format(new BigDecimal("0.5"))).isEqualTo("0.50");
    }
}
