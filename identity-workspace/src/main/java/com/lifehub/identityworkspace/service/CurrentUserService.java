package com.lifehub.identityworkspace.service;

import com.lifehub.identityworkspace.model.entity.User;

/** Turns the Logto subject of the current token into a LifeHub user, provisioning one on first login. */
public interface CurrentUserService {

    /**
     * Returns the LifeHub user for this Logto subject, creating the user and their personal workspace on first login.
     */
    User resolve(String logtoSubject);
}
