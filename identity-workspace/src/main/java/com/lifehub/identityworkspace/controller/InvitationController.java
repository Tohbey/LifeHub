package com.lifehub.identityworkspace.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.identityworkspace.model.dto.WorkspaceDTO;
import com.lifehub.identityworkspace.service.InvitationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
class InvitationController {

    private final InvitationService invitationService;

    /** POST /api/v1/invitations/{token}/accept: joins the household the invite token belongs to. */
    @PostMapping("/{token}/accept")
    WorkspaceDTO acceptInvitation(@AuthenticationPrincipal Jwt jwt, @PathVariable String token) {
        return invitationService.acceptInvitation(jwt.getSubject(), token);
    }
}
