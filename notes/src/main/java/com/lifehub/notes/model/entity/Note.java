package com.lifehub.notes.model.entity;

import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import com.lifehub.starter.persistence.AssignedIdEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A note: the Wi-Fi password, the boiler's service history, a packing list. */
@Entity
@Table(name = "notes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Note extends AssignedIdEntity {

    /** The workspace it belongs to. */
    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    /** Its title. */
    @Column(name = "title", nullable = false)
    private String title;

    /** Its text, plain. */
    @Column(name = "body")
    private String body;

    /** Personal or shared. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "visibility", nullable = false)
    private Visibility visibility;

    /** Pinned notes are listed first. */
    @Column(name = "pinned", nullable = false)
    private boolean pinned;

    /** Lower-case tags. Loaded in batches, so a page of notes costs two queries, not one per note. */
    @ElementCollection(fetch = FetchType.EAGER)
    @BatchSize(size = 100)
    @CollectionTable(name = "note_tags", joinColumns = @JoinColumn(name = "note_id"))
    @Column(name = "tag", nullable = false)
    private Set<String> tags = new TreeSet<>();

    /** The member who wrote it. */
    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private UUID createdByUserId;

    /** When it was written. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** When it last changed. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Writes a note. */
    public Note(UUID id, UUID workspaceId, String title, String body, Visibility visibility, boolean pinned,
            Set<String> tags, UUID createdByUserId, Instant now) {
        super(id);
        this.workspaceId = workspaceId;
        this.createdByUserId = createdByUserId;
        this.createdAt = now;
        edit(title, body, visibility, pinned, tags, now);
    }

    /** Replaces its content. */
    public void edit(String title, String body, Visibility visibility, boolean pinned, Set<String> tags, Instant now) {
        this.title = title;
        this.body = body;
        this.visibility = visibility;
        this.pinned = pinned;
        this.tags.clear();
        this.tags.addAll(tags);
        this.updatedAt = now;
    }
}
