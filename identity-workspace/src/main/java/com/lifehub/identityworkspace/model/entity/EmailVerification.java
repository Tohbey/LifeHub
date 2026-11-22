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

/** A one-time link that proves the user can read mail at an address. Only the token's hash is stored. */
@Entity
@Table(name = "email_verifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends AssignedIdEntity {

    /** The user who asked to verify. */
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** The address the link was sent to. */
    @Column(name = "email", nullable = false, updatable = false)
    private String email;

    /** SHA-256 hex of the link's token. */
    @Column(name = "token_hash", nullable = false, updatable = false)
    private String tokenHash;

    /** After this the link no longer works. */
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    /** When the link was used, or null. */
    @Column(name = "used_at")
    private Instant usedAt;

    /** When the link was created. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Creates an unused link. Pass the token's hash, never the token. */
    public EmailVerification(UUID id, UUID userId, String email, String tokenHash, Instant now, Instant expiresAt) {
        super(id);
        this.userId = userId;
        this.email = email;
        this.tokenHash = tokenHash;
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    /** True while unused and unexpired. */
    public boolean isUsable(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    /** Marks the link used, so it cannot be used again. */
    public void use(Instant now) {
        this.usedAt = now;
    }
}
