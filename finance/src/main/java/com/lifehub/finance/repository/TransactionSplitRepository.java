package com.lifehub.finance.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.lifehub.finance.model.entity.TransactionSplit;

@Repository
public interface TransactionSplitRepository extends JpaRepository<TransactionSplit, UUID> {

    /** The splits of one transaction. */
    List<TransactionSplit> findByTransactionId(UUID transactionId);

    /** The splits of several transactions, for one page of results. */
    List<TransactionSplit> findByTransactionIdIn(Collection<UUID> transactionIds);

    @Modifying(flushAutomatically = true)
    @Query("delete from TransactionSplit s where s.transactionId = :transactionId")
    /** Removes all splits of a transaction, before new ones are written. */
    void deleteByTransactionId(UUID transactionId);
}
