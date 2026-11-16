package com.lifehub.identityworkspace.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.identityworkspace.model.dto.UpdateUserRequest;
import com.lifehub.identityworkspace.model.dto.UserDTO;
import com.lifehub.identityworkspace.model.dto.WorkspaceDTO;
import com.lifehub.identityworkspace.service.ProfileService;
import com.lifehub.starter.web.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** The signed-in user's own profile and workspaces. */
@RestController
@Tag(name = "Profile", description = "The signed-in user's profile and the workspaces they can switch between.")
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
class UserController {

    private final ProfileService profileService;

    /** GET /api/v1/me: returns the signed-in user's profile, creating the user on first login. */
    @Operation(summary = "Get my profile",
            description = "Returns the signed-in user's profile. The first call creates the LifeHub user and their "
                    + "personal workspace.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Your profile",
                    content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    UserDTO getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return profileService.getCurrentUser(jwt.getSubject());
    }

    /** PATCH /api/v1/me: updates the signed-in user's display name and/or email. */
    @Operation(summary = "Update my profile",
            description = "Changes your display name and/or email. Fields left out are unchanged; an empty string "
                    + "clears a field.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated",
                    content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping
    UserDTO updateCurrentUser(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateUserRequest request) {
        return profileService.updateCurrentUser(jwt.getSubject(), request);
    }

    /** Oldest membership first, so the personal workspace leads. */
    @Operation(summary = "List my workspaces",
            description = "The workspaces you can switch between, with your role in each. Your personal workspace "
                    + "comes first.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "One page of workspaces"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/workspaces")
    PageResponse<WorkspaceDTO> getWorkspaces(@AuthenticationPrincipal Jwt jwt,
            @PageableDefault(sort = "joinedAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return profileService.getWorkspaces(jwt.getSubject(), pageable);
    }
}
