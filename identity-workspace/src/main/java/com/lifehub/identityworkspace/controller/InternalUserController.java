package com.lifehub.identityworkspace.controller;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.identityworkspace.service.LocalMembershipClient;
import com.lifehub.starter.membership.InternalMembershipApi;
import com.lifehub.starter.membership.InternalMembershipApi.UserLookupResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Lets other services turn the Logto subject of a token into a LifeHub user ID, for endpoints about the
 * caller rather than a workspace. Never routed by the gateway; needs a service token with membership:read.
 */
@RestController
@Tag(name = "Internal: users",
        description = "Service-to-service user lookups. Needs a service token with membership:read.")
@PreAuthorize("hasAuthority('SCOPE_" + InternalMembershipApi.SCOPE + "')")
@RequiredArgsConstructor
class InternalUserController {

    private final LocalMembershipClient memberships;

    /** Returns the user ID for this Logto subject, or 404 if nobody has signed in with it. */
    @Operation(summary = "Look up a user ID by Logto subject",
            description = "Service-to-service. Turns the subject of a user token into a LifeHub user ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The user ID",
                    content = @Content(schema = @Schema(implementation = UserLookupResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid service token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "The service token lacks the membership:read permission",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404",
                    description = "Nobody has signed in with this subject", content = @Content)
    })
    @GetMapping(path = InternalMembershipApi.USERS_PATH, params = "sub")
    ResponseEntity<UserLookupResponse> bySubject(@RequestParam String sub) {
        return memberships.findUserIdBySubject(sub)
                .map(id -> ResponseEntity.ok(new UserLookupResponse(id)))
                .orElse(ResponseEntity.notFound().build());
    }
}
