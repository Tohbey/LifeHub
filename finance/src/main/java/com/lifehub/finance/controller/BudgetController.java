package com.lifehub.finance.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.finance.model.dto.BudgetDTO;
import com.lifehub.finance.model.dto.CreateBudgetRequest;
import com.lifehub.finance.service.BudgetService;
import com.lifehub.starter.web.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Budgets", description = "Monthly spending limits per expense category.")
@RequestMapping("/api/v1/workspaces/{workspaceId}/budgets")
@RequiredArgsConstructor
class BudgetController {

    private final BudgetService budgetService;

    /** GET /api/v1/workspaces/{id}/budgets: budgets with this month's spending. */
    @Operation(summary = "List budgets",
            description = "Each budget with what has been spent against it this month (UTC).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "One page of budgets"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Workspace not found, or you are not a member of it",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    PageResponse<BudgetDTO> getBudgets(@PathVariable UUID workspaceId,
            @PageableDefault(sort = "createdAt") Pageable pageable) {
        return budgetService.getBudgets(workspaceId, pageable);
    }

    /** POST /api/v1/workspaces/{id}/budgets: sets a monthly limit for a category. Owners and admins only. */
    @Operation(summary = "Set a monthly budget",
            description = "A monthly limit for an expense category, in one currency. Members are alerted at 80% and "
                    + "100%. Owners and admins only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Budget created",
                    content = @Content(schema = @Schema(implementation = BudgetDTO.class))),
            @ApiResponse(responseCode = "400",
                    description = "Invalid request, or not an expense category of this workspace",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Only owners and admins can set budgets",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Workspace not found, or you are not a member of it",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "This category already has a budget in this currency",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BudgetDTO createBudget(@PathVariable UUID workspaceId, @Valid @RequestBody CreateBudgetRequest request) {
        return budgetService.createBudget(workspaceId, request);
    }
}
