package com.lifehub.identityworkspace.mapper;

import com.lifehub.identityworkspace.model.dto.WorkspaceDTO;
import com.lifehub.identityworkspace.model.entity.Workspace;
import com.lifehub.identityworkspace.model.entity.WorkspaceMember;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WorkspaceMapper {

    /** Maps a workspace and the user's membership of it to a DTO. */
    public static WorkspaceDTO toDto(Workspace workspace, WorkspaceMember membership) {
        return new WorkspaceDTO(workspace.getId(), workspace.getType(), workspace.getName(), membership.getRole());
    }
}
