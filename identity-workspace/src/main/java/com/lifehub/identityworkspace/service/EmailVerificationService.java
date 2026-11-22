package com.lifehub.identityworkspace.service;

import com.lifehub.identityworkspace.model.dto.UserDTO;

/** Proving that a user's profile email is theirs, with a one-time link sent to it. */
public interface EmailVerificationService {

    /** Sends a verification link to the caller's current email. 400 without an email, 429 if asked too often. */
    void requestVerification(String subject);

    /** Uses a link: the caller must be the user it was sent to, and their email must not have changed since. */
    UserDTO confirm(String subject, String token);
}
