package com.lifehub.finance.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.lifehub.finance.model.entity.EntryType;

/**
 * {@code payerUserId} defaults to the caller. {@code splits} makes it a shared expense: every participant must be
 * a member and the shares must add up to {@code amount}.
 */
public record CreateTransactionRequest(
        @NotNull EntryType type,
        @NotBlank @Size(max = 200) String description,
        @NotNull @Positive @Digits(integer = 15, fraction = 2) BigDecimal amount,
        @NotNull @Pattern(regexp = "[A-Z]{3}", message = "must be an ISO 4217 code such as GBP") String currency,
        UUID categoryId,
        UUID payerUserId,
        @NotNull Instant occurredAt,
        @Size(max = 20) List<@Valid @NotNull SplitRequest> splits) {
}
