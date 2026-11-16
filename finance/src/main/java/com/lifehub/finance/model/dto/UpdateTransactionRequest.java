package com.lifehub.finance.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Fields left out are unchanged. {@code splits} replaces all splits; an empty list makes the expense personal.
 * {@code clearCategory} removes the category. The type (income or expense) cannot change.
 */
public record UpdateTransactionRequest(
        @Size(min = 1, max = 200) String description,
        @Positive @Digits(integer = 15, fraction = 2) BigDecimal amount,
        @Pattern(regexp = "[A-Z]{3}", message = "must be an ISO 4217 code such as GBP") String currency,
        UUID categoryId,
        Boolean clearCategory,
        UUID payerUserId,
        Instant occurredAt,
        @Size(max = 20) List<@Valid SplitRequest> splits) {
}
