package com.lifehub.notification.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.notification.model.dto.NotificationPreferenceDTO;
import com.lifehub.notification.model.dto.UpdatePreferencesRequest;
import com.lifehub.notification.service.PreferenceService;
import com.lifehub.starter.membership.WorkspaceAccess;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** The signed-in user's notification settings. */
@RestController
@Tag(name = "Notification preferences",
        description = "Which notifications the signed-in user gets, in the app and by email.")
@RequestMapping("/api/v1/me/notification-preferences")
@RequiredArgsConstructor
class PreferenceController {

    private final PreferenceService preferenceService;
    private final WorkspaceAccess access;

    /** GET /api/v1/me/notification-preferences: every type with its in-app and email setting. */
    @Operation(summary = "Get my notification settings",
            description = "Every notification type with its in-app and email setting. In-app is on and email off "
                    + "until you change them.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Your settings"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "You have not signed in to LifeHub yet",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    List<NotificationPreferenceDTO> getPreferences() {
        return preferenceService.getPreferences(access.currentUserId());
    }

    /** PUT /api/v1/me/notification-preferences: changes the listed types; others stay as they are. */
    @Operation(summary = "Change my notification settings",
            description = "Changes the listed types; others keep their current setting. Returns the full list.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Your settings after the change"),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "You have not signed in to LifeHub yet",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping
    List<NotificationPreferenceDTO> updatePreferences(@Valid @RequestBody UpdatePreferencesRequest request) {
        return preferenceService.updatePreferences(access.currentUserId(), request);
    }
}
