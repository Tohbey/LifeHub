package com.lifehub.finance.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** A budget with what has been spent against it in the current month (UTC). */
public record BudgetDTO(UUID id, UUID categoryId, BigDecimal monthlyLimit, String currency, LocalDate periodStart,
        LocalDate periodEnd, BigDecimal spent) {
}
