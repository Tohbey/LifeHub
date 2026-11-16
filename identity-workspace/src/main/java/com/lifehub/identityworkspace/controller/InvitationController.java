package com.lifehub.identityworkspace.controller;

import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.identityworkspace.model.dto.WorkspaceDTO;
import com.lifehub.identityworkspace.service.InvitationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Invitations", description = "Joining a household with an invite link.")
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
class InvitationController {

    private final InvitationService invitationService;

    /** POST /api/v1/invitations/{token}/accept: joins the household the invite token belongs to. */
    @Operation(summary = "Accept an invitation",
            description = "Joins the household the invite token belongs to, with the invited role. Each token works "
                    + "once.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Joined; the household workspace",
                    content = @Content(schema = @Schema(implementation = WorkspaceDTO.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "The invitation is invalid, used or expired",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "You are already a member of this household",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{token}/accept")
    WorkspaceDTO acceptInvitation(@AuthenticationPrincipal Jwt jwt, @PathVariable String token) {
        return invitationService.acceptInvitation(jwt.getSubject(), token);
    }
}
