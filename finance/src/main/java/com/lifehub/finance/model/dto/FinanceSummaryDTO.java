package com.lifehub.finance.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Totals between two instants, one entry per currency (currencies are never converted or combined). */
public record FinanceSummaryDTO(Instant from, Instant to, List<CurrencyTotals> currencies) {

    public record CurrencyTotals(String currency, BigDecimal income, BigDecimal expense, BigDecimal net,
            List<CategoryTotal> expenseByCategory) {
    }

    /** {@code categoryId} is null for uncategorised expenses. */
    public record CategoryTotal(UUID categoryId, String name, BigDecimal amount) {
    }
}
