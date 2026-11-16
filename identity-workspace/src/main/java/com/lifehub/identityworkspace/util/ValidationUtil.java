package com.lifehub.identityworkspace.util;

import java.time.DateTimeException;
import java.time.ZoneId;

import com.lifehub.starter.web.ApiException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidationUtil {

    /** Returns the canonical IANA ID, or fails with 400 for an unknown timezone. */
    public static String requireTimezone(String timezone) {
        try {
            return ZoneId.of(timezone).getId();
        } catch (DateTimeException ex) {
            throw ApiException.badRequest("Unknown timezone: " + timezone);
        }
    }
}
