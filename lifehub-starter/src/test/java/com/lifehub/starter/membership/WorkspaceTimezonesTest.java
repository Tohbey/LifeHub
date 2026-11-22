package com.lifehub.starter.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class WorkspaceTimezonesTest {

    private final MembershipClient client = mock(MembershipClient.class);
    private final WorkspaceTimezones zones = new WorkspaceTimezones(client, ZoneId.of("Europe/London"));
    private final UUID ws = UUID.randomUUID();

    @Test
    void usesTheHouseholdTimezone() {
        when(client.findWorkspace(ws)).thenReturn(Optional.of(info("Africa/Lagos")));
        assertThat(zones.zoneOf(ws)).isEqualTo(ZoneId.of("Africa/Lagos"));
    }

    @Test
    void fallsBackToTheDefaultForPersonalWorkspacesFailuresAndBadData() {
        when(client.findWorkspace(ws)).thenReturn(Optional.of(info(null)));
        assertThat(zones.zoneOf(ws)).isEqualTo(ZoneId.of("Europe/London"));

        when(client.findWorkspace(ws)).thenReturn(Optional.of(info("Mars/Olympus")));
        assertThat(zones.zoneOf(ws)).isEqualTo(ZoneId.of("Europe/London"));

        when(client.findWorkspace(ws)).thenThrow(new MembershipUnavailableException("down", new RuntimeException()));
        assertThat(zones.zoneOf(ws)).isEqualTo(ZoneId.of("Europe/London"));
    }

    /** A household with this timezone. */
    private InternalMembershipApi.WorkspaceInfoResponse info(String timezone) {
        return new InternalMembershipApi.WorkspaceInfoResponse(ws, timezone == null ? "PERSONAL" : "HOUSEHOLD", "Home",
                timezone, "GBP", List.of());
    }
}
