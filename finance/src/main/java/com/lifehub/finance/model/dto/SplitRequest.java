package com.lifehub.finance.model.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** One member's share of a shared expense. */
public record SplitRequest(@NotNull UUID userId, @NotNull @Positive @Digits(integer = 15, fraction = 2) BigDecimal amount) {
}
