package com.lifehub.finance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Money formatting for events: a string decimal with two places (deploy/docs/events.md). */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Money {

    /** Formats an amount as "120.00". */
    public static String format(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }
}
