package com.lifehub.identityworkspace.mapper;

import com.lifehub.identityworkspace.model.dto.HouseholdDTO;
import com.lifehub.identityworkspace.model.entity.Household;
import com.lifehub.identityworkspace.model.entity.Workspace;
import com.lifehub.starter.membership.WorkspaceRole;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HouseholdMapper {

    /** A household's name lives on its workspace; both share one ID. */
    public static HouseholdDTO toDto(Workspace workspace, Household household, WorkspaceRole yourRole) {
        return new HouseholdDTO(workspace.getId(), workspace.getName(), household.getDefaultCurrency(),
                household.getTimezone(), household.getCreatedAt(), yourRole);
    }
}
