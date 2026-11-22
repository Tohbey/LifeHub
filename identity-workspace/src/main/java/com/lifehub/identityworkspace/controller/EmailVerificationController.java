package com.lifehub.identityworkspace.controller;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.identityworkspace.model.dto.UserDTO;
import com.lifehub.identityworkspace.service.EmailVerificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** Verifying the signed-in user's email address. */
@RestController
@Tag(name = "Email verification", description = "Proving the profile email is yours, with a one-time link.")
@RequestMapping("/api/v1/me/email-verification")
@RequiredArgsConstructor
class EmailVerificationController {

    private final EmailVerificationService verifications;

    /** POST /api/v1/me/email-verification: sends a verification link to the profile email. Returns 202. */
    @Operation(summary = "Send a verification link",
            description = "Mails a one-time link (valid 24 hours) to your profile email. At most one a minute.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Link sent", content = @Content),
            @ApiResponse(responseCode = "400", description = "Your profile has no email",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Your email is already verified",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "429", description = "A link was sent less than a minute ago",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "Email sending is not set up, or failed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    ResponseEntity<Void> requestVerification(@AuthenticationPrincipal Jwt jwt) {
        verifications.requestVerification(jwt.getSubject());
        return ResponseEntity.accepted().build();
    }

    /** POST /api/v1/me/email-verification/{token}: uses a link from the email. */
    @Operation(summary = "Confirm an email address",
            description = "Uses the link from the email. You must be signed in as the user it was sent to.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email verified",
                    content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "The link is invalid, used, expired or not yours",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Your email changed after the link was sent",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{token}")
    UserDTO confirm(@AuthenticationPrincipal Jwt jwt, @PathVariable String token) {
        return verifications.confirm(jwt.getSubject(), token);
    }
}
