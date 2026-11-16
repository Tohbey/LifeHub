package com.lifehub.identityworkspace.controller;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.identityworkspace.model.dto.ChangeRoleRequest;
import com.lifehub.identityworkspace.model.dto.CreateHouseholdRequest;
import com.lifehub.identityworkspace.model.dto.CreateInvitationRequest;
import com.lifehub.identityworkspace.model.dto.HouseholdDTO;
import com.lifehub.identityworkspace.model.dto.InvitationDTO;
import com.lifehub.identityworkspace.model.dto.MemberDTO;
import com.lifehub.identityworkspace.model.dto.UpdateHouseholdRequest;
import com.lifehub.identityworkspace.service.HouseholdService;
import com.lifehub.starter.web.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Households", description = "Households, their members and roles, and invitations.")
@RequestMapping("/api/v1/households")
@RequiredArgsConstructor
class HouseholdController {

    private final HouseholdService householdService;

    /** POST /api/v1/households: creates a household with the caller as owner. Returns 201 with its location. */
    @Operation(summary = "Create a household",
            description = "Creates a household and its shared workspace, with you as the owner. Currency defaults to "
                    + "GBP and timezone to Europe/London.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Household created",
                    content = @Content(schema = @Schema(implementation = HouseholdDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    ResponseEntity<HouseholdDTO> createHousehold(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateHouseholdRequest request) {
        HouseholdDTO household = householdService.createHousehold(jwt.getSubject(), request);
        return ResponseEntity.created(URI.create("/api/v1/households/" + household.id())).body(household);
    }

    /** GET /api/v1/households/{id}: returns a household's details and the caller's role in it. */
    @Operation(summary = "Get a household", description = "Returns a household you belong to, with your role in it.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The household",
                    content = @Content(schema = @Schema(implementation = HouseholdDTO.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Household not found, or you are not a member of it",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{householdId}")
    HouseholdDTO getHousehold(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId) {
        return householdService.getHousehold(jwt.getSubject(), householdId);
    }

    /** PATCH /api/v1/households/{id}: changes a household's name, currency or timezone. Owners and admins only. */
    @Operation(summary = "Update a household",
            description = "Changes the name, currency or timezone. Owners and admins only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Household updated",
                    content = @Content(schema = @Schema(implementation = HouseholdDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Only owners and admins can change household settings",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Household not found, or you are not a member of it",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{householdId}")
    HouseholdDTO updateHousehold(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId,
            @Valid @RequestBody UpdateHouseholdRequest request) {
        return householdService.updateHousehold(jwt.getSubject(), householdId, request);
    }

    /**
     * POST /api/v1/households/{id}/invitations: invites someone by email. Returns 201 with the one-time invite token.
     */
    @Operation(summary = "Invite someone to a household",
            description = "Creates a 7-day, single-use invitation. The response carries the invite token once; it "
                    + "cannot be fetched again. Owners invite admins or members; admins invite members.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Invitation created, with its token",
                    content = @Content(schema = @Schema(implementation = InvitationDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Your role cannot invite at this role",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Household not found, or you are not a member of it",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{householdId}/invitations")
    ResponseEntity<InvitationDTO> inviteMember(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId,
            @Valid @RequestBody CreateInvitationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(householdService.inviteMember(jwt.getSubject(), householdId, request));
    }

    /** GET /api/v1/households/{id}/members: lists the household's active members, longest-standing first. */
    @Operation(summary = "List household members",
            description = "The active members of a household you belong to, longest-standing first.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "One page of members"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Household not found, or you are not a member of it",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{householdId}/members")
    PageResponse<MemberDTO> getMembers(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId,
            @PageableDefault(sort = "joinedAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return householdService.getMembers(jwt.getSubject(), householdId, pageable);
    }

    /**
     * PATCH /api/v1/households/{id}/members/{userId}: changes a member's role, or hands over ownership. Owner only.
     */
    @Operation(summary = "Change a member's role",
            description = "Owner only. Setting OWNER hands over ownership: you become an admin.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role changed",
                    content = @Content(schema = @Schema(implementation = MemberDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Only the owner can change roles, and not their own",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Household or member not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{householdId}/members/{userId}")
    MemberDTO changeMemberRole(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId,
            @PathVariable UUID userId, @Valid @RequestBody ChangeRoleRequest request) {
        return householdService.changeMemberRole(jwt.getSubject(), householdId, userId, request);
    }

    /**
     * DELETE /api/v1/households/{id}/members/{userId}: removes a member, or leaves when the ID is the caller's own.
     * Returns 204.
     */
    @Operation(summary = "Remove a member, or leave",
            description = "Owners remove anyone else, admins remove members. Using your own user ID means leaving; "
                    + "the owner cannot leave.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Member removed", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403",
                    description = "Your role cannot remove this member, or you are the owner",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Household or member not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{householdId}/members/{userId}")
    ResponseEntity<Void> removeMember(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId,
            @PathVariable UUID userId) {
        householdService.removeMember(jwt.getSubject(), householdId, userId);
        return ResponseEntity.noContent().build();
    }
}
