package com.lifehub.finance.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.lifehub.finance.model.dto.CreateTransactionRequest;
import com.lifehub.finance.model.dto.TransactionDTO;
import com.lifehub.finance.model.dto.UpdateTransactionRequest;
import com.lifehub.finance.model.entity.EntryType;
import com.lifehub.starter.web.PageResponse;

/**
 * Income and expenses. Every member can read and record them. Changing or deleting one is for the person who
 * recorded it, the payer, or an owner or admin.
 */
public interface TransactionService {

    /** Pages through transactions, newest first by default, with optional filters. */
    PageResponse<TransactionDTO> getTransactions(UUID workspaceId, EntryType type, UUID categoryId, Instant from,
            Instant to, Pageable pageable);

    /** Returns one transaction with its splits. */
    TransactionDTO getTransaction(UUID workspaceId, UUID transactionId);

    /** Records a transaction and its splits; publishes transaction.created and any budget alert. */
    TransactionDTO createTransaction(UUID workspaceId, CreateTransactionRequest request);

    /** Changes a transaction; publishes transaction.updated and any budget alert. */
    TransactionDTO updateTransaction(UUID workspaceId, UUID transactionId, UpdateTransactionRequest request);

    /** Soft deletes a transaction; publishes transaction.deleted. */
    void deleteTransaction(UUID workspaceId, UUID transactionId);
}
