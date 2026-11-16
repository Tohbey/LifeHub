package com.lifehub.planner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import com.lifehub.starter.id.Ids;
import com.lifehub.starter.membership.Membership;
import com.lifehub.starter.membership.MembershipClient;
import com.lifehub.starter.membership.WorkspaceRole;

import tools.jackson.databind.json.JsonMapper;

/**
 * Real MySQL with membership mocked: tasks, calendar, permissions, the scheduler's once-only events and the
 * outbox. The scheduler is called directly. Skipped without Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PlannerIntegrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4").withDatabaseName("lifehub_planner");

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
        registry.add("lifehub.planner.scan-interval-ms", () -> "3600000");
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    JsonMapper json;

    @Autowired
    PlannerScheduler scheduler;

    @MockitoBean
    MembershipClient membershipClient;

    private final UUID household = Ids.newId();
    private final UUID alice = Ids.newId();
    private final UUID bob = Ids.newId();

    /** Alice owns the household; Bob is a member. */
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
    void assigningCompletingAndOverdueTasksPublishTheirEventsOnce() throws Exception {
        String taskId = json.readTree(mvc.perform(post(path("/tasks")).with(user("alice"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Pay bill\",\"assigneeUserId\":\"%s\",\"dueAt\":\"%s\"}"
                        .formatted(bob, Instant.now().minusSeconds(60))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asString();

        scheduler.announceOverdueTasks();
        scheduler.announceOverdueTasks();

        mvc.perform(patch(path("/tasks/" + taskId)).with(user("bob")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedByUserId").value(bob.toString()));
        mvc.perform(get(path("/tasks")).param("mine", "true").with(user("bob")))
                .andExpect(jsonPath("$.totalItems").value(1));

        assertThat(outbox()).containsExactly("task.created", "task.assigned", "task.overdue", "task.completed");
    }

    @Test
    void tasksCannotBeAssignedToNonMembersAndNonMembersSeeNothing() throws Exception {
        mvc.perform(post(path("/tasks")).with(user("alice")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"x\",\"assigneeUserId\":\"%s\"}".formatted(Ids.newId())))
                .andExpect(status().isBadRequest());
        mvc.perform(get(path("/tasks")).with(user("mallory"))).andExpect(status().isNotFound());
    }

    @Test
    void remindersAreSentOnceAndOnlyTheCreatorOrAnAdminCanChangeAnEvent() throws Exception {
        Instant start = Instant.now().plusSeconds(600);
        String eventId = json.readTree(mvc.perform(post(path("/events")).with(user("bob"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Dentist","startsAt":"%s","endsAt":"%s","reminderMinutesBefore":30,
                         "attendeeUserIds":["%s","%s"]}""".formatted(start, start.plusSeconds(1800), alice, bob)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asString();

        scheduler.announceDueReminders();
        scheduler.announceDueReminders();

        mvc.perform(patch(path("/events/" + eventId)).with(user("alice")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Dentist (moved)\"}")).andExpect(status().isOk());
        mvc.perform(patch(path("/events/" + eventId)).with(user("bob")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"cancelled\":true}")).andExpect(status().isOk());
        mvc.perform(get(path("/events")).with(user("alice"))).andExpect(jsonPath("$.totalItems").value(0));

        assertThat(outbox()).containsExactly("calendar.event.created", "calendar.event.reminder.due",
                "calendar.event.updated", "calendar.event.cancelled");
    }

    /** A path under the test household. */
    private String path(String suffix) {
        return "/api/v1/workspaces/" + household + suffix;
    }

    /** The event types this test household produced, oldest first. */
    private List<String> outbox() {
        return jdbc.sql("SELECT event_type FROM outbox WHERE message_key = :key ORDER BY created_at, id")
                .param("key", household.toString()).query(String.class).list();
    }

    /** A user token with this subject. */
    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor user(String sub) {
        return jwt().jwt(j -> j.subject(sub));
    }
}
