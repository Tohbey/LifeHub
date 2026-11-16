package com.lifehub.identityworkspace.controller;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/households")
@RequiredArgsConstructor
class HouseholdController {

    private final HouseholdService householdService;

    /** POST /api/v1/households: creates a household with the caller as owner. Returns 201 with its location. */
    @PostMapping
    ResponseEntity<HouseholdDTO> createHousehold(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateHouseholdRequest request) {
        HouseholdDTO household = householdService.createHousehold(jwt.getSubject(), request);
        return ResponseEntity.created(URI.create("/api/v1/households/" + household.id())).body(household);
    }

    /** GET /api/v1/households/{id}: returns a household's details and the caller's role in it. */
    @GetMapping("/{householdId}")
    HouseholdDTO getHousehold(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId) {
        return householdService.getHousehold(jwt.getSubject(), householdId);
    }

    /** PATCH /api/v1/households/{id}: changes a household's name, currency or timezone. Owners and admins only. */
    @PatchMapping("/{householdId}")
    HouseholdDTO updateHousehold(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId,
            @Valid @RequestBody UpdateHouseholdRequest request) {
        return householdService.updateHousehold(jwt.getSubject(), householdId, request);
    }

    /**
     * POST /api/v1/households/{id}/invitations: invites someone by email. Returns 201 with the one-time invite token.
     */
    @PostMapping("/{householdId}/invitations")
    ResponseEntity<InvitationDTO> inviteMember(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId,
            @Valid @RequestBody CreateInvitationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(householdService.inviteMember(jwt.getSubject(), householdId, request));
    }

    /** GET /api/v1/households/{id}/members: lists the household's active members, longest-standing first. */
    @GetMapping("/{householdId}/members")
    PageResponse<MemberDTO> getMembers(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId,
            @PageableDefault(sort = "joinedAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return householdService.getMembers(jwt.getSubject(), householdId, pageable);
    }

    /**
     * PATCH /api/v1/households/{id}/members/{userId}: changes a member's role, or hands over ownership. Owner only.
     */
    @PatchMapping("/{householdId}/members/{userId}")
    MemberDTO changeMemberRole(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId,
            @PathVariable UUID userId, @Valid @RequestBody ChangeRoleRequest request) {
        return householdService.changeMemberRole(jwt.getSubject(), householdId, userId, request);
    }

    /**
     * DELETE /api/v1/households/{id}/members/{userId}: removes a member, or leaves when the ID is the caller's own. Returns 204.
     */
    @DeleteMapping("/{householdId}/members/{userId}")
    ResponseEntity<Void> removeMember(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId,
            @PathVariable UUID userId) {
        householdService.removeMember(jwt.getSubject(), householdId, userId);
        return ResponseEntity.noContent().build();
    }
}
