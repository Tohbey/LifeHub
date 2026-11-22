package com.lifehub.audit.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.lifehub.audit.model.dto.AuditEntryDTO;
import com.lifehub.audit.repository.AuditLogRepository;
import com.lifehub.starter.events.EventEnvelope;
import com.lifehub.starter.events.EventReader;
import com.lifehub.starter.membership.MembershipClient;
import com.lifehub.starter.membership.MembershipUnavailableException;
import com.lifehub.starter.membership.WorkspaceAccess;
import com.lifehub.starter.membership.WorkspaceRole;
import com.lifehub.starter.web.PageResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
@Slf4j
class AuditServiceImpl implements AuditService {

    private static final Pattern UUID_TEXT =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final WorkspaceAccess access;
    private final AuditLogRepository auditLogs;
    private final MembershipClient identity;
    private final EventReader reader;

    /**
     * Requires owner or admin, reads one page, and asks identity-workspace for the names of every user the page
     * mentions in one call, so each entry carries its actor's name and a name for each person in its payload.
     */
    @Override
    public PageResponse<AuditEntryDTO> getEntries(UUID workspaceId, String type, Instant from, Instant to,
            Pageable pageable) {
        access.require(workspaceId, WorkspaceRole.ADMIN);
        List<EventEnvelope<JsonNode>> events = auditLogs
                .find(workspaceId, type, from, to, pageable.getPageSize(), pageable.getOffset()).stream()
                .map(row -> reader.read(row.envelope()))
                .toList();

        Set<UUID> mentioned = new HashSet<>();
        for (EventEnvelope<JsonNode> event : events) {
            if (event.actorUserId() != null) {
                mentioned.add(event.actorUserId());
            }
            collectIds(event.payload(), mentioned);
        }
        Map<UUID, String> known = displayNames(mentioned);

        List<AuditEntryDTO> entries = events.stream().map(event -> {
            Set<UUID> ids = new HashSet<>();
            collectIds(event.payload(), ids);
            if (event.actorUserId() != null) {
                ids.add(event.actorUserId());
            }
            Map<UUID, String> people = ids.stream().filter(known::containsKey)
                    .collect(Collectors.toMap(id -> id, known::get));
            return new AuditEntryDTO(event.eventId(), event.type(), event.version(), event.occurredAt(),
                    event.actorUserId(), event.actorUserId() == null ? null : known.get(event.actorUserId()),
                    event.payload(), people);
        }).toList();
        long total = auditLogs.count(workspaceId, type, from, to);
        return PageResponse.of(new PageImpl<>(entries, pageable, total), entry -> entry);
    }

    /**
     * Names from identity-workspace, without users who have none. If it cannot answer, the page is still shown,
     * with IDs instead of names: the caller's access was already checked, so failing here would only hide history.
     */
    private Map<UUID, String> displayNames(Set<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        try {
            Map<UUID, String> named = new HashMap<>();
            identity.findDisplayNames(userIds).forEach((id, name) -> {
                if (name != null && !name.isBlank()) {
                    named.put(id, name);
                }
            });
            return named;
        } catch (MembershipUnavailableException ex) {
            log.warn("Showing the audit log without names: identity-workspace did not answer", ex);
            return Map.of();
        }
    }

    /** Adds every UUID-looking string in the payload; only the ones that are known users get a name. */
    static void collectIds(JsonNode node, Set<UUID> ids) {
        if (node == null) {
            return;
        }
        if (node.isString() && UUID_TEXT.matcher(node.asString()).matches()) {
            ids.add(UUID.fromString(node.asString()));
        } else if (node.isContainer()) {
            for (JsonNode child : node.values()) {
                collectIds(child, ids);
            }
        }
    }
}
