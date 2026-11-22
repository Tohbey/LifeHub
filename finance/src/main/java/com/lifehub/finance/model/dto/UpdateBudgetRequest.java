package com.lifehub.finance.model.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** A new monthly limit for a budget. The category and currency cannot change; make a new budget instead. */
public record UpdateBudgetRequest(@NotNull @Positive @Digits(integer = 15, fraction = 2) BigDecimal monthlyLimit) {
}
