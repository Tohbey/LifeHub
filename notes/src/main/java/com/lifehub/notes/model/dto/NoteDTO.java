package com.lifehub.notes.model.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.lifehub.notes.model.entity.Visibility;

public record NoteDTO(UUID id, UUID workspaceId, String title, String body, Visibility visibility, boolean pinned,
        List<String> tags, UUID createdByUserId, Instant createdAt, Instant updatedAt) {
}
