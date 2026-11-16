package com.lifehub.finance.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.finance.mapper.TransactionMapper;
import com.lifehub.finance.model.dto.CreateTransactionRequest;
import com.lifehub.finance.model.dto.SplitRequest;
import com.lifehub.finance.model.dto.TransactionDTO;
import com.lifehub.finance.model.dto.UpdateTransactionRequest;
import com.lifehub.finance.model.entity.Category;
import com.lifehub.finance.model.entity.EntryType;
import com.lifehub.finance.model.entity.Transaction;
import com.lifehub.finance.model.entity.TransactionSplit;
import com.lifehub.finance.model.event.FinanceEvents;
import com.lifehub.finance.repository.CategoryRepository;
import com.lifehub.finance.repository.TransactionRepository;
import com.lifehub.finance.repository.TransactionSplitRepository;
import com.lifehub.starter.events.DomainEventPublisher;
import com.lifehub.starter.id.Ids;
import com.lifehub.starter.membership.Membership;
import com.lifehub.starter.membership.WorkspaceAccess;
import com.lifehub.starter.membership.WorkspaceRole;
import com.lifehub.starter.web.ApiException;
import com.lifehub.starter.web.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class TransactionServiceImpl implements TransactionService {

    private final WorkspaceAccess access;
    private final TransactionRepository transactions;
    private final TransactionSplitRepository splits;
    private final CategoryRepository categories;
    private final BudgetMonitor budgetMonitor;
    private final DomainEventPublisher events;
    private final Clock clock;

    /** Checks membership, reads one page and loads the page's splits in one query. */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionDTO> getTransactions(UUID workspaceId, EntryType type, UUID categoryId,
            Instant from, Instant to, Pageable pageable) {
        access.require(workspaceId);
        Page<Transaction> page = transactions.search(workspaceId, type, categoryId, from, to, pageable);
        Map<UUID, List<TransactionSplit>> byTransaction = splits
                .findByTransactionIdIn(page.map(Transaction::getId).getContent()).stream()
                .collect(Collectors.groupingBy(TransactionSplit::getTransactionId));
        return PageResponse.of(page, t -> TransactionMapper.toDto(t, byTransaction.getOrDefault(t.getId(), List.of())));
    }

    /** Checks membership, then loads the transaction from this workspace only. */
    @Override
    @Transactional(readOnly = true)
    public TransactionDTO getTransaction(UUID workspaceId, UUID transactionId) {
        access.require(workspaceId);
        Transaction transaction = load(workspaceId, transactionId);
        return TransactionMapper.toDto(transaction, splits.findByTransactionId(transactionId));
    }

    /**
     * Checks the caller, payer and split participants are members, validates the category and splits, saves,
     * publishes transaction.created and checks the budget.
     */
    @Override
    @Transactional
    public TransactionDTO createTransaction(UUID workspaceId, CreateTransactionRequest request) {
        Membership caller = access.require(workspaceId);
        UUID payer = request.payerUserId() == null ? caller.userId() : request.payerUserId();
        requireCategory(workspaceId, request.categoryId(), request.type());
        SplitPolicy.validate(request.type(), request.amount(), request.splits());
        requireMembers(workspaceId, payer, request.splits());

        Transaction transaction = transactions.save(new Transaction(Ids.newId(), workspaceId, request.type(),
                request.description().strip(), request.amount(), request.currency(), request.categoryId(), payer,
                request.occurredAt(), caller.userId(), clock.instant()));
        List<TransactionSplit> saved = saveSplits(transaction.getId(), request.splits());

        events.publish(FinanceEvents.TRANSACTION_CREATED, workspaceId, caller.userId(),
                new FinanceEvents.TransactionCreated(transaction.getId(), transaction.getType().name(),
                        transaction.getDescription(), Money.format(transaction.getAmount()), transaction.getCurrency(),
                        transaction.getCategoryId(), payer, transaction.getOccurredAt(), eventSplits(saved)));
        budgetMonitor.check(transaction, caller.userId());
        return TransactionMapper.toDto(transaction, saved);
    }

    /**
     * Checks the caller may change it, applies only the given fields, re-validates splits against the new
     * amount, publishes transaction.updated listing what changed, and checks the budget.
     */
    @Override
    @Transactional
    public TransactionDTO updateTransaction(UUID workspaceId, UUID transactionId, UpdateTransactionRequest request) {
        Membership caller = access.require(workspaceId);
        Transaction transaction = load(workspaceId, transactionId);
        requireCanChange(caller, transaction);

        UUID categoryId = Boolean.TRUE.equals(request.clearCategory()) ? null
                : request.categoryId() != null ? request.categoryId() : transaction.getCategoryId();
        if (request.categoryId() != null) {
            requireCategory(workspaceId, request.categoryId(), transaction.getType());
        }
        String description = request.description() != null ? request.description().strip() : transaction.getDescription();
        BigDecimal amount = request.amount() != null ? request.amount() : transaction.getAmount();
        String currency = request.currency() != null ? request.currency() : transaction.getCurrency();
        UUID payer = request.payerUserId() != null ? request.payerUserId() : transaction.getPayerUserId();
        Instant occurredAt = request.occurredAt() != null ? request.occurredAt() : transaction.getOccurredAt();

        List<TransactionSplit> current = splits.findByTransactionId(transactionId);
        List<SplitRequest> newSplits = request.splits() != null ? request.splits()
                : current.stream().map(s -> new SplitRequest(s.getUserId(), s.getAmount())).toList();
        SplitPolicy.validate(transaction.getType(), amount, newSplits);
        requireMembers(workspaceId, payer, request.payerUserId() != null || request.splits() != null ? newSplits : List.of());

        List<String> changed = new ArrayList<>();
        addIfChanged(changed, "description", transaction.getDescription(), description);
        if (transaction.getAmount().compareTo(amount) != 0) {
            changed.add("amount");
        }
        addIfChanged(changed, "currency", transaction.getCurrency(), currency);
        addIfChanged(changed, "categoryId", transaction.getCategoryId(), categoryId);
        addIfChanged(changed, "payerUserId", transaction.getPayerUserId(), payer);
        addIfChanged(changed, "occurredAt", transaction.getOccurredAt(), occurredAt);

        List<TransactionSplit> result = current;
        if (request.splits() != null && !sameSplits(current, request.splits())) {
            splits.deleteByTransactionId(transactionId);
            result = saveSplits(transactionId, request.splits());
            changed.add("splits");
        }
        if (changed.isEmpty()) {
            return TransactionMapper.toDto(transaction, result);
        }
        transaction.update(description, amount, currency, categoryId, payer, occurredAt, clock.instant());
        events.publish(FinanceEvents.TRANSACTION_UPDATED, workspaceId, caller.userId(),
                new FinanceEvents.TransactionUpdated(transactionId, changed, Money.format(amount), currency,
                        eventSplits(result)));
        budgetMonitor.check(transaction, caller.userId());
        return TransactionMapper.toDto(transaction, result);
    }

    /** Checks the caller may change it, soft deletes it and publishes transaction.deleted. */
    @Override
    @Transactional
    public void deleteTransaction(UUID workspaceId, UUID transactionId) {
        Membership caller = access.require(workspaceId);
        Transaction transaction = load(workspaceId, transactionId);
        requireCanChange(caller, transaction);
        transaction.delete(clock.instant());
        events.publish(FinanceEvents.TRANSACTION_DELETED, workspaceId, caller.userId(),
                new FinanceEvents.TransactionDeleted(transactionId));
    }

    /** Loads a live transaction of this workspace, or 404. */
    private Transaction load(UUID workspaceId, UUID transactionId) {
        return transactions.findByIdAndWorkspaceIdAndDeletedAtIsNull(transactionId, workspaceId)
                .orElseThrow(() -> ApiException.notFound("Transaction not found."));
    }

    /** Only the recorder, the payer, or an owner or admin may change or delete a transaction. */
    private static void requireCanChange(Membership caller, Transaction transaction) {
        if (!caller.userId().equals(transaction.getCreatedByUserId())
                && !caller.userId().equals(transaction.getPayerUserId())
                && !caller.role().isAtLeast(WorkspaceRole.ADMIN)) {
            throw ApiException.forbidden("Only the person who recorded this, the payer, or an admin can change it.");
        }
    }

    /** A category, when given, must belong to this workspace and match the transaction type. */
    private void requireCategory(UUID workspaceId, UUID categoryId, EntryType type) {
        if (categoryId == null) {
            return;
        }
        Category category = categories.findByIdAndWorkspaceId(categoryId, workspaceId)
                .orElseThrow(() -> ApiException.badRequest("Unknown category."));
        if (category.getType() != type) {
            throw ApiException.badRequest("The category is for " + category.getType().name().toLowerCase() + ".");
        }
    }

    /** The payer and every split participant must be active members of the workspace (one lookup). */
    private void requireMembers(UUID workspaceId, UUID payer, List<SplitRequest> splitRequests) {
        Set<UUID> people = new HashSet<>();
        people.add(payer);
        if (splitRequests != null) {
            splitRequests.forEach(s -> people.add(s.userId()));
        }
        access.requireAllMembers(workspaceId, people);
    }

    /** Saves the splits of a transaction. */
    private List<TransactionSplit> saveSplits(UUID transactionId, List<SplitRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        return splits.saveAll(requests.stream()
                .map(r -> new TransactionSplit(Ids.newId(), transactionId, r.userId(), r.amount()))
                .toList());
    }

    /** True when the requested splits are the same people with the same amounts. */
    private static boolean sameSplits(List<TransactionSplit> current, List<SplitRequest> requested) {
        if (current.size() != requested.size()) {
            return false;
        }
        Map<UUID, BigDecimal> byUser = current.stream()
                .collect(Collectors.toMap(TransactionSplit::getUserId, TransactionSplit::getAmount));
        return requested.stream().allMatch(r -> byUser.containsKey(r.userId())
                && byUser.get(r.userId()).compareTo(r.amount()) == 0);
    }

    /** The splits as they appear in events. */
    private static List<FinanceEvents.Split> eventSplits(List<TransactionSplit> saved) {
        return saved.stream().map(s -> new FinanceEvents.Split(s.getUserId(), Money.format(s.getAmount()))).toList();
    }

    /** Adds the field name when the value changed. */
    private static void addIfChanged(List<String> changed, String field, Object before, Object after) {
        if (!Objects.equals(before, after)) {
            changed.add(field);
        }
    }
}
