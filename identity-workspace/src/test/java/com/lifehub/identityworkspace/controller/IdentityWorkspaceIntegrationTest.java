package com.lifehub.identityworkspace.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs the real service against MySQL: Liquibase changelog, Hibernate schema validation, security chains,
 * membership rules and outbox writes. Tokens are injected with {@code jwt()}, so signature checks are
 * not exercised here. Skipped when Docker is not available.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class IdentityWorkspaceIntegrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4").withDatabaseName("lifehub_identity");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.liquibase.user", MYSQL::getUsername);
        registry.add("spring.liquibase.password", MYSQL::getPassword);
        registry.add("lifehub.security.issuer", () -> "https://auth.lifehub.test/oidc");
        registry.add("lifehub.security.audience", () -> "https://api.lifehub.test");
        // Tests never call Logto, so they must not depend on a local CA file.
        registry.add("lifehub.security.ca-certificate", () -> "");
        registry.add("lifehub.security.internal-audience", () -> "https://internal.lifehub.test");
        registry.add("lifehub.outbox.relay-enabled", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    JsonMapper json;

    @Test
    void firstRequestProvisionsUserAndPersonalWorkspaceOnce() throws Exception {
        String sub = newSubject();

        String firstId = body(mvc.perform(get("/api/v1/me").with(user(sub))).andExpect(status().isOk())).get("id").asString();
        String secondId = body(mvc.perform(get("/api/v1/me").with(user(sub))).andExpect(status().isOk())).get("id").asString();
        assertThat(secondId).isEqualTo(firstId);

        mvc.perform(get("/api/v1/me/workspaces").with(user(sub)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].type").value("PERSONAL"))
                .andExpect(jsonPath("$.items[0].role").value("OWNER"));

        assertThat(eventTypes(firstId)).containsExactly("user.created");
    }

    @Test
    void concurrentFirstRequestsCreateOneUser() throws Exception {
        String sub = newSubject();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Callable<String>> calls = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                calls.add(() -> body(mvc.perform(get("/api/v1/me").with(user(sub))).andExpect(status().isOk()))
                        .get("id").asString());
            }
            List<String> ids = new ArrayList<>();
            for (Future<String> f : pool.invokeAll(calls)) {
                ids.add(f.get());
            }
            assertThat(ids).containsOnly(ids.get(0));
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void householdLifecycle() throws Exception {
        String alice = newSubject();
        String bob = newSubject();
        String aliceId = userId(alice);
        String bobId = userId(bob);

        // Alice creates a household and is its owner.
        String householdId = body(mvc.perform(post("/api/v1/households").with(user(alice))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Fafowora Home\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.yourRole").value("OWNER"))
                .andExpect(jsonPath("$.defaultCurrency").value("GBP"))).get("id").asString();

        // Bob cannot see it.
        mvc.perform(get("/api/v1/households/" + householdId).with(user(bob))).andExpect(status().isNotFound());

        // Alice invites Bob, Bob accepts.
        String token = body(mvc.perform(post("/api/v1/households/" + householdId + "/invitations").with(user(alice))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"Bob@Example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("bob@example.com"))).get("token").asString();
        mvc.perform(post("/api/v1/invitations/" + token + "/accept").with(user(bob)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(householdId))
                .andExpect(jsonPath("$.role").value("MEMBER"));

        // The token is single use.
        mvc.perform(post("/api/v1/invitations/" + token + "/accept").with(user(newSubject())))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/v1/households/" + householdId + "/members").with(user(bob)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2));

        // A member cannot invite or change roles.
        mvc.perform(post("/api/v1/households/" + householdId + "/invitations").with(user(bob))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"x@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/households/" + householdId + "/members/" + aliceId).with(user(bob))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"MEMBER\"}"))
                .andExpect(status().isForbidden());

        // The owner cannot leave, but can hand over ownership and then leave.
        mvc.perform(delete("/api/v1/households/" + householdId + "/members/" + aliceId).with(user(alice)))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/households/" + householdId + "/members/" + bobId).with(user(alice))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"OWNER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OWNER"));
        mvc.perform(get("/api/v1/households/" + householdId).with(user(alice)))
                .andExpect(jsonPath("$.yourRole").value("ADMIN"));
        mvc.perform(delete("/api/v1/households/" + householdId + "/members/" + aliceId).with(user(alice)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/households/" + householdId).with(user(alice))).andExpect(status().isNotFound());

        assertThat(eventTypes(householdId)).containsExactly(
                "household.created",
                "member.invited",
                "member.added",
                "member.role.changed",
                "member.role.changed",
                "member.removed");
    }

    @Test
    void anInviteLinkUsedByTwoPeopleAtOnceAddsOnlyOne() throws Exception {
        String alice = newSubject();
        String householdId = createHousehold(alice);
        String token = body(mvc.perform(post("/api/v1/households/" + householdId + "/invitations").with(user(alice))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"guest@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated())).get("token").asString();
        List<String> guests = List.of(newSubject(), newSubject(), newSubject(), newSubject());
        for (String guest : guests) {
            userId(guest);
        }

        ExecutorService pool = Executors.newFixedThreadPool(guests.size());
        try {
            List<Callable<Integer>> calls = new ArrayList<>();
            for (String guest : guests) {
                calls.add(() -> mvc.perform(post("/api/v1/invitations/" + token + "/accept").with(user(guest)))
                        .andReturn().getResponse().getStatus());
            }
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> f : pool.invokeAll(calls)) {
                statuses.add(f.get());
            }
            assertThat(statuses).containsOnlyOnce(200).filteredOn(s -> s != 200).containsOnly(404);
        } finally {
            pool.shutdown();
        }
        mvc.perform(get("/api/v1/households/" + householdId + "/members").with(user(alice)))
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void householdEndpointsDoNotReachPersonalWorkspaces() throws Exception {
        String alice = newSubject();
        String personalId = body(mvc.perform(get("/api/v1/me/workspaces").with(user(alice))))
                .get("items").get(0).get("id").asString();

        mvc.perform(get("/api/v1/households/" + personalId).with(user(alice))).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/households/" + personalId + "/invitations").with(user(alice))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"x@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void internalMembershipLookupNeedsAServiceTokenWithScope() throws Exception {
        String alice = newSubject();
        String householdId = createHousehold(alice);
        String path = "/internal/v1/workspaces/" + householdId + "/members";

        mvc.perform(get(path).param("sub", alice).with(service()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.userId").value(userId(alice)));
        mvc.perform(get(path).param("sub", newSubject()).with(service())).andExpect(status().isNotFound());
        mvc.perform(get(path).param("userId", userId(alice), UUID.randomUUID().toString()).with(service()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(1));

        // A token without the membership scope is refused.
        mvc.perform(get(path).param("sub", alice).with(user(alice))).andExpect(status().isForbidden());
        mvc.perform(get(path).param("sub", alice)).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidRequestsReturnProblemDetails() throws Exception {
        String alice = newSubject();

        mvc.perform(post("/api/v1/households").with(user(alice))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Home\",\"defaultCurrency\":\"pounds\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mvc.perform(post("/api/v1/households").with(user(alice))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Home\",\"timezone\":\"Mars/Olympus\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown timezone: Mars/Olympus"));
        mvc.perform(post("/api/v1/invitations/not-a-real-token/accept").with(user(alice)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/something-else")).andExpect(status().isForbidden());
    }

    private String createHousehold(String sub) throws Exception {
        return body(mvc.perform(post("/api/v1/households").with(user(sub))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Home\"}"))
                .andExpect(status().isCreated())).get("id").asString();
    }

    private String userId(String sub) throws Exception {
        return body(mvc.perform(get("/api/v1/me").with(user(sub)))).get("id").asString();
    }

    private List<String> eventTypes(String key) {
        return jdbc.sql("SELECT event_type FROM outbox WHERE message_key = ? ORDER BY created_at, id")
                .param(key)
                .query(String.class)
                .list();
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private static JwtRequestPostProcessor user(String sub) {
        return jwt().jwt(j -> j.subject(sub));
    }

    private static JwtRequestPostProcessor service() {
        return jwt().jwt(j -> j.subject("finance-m2m-app"))
                .authorities(new SimpleGrantedAuthority("SCOPE_membership:read"));
    }

    private static String newSubject() {
        return "logto-" + UUID.randomUUID();
    }
}
