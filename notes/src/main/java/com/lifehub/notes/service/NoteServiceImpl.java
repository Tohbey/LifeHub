package com.lifehub.notes.service;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.notes.mapper.NotesMapper;
import com.lifehub.notes.model.dto.NoteDTO;
import com.lifehub.notes.model.dto.SaveNoteRequest;
import com.lifehub.notes.model.dto.TagDTO;
import com.lifehub.notes.model.dto.Tags;
import com.lifehub.notes.model.entity.Note;
import com.lifehub.notes.repository.BookmarkRepository;
import com.lifehub.notes.repository.NoteRepository;
import com.lifehub.notes.repository.TagCount;
import com.lifehub.starter.id.Ids;
import com.lifehub.starter.membership.Membership;
import com.lifehub.starter.membership.WorkspaceAccess;
import com.lifehub.starter.membership.WorkspaceRole;
import com.lifehub.starter.web.ApiException;
import com.lifehub.starter.web.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class NoteServiceImpl implements NoteService {

    private static final String NOT_FOUND = "Note not found.";

    private final WorkspaceAccess access;
    private final NoteRepository notes;
    private final BookmarkRepository bookmarks;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NoteDTO> getNotes(UUID workspaceId, String q, String tag, Pageable pageable) {
        Membership caller = access.require(workspaceId);
        return PageResponse.of(notes.search(workspaceId, caller.userId(), blankToNull(q), tagOrNull(tag), pageable),
                NotesMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public NoteDTO getNote(UUID workspaceId, UUID noteId) {
        Membership caller = access.require(workspaceId);
        return NotesMapper.toDto(load(workspaceId, noteId, caller));
    }

    @Override
    @Transactional
    public NoteDTO createNote(UUID workspaceId, SaveNoteRequest r) {
        Membership caller = access.require(workspaceId);
        return NotesMapper.toDto(notes.save(new Note(Ids.newId(), workspaceId, r.title().strip(), blankToNull(r.body()),
                r.visibility(), Boolean.TRUE.equals(r.pinned()), Tags.normalise(r.tags()), caller.userId(),
                clock.instant())));
    }

    /** Anyone who can see it may edit it, but only its author may change who can see it. */
    @Override
    @Transactional
    public NoteDTO replaceNote(UUID workspaceId, UUID noteId, SaveNoteRequest r) {
        Membership caller = access.require(workspaceId);
        Note note = load(workspaceId, noteId, caller);
        if (r.visibility() != note.getVisibility() && !note.getCreatedByUserId().equals(caller.userId())) {
            throw ApiException.forbidden("Only the person who wrote this note can change who sees it.");
        }
        note.edit(r.title().strip(), blankToNull(r.body()), r.visibility(), Boolean.TRUE.equals(r.pinned()),
                Tags.normalise(r.tags()), clock.instant());
        return NotesMapper.toDto(note);
    }

    @Override
    @Transactional
    public void deleteNote(UUID workspaceId, UUID noteId) {
        Membership caller = access.require(workspaceId);
        Note note = load(workspaceId, noteId, caller);
        if (!note.getCreatedByUserId().equals(caller.userId()) && !caller.role().isAtLeast(WorkspaceRole.ADMIN)) {
            throw ApiException.forbidden("Only the person who wrote this note, or an owner or admin, can delete it.");
        }
        notes.delete(note);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagDTO> getTags(UUID workspaceId) {
        Membership caller = access.require(workspaceId);
        Map<String, long[]> counts = new TreeMap<>();
        for (TagCount t : notes.countTags(workspaceId, caller.userId())) {
            counts.computeIfAbsent(t.tag(), k -> new long[2])[0] = t.count();
        }
        for (TagCount t : bookmarks.countTags(workspaceId, caller.userId())) {
            counts.computeIfAbsent(t.tag(), k -> new long[2])[1] = t.count();
        }
        return counts.entrySet().stream().map(e -> new TagDTO(e.getKey(), e.getValue()[0], e.getValue()[1])).toList();
    }

    /** A note the caller can see; someone else's personal note is "not found", like one that does not exist. */
    private Note load(UUID workspaceId, UUID noteId, Membership caller) {
        return notes.findVisible(noteId, workspaceId, caller.userId())
                .orElseThrow(() -> ApiException.notFound(NOT_FOUND));
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    static String tagOrNull(String tag) {
        return Tags.normalise(tag == null ? List.of() : List.of(tag)).stream().findFirst().orElse(null);
    }
}
