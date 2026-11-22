package com.lifehub.identityworkspace.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.identityworkspace.service.LocalMembershipClient;
import com.lifehub.starter.membership.InternalMembershipApi;
import com.lifehub.starter.membership.InternalMembershipApi.UserLookupResponse;
import com.lifehub.starter.membership.InternalMembershipApi.UserNamesResponse;
import com.lifehub.starter.web.ApiException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Lets other services turn the Logto subject of a token into a LifeHub user ID (for endpoints about the caller
 * rather than a workspace), and user IDs into display names (to show people by name). Never routed by the
 * gateway; needs a service token with membership:read.
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

    /** Returns the display names of up to 100 users. Unknown IDs are left out; a user without a name maps to null. */
    @Operation(summary = "Look up display names by user ID",
            description = "Service-to-service. Turns up to 100 LifeHub user IDs into display names, for showing people "
                    + "by name, for example in the audit log.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The names of the known users",
                    content = @Content(schema = @Schema(implementation = UserNamesResponse.class))),
            @ApiResponse(responseCode = "400", description = "More than 100 user IDs",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid service token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "The service token lacks the membership:read permission",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping(path = InternalMembershipApi.USERS_PATH, params = "userId")
    UserNamesResponse displayNames(@RequestParam("userId") List<UUID> userIds) {
        if (userIds.size() > InternalMembershipApi.MAX_BULK) {
            throw ApiException.badRequest("At most " + InternalMembershipApi.MAX_BULK + " user IDs per lookup.");
        }
        return new UserNamesResponse(memberships.findDisplayNames(userIds));
    }
}
