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
 * Household settings. A household's ID is the ID of its shared workspace, so "household ID" and
 * "workspace ID" are the same value everywhere, including in events. The name lives on the workspace.
 */
@Entity
@Table(name = "households")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Household extends AssignedIdEntity {

    /** The ISO 4217 currency used by default for shared money, for example GBP. */
    @Column(name = "default_currency", nullable = false)
    private String defaultCurrency;

    /** The IANA timezone used to show household dates, for example Europe/London. */
    @Column(name = "timezone", nullable = false)
    private String timezone;

    /** The user who created the household. */
    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private UUID createdByUserId;

    /** When the household was created. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** When the settings last changed. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Creates the settings for a household workspace, using the workspace's ID as its own. */
    public Household(UUID workspaceId, String defaultCurrency, String timezone, UUID createdByUserId, Instant now) {
        super(workspaceId);
        this.defaultCurrency = defaultCurrency;
        this.timezone = timezone;
        this.createdByUserId = createdByUserId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Changes the currency and/or timezone. A null value is left unchanged. */
    public void changeSettings(String defaultCurrency, String timezone, Instant now) {
        if (defaultCurrency != null) {
            this.defaultCurrency = defaultCurrency;
        }
        if (timezone != null) {
            this.timezone = timezone;
        }
        this.updatedAt = now;
    }
}
