package com.lifehub.planner.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.lifehub.planner.model.entity.Frequency;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Works out the occurrences of a recurring event. Each occurrence is the series start plus k steps, counted in
 * the workspace's timezone, so a weekly 18:00 event stays at 18:00 local time across daylight saving, and a
 * monthly event on the 31st falls on the last day of shorter months without drifting.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RecurrenceCalculator {

    /** Safety net: no series is expanded beyond this many steps. */
    static final int MAX_STEPS = 20_000;

    /** A repeat rule. {@code until} is the last local day an occurrence may start on, or null for no end. */
    public record Rule(Frequency frequency, int interval, LocalDate until) {
    }

    /** One occurrence of a series. */
    public record Occurrence(Instant start, Instant end) {
    }

    /** The occurrences that overlap [from, to), oldest first, at most {@code limit}. */
    public static List<Occurrence> between(Instant seriesStart, Instant seriesEnd, Rule rule, ZoneId zone, Instant from,
            Instant to, int limit) {
        Duration length = Duration.between(seriesStart, seriesEnd);
        ZonedDateTime first = seriesStart.atZone(zone);
        List<Occurrence> result = new ArrayList<>();
        for (int k = 0; k < MAX_STEPS && result.size() < limit; k++) {
            ZonedDateTime start = step(first, rule, k);
            if (!start.toInstant().isBefore(to) || isAfterUntil(start, rule)) {
                break;
            }
            Instant end = start.toInstant().plus(length);
            if (end.isAfter(from)) {
                result.add(new Occurrence(start.toInstant(), end));
            }
        }
        return result;
    }

    /** The first occurrence starting at or after {@code at}, if the series has one. */
    public static Optional<Instant> nextStartOnOrAfter(Instant seriesStart, Rule rule, ZoneId zone, Instant at) {
        ZonedDateTime first = seriesStart.atZone(zone);
        for (int k = 0; k < MAX_STEPS; k++) {
            ZonedDateTime start = step(first, rule, k);
            if (isAfterUntil(start, rule)) {
                return Optional.empty();
            }
            if (!start.toInstant().isBefore(at)) {
                return Optional.of(start.toInstant());
            }
        }
        return Optional.empty();
    }

    /** The first occurrence starting strictly after {@code after}, if the series has one. */
    public static Optional<Instant> nextStartAfter(Instant seriesStart, Rule rule, ZoneId zone, Instant after) {
        return nextStartOnOrAfter(seriesStart, rule, zone, after.plusNanos(1));
    }

    /** The k-th occurrence's start: always counted from the first, so month-end dates do not drift. */
    private static ZonedDateTime step(ZonedDateTime first, Rule rule, int k) {
        return first.plus((long) k * rule.interval(), rule.frequency().unit());
    }

    /** True once an occurrence would start after the rule's last day. */
    private static boolean isAfterUntil(ZonedDateTime start, Rule rule) {
        return rule.until() != null && start.toLocalDate().isAfter(rule.until());
    }
}
