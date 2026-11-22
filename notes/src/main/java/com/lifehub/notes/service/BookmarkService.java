package com.lifehub.notes.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.lifehub.notes.model.dto.BookmarkDTO;
import com.lifehub.notes.model.dto.SaveBookmarkRequest;
import com.lifehub.starter.web.PageResponse;

/** Saved links, with the same visibility rules as notes. */
public interface BookmarkService {

    /** Newest first; {@code q} searches title, link and description, {@code tag} filters. */
    PageResponse<BookmarkDTO> getBookmarks(UUID workspaceId, String q, String tag, Pageable pageable);

    BookmarkDTO createBookmark(UUID workspaceId, SaveBookmarkRequest request);

    BookmarkDTO replaceBookmark(UUID workspaceId, UUID bookmarkId, SaveBookmarkRequest request);

    void deleteBookmark(UUID workspaceId, UUID bookmarkId);
}
