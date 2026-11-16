package com.lifehub.shoppingmeals.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.lifehub.starter.membership.Membership;
import com.lifehub.starter.membership.WorkspaceAccess;
import com.lifehub.starter.web.ApiException;

import lombok.RequiredArgsConstructor;

/**
 * Membership check for endpoints keyed by a list or item ID instead of a workspace ID. A resource in a
 * workspace the caller does not belong to gets exactly the same 404 as one that does not exist, so IDs
 * cannot be probed.
 */
@Component
@RequiredArgsConstructor
class ResourceAccess {

    private final WorkspaceAccess access;

    /** Requires membership of the resource's workspace, turning "not a member" into this resource's 404. */
    Membership require(UUID workspaceId, String notFoundMessage) {
        try {
            return access.require(workspaceId);
        } catch (ApiException ex) {
            if (ex.status() == HttpStatus.NOT_FOUND) {
                throw ApiException.notFound(notFoundMessage);
            }
            throw ex;
        }
    }
}
