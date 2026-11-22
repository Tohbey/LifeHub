package com.lifehub.finance.service;

import java.time.Instant;
import java.util.UUID;

import com.lifehub.finance.model.dto.FinanceSummaryDTO;

/** Income, expense and net totals for a period. */
public interface FinanceSummaryService {

    /** Totals per currency between {@code from} (inclusive) and {@code to} (exclusive); defaults to this month in the workspace's timezone. */
    FinanceSummaryDTO getSummary(UUID workspaceId, Instant from, Instant to);
}
