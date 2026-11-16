package com.lifehub.identityworkspace.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.identityworkspace.service.LocalMembershipClient;
import com.lifehub.starter.membership.InternalMembershipApi;
import com.lifehub.starter.membership.InternalMembershipApi.MemberListResponse;
import com.lifehub.starter.membership.InternalMembershipApi.MemberLookupResponse;
import com.lifehub.starter.web.ApiException;

import lombok.RequiredArgsConstructor;

/**
 * The membership lookup other services call (ADR 0005). Never routed by the gateway; the starter only
 * accepts service tokens with the internal audience on {@code /internal/**}.
 */
@RestController
@PreAuthorize("hasAuthority('SCOPE_" + InternalMembershipApi.SCOPE + "')")
@RequiredArgsConstructor
class InternalMembershipController {

    private static final int MAX_BULK = 100;

    private final LocalMembershipClient memberships;

    /** Returns the Logto subject's active membership of the workspace, or 404 when they are not a member. */
    @GetMapping(path = InternalMembershipApi.PATH, params = "sub")
    ResponseEntity<MemberLookupResponse> bySubject(@PathVariable UUID workspaceId, @RequestParam String sub) {
        return memberships.findBySubject(workspaceId, sub)
                .map(m -> ResponseEntity.ok(new MemberLookupResponse(m.workspaceId(), m.userId(), m.role())))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Returns which of the given user IDs (at most 100) are active members of the workspace, with their roles. */
    @GetMapping(path = InternalMembershipApi.PATH, params = "userId")
    MemberListResponse byUserIds(@PathVariable UUID workspaceId, @RequestParam("userId") List<UUID> userIds) {
        if (userIds.size() > MAX_BULK) {
            throw ApiException.badRequest("At most " + MAX_BULK + " user IDs per lookup.");
        }
        var members = memberships.findMembers(workspaceId, userIds).entrySet().stream()
                .map(e -> new MemberListResponse.Member(e.getKey(), e.getValue()))
                .toList();
        return new MemberListResponse(workspaceId, members);
    }
}
