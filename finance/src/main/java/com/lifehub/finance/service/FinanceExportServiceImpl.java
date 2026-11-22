package com.lifehub.finance.service;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.finance.model.entity.Category;
import com.lifehub.finance.model.entity.Transaction;
import com.lifehub.finance.model.entity.TransactionSplit;
import com.lifehub.finance.repository.CategoryRepository;
import com.lifehub.finance.repository.TransactionRepository;
import com.lifehub.finance.repository.TransactionSplitRepository;
import com.lifehub.finance.util.CsvUtil;
import com.lifehub.starter.membership.MembershipClient;
import com.lifehub.starter.membership.MembershipUnavailableException;
import com.lifehub.starter.membership.WorkspaceAccess;
import com.lifehub.starter.membership.WorkspaceTimezones;
import com.lifehub.starter.web.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
class FinanceExportServiceImpl implements FinanceExportService {

    static final int MAX_ROWS = 10_000;
    static final List<String> HEADER =
            List.of("Date", "Type", "Description", "Category", "Amount", "Currency", "Paid by", "Splits");
    private static final DateTimeFormatter LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final WorkspaceAccess access;
    private final TransactionRepository transactions;
    private final TransactionSplitRepository splits;
    private final CategoryRepository categories;
    private final MembershipClient identity;
    private final WorkspaceTimezones zones;
    private final Clock clock;

    /**
     * Checks membership, reads the range, and writes one row per transaction with dates in the workspace's
     * timezone and people by name (by ID if identity-workspace cannot answer).
     */
    @Override
    @Transactional(readOnly = true)
    public Export exportTransactions(UUID workspaceId, Instant from, Instant to) {
        access.require(workspaceId);
        ZoneId zone = zones.zoneOf(workspaceId);
        YearMonth month = BudgetThresholds.period(clock.instant(), zone);
        Instant start = from != null ? from : BudgetThresholds.start(month, zone);
        Instant end = to != null ? to : BudgetThresholds.end(month, zone);
        if (!start.isBefore(end)) {
            throw ApiException.badRequest("'from' must be before 'to'.");
        }
        List<Transaction> rows = transactions.findForExport(workspaceId, start, end, PageRequest.of(0, MAX_ROWS + 1));
        if (rows.size() > MAX_ROWS) {
            throw ApiException.badRequest("More than " + MAX_ROWS + " transactions in this range. Export a shorter range.");
        }
        Map<UUID, List<TransactionSplit>> splitsByTransaction = splits
                .findByTransactionIdIn(rows.stream().map(Transaction::getId).toList()).stream()
                .collect(Collectors.groupingBy(TransactionSplit::getTransactionId));
        Map<UUID, String> categoryNames = categories.findAllById(rows.stream().map(Transaction::getCategoryId)
                        .filter(id -> id != null).distinct().toList()).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        Set<UUID> people = new HashSet<>();
        rows.forEach(t -> people.add(t.getPayerUserId()));
        splitsByTransaction.values().forEach(list -> list.forEach(s -> people.add(s.getUserId())));
        Function<UUID, String> name = names(people);

        StringBuilder csv = new StringBuilder("﻿").append(CsvUtil.row(HEADER));
        for (Transaction t : rows) {
            String splitText = splitsByTransaction.getOrDefault(t.getId(), List.of()).stream()
                    .map(s -> name.apply(s.getUserId()) + ": " + Money.format(s.getAmount()))
                    .collect(Collectors.joining("; "));
            csv.append(CsvUtil.row(List.of(
                    LOCAL.format(t.getOccurredAt().atZone(zone)),
                    t.getType().name(),
                    t.getDescription(),
                    t.getCategoryId() == null ? "" : categoryNames.getOrDefault(t.getCategoryId(), ""),
                    Money.format(t.getAmount()),
                    t.getCurrency(),
                    name.apply(t.getPayerUserId()),
                    splitText)));
        }
        String fileName = "lifehub-transactions-" + LOCAL.format(start.atZone(zone)).substring(0, 10) + "-to-"
                + LOCAL.format(end.minusSeconds(1).atZone(zone)).substring(0, 10) + ".csv";
        return new Export(fileName, csv.toString());
    }

    /** Names from identity-workspace, falling back to the user ID when a name is missing or it cannot answer. */
    private Function<UUID, String> names(Set<UUID> people) {
        Map<UUID, String> known;
        try {
            known = identity.findDisplayNames(people);
        } catch (MembershipUnavailableException ex) {
            log.warn("Exporting without names: identity-workspace did not answer", ex);
            known = Map.of();
        }
        Map<UUID, String> found = known;
        return id -> {
            String n = found.get(id);
            return n == null || n.isBlank() ? id.toString() : n;
        };
    }
}
