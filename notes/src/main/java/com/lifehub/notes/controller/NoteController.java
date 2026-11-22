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

import com.lifehub.notes.model.dto.NoteDTO;
import com.lifehub.notes.model.dto.SaveNoteRequest;
import com.lifehub.notes.service.NoteService;
import com.lifehub.starter.web.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Notes", description = "Personal and shared notes.")
@RequestMapping("/api/v1/workspaces/{workspaceId}/notes")
@RequiredArgsConstructor
class NoteController {

    private final NoteService noteService;

    /** GET /api/v1/workspaces/{id}/notes: your notes and the shared ones; {@code ?q=}, {@code ?tag=}. */
    @Operation(summary = "List notes",
            description = "Your personal notes and every shared one, pinned first, then most recently changed. q searches "
                    + "the title and text; tag keeps only notes with that tag.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "One page of notes"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Workspace not found, or you are not a member of it", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "Membership could not be checked right now; try again shortly", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    PageResponse<NoteDTO> getNotes(@PathVariable UUID workspaceId, @RequestParam(required = false) String q,
            @RequestParam(required = false) String tag, @PageableDefault Pageable pageable) {
        return noteService.getNotes(workspaceId, q, tag, pageable);
    }

    /** GET /api/v1/workspaces/{id}/notes/{noteId}. */
    @Operation(summary = "Get a note", description = "Someone else's personal note is not found, like one that does not exist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The note"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Note or workspace not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{noteId}")
    NoteDTO getNote(@PathVariable UUID workspaceId, @PathVariable UUID noteId) {
        return noteService.getNote(workspaceId, noteId);
    }

    /** POST /api/v1/workspaces/{id}/notes. */
    @Operation(summary = "Write a note",
            description = "visibility PERSONAL (only you) or SHARED (every member). Up to 10 tags. Any member.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Note saved",
                    content = @Content(schema = @Schema(implementation = NoteDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Workspace not found, or you are not a member of it", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    NoteDTO createNote(@PathVariable UUID workspaceId, @Valid @RequestBody SaveNoteRequest request) {
        return noteService.createNote(workspaceId, request);
    }

    /** PUT /api/v1/workspaces/{id}/notes/{noteId}. */
    @Operation(summary = "Replace a note",
            description = "Anyone who can see a note can edit it; only its author can change who sees it.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Note saved"),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Only the author can change who sees it", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Note or workspace not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{noteId}")
    NoteDTO replaceNote(@PathVariable UUID workspaceId, @PathVariable UUID noteId,
            @Valid @RequestBody SaveNoteRequest request) {
        return noteService.replaceNote(workspaceId, noteId, request);
    }

    /** DELETE /api/v1/workspaces/{id}/notes/{noteId}. */
    @Operation(summary = "Delete a note", description = "Its author, or an owner or admin for a shared note.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Note deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Only the author or an owner or admin can delete it", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Note or workspace not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteNote(@PathVariable UUID workspaceId, @PathVariable UUID noteId) {
        noteService.deleteNote(workspaceId, noteId);
    }
}
