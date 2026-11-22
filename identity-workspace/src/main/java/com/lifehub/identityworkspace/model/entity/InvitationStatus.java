package com.lifehub.identityworkspace.model.entity;

/** Where an invitation stands. Only a PENDING, unexpired one can be accepted. */
public enum InvitationStatus {
    PENDING,
    ACCEPTED,
    REVOKED
}
