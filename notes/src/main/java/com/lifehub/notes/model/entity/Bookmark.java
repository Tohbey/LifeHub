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

/** A saved link: the council's bin calendar, the school's term dates. */
@Entity
@Table(name = "bookmarks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookmark extends AssignedIdEntity {

    /** The workspace it belongs to. */
    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    /** The link, http or https only. */
    @Column(name = "url", nullable = false)
    private String url;

    /** Its title. */
    @Column(name = "title", nullable = false)
    private String title;

    /** A short description, or null. */
    @Column(name = "description")
    private String description;

    /** Personal or shared. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "visibility", nullable = false)
    private Visibility visibility;

    /** Lower-case tags, loaded in batches. */
    @ElementCollection(fetch = FetchType.EAGER)
    @BatchSize(size = 100)
    @CollectionTable(name = "bookmark_tags", joinColumns = @JoinColumn(name = "bookmark_id"))
    @Column(name = "tag", nullable = false)
    private Set<String> tags = new TreeSet<>();

    /** The member who saved it. */
    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private UUID createdByUserId;

    /** When it was saved. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** When it last changed. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Saves a link. */
    public Bookmark(UUID id, UUID workspaceId, String url, String title, String description, Visibility visibility,
            Set<String> tags, UUID createdByUserId, Instant now) {
        super(id);
        this.workspaceId = workspaceId;
        this.createdByUserId = createdByUserId;
        this.createdAt = now;
        edit(url, title, description, visibility, tags, now);
    }

    /** Replaces its details. */
    public void edit(String url, String title, String description, Visibility visibility, Set<String> tags,
            Instant now) {
        this.url = url;
        this.title = title;
        this.description = description;
        this.visibility = visibility;
        this.tags.clear();
        this.tags.addAll(tags);
        this.updatedAt = now;
    }
}
