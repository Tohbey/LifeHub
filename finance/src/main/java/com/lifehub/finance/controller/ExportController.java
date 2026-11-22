package com.lifehub.finance.controller;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.finance.service.FinanceExportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Finance export", description = "Transactions as a CSV file.")
@RequiredArgsConstructor
class ExportController {

    private final FinanceExportService exportService;

    /** GET /api/v1/workspaces/{id}/finance/export: transactions in [from, to) as a CSV download. */
    @Operation(summary = "Export transactions as CSV",
            description = "One row per transaction, oldest first, with local dates and people by name. This month by "
                    + "default. At most 10,000 rows; values that a spreadsheet would run as formulas are neutralised.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The CSV file",
                    content = @Content(mediaType = "text/csv", schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Invalid range, or more than 10,000 transactions",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Workspace not found, or you are not a member of it",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/api/v1/workspaces/{workspaceId}/finance/export")
    ResponseEntity<byte[]> export(@PathVariable UUID workspaceId, @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        FinanceExportService.Export export = exportService.exportTransactions(workspaceId, from, to);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(export.fileName()).build().toString())
                .body(export.csv().getBytes(StandardCharsets.UTF_8));
    }
}
