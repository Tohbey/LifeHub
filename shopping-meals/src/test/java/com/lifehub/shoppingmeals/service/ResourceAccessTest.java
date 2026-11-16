package com.lifehub.shoppingmeals.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.lifehub.starter.membership.Membership;
import com.lifehub.starter.membership.WorkspaceAccess;
import com.lifehub.starter.membership.WorkspaceRole;
import com.lifehub.starter.web.ApiException;

class ResourceAccessTest {

    private final WorkspaceAccess workspaceAccess = mock(WorkspaceAccess.class);
    private final ResourceAccess access = new ResourceAccess(workspaceAccess);
    private final UUID workspace = UUID.randomUUID();

    @Test
    void aNonMemberGetsTheResourcesOwnNotFound() {
        when(workspaceAccess.require(workspace)).thenThrow(ApiException.notFound("Workspace not found."));

        assertThatThrownBy(() -> access.require(workspace, "Shopping item not found."))
                .isInstanceOf(ApiException.class)
                .hasMessage("Shopping item not found.")
                .extracting(ex -> ((ApiException) ex).status()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void otherFailuresAndMembersPassThrough() {
        Membership member = new Membership(workspace, UUID.randomUUID(), WorkspaceRole.MEMBER);
        when(workspaceAccess.require(workspace)).thenReturn(member);
        assertThat(access.require(workspace, "x")).isEqualTo(member);

        when(workspaceAccess.require(workspace)).thenThrow(ApiException.forbidden("No."));
        assertThatThrownBy(() -> access.require(workspace, "x")).hasMessage("No.");
    }
}
