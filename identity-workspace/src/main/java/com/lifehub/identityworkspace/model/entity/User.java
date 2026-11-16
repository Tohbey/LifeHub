package com.lifehub.identityworkspace.model.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.lifehub.starter.persistence.AssignedIdEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A LifeHub user. Created on the first authenticated request, keyed by the Logto subject. Inside LifeHub
 * only {@link #getId()} is used; the Logto subject never leaves this service (ADR 0004).
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AssignedIdEntity {

    /** The Logto subject this user signs in as. */
    @Column(name = "logto_subject", nullable = false, updatable = false)
    private String logtoSubject;

    /** The name shown to other members, or null if not set. */
    @Column(name = "display_name")
    private String displayName;

    /** The user's email as they entered it, lowercased, or null. Not verified. */
    @Column(name = "email")
    private String email;

    /** When the user first signed in. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** When the profile last changed. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Creates a new user for a Logto subject, with no profile details yet. */
    public User(UUID id, String logtoSubject, Instant now) {
        super(id);
        this.logtoSubject = logtoSubject;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Updates the profile. A null field is left unchanged; a blank one clears it. Email is lowercased. */
    public void updateProfile(String displayName, String email, Instant now) {
        if (displayName != null) {
            this.displayName = displayName.isBlank() ? null : displayName.strip();
        }
        if (email != null) {
            this.email = email.isBlank() ? null : email.strip().toLowerCase();
        }
        this.updatedAt = now;
    }
}
