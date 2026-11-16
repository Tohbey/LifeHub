package com.lifehub.shoppingmeals.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class ShoppingIntegrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4").withDatabaseName("lifehub_shopping");

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

    /** Alice is a member; everyone else is not. */
    @BeforeEach
    void members() {
        when(membershipClient.findBySubject(eq(household), anyString())).thenReturn(Optional.empty());
        when(membershipClient.findBySubject(household, "alice"))
                .thenReturn(Optional.of(new Membership(household, Ids.newId(), WorkspaceRole.MEMBER)));
    }

    @Test
    void itemsAreAddedTickedOffAndRemoved() throws Exception {
        String listId = id(mvc.perform(post("/api/v1/workspaces/" + household + "/shopping-lists").with(user("alice"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Weekly shop\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String itemId = id(mvc.perform(post("/api/v1/shopping-lists/" + listId + "/items").with(user("alice"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Milk\",\"quantity\":\"2\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());

        mvc.perform(get("/api/v1/workspaces/" + household + "/shopping-lists").with(user("alice")))
                .andExpect(jsonPath("$.items[0].openItems").value(1));
        mvc.perform(patch("/api/v1/shopping-items/" + itemId).with(user("alice"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"purchased\":true}"))
                .andExpect(jsonPath("$.purchased").value(true));
        mvc.perform(get("/api/v1/shopping-lists/" + listId + "/items").param("open", "true").with(user("alice")))
                .andExpect(jsonPath("$.totalItems").value(0));
        mvc.perform(delete("/api/v1/shopping-items/" + itemId).with(user("alice"))).andExpect(status().isNoContent());

        assertThat(jdbc.sql("SELECT event_type FROM outbox ORDER BY created_at, id").query(String.class).list())
                .containsExactly("shopping.item.added", "shopping.item.purchased");
    }

    @Test
    void aNonMemberCannotTellAListExists() throws Exception {
        String listId = id(mvc.perform(post("/api/v1/workspaces/" + household + "/shopping-lists").with(user("alice"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Private\"}"))
                .andReturn().getResponse().getContentAsString());

        String existing = mvc.perform(get("/api/v1/shopping-lists/" + listId + "/items").with(user("mallory")))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        String missing = mvc.perform(get("/api/v1/shopping-lists/" + UUID.randomUUID() + "/items").with(user("mallory")))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(existing).get("detail")).isEqualTo(json.readTree(missing).get("detail"));
        assertThat(List.of(existing)).isNotEmpty();
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
