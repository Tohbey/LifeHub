package com.lifehub.notes.model.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.lifehub.notes.model.entity.Visibility;

public record BookmarkDTO(UUID id, UUID workspaceId, String url, String title, String description,
        Visibility visibility, List<String> tags, UUID createdByUserId, Instant createdAt, Instant updatedAt) {
}
