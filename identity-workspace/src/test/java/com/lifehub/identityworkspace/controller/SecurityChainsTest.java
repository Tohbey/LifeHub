package com.lifehub.identityworkspace.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.lifehub.identityworkspace.model.dto.UserDTO;
import com.lifehub.identityworkspace.service.LocalMembershipClient;
import com.lifehub.identityworkspace.service.ProfileService;
import com.lifehub.starter.membership.InternalMembershipApi;
import com.lifehub.starter.membership.Membership;
import com.lifehub.starter.membership.WorkspaceRole;
import com.lifehub.starter.security.LifehubSecurityAutoConfiguration;
import com.lifehub.starter.web.WebAutoConfiguration;

/**
 * The starter's security chains as identity-workspace uses them, without a database: which paths need a
 * token, and that the internal membership API needs the membership:read scope. Runs without Docker.
 */
@WebMvcTest(controllers = { InternalMembershipController.class, InternalUserController.class, UserController.class })
@ImportAutoConfiguration({ LifehubSecurityAutoConfiguration.class, WebAutoConfiguration.class })
@TestPropertySource(properties = {
        "lifehub.security.issuer=https://auth.lifehub.test/oidc",
        "lifehub.security.audience=https://api.lifehub.test",
        "lifehub.security.ca-certificate=",
        "lifehub.security.internal-audience=https://internal.lifehub.test" })
class SecurityChainsTest {

    private static final String MEMBERS = "/internal/v1/workspaces/" + UUID.randomUUID() + "/members";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    LocalMembershipClient memberships;

    @MockitoBean
    ProfileService profileService;

    @Test
    void apiNeedsAToken() throws Exception {
        mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void apiWithATokenReachesTheController() throws Exception {
        when(profileService.getCurrentUser("user-1")).thenReturn(new UserDTO(UUID.randomUUID(), null, null, null));
        mvc.perform(get("/api/v1/me").with(jwt().jwt(j -> j.subject("user-1")))).andExpect(status().isOk());
    }

    @Test
    void internalApiNeedsAToken() throws Exception {
        mvc.perform(get(MEMBERS).param("sub", "x")).andExpect(status().isUnauthorized());
    }

    @Test
    void internalApiRefusesATokenWithoutTheMembershipScope() throws Exception {
        mvc.perform(get(MEMBERS).param("sub", "x").with(jwt())).andExpect(status().isForbidden());
    }

    @Test
    void internalApiAnswersATokenWithTheMembershipScope() throws Exception {
        when(memberships.findBySubject(any(), anyString())).thenReturn(
                Optional.of(new Membership(UUID.randomUUID(), UUID.randomUUID(), WorkspaceRole.MEMBER)));
        mvc.perform(get(MEMBERS).param("sub", "x").with(scope())).andExpect(status().isOk());
    }

    @Test
    void internalBulkLookupIsCapped() throws Exception {
        var request = get(MEMBERS).with(scope());
        for (int i = 0; i < 101; i++) {
            request.param("userId", UUID.randomUUID().toString());
        }
        mvc.perform(request).andExpect(status().isBadRequest());
    }

    @Test
    void internalUserLookupNeedsTheMembershipScope() throws Exception {
        UUID userId = UUID.randomUUID();
        when(memberships.findUserIdBySubject("user-1")).thenReturn(Optional.of(userId));

        mvc.perform(get("/internal/v1/users").param("sub", "user-1").with(jwt())).andExpect(status().isForbidden());
        mvc.perform(get("/internal/v1/users").param("sub", "user-1").with(scope())).andExpect(status().isOk());
        mvc.perform(get("/internal/v1/users").param("sub", "nobody").with(scope())).andExpect(status().isNotFound());
    }

    @Test
    void otherPathsAreDenied() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isForbidden());
        mvc.perform(get("/anything").with(jwt())).andExpect(status().isForbidden());
    }

    /** A service token carrying the membership:read scope. */
    private static JwtRequestPostProcessor scope() {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_" + InternalMembershipApi.SCOPE));
    }
}
