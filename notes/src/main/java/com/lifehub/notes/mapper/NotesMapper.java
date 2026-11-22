package com.lifehub.notes.mapper;

import java.util.List;
import java.util.TreeSet;

import com.lifehub.notes.model.dto.BookmarkDTO;
import com.lifehub.notes.model.dto.NoteDTO;
import com.lifehub.notes.model.entity.Bookmark;
import com.lifehub.notes.model.entity.Note;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NotesMapper {

    /** Maps a note to a DTO, tags sorted. */
    public static NoteDTO toDto(Note n) {
        return new NoteDTO(n.getId(), n.getWorkspaceId(), n.getTitle(), n.getBody(), n.getVisibility(), n.isPinned(),
                List.copyOf(new TreeSet<>(n.getTags())), n.getCreatedByUserId(), n.getCreatedAt(),
                n.getUpdatedAt());
    }

    /** Maps a bookmark to a DTO, tags sorted. */
    public static BookmarkDTO toDto(Bookmark b) {
        return new BookmarkDTO(b.getId(), b.getWorkspaceId(), b.getUrl(), b.getTitle(), b.getDescription(),
                b.getVisibility(), List.copyOf(new TreeSet<>(b.getTags())), b.getCreatedByUserId(),
                b.getCreatedAt(), b.getUpdatedAt());
    }
}
