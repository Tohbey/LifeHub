package com.lifehub.finance.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.finance.model.dto.FinanceSummaryDTO;
import com.lifehub.finance.model.entity.Category;
import com.lifehub.finance.model.entity.EntryType;
import com.lifehub.finance.repository.CategoryRepository;
import com.lifehub.finance.repository.TransactionRepository;
import com.lifehub.finance.repository.TransactionTotal;
import com.lifehub.starter.membership.WorkspaceAccess;
import com.lifehub.starter.web.ApiException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class FinanceSummaryServiceImpl implements FinanceSummaryService {

    private final WorkspaceAccess access;
    private final TransactionRepository transactions;
    private final CategoryRepository categories;
    private final Clock clock;

    /** Checks membership, sums by currency, type and category, and names the categories. */
    @Override
    @Transactional(readOnly = true)
    public FinanceSummaryDTO getSummary(UUID workspaceId, Instant from, Instant to) {
        access.require(workspaceId);
        YearMonth month = BudgetThresholds.period(clock.instant());
        Instant start = from != null ? from : BudgetThresholds.start(month);
        Instant end = to != null ? to : BudgetThresholds.end(month);
        if (!start.isBefore(end)) {
            throw ApiException.badRequest("'from' must be before 'to'.");
        }
        List<TransactionTotal> totals = transactions.totals(workspaceId, start, end);
        Map<UUID, String> names = categories.findAllById(totals.stream().map(TransactionTotal::categoryId)
                        .filter(Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        Map<String, List<TransactionTotal>> byCurrency = totals.stream()
                .collect(Collectors.groupingBy(TransactionTotal::currency, TreeMap::new, Collectors.toList()));
        List<FinanceSummaryDTO.CurrencyTotals> result = new ArrayList<>();
        byCurrency.forEach((currency, rows) -> {
            BigDecimal income = sum(rows, EntryType.INCOME);
            BigDecimal expense = sum(rows, EntryType.EXPENSE);
            List<FinanceSummaryDTO.CategoryTotal> byCategory = rows.stream()
                    .filter(r -> r.type() == EntryType.EXPENSE)
                    .map(r -> new FinanceSummaryDTO.CategoryTotal(r.categoryId(),
                            r.categoryId() == null ? "Uncategorised" : names.getOrDefault(r.categoryId(), "Unknown"),
                            r.amount()))
                    .sorted(Comparator.comparing(FinanceSummaryDTO.CategoryTotal::amount).reversed())
                    .toList();
            result.add(new FinanceSummaryDTO.CurrencyTotals(currency, income, expense, income.subtract(expense), byCategory));
        });
        return new FinanceSummaryDTO(start, end, result);
    }

    /** Adds up the rows of one type. */
    private static BigDecimal sum(List<TransactionTotal> rows, EntryType type) {
        return rows.stream().filter(r -> r.type() == type).map(TransactionTotal::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
