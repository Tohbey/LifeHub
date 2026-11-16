package com.lifehub.identityworkspace.service;

import com.lifehub.identityworkspace.model.dto.WorkspaceDTO;

/**
 * The invite link is the credential: whoever is signed in and holds an unused, unexpired token joins.
 * Logto access tokens carry no verified email to compare against.
 */
public interface InvitationService {

    /**
     * Joins the household the token belongs to. Fails with 404 for an unknown, used or expired token,
     * and 409 if the caller is already a member.
     */
    WorkspaceDTO acceptInvitation(String subject, String token);
}
