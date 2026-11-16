package com.lifehub.audit.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.audit.model.dto.AuditEntryDTO;
import com.lifehub.audit.service.AuditService;
import com.lifehub.starter.web.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Audit log", description = "Every event recorded for a workspace. Owners and admins only.")
@RequiredArgsConstructor
class AuditController {

    private final AuditService auditService;

    /** GET /api/v1/workspaces/{id}/audit: the workspace's events, newest first. Owners and admins only. */
    @Operation(summary = "Read the audit log",
            description = "Every event recorded for the workspace, newest first, exactly as published. Filter by type "
                    + "and a [from, to) time range. Owners and admins only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "One page of audit entries"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Only owners and admins can read the audit log",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Workspace not found, or you are not a member of it",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/api/v1/workspaces/{workspaceId}/audit")
    PageResponse<AuditEntryDTO> getEntries(@PathVariable UUID workspaceId, @RequestParam(required = false) String type,
            @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
            @PageableDefault(size = 50) Pageable pageable) {
        return auditService.getEntries(workspaceId, type, from, to, pageable);
    }
}
