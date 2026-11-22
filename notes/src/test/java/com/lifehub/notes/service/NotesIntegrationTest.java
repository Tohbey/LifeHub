package com.lifehub.notes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import com.lifehub.starter.id.Ids;
import com.lifehub.starter.membership.Membership;
import com.lifehub.starter.membership.MembershipClient;
import com.lifehub.starter.membership.WorkspaceRole;

import tools.jackson.databind.json.JsonMapper;

/** Real MySQL with membership mocked: lists, items, the resource-keyed checks and the outbox. Skipped without Docker. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class NotesIntegrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4").withDatabaseName("lifehub_notes");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.liquibase.user", MYSQL::getUsername);
        registry.add("spring.liquibase.password", MYSQL::getPassword);
        registry.add("lifehub.security.issuer", () -> "https://auth.lifehub.test/oidc");
        registry.add("lifehub.security.audience", () -> "https://api.lifehub.test");
        registry.add("lifehub.security.internal-audience", () -> "https://internal.lifehub.test");
        registry.add("lifehub.security.ca-certificate", () -> "");
        registry.add("lifehub.service-auth.client-id", () -> "test");
        registry.add("lifehub.service-auth.client-secret", () -> "test");
        registry.add("lifehub.outbox.relay-enabled", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    JsonMapper json;

    @MockitoBean
    MembershipClient membershipClient;

    private final UUID household = Ids.newId();

    /** Alice and Bob are members; everyone else is not. */
    @BeforeEach
    void members() {
        when(membershipClient.findBySubject(eq(household), anyString())).thenReturn(Optional.empty());
        when(membershipClient.findBySubject(household, "alice"))
                .thenReturn(Optional.of(new Membership(household, Ids.newId(), WorkspaceRole.MEMBER)));
        when(membershipClient.findBySubject(household, "bob"))
                .thenReturn(Optional.of(new Membership(household, Ids.newId(), WorkspaceRole.MEMBER)));
    }

    @Test
    void aPersonalNoteIsInvisibleToOtherMembersAndASharedOneIsNot() throws Exception {
        String diary = id(mvc.perform(post(path("/notes")).with(user("alice")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Diary\",\"body\":\"private\",\"visibility\":\"PERSONAL\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String wifi = id(mvc.perform(post(path("/notes")).with(user("alice")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Wi-Fi\",\"body\":\"pass: hunter2\",\"visibility\":\"SHARED\","
                                + "\"pinned\":true,\"tags\":[\" Home \",\"home\",\"Admin\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tags[0]").value("admin"))
                .andExpect(jsonPath("$.tags.length()").value(2))
                .andReturn().getResponse().getContentAsString());

        mvc.perform(get(path("/notes")).with(user("bob")))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].id").value(wifi));
        mvc.perform(get(path("/notes/" + diary)).with(user("bob"))).andExpect(status().isNotFound());
        mvc.perform(get(path("/notes")).param("q", "HUNTER").with(user("bob"))).andExpect(jsonPath("$.totalItems").value(1));
        mvc.perform(get(path("/notes")).param("tag", "Home").with(user("alice"))).andExpect(jsonPath("$.totalItems").value(1));

        // Bob may edit the shared note's text, but not make it personal or delete it.
        mvc.perform(put(path("/notes/" + wifi)).with(user("bob")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Wi-Fi\",\"body\":\"pass: new\",\"visibility\":\"SHARED\"}"))
                .andExpect(status().isOk());
        mvc.perform(put(path("/notes/" + wifi)).with(user("bob")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Wi-Fi\",\"visibility\":\"PERSONAL\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete(path("/notes/" + wifi)).with(user("bob"))).andExpect(status().isForbidden());

        mvc.perform(get(path("/tags")).with(user("bob")))
                .andExpect(jsonPath("$[0].tag").value("admin"))
                .andExpect(jsonPath("$[1].tag").value("home"))
                .andExpect(jsonPath("$[1].notes").value(1));
    }

    @Test
    void bookmarksAcceptOnlyHttpLinks() throws Exception {
        mvc.perform(post(path("/bookmarks")).with(user("alice")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"javascript:alert(1)\",\"visibility\":\"SHARED\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(path("/bookmarks")).with(user("alice")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://www.gov.uk/bin-collection\",\"visibility\":\"SHARED\",\"tags\":[\"bins\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("www.gov.uk"));
        mvc.perform(get(path("/bookmarks")).param("tag", "bins").with(user("bob")))
                .andExpect(jsonPath("$.items[0].url").value("https://www.gov.uk/bin-collection"));
        mvc.perform(get(path("/bookmarks")).with(user("mallory"))).andExpect(status().isNotFound());
    }

    private String path(String rest) {
        return "/api/v1/workspaces/" + household + rest;
    }

    /** The "id" field of a JSON response. */
    private String id(String body) {
        return json.readTree(body).get("id").asString();
    }

    /** A user token with this subject. */
    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor user(String sub) {
        return jwt().jwt(j -> j.subject(sub));
    }
}
