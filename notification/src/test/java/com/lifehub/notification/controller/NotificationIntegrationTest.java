package com.lifehub.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import com.lifehub.notification.service.EventDispatcher;
import com.lifehub.starter.events.EventEnvelope;
import com.lifehub.starter.id.Ids;
import com.lifehub.starter.membership.MembershipClient;

import tools.jackson.databind.json.JsonMapper;

/**
 * Real MySQL: Liquibase, entity mapping, the directory, dispatch, grouping, duplicates and the API.
 * Events are handed to the dispatcher directly; Kafka is not started. Skipped without Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class NotificationIntegrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4").withDatabaseName("lifehub_notification");

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
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    EventDispatcher dispatcher;

    @Autowired
    JsonMapper json;

    @MockitoBean
    MembershipClient membershipClient;

    private final UUID household = Ids.newId();
    private final UUID alice = Ids.newId();
    private final UUID bob = Ids.newId();

    @BeforeEach
    void setUpHousehold() {
        when(membershipClient.findUserIdBySubject("bob")).thenReturn(Optional.of(bob));
        when(membershipClient.findUserIdBySubject("alice")).thenReturn(Optional.of(alice));
        dispatch("user.created", null, alice, Map.of("userId", alice, "personalWorkspaceId", Ids.newId(), "displayName", "Alice"));
        dispatch("user.created", null, bob, Map.of("userId", bob, "personalWorkspaceId", Ids.newId(), "displayName", "Bob"));
        dispatch("household.created", household, alice, Map.of("householdId", household, "name", "Home", "ownerUserId", alice));
        dispatch("member.added", household, bob, Map.of("householdId", household, "userId", bob, "role", "MEMBER"));
    }

    @Test
    void anAssignedTaskReachesTheAssigneeOnceEvenIfDeliveredTwice() throws Exception {
        UUID taskId = Ids.newId();
        var event = envelope("task.assigned", household, alice,
                Map.of("taskId", taskId, "title", "Pay bill", "assigneeUserId", bob));
        dispatcher.dispatch(event);
        dispatcher.dispatch(event);

        mvc.perform(get("/api/v1/me/notifications").with(jwt().jwt(j -> j.subject("bob"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.type == 'TASK_ASSIGNED')].title").value("Task assigned: Pay bill"));
        mvc.perform(get("/api/v1/me/notifications").with(jwt().jwt(j -> j.subject("alice"))))
                .andExpect(jsonPath("$.items[?(@.type == 'TASK_ASSIGNED')]").isEmpty());
    }

    @Test
    void shoppingUpdatesForOneListAreGrouped() throws Exception {
        UUID list = Ids.newId();
        for (String item : new String[] { "Milk", "Eggs", "Bread" }) {
            dispatch("shopping.item.added", household, alice,
                    Map.of("shoppingListId", list, "listName", "Weekly shop", "itemId", Ids.newId(), "name", item));
        }

        mvc.perform(get("/api/v1/me/notifications").with(jwt().jwt(j -> j.subject("bob"))))
                .andExpect(jsonPath("$.items[?(@.type == 'SHOPPING_LIST_UPDATE')].count").value(3))
                .andExpect(jsonPath("$.items[?(@.type == 'SHOPPING_LIST_UPDATE')].body").value("3 items added to Weekly shop."));
    }

    @Test
    void usersCanOnlyMarkTheirOwnNotificationsRead() throws Exception {
        dispatch("task.assigned", household, alice, Map.of("taskId", Ids.newId(), "title", "Bins", "assigneeUserId", bob));
        String id = json.readTree(mvc.perform(get("/api/v1/me/notifications").param("unread", "true")
                .with(jwt().jwt(j -> j.subject("bob")))).andReturn().getResponse().getContentAsString())
                .get("items").get(0).get("id").asString();

        mvc.perform(patch("/api/v1/me/notifications/" + id).with(jwt().jwt(j -> j.subject("alice")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"read\":true}"))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/me/notifications/" + id).with(jwt().jwt(j -> j.subject("bob")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"read\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void turningATypeOffStopsItsNotifications() throws Exception {
        mvc.perform(put("/api/v1/me/notification-preferences").with(jwt().jwt(j -> j.subject("bob")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"preferences\":[{\"type\":\"TASK_OVERDUE\",\"inApp\":false,\"email\":false}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type == 'TASK_OVERDUE')].inApp").value(false));

        dispatch("task.overdue", household, null, Map.of("taskId", Ids.newId(), "title", "Late", "assigneeUserId", bob));

        mvc.perform(get("/api/v1/me/notifications").with(jwt().jwt(j -> j.subject("bob"))))
                .andExpect(jsonPath("$.items[?(@.type == 'TASK_OVERDUE')]").isEmpty());
    }

    @Test
    void anUnknownUserGetsNotFoundAndNoTokenGetsUnauthorized() throws Exception {
        when(membershipClient.findUserIdBySubject(anyString())).thenReturn(Optional.empty());
        mvc.perform(get("/api/v1/me/notifications").with(jwt().jwt(j -> j.subject("stranger")))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/me/notifications")).andExpect(status().isUnauthorized());
        assertThat(alice).isNotEqualTo(bob);
    }

    /** Dispatches an event with this payload. */
    private void dispatch(String type, UUID workspaceId, UUID actor, Map<String, Object> payload) {
        dispatcher.dispatch(envelope(type, workspaceId, actor, payload));
    }

    /** Builds an envelope with a new event ID, as Kafka would deliver it. */
    private EventEnvelope<tools.jackson.databind.JsonNode> envelope(String type, UUID workspaceId, UUID actor,
            Map<String, Object> payload) {
        return new EventEnvelope<>(Ids.newId(), type, 1, Instant.now(), workspaceId, actor, json.valueToTree(payload));
    }
}
