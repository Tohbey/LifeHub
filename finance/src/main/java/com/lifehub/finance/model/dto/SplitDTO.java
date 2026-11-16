package com.lifehub.finance.model.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SplitDTO(UUID userId, BigDecimal amount) {
}
