package com.lifehub.finance.model.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/** A monthly limit for an expense category. */
public record CreateBudgetRequest(
        @NotNull UUID categoryId,
        @NotNull @Positive @Digits(integer = 15, fraction = 2) BigDecimal monthlyLimit,
        @NotNull @Pattern(regexp = "[A-Z]{3}", message = "must be an ISO 4217 code such as GBP") String currency) {
}
