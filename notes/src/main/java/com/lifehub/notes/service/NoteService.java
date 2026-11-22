package com.lifehub.notes.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.lifehub.notes.model.dto.NoteDTO;
import com.lifehub.notes.model.dto.SaveNoteRequest;
import com.lifehub.notes.model.dto.TagDTO;
import com.lifehub.starter.web.PageResponse;

/**
 * Notes and the tags used across notes and bookmarks. A personal note is visible, editable and deletable only by
 * its author. A shared note is visible to and editable by every member; its author or an owner or admin deletes it.
 */
public interface NoteService {

    /** Pinned first, then most recently changed; {@code q} searches title and text, {@code tag} filters. */
    PageResponse<NoteDTO> getNotes(UUID workspaceId, String q, String tag, Pageable pageable);

    NoteDTO getNote(UUID workspaceId, UUID noteId);

    NoteDTO createNote(UUID workspaceId, SaveNoteRequest request);

    NoteDTO replaceNote(UUID workspaceId, UUID noteId, SaveNoteRequest request);

    void deleteNote(UUID workspaceId, UUID noteId);

    /** Every tag on what the caller can see, with counts, alphabetically. */
    List<TagDTO> getTags(UUID workspaceId);
}
