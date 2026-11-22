package com.lifehub.notes.model.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.lifehub.notes.model.entity.Visibility;

/** Writes a note, or replaces one as a whole (PUT). Tags are letters, digits, spaces and hyphens. */
public record SaveNoteRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 20000) String body,
        @NotNull Visibility visibility,
        Boolean pinned,
        @Size(max = 10) List<@NotBlank @Size(max = 40) @Pattern(regexp = Tags.PATTERN, message = Tags.MESSAGE) String> tags) {
}
