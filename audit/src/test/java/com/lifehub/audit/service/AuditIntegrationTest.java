package com.lifehub.audit.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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

/** Real MySQL: events are stored once and only admins read them. Skipped without Docker. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AuditIntegrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4").withDatabaseName("lifehub_audit");

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
    AuditRecorder recorder;

    @MockitoBean
    MembershipClient membershipClient;

    @Test
    void eventsAreStoredOnceAndOnlyAdminsCanReadThem() throws Exception {
        UUID household = Ids.newId();
        when(membershipClient.findBySubject(eq(household), anyString())).thenReturn(Optional.empty());
        when(membershipClient.findBySubject(household, "owner"))
                .thenReturn(Optional.of(new Membership(household, Ids.newId(), WorkspaceRole.OWNER)));
        when(membershipClient.findBySubject(household, "member"))
                .thenReturn(Optional.of(new Membership(household, Ids.newId(), WorkspaceRole.MEMBER)));

        String event = """
                {"eventId":"%s","type":"task.created","version":1,"occurredAt":"%s","workspaceId":"%s",
                 "actorUserId":null,"payload":{"taskId":"%s","title":"Bins"}}
                """.formatted(Ids.newId(), Instant.now(), household, Ids.newId());
        UUID owner = Ids.newId();
        when(membershipClient.findDisplayNames(any())).thenReturn(Map.of(owner, "Alice"));
        event = event.replace("\"actorUserId\":null", "\"actorUserId\":\"" + owner + "\"");
        recorder.record("task.events", event);
        recorder.record("task.events", event);

        mvc.perform(get("/api/v1/workspaces/" + household + "/audit").with(jwt().jwt(j -> j.subject("owner"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].payload.title").value("Bins"))
                .andExpect(jsonPath("$.items[0].actorName").value("Alice"));
        mvc.perform(get("/api/v1/workspaces/" + household + "/audit").with(jwt().jwt(j -> j.subject("member"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/workspaces/" + household + "/audit").with(jwt().jwt(j -> j.subject("stranger"))))
                .andExpect(status().isNotFound());
    }
}
