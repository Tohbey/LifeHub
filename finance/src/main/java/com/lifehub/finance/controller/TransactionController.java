package com.lifehub.finance.controller;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.finance.model.dto.CreateTransactionRequest;
import com.lifehub.finance.model.dto.TransactionDTO;
import com.lifehub.finance.model.dto.UpdateTransactionRequest;
import com.lifehub.finance.model.entity.EntryType;
import com.lifehub.finance.service.TransactionService;
import com.lifehub.starter.web.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Transactions", description = "Income and expenses, including shared expenses with splits.")
@RequestMapping("/api/v1/workspaces/{workspaceId}/transactions")
@RequiredArgsConstructor
class TransactionController {

    private final TransactionService transactionService;

    /** GET /api/v1/workspaces/{id}/transactions: newest first; filter by type, categoryId, from and to. */
    @Operation(summary = "List transactions",
            description = "Newest first. Filter by type, categoryId, and a [from, to) time range.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "One page of transactions"),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Workspace not found, or you are not a member of it",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    PageResponse<TransactionDTO> getTransactions(@PathVariable UUID workspaceId,
            @RequestParam(required = false) EntryType type, @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
            @PageableDefault(sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return transactionService.getTransactions(workspaceId, type, categoryId, from, to, pageable);
    }

    /** POST /api/v1/workspaces/{id}/transactions: records income or an expense, optionally split. Returns 201. */
    @Operation(summary = "Record income or an expense",
            description = "The payer defaults to you. Add splits to make it a shared expense: every participant must "
                    + "be a member and the shares must add up to the amount.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transaction recorded",
                    content = @Content(schema = @Schema(implementation = TransactionDTO.class))),
            @ApiResponse(responseCode = "400",
                    description = "Invalid request, splits that do not add up, an unknown category, or a payer or "
                            + "participant who is not a member",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Workspace not found, or you are not a member of it",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    ResponseEntity<TransactionDTO> createTransaction(@PathVariable UUID workspaceId,
            @Valid @RequestBody CreateTransactionRequest request) {
        TransactionDTO created = transactionService.createTransaction(workspaceId, request);
        return ResponseEntity.created(URI.create("/api/v1/workspaces/" + workspaceId + "/transactions/" + created.id()))
                .body(created);
    }

    /** GET /api/v1/workspaces/{id}/transactions/{transactionId}: one transaction with its splits. */
    @Operation(summary = "Get a transaction", description = "One transaction with its splits.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The transaction",
                    content = @Content(schema = @Schema(implementation = TransactionDTO.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Transaction or workspace not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{transactionId}")
    TransactionDTO getTransaction(@PathVariable UUID workspaceId, @PathVariable UUID transactionId) {
        return transactionService.getTransaction(workspaceId, transactionId);
    }

    /** PATCH /api/v1/workspaces/{id}/transactions/{transactionId}: changes the given fields. */
    @Operation(summary = "Update a transaction",
            description = "Changes the given fields. Only the person who recorded it, the payer, or an owner or admin "
                    + "may change it.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction updated",
                    content = @Content(schema = @Schema(implementation = TransactionDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request, or splits that do not add up",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "You did not record or pay this, and are not an admin",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Transaction or workspace not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{transactionId}")
    TransactionDTO updateTransaction(@PathVariable UUID workspaceId, @PathVariable UUID transactionId,
            @Valid @RequestBody UpdateTransactionRequest request) {
        return transactionService.updateTransaction(workspaceId, transactionId, request);
    }

    /** DELETE /api/v1/workspaces/{id}/transactions/{transactionId}: soft deletes it. Returns 204. */
    @Operation(summary = "Delete a transaction",
            description = "Soft deletes it: it is kept for history but no longer shown or counted.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Transaction deleted", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "You did not record or pay this, and are not an admin",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Transaction or workspace not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{transactionId}")
    ResponseEntity<Void> deleteTransaction(@PathVariable UUID workspaceId, @PathVariable UUID transactionId) {
        transactionService.deleteTransaction(workspaceId, transactionId);
        return ResponseEntity.noContent().build();
    }
}
