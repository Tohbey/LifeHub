package com.lifehub.finance.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.lifehub.finance.model.entity.EntryType;
import com.lifehub.finance.model.entity.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /** Finds a transaction only if it belongs to this workspace and is not deleted. */
    Optional<Transaction> findByIdAndWorkspaceIdAndDeletedAtIsNull(UUID id, UUID workspaceId);

    @Query("""
            select t from Transaction t
            where t.workspaceId = :workspaceId and t.deletedAt is null
              and (:type is null or t.type = :type)
              and (:categoryId is null or t.categoryId = :categoryId)
              and (:from is null or t.occurredAt >= :from)
              and (:to is null or t.occurredAt < :to)
            """)
    /** Pages through a workspace's transactions, with optional filters (null means no filter). */
    Page<Transaction> search(UUID workspaceId, EntryType type, UUID categoryId, Instant from, Instant to,
            Pageable pageable);

    @Query("""
            select new com.lifehub.finance.repository.TransactionTotal(t.currency, t.type, t.categoryId, sum(t.amount))
            from Transaction t
            where t.workspaceId = :workspaceId and t.deletedAt is null and t.occurredAt >= :from and t.occurredAt < :to
            group by t.currency, t.type, t.categoryId
            """)
    /** Totals per currency, type and category between two instants. */
    List<TransactionTotal> totals(UUID workspaceId, Instant from, Instant to);

    @Query("""
            select coalesce(sum(t.amount), 0) from Transaction t
            where t.workspaceId = :workspaceId and t.deletedAt is null
              and t.type = com.lifehub.finance.model.entity.EntryType.EXPENSE
              and t.categoryId = :categoryId and t.currency = :currency
              and t.occurredAt >= :from and t.occurredAt < :to
            """)
    /** What was spent in one category and currency between two instants. */
    BigDecimal spent(UUID workspaceId, UUID categoryId, String currency, Instant from, Instant to);
}
