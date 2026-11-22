package com.lifehub.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import com.lifehub.audit.repository.AuditLogRepository;
import com.lifehub.starter.events.EventReader;
import com.lifehub.starter.membership.Membership;
import com.lifehub.starter.membership.MembershipClient;
import com.lifehub.starter.membership.MembershipUnavailableException;
import com.lifehub.starter.membership.WorkspaceAccess;
import com.lifehub.starter.membership.WorkspaceRole;

import tools.jackson.databind.json.JsonMapper;

class AuditNamesTest {

    private final UUID workspace = UUID.randomUUID();
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();
    private final UUID nameless = UUID.randomUUID();

    private final WorkspaceAccess access = mock(WorkspaceAccess.class);
    private final AuditLogRepository logs = mock(AuditLogRepository.class);
    private final MembershipClient identity = mock(MembershipClient.class);
    private final AuditServiceImpl service =
            new AuditServiceImpl(access, logs, identity, new EventReader(JsonMapper.builder().build()));

    /** One stored expense by Alice, split with Bob and someone without a name. */
    private void oneStoredEvent() {
        when(access.require(workspace, WorkspaceRole.ADMIN)).thenReturn(new Membership(workspace, alice, WorkspaceRole.OWNER));
        String envelope = """
                {"eventId":"%s","type":"transaction.created","version":1,"occurredAt":"2026-11-18T10:00:00Z",
                 "workspaceId":"%s","actorUserId":"%s",
                 "payload":{"payerUserId":"%s","splits":[{"userId":"%s"},{"userId":"%s"}]}}
                """.formatted(UUID.randomUUID(), workspace, alice, alice, bob, nameless);
        when(logs.find(eq(workspace), any(), any(), any(), anyInt(), anyLong())).thenReturn(List.of(
                new AuditLogRepository.Row(UUID.randomUUID(), "transaction.created", 1, Instant.now(), alice, envelope)));
        when(logs.count(eq(workspace), any(), any(), any())).thenReturn(1L);
    }

    @Test
    void namesTheActorAndEveryoneInThePayloadWithOneLookup() {
        oneStoredEvent();
        Map<UUID, String> names = new HashMap<>(Map.of(alice, "Alice", bob, "Bob"));
        names.put(nameless, null);
        when(identity.findDisplayNames(any())).thenReturn(names);

        var entry = service.getEntries(workspace, null, null, null, PageRequest.of(0, 50)).items().get(0);

        assertThat(entry.actorName()).isEqualTo("Alice");
        assertThat(entry.people()).containsOnly(Map.entry(alice, "Alice"), Map.entry(bob, "Bob"));
    }

    @Test
    void stillShowsTheLogWhenIdentityDoesNotAnswer() {
        oneStoredEvent();
        when(identity.findDisplayNames(any())).thenThrow(new MembershipUnavailableException("down", new RuntimeException()));

        var page = service.getEntries(workspace, null, null, null, PageRequest.of(0, 50));

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().get(0).actorName()).isNull();
        assertThat(page.items().get(0).people()).isEmpty();
    }
}
