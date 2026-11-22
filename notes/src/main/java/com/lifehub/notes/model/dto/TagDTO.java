package com.lifehub.notes.model.dto;

/** A tag and how many visible notes and bookmarks carry it. */
public record TagDTO(String tag, long notes, long bookmarks) {
}
