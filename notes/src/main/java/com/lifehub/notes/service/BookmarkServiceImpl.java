package com.lifehub.notes.service;

import java.net.URI;
import java.time.Clock;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lifehub.notes.mapper.NotesMapper;
import com.lifehub.notes.model.dto.BookmarkDTO;
import com.lifehub.notes.model.dto.SaveBookmarkRequest;
import com.lifehub.notes.model.dto.Tags;
import com.lifehub.notes.model.entity.Bookmark;
import com.lifehub.notes.repository.BookmarkRepository;
import com.lifehub.starter.id.Ids;
import com.lifehub.starter.membership.Membership;
import com.lifehub.starter.membership.WorkspaceAccess;
import com.lifehub.starter.membership.WorkspaceRole;
import com.lifehub.starter.web.ApiException;
import com.lifehub.starter.web.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class BookmarkServiceImpl implements BookmarkService {

    private final WorkspaceAccess access;
    private final BookmarkRepository bookmarks;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookmarkDTO> getBookmarks(UUID workspaceId, String q, String tag, Pageable pageable) {
        Membership caller = access.require(workspaceId);
        return PageResponse.of(bookmarks.search(workspaceId, caller.userId(), NoteServiceImpl.blankToNull(q),
                NoteServiceImpl.tagOrNull(tag), pageable), NotesMapper::toDto);
    }

    @Override
    @Transactional
    public BookmarkDTO createBookmark(UUID workspaceId, SaveBookmarkRequest r) {
        Membership caller = access.require(workspaceId);
        String url = url(r.url());
        return NotesMapper.toDto(bookmarks.save(new Bookmark(Ids.newId(), workspaceId, url, title(r.title(), url),
                NoteServiceImpl.blankToNull(r.description()), r.visibility(), Tags.normalise(r.tags()), caller.userId(),
                clock.instant())));
    }

    @Override
    @Transactional
    public BookmarkDTO replaceBookmark(UUID workspaceId, UUID bookmarkId, SaveBookmarkRequest r) {
        Membership caller = access.require(workspaceId);
        Bookmark b = load(workspaceId, bookmarkId, caller);
        if (r.visibility() != b.getVisibility() && !b.getCreatedByUserId().equals(caller.userId())) {
            throw ApiException.forbidden("Only the person who saved this link can change who sees it.");
        }
        String url = url(r.url());
        b.edit(url, title(r.title(), url), NoteServiceImpl.blankToNull(r.description()), r.visibility(),
                Tags.normalise(r.tags()), clock.instant());
        return NotesMapper.toDto(b);
    }

    @Override
    @Transactional
    public void deleteBookmark(UUID workspaceId, UUID bookmarkId) {
        Membership caller = access.require(workspaceId);
        Bookmark b = load(workspaceId, bookmarkId, caller);
        if (!b.getCreatedByUserId().equals(caller.userId()) && !caller.role().isAtLeast(WorkspaceRole.ADMIN)) {
            throw ApiException.forbidden("Only the person who saved this link, or an owner or admin, can delete it.");
        }
        bookmarks.delete(b);
    }

    private Bookmark load(UUID workspaceId, UUID bookmarkId, Membership caller) {
        return bookmarks.findVisible(bookmarkId, workspaceId, caller.userId())
                .orElseThrow(() -> ApiException.notFound("Bookmark not found."));
    }

    /** The link, checked again by parsing: an http(s) URI with a host. */
    static String url(String raw) {
        String url = raw.strip();
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
            if ((scheme.equals("http") || scheme.equals("https")) && uri.getHost() != null) {
                return url;
            }
        } catch (IllegalArgumentException ignored) {
            // Falls through to the error below.
        }
        throw ApiException.badRequest("Enter a full http or https link, such as https://example.com.");
    }

    /** The given title, or the link's host ("www.gov.uk"). */
    static String title(String title, String url) {
        String t = NoteServiceImpl.blankToNull(title);
        return t != null ? t : URI.create(url).getHost();
    }
}
