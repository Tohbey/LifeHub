package com.lifehub.finance.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;

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
    void periodsAreCalendarMonthsInTheWorkspacesTimezone() {
        ZoneId london = ZoneId.of("Europe/London");
        ZoneId lagos = ZoneId.of("Africa/Lagos");
        Instant lateOnTheLastEvening = Instant.parse("2026-11-30T23:30:00Z");

        assertThat(BudgetThresholds.period(lateOnTheLastEvening, london)).isEqualTo(YearMonth.of(2026, 11));
        // 00:30 on 1 December in Lagos already counts towards December.
        assertThat(BudgetThresholds.period(lateOnTheLastEvening, lagos)).isEqualTo(YearMonth.of(2026, 12));
        assertThat(BudgetThresholds.start(YearMonth.of(2026, 12), lagos)).isEqualTo(Instant.parse("2026-11-30T23:00:00Z"));
        // London is on summer time in July: the month starts at 23:00 UTC the day before.
        assertThat(BudgetThresholds.start(YearMonth.of(2026, 7), london)).isEqualTo(Instant.parse("2026-06-30T23:00:00Z"));
        assertThat(BudgetThresholds.end(YearMonth.of(2026, 11), london)).isEqualTo(Instant.parse("2026-12-01T00:00:00Z"));
    }

    @Test
    void moneyIsFormattedWithTwoDecimals() {
        assertThat(Money.format(new BigDecimal("120"))).isEqualTo("120.00");
        assertThat(Money.format(new BigDecimal("0.5"))).isEqualTo("0.50");
    }
}
