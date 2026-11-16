package com.lifehub.identityworkspace.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

import lombok.RequiredArgsConstructor;

/** The signed-in user's own profile and workspaces. */
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
class UserController {

    private final ProfileService profileService;

    /** GET /api/v1/me: returns the signed-in user's profile, creating the user on first login. */
    @GetMapping
    UserDTO getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return profileService.getCurrentUser(jwt.getSubject());
    }

    /** PATCH /api/v1/me: updates the signed-in user's display name and/or email. */
    @PatchMapping
    UserDTO updateCurrentUser(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateUserRequest request) {
        return profileService.updateCurrentUser(jwt.getSubject(), request);
    }

    /** Oldest membership first, so the personal workspace leads. */
    @GetMapping("/workspaces")
    PageResponse<WorkspaceDTO> getWorkspaces(@AuthenticationPrincipal Jwt jwt,
            @PageableDefault(sort = "joinedAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return profileService.getWorkspaces(jwt.getSubject(), pageable);
    }
}
