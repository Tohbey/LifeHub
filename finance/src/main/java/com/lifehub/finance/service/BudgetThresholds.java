package com.lifehub.finance.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Budget periods (calendar months in UTC) and the thresholds that trigger an alert. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BudgetThresholds {

    /** Alerts are raised when spending reaches these percentages of the limit. */
    public static final List<Integer> PERCENTS = List.of(80, 100);

    /** The thresholds that {@code spent} has reached for this limit, lowest first. */
    public static List<Integer> reached(BigDecimal limit, BigDecimal spent) {
        BigDecimal hundredTimesSpent = spent.multiply(BigDecimal.valueOf(100));
        return PERCENTS.stream()
                .filter(percent -> hundredTimesSpent.compareTo(limit.multiply(BigDecimal.valueOf(percent))) >= 0)
                .toList();
    }

    /** The month (UTC) an instant falls in. */
    public static YearMonth period(Instant instant) {
        return YearMonth.from(instant.atZone(ZoneOffset.UTC));
    }

    /** The first moment of a month, UTC. */
    public static Instant start(YearMonth month) {
        return month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /** The first moment after a month, UTC. */
    public static Instant end(YearMonth month) {
        return start(month.plusMonths(1));
    }

    /** The last day of a month. */
    public static LocalDate lastDay(YearMonth month) {
        return month.atEndOfMonth();
    }
}
