package com.lifehub.notes.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.notes.model.dto.TagDTO;
import com.lifehub.notes.service.NoteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Tags", description = "The tags used on notes and bookmarks.")
@RequiredArgsConstructor
class TagController {

    private final NoteService noteService;

    /** GET /api/v1/workspaces/{id}/tags: every tag on what you can see, with counts. */
    @Operation(summary = "List tags",
            description = "Every tag on the notes and bookmarks you can see, alphabetically, with how many of each carry it.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The tags"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Workspace not found, or you are not a member of it", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/api/v1/workspaces/{workspaceId}/tags")
    List<TagDTO> getTags(@PathVariable UUID workspaceId) {
        return noteService.getTags(workspaceId);
    }
}
