package com.lifehub.finance.service;

import java.time.Instant;
import java.util.UUID;

/** Transactions as a CSV file, for spreadsheets and accountants. */
public interface FinanceExportService {

    /** The CSV export and its file name. */
    record Export(String fileName, String csv) {
    }

    /** Transactions in [from, to), oldest first; this month (workspace timezone) by default. At most 10,000 rows. */
    Export exportTransactions(UUID workspaceId, Instant from, Instant to);
}
