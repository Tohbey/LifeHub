package com.lifehub.identityworkspace.model.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.lifehub.starter.membership.WorkspaceRole;
import com.lifehub.starter.persistence.AssignedIdEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * An invitation to join a household. Only a SHA-256 hash of the token is stored; the token itself is
 * returned once, when the invitation is created, and travels in the invite link.
 */
@Entity
@Table(name = "household_invitations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HouseholdInvitation extends AssignedIdEntity {

    /** The household the invitation is for. */
    @Column(name = "household_id", nullable = false, updatable = false)
    private UUID householdId;

    /** The email address the invitation was sent to, lowercased. */
    @Column(name = "email", nullable = false, updatable = false)
    private String email;

    /** The role the invitee gets on joining. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "role", nullable = false, updatable = false)
    private WorkspaceRole role;

    /** SHA-256 hex of the invite token. The token itself is never stored. */
    @Column(name = "token_hash", nullable = false, updatable = false)
    private String tokenHash;

    /** The member who sent the invitation. */
    @Column(name = "invited_by_user_id", nullable = false, updatable = false)
    private UUID invitedByUserId;

    /** Whether the invitation is still pending or has been accepted. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false)
    private InvitationStatus status;

    /** After this moment the invitation can no longer be accepted. */
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    /** When the invitation was sent. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** The user who accepted the invitation, or null while pending. */
    @Column(name = "accepted_by_user_id")
    private UUID acceptedByUserId;

    /** When the invitation was accepted, or null while pending. */
    @Column(name = "accepted_at")
    private Instant acceptedAt;

    /** Creates a pending invitation. Pass the token's hash, never the token itself. */
    public HouseholdInvitation(UUID id, UUID householdId, String email, WorkspaceRole role, String tokenHash,
            UUID invitedByUserId, Instant now, Instant expiresAt) {
        super(id);
        this.householdId = householdId;
        this.email = email;
        this.role = role;
        this.tokenHash = tokenHash;
        this.invitedByUserId = invitedByUserId;
        this.status = InvitationStatus.PENDING;
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    /** True while the invitation is pending and not yet expired. */
    public boolean isUsable(Instant now) {
        return status == InvitationStatus.PENDING && now.isBefore(expiresAt);
    }

    /** Records that this user accepted the invitation, so it cannot be used again. */
    public void accept(UUID userId, Instant now) {
        this.status = InvitationStatus.ACCEPTED;
        this.acceptedByUserId = userId;
        this.acceptedAt = now;
    }
}
