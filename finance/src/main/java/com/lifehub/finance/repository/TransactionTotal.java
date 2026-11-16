package com.lifehub.finance.repository;

import java.math.BigDecimal;
import java.util.UUID;

import com.lifehub.finance.model.entity.EntryType;

/**
 * One row of a summary: the total of one currency, type and category. A top-level record, because Hibernate's
 * {@code select new} cannot name a record nested in another type.
 */
public record TransactionTotal(String currency, EntryType type, UUID categoryId, BigDecimal amount) {
}
