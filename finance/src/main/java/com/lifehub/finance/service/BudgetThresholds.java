package com.lifehub.finance.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Budget periods (calendar months in the workspace's timezone) and the thresholds that trigger an alert. */
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

    /** The month an instant falls in, in this timezone. */
    public static YearMonth period(Instant instant, ZoneId zone) {
        return YearMonth.from(instant.atZone(zone));
    }

    /** The first moment of a month in this timezone. */
    public static Instant start(YearMonth month, ZoneId zone) {
        return month.atDay(1).atStartOfDay(zone).toInstant();
    }

    /** The first moment after a month in this timezone. */
    public static Instant end(YearMonth month, ZoneId zone) {
        return start(month.plusMonths(1), zone);
    }

    /** The last day of a month. */
    public static LocalDate lastDay(YearMonth month) {
        return month.atEndOfMonth();
    }
}
