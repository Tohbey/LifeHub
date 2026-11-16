package com.lifehub.starter.events;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** LifeHub stores instants in {@code DATETIME(6)} columns as UTC wall-clock time. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class Dates {

    /** Converts an instant to UTC wall-clock time for binding to a DATETIME column. */
    static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
