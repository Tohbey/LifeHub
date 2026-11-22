package com.lifehub.planner.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.lifehub.planner.model.entity.Frequency;
import com.lifehub.planner.service.RecurrenceCalculator.Occurrence;
import com.lifehub.planner.service.RecurrenceCalculator.Rule;

class RecurrenceCalculatorTest {

    private static final ZoneId LONDON = ZoneId.of("Europe/London");

    @Test
    void aWeeklyEventKeepsItsLocalTimeAcrossTheClockChange() {
        // 18:00 London on Thursday 22 October 2026 (BST, UTC+1); the clocks go back on 25 October.
        Instant start = Instant.parse("2026-10-22T17:00:00Z");
        List<Occurrence> occurrences = RecurrenceCalculator.between(start, start.plusSeconds(3600),
                new Rule(Frequency.WEEKLY, 1, null), LONDON, start, Instant.parse("2026-11-06T00:00:00Z"), 10);

        assertThat(occurrences).extracting(Occurrence::start).containsExactly(
                Instant.parse("2026-10-22T17:00:00Z"),
                Instant.parse("2026-10-29T18:00:00Z"), // still 18:00 local, now GMT
                Instant.parse("2026-11-05T18:00:00Z"));
    }

    @Test
    void aMonthlyEventOnThe31stUsesTheLastDayOfShortMonthsWithoutDrifting() {
        Instant start = Instant.parse("2027-01-31T09:00:00Z");
        List<Occurrence> occurrences = RecurrenceCalculator.between(start, start.plusSeconds(1800),
                new Rule(Frequency.MONTHLY, 1, null), LONDON, start, Instant.parse("2027-04-01T00:00:00Z"), 10);

        assertThat(occurrences).extracting(o -> o.start().atZone(LONDON).toLocalDate()).containsExactly(
                LocalDate.of(2027, 1, 31), LocalDate.of(2027, 2, 28), LocalDate.of(2027, 3, 31));
    }

    @Test
    void stopsAfterTheUntilDayAndHonoursTheInterval() {
        Instant start = Instant.parse("2026-12-01T10:00:00Z");
        List<Occurrence> occurrences = RecurrenceCalculator.between(start, start.plusSeconds(600),
                new Rule(Frequency.DAILY, 2, LocalDate.of(2026, 12, 7)), LONDON, start,
                Instant.parse("2027-01-01T00:00:00Z"), 100);

        assertThat(occurrences).extracting(o -> o.start().atZone(LONDON).getDayOfMonth()).containsExactly(1, 3, 5, 7);
    }

    @Test
    void anOccurrenceStillRunningAtTheStartOfTheRangeIsIncluded() {
        Instant start = Instant.parse("2026-12-01T22:00:00Z");
        List<Occurrence> occurrences = RecurrenceCalculator.between(start, start.plusSeconds(4 * 3600),
                new Rule(Frequency.DAILY, 1, null), LONDON, Instant.parse("2026-12-03T00:00:00Z"),
                Instant.parse("2026-12-03T12:00:00Z"), 10);

        assertThat(occurrences).extracting(Occurrence::start).containsExactly(Instant.parse("2026-12-02T22:00:00Z"));
    }

    @Test
    void findsTheNextOccurrence() {
        Instant start = Instant.parse("2026-12-01T10:00:00Z");
        Rule weekly = new Rule(Frequency.WEEKLY, 1, LocalDate.of(2026, 12, 20));

        assertThat(RecurrenceCalculator.nextStartOnOrAfter(start, weekly, LONDON, Instant.parse("2026-12-08T10:00:00Z")))
                .contains(Instant.parse("2026-12-08T10:00:00Z"));
        assertThat(RecurrenceCalculator.nextStartAfter(start, weekly, LONDON, Instant.parse("2026-12-08T10:00:00Z")))
                .contains(Instant.parse("2026-12-15T10:00:00Z"));
        assertThat(RecurrenceCalculator.nextStartAfter(start, weekly, LONDON, Instant.parse("2026-12-15T10:00:00Z"))).isEmpty();
    }
}
