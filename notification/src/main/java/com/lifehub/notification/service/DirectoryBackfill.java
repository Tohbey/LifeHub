package com.lifehub.notification.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.lifehub.notification.repository.DirectoryRepository;
import com.lifehub.starter.membership.InternalMembershipApi.MemberListResponse;
import com.lifehub.starter.membership.InternalMembershipApi.WorkspaceInfoResponse;
import com.lifehub.starter.membership.MembershipClient;
import com.lifehub.starter.membership.MembershipUnavailableException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Fills in a workspace this service has never heard of, from identity-workspace: its name, active members and
 * their names. Needed for workspaces whose identity events are older than Kafka's retention (14 days). Best
 * effort: if identity-workspace cannot answer, the event is still handled, and the next event for the
 * workspace tries again.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class DirectoryBackfill {

    private final MembershipClient identity;
    private final DirectoryRepository directory;

    /** Copies the workspace, its members and their names into the directory, if it is not known yet. */
    void ensureKnown(UUID workspaceId) {
        if (workspaceId == null || directory.knowsWorkspace(workspaceId)) {
            return;
        }
        try {
            identity.findWorkspace(workspaceId).ifPresent(this::copy);
        } catch (MembershipUnavailableException ex) {
            log.warn("Could not fill in workspace {} from identity-workspace; will retry on its next event", workspaceId, ex);
        }
    }

    /** Writes one workspace and its people. Existing names and emails are never overwritten. */
    private void copy(WorkspaceInfoResponse workspace) {
        directory.saveWorkspace(workspace.workspaceId(),
                "PERSONAL".equals(workspace.type()) ? DirectoryUpdater.PERSONAL_WORKSPACE_NAME : workspace.name());
        List<UUID> people = workspace.members().stream().map(MemberListResponse.Member::userId).toList();
        workspace.members().forEach(m -> directory.saveMember(workspace.workspaceId(), m.userId(), m.role().name()));
        Map<UUID, String> names = people.isEmpty() ? Map.of() : identity.findDisplayNames(people);
        people.forEach(id -> directory.saveNewRecipient(id, names.get(id)));
        log.info("Filled in workspace {} with {} member(s) from identity-workspace", workspace.workspaceId(), people.size());
    }
}
