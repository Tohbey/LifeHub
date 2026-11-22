package com.lifehub.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.lifehub.notification.repository.DirectoryRepository;
import com.lifehub.starter.membership.InternalMembershipApi.MemberListResponse;
import com.lifehub.starter.membership.InternalMembershipApi.WorkspaceInfoResponse;
import com.lifehub.starter.membership.MembershipClient;
import com.lifehub.starter.membership.MembershipUnavailableException;
import com.lifehub.starter.membership.WorkspaceRole;

class DirectoryBackfillTest {

    private final MembershipClient identity = mock(MembershipClient.class);
    private final DirectoryRepository directory = mock(DirectoryRepository.class);
    private final DirectoryBackfill backfill = new DirectoryBackfill(identity, directory);
    private final UUID home = UUID.randomUUID();
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    @Test
    void copiesAnUnknownHouseholdItsMembersAndTheirNames() {
        when(directory.knowsWorkspace(home)).thenReturn(false);
        when(identity.findWorkspace(home)).thenReturn(Optional.of(new WorkspaceInfoResponse(home, "HOUSEHOLD", "Home",
                "Europe/London", "GBP", List.of(new MemberListResponse.Member(alice, WorkspaceRole.OWNER),
                        new MemberListResponse.Member(bob, WorkspaceRole.MEMBER)))));
        when(identity.findDisplayNames(any())).thenReturn(Map.of(alice, "Alice"));

        backfill.ensureKnown(home);

        verify(directory).saveWorkspace(home, "Home");
        verify(directory).saveMember(home, alice, "OWNER");
        verify(directory).saveMember(home, bob, "MEMBER");
        verify(directory).saveNewRecipient(alice, "Alice");
        verify(directory).saveNewRecipient(bob, null);
    }

    @Test
    void leavesAKnownWorkspaceAloneAndSurvivesIdentityBeingDown() {
        when(directory.knowsWorkspace(home)).thenReturn(true);
        backfill.ensureKnown(home);
        verify(identity, never()).findWorkspace(any());

        UUID other = UUID.randomUUID();
        when(identity.findWorkspace(other)).thenThrow(new MembershipUnavailableException("down", new RuntimeException()));
        backfill.ensureKnown(other);
        verify(directory, never()).saveWorkspace(any(), any());
    }
}
