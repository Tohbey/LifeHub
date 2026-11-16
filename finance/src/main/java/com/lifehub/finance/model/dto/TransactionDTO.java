package com.lifehub.finance.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.lifehub.finance.model.entity.EntryType;

public record TransactionDTO(UUID id, EntryType type, String description, BigDecimal amount, String currency,
        UUID categoryId, UUID payerUserId, Instant occurredAt, UUID createdByUserId, List<SplitDTO> splits) {
}
