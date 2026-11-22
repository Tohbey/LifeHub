package com.lifehub.notification.service;

import org.springframework.stereotype.Component;

import com.lifehub.notification.model.event.ConsumedEvents;
import com.lifehub.notification.repository.DirectoryRepository;
import com.lifehub.starter.events.EventEnvelope;
import com.lifehub.starter.events.EventReader;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

/** Applies identity events to this service's copy of workspaces, memberships and people. */
@Component
@RequiredArgsConstructor
class DirectoryUpdater {

    static final String PERSONAL_WORKSPACE_NAME = "Personal";

    private final EventReader reader;
    private final DirectoryRepository directory;

    /** Updates the directory if this is an identity event that changes it; ignores every other event. */
    void apply(EventEnvelope<JsonNode> event) {
        switch (event.type()) {
            case "user.created" -> {
                var p = reader.payload(event, ConsumedEvents.UserCreated.class);
                directory.saveNewRecipient(p.userId(), p.displayName());
                directory.saveWorkspace(p.personalWorkspaceId(), PERSONAL_WORKSPACE_NAME);
                directory.saveMember(p.personalWorkspaceId(), p.userId(), "OWNER");
            }
            case "user.profile.updated" -> {
                var p = reader.payload(event, ConsumedEvents.UserProfileUpdated.class);
                // Only a verified address is ever mailed: anyone can type any address into their profile.
                directory.saveRecipient(p.userId(), p.displayName(), p.emailVerified() ? p.email() : null);
            }
            case "household.created" -> {
                var p = reader.payload(event, ConsumedEvents.HouseholdCreated.class);
                directory.saveWorkspace(p.householdId(), p.name());
                directory.saveMember(p.householdId(), p.ownerUserId(), "OWNER");
            }
            case "household.updated" -> {
                var p = reader.payload(event, ConsumedEvents.HouseholdUpdated.class);
                directory.saveWorkspace(p.householdId(), p.name());
            }
            case "member.added" -> {
                var p = reader.payload(event, ConsumedEvents.MemberAdded.class);
                directory.saveMember(p.householdId(), p.userId(), p.role());
            }
            case "member.removed" -> {
                var p = reader.payload(event, ConsumedEvents.MemberRemoved.class);
                directory.saveMember(p.householdId(), p.userId(), null);
            }
            case "member.role.changed" -> {
                var p = reader.payload(event, ConsumedEvents.MemberRoleChanged.class);
                directory.changeRole(p.householdId(), p.userId(), p.newRole());
            }
            default -> {
                // Not an identity event: nothing to record.
            }
        }
    }
}
