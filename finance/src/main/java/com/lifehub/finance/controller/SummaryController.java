package com.lifehub.finance.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.finance.model.dto.FinanceSummaryDTO;
import com.lifehub.finance.service.FinanceSummaryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Finance summary", description = "Income, expense and net totals for a period.")
@RequiredArgsConstructor
class SummaryController {

    private final FinanceSummaryService summaryService;

    /** GET /api/v1/workspaces/{id}/finance/summary: totals for [from, to), this month by default. */
    @Operation(summary = "Get a finance summary",
            description = "Income, expense and net totals for [from, to), per currency, with expenses by category. "
                    + "Defaults to the current month (UTC).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The summary",
                    content = @Content(schema = @Schema(implementation = FinanceSummaryDTO.class))),
            @ApiResponse(responseCode = "400", description = "\"from\" is not before \"to\"",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Workspace not found, or you are not a member of it",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/api/v1/workspaces/{workspaceId}/finance/summary")
    FinanceSummaryDTO getSummary(@PathVariable UUID workspaceId, @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        return summaryService.getSummary(workspaceId, from, to);
    }
}
