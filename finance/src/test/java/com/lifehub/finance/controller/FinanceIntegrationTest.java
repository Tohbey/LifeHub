package com.lifehub.finance.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import com.lifehub.starter.id.Ids;
import com.lifehub.starter.membership.Membership;
import com.lifehub.starter.membership.MembershipClient;
import com.lifehub.starter.membership.WorkspaceRole;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Real MySQL with identity-workspace's membership API mocked: Liquibase, mapping, splits, permissions,
 * budgets and the outbox. Skipped without Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class FinanceIntegrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4").withDatabaseName("lifehub_finance");

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
    private final UUID alice = Ids.newId();
    private final UUID bob = Ids.newId();
    private final UUID carol = Ids.newId();

    /** Alice owns the household and Bob is a member; Carol is not a member. */
    @BeforeEach
    void members() {
        Map<UUID, WorkspaceRole> members = Map.of(alice, WorkspaceRole.OWNER, bob, WorkspaceRole.MEMBER);
        when(membershipClient.findBySubject(eq(household), anyString())).thenReturn(Optional.empty());
        when(membershipClient.findBySubject(household, "alice")).thenReturn(Optional.of(new Membership(household, alice, WorkspaceRole.OWNER)));
        when(membershipClient.findBySubject(household, "bob")).thenReturn(Optional.of(new Membership(household, bob, WorkspaceRole.MEMBER)));
        when(membershipClient.findMembers(eq(household), any())).thenAnswer(inv -> ((Collection<UUID>) inv.getArgument(1)).stream()
                .filter(members::containsKey).collect(Collectors.toMap(id -> id, members::get)));
    }

    @Test
    void aSharedExpenseIsRecordedWithItsSplitsAndPublished() throws Exception {
        String id = body(mvc.perform(post(path("/transactions")).with(user("alice")).contentType(MediaType.APPLICATION_JSON)
                .content(expense("120.00", split(alice, "60.00"), split(bob, "60.00"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.splits.length()").value(2))
                .andExpect(jsonPath("$.payerUserId").value(alice.toString()))).get("id").asString();

        mvc.perform(get(path("/transactions/" + id)).with(user("bob"))).andExpect(status().isOk());
        assertThat(outbox()).contains("transaction.created");
    }

    @Test
    void splitsMustAddUpAndOnlyIncludeMembers() throws Exception {
        mvc.perform(post(path("/transactions")).with(user("alice")).contentType(MediaType.APPLICATION_JSON)
                .content(expense("120.00", split(alice, "60.00"), split(bob, "50.00"))))
                .andExpect(status().isBadRequest());
        mvc.perform(post(path("/transactions")).with(user("alice")).contentType(MediaType.APPLICATION_JSON)
                .content(expense("120.00", split(alice, "60.00"), split(carol, "60.00"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonMembersSeeNothingAndMembersCannotChangeOthersTransactions() throws Exception {
        String id = body(mvc.perform(post(path("/transactions")).with(user("alice")).contentType(MediaType.APPLICATION_JSON)
                .content(expense("10.00"))).andExpect(status().isCreated())).get("id").asString();

        mvc.perform(get(path("/transactions")).with(user("carol"))).andExpect(status().isNotFound());
        mvc.perform(patch(path("/transactions/" + id)).with(user("bob")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":5}")).andExpect(status().isForbidden());
        mvc.perform(delete(path("/transactions/" + id)).with(user("alice"))).andExpect(status().isNoContent());
        mvc.perform(get(path("/transactions/" + id)).with(user("alice"))).andExpect(status().isNotFound());
    }

    @Test
    void crossingABudgetPublishesEachThresholdOnce() throws Exception {
        String category = body(mvc.perform(post(path("/finance/categories")).with(user("alice"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"EXPENSE\",\"name\":\"Food\"}"))
                .andExpect(status().isCreated())).get("id").asString();
        mvc.perform(post(path("/budgets")).with(user("bob")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryId\":\"%s\",\"monthlyLimit\":100,\"currency\":\"GBP\"}".formatted(category)))
                .andExpect(status().isForbidden());
        mvc.perform(post(path("/budgets")).with(user("alice")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryId\":\"%s\",\"monthlyLimit\":100,\"currency\":\"GBP\"}".formatted(category)))
                .andExpect(status().isCreated());

        for (String amount : List.of("85.00", "10.00", "10.00", "10.00")) {
            mvc.perform(post(path("/transactions")).with(user("alice")).contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"type":"EXPENSE","description":"Food","amount":%s,"currency":"GBP","categoryId":"%s",
                             "occurredAt":"%s"}""".formatted(amount, category, java.time.Instant.now())))
                    .andExpect(status().isCreated());
        }

        assertThat(outbox().stream().filter("budget.threshold.exceeded"::equals)).hasSize(2);
        mvc.perform(get(path("/budgets")).with(user("bob")))
                .andExpect(jsonPath("$.items[0].spent").value(115.00));
        mvc.perform(get(path("/finance/summary")).with(user("bob")))
                .andExpect(jsonPath("$.currencies[0].expenseByCategory[0].name").value("Food"));
    }

    /** A path under the test household. */
    private String path(String suffix) {
        return "/api/v1/workspaces/" + household + suffix;
    }

    /** An expense body in GBP, now, with these splits. */
    private static String expense(String amount, String... splits) {
        return """
                {"type":"EXPENSE","description":"Electricity","amount":%s,"currency":"GBP","occurredAt":"2026-11-09T10:00:00Z",
                 "splits":[%s]}""".formatted(amount, String.join(",", splits));
    }

    /** One split as JSON. */
    private static String split(UUID user, String amount) {
        return "{\"userId\":\"%s\",\"amount\":%s}".formatted(user, amount);
    }

    /** The event types in the outbox. */
    private List<String> outbox() {
        return jdbc.sql("SELECT event_type FROM outbox").query(String.class).list();
    }

    /** Reads the response body as JSON. */
    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    /** A user token with this subject. */
    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor user(String sub) {
        return jwt().jwt(j -> j.subject(sub));
    }
}
