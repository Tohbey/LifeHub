package com.lifehub.planner.model.entity;

import java.time.temporal.ChronoUnit;

/** How often a recurring event repeats. */
public enum Frequency {
    DAILY(ChronoUnit.DAYS),
    WEEKLY(ChronoUnit.WEEKS),
    MONTHLY(ChronoUnit.MONTHS),
    YEARLY(ChronoUnit.YEARS);

    private final ChronoUnit unit;

    Frequency(ChronoUnit unit) {
        this.unit = unit;
    }

    /** The calendar unit one step is measured in. */
    public ChronoUnit unit() {
        return unit;
    }
}
