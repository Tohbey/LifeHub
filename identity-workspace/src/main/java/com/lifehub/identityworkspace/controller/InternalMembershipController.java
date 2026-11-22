package com.lifehub.identityworkspace.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.identityworkspace.service.LocalMembershipClient;
import com.lifehub.starter.membership.InternalMembershipApi;
import com.lifehub.starter.membership.InternalMembershipApi.MemberListResponse;
import com.lifehub.starter.membership.InternalMembershipApi.MemberLookupResponse;
import com.lifehub.starter.membership.InternalMembershipApi.WorkspaceInfoResponse;
import com.lifehub.starter.web.ApiException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * The membership lookup other services call (ADR 0005). Never routed by the gateway; the starter only
 * accepts service tokens with the internal audience on {@code /internal/**}.
 */
@RestController
@Tag(name = "Internal: membership",
        description = "Service-to-service membership lookups. Needs a service token with membership:read.")
@PreAuthorize("hasAuthority('SCOPE_" + InternalMembershipApi.SCOPE + "')")
@RequiredArgsConstructor
class InternalMembershipController {

    private static final int MAX_BULK = 100;

    private final LocalMembershipClient memberships;

    /** Returns the Logto subject's active membership of the workspace, or 404 when they are not a member. */
    @Operation(summary = "Look up a member by Logto subject",
            description = "Service-to-service. Returns the subject's active membership of the workspace.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The membership",
                    content = @Content(schema = @Schema(implementation = MemberLookupResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid service token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "The service token lacks the membership:read permission",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Not an active member", content = @Content)
    })
    @GetMapping(path = InternalMembershipApi.PATH, params = "sub")
    ResponseEntity<MemberLookupResponse> bySubject(@PathVariable UUID workspaceId, @RequestParam String sub) {
        return memberships.findBySubject(workspaceId, sub)
                .map(m -> ResponseEntity.ok(new MemberLookupResponse(m.workspaceId(), m.userId(), m.role())))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Returns which of the given user IDs (at most 100) are active members of the workspace, with their roles. */
    @Operation(summary = "Check which users are members",
            description = "Service-to-service. Returns which of up to 100 user IDs are active members, with their "
                    + "roles.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The active members among the IDs",
                    content = @Content(schema = @Schema(implementation = MemberListResponse.class))),
            @ApiResponse(responseCode = "400", description = "More than 100 user IDs",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid service token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "The service token lacks the membership:read permission",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping(path = InternalMembershipApi.PATH, params = "userId")
    MemberListResponse byUserIds(@PathVariable UUID workspaceId, @RequestParam("userId") List<UUID> userIds) {
        if (userIds.size() > MAX_BULK) {
            throw ApiException.badRequest("At most " + MAX_BULK + " user IDs per lookup.");
        }
        var members = memberships.findMembers(workspaceId, userIds).entrySet().stream()
                .map(e -> new MemberListResponse.Member(e.getKey(), e.getValue()))
                .toList();
        return new MemberListResponse(workspaceId, members);
    }

    /** Returns the workspace's name, settings and active members, or 404 when it does not exist. */
    @Operation(summary = "Describe a workspace",
            description = "Service-to-service. The workspace's type, name, timezone and currency (households only) and "
                    + "its active members. Used for local-time budgets and calendars, and to rebuild directories.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The workspace",
                    content = @Content(schema = @Schema(implementation = WorkspaceInfoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid service token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "The service token lacks the membership:read permission",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "No such workspace", content = @Content)
    })
    @GetMapping(InternalMembershipApi.WORKSPACE_PATH)
    ResponseEntity<WorkspaceInfoResponse> workspace(@PathVariable UUID workspaceId) {
        return memberships.findWorkspace(workspaceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
