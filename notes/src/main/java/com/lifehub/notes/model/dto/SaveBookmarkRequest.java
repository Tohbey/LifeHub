package com.lifehub.notes.model.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.lifehub.notes.model.entity.Visibility;

/**
 * Saves a link, or replaces one as a whole (PUT). Only http and https links are accepted, so a saved bookmark
 * can never run script when someone opens it. The title defaults to the link's host.
 */
public record SaveBookmarkRequest(
        @NotBlank @Size(max = 2000) @Pattern(regexp = "(?i)https?://[^\\s/$.?#][^\\s]*", message = "must be an http or https link") String url,
        @Size(max = 200) String title,
        @Size(max = 500) String description,
        @NotNull Visibility visibility,
        @Size(max = 10) List<@NotBlank @Size(max = 40) @Pattern(regexp = Tags.PATTERN, message = Tags.MESSAGE) String> tags) {
}
