package com.lifehub.notes.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.notes.model.dto.BookmarkDTO;
import com.lifehub.notes.model.dto.SaveBookmarkRequest;
import com.lifehub.notes.service.BookmarkService;
import com.lifehub.starter.web.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Bookmarks", description = "Saved links, personal or shared.")
@RequestMapping("/api/v1/workspaces/{workspaceId}/bookmarks")
@RequiredArgsConstructor
class BookmarkController {

    private final BookmarkService bookmarkService;

    /** GET /api/v1/workspaces/{id}/bookmarks: newest first; {@code ?q=}, {@code ?tag=}. */
    @Operation(summary = "List bookmarks",
            description = "Your personal links and every shared one, newest first. q searches title, link and "
                    + "description; tag filters.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "One page of bookmarks"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Workspace not found, or you are not a member of it", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    PageResponse<BookmarkDTO> getBookmarks(@PathVariable UUID workspaceId, @RequestParam(required = false) String q,
            @RequestParam(required = false) String tag, @PageableDefault Pageable pageable) {
        return bookmarkService.getBookmarks(workspaceId, q, tag, pageable);
    }

    /** POST /api/v1/workspaces/{id}/bookmarks. */
    @Operation(summary = "Save a link",
            description = "http or https links only. The title defaults to the link's host. Any member.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Link saved",
                    content = @Content(schema = @Schema(implementation = BookmarkDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request, or not an http(s) link", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Workspace not found, or you are not a member of it", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BookmarkDTO createBookmark(@PathVariable UUID workspaceId, @Valid @RequestBody SaveBookmarkRequest request) {
        return bookmarkService.createBookmark(workspaceId, request);
    }

    /** PUT /api/v1/workspaces/{id}/bookmarks/{bookmarkId}. */
    @Operation(summary = "Replace a link", description = "Anyone who can see it; only its author can change who sees it.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Link saved"),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Only the author can change who sees it", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Bookmark or workspace not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{bookmarkId}")
    BookmarkDTO replaceBookmark(@PathVariable UUID workspaceId, @PathVariable UUID bookmarkId,
            @Valid @RequestBody SaveBookmarkRequest request) {
        return bookmarkService.replaceBookmark(workspaceId, bookmarkId, request);
    }

    /** DELETE /api/v1/workspaces/{id}/bookmarks/{bookmarkId}. */
    @Operation(summary = "Delete a link", description = "Its author, or an owner or admin for a shared link.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Link deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Only the author or an owner or admin can delete it", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Bookmark or workspace not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{bookmarkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteBookmark(@PathVariable UUID workspaceId, @PathVariable UUID bookmarkId) {
        bookmarkService.deleteBookmark(workspaceId, bookmarkId);
    }
}
