package com.lifehub.identityworkspace.service;

import org.springframework.data.domain.Pageable;

import com.lifehub.identityworkspace.model.dto.UpdateUserRequest;
import com.lifehub.identityworkspace.model.dto.UserDTO;
import com.lifehub.identityworkspace.model.dto.WorkspaceDTO;
import com.lifehub.starter.web.PageResponse;

public interface ProfileService {

    /** Returns the current user's profile. */
    UserDTO getCurrentUser(String subject);

    /** Updates the current user's profile. Fields left out are unchanged; an empty string clears a field. */
    UserDTO updateCurrentUser(String subject, UpdateUserRequest request);

    /** The workspaces the user can switch between. */
    PageResponse<WorkspaceDTO> getWorkspaces(String subject, Pageable pageable);
}
