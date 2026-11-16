package com.lifehub.starter.persistence;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** LifeHub stores instants in {@code DATETIME(6)} columns as UTC wall-clock time. Use it when binding with JdbcClient. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Dates {

    /** Converts an instant to UTC wall-clock time for binding to a DATETIME column. */
    public static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
