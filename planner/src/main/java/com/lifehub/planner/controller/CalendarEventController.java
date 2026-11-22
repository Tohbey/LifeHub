package com.lifehub.planner.controller;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lifehub.planner.model.dto.CalendarEventDTO;
import com.lifehub.planner.model.dto.CreateCalendarEventRequest;
import com.lifehub.planner.model.dto.UpdateCalendarEventRequest;
import com.lifehub.planner.service.CalendarService;
import com.lifehub.starter.web.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Calendar", description = "A workspace's calendar events, attendees and reminders.")
@RequestMapping("/api/v1/workspaces/{workspaceId}/events")
@RequiredArgsConstructor
class CalendarEventController {

    private final CalendarService calendarService;

    /** GET /api/v1/workspaces/{id}/events: events overlapping [from, to), the next 31 days by default. */
    @Operation(summary = "List calendar events",
            description = "Events overlapping [from, to), at most 400 days; the next 31 days by default.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "One page of events"),
            @ApiResponse(responseCode = "400", description = "Invalid time range",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Workspace not found, or you are not a member of it",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    PageResponse<CalendarEventDTO> getEvents(@PathVariable UUID workspaceId,
            @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "false") boolean includeCancelled,
            @PageableDefault(size = 100, sort = "startsAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return calendarService.getEvents(workspaceId, from, to, includeCancelled, pageable);
    }

    /** POST /api/v1/workspaces/{id}/events: adds an event. Returns 201. */
    @Operation(summary = "Add a calendar event",
            description = "Attendees must be members, and the event cannot start in the past. An optional reminder "
                    + "is sent the given number of minutes before each occurrence; add a recurrence to repeat it.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Event added",
                    content = @Content(schema = @Schema(implementation = CalendarEventDTO.class))),
            @ApiResponse(responseCode = "400",
                    description = "Invalid request, a start in the past, an end before the start, or an attendee who is not a member",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Workspace not found, or you are not a member of it",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    ResponseEntity<CalendarEventDTO> createEvent(@PathVariable UUID workspaceId,
            @Valid @RequestBody CreateCalendarEventRequest request) {
        CalendarEventDTO event = calendarService.createEvent(workspaceId, request);
        return ResponseEntity.created(URI.create("/api/v1/workspaces/" + workspaceId + "/events/" + event.id()))
                .body(event);
    }

    /** GET /api/v1/workspaces/{id}/events/{eventId}: one event with its attendees. */
    @Operation(summary = "Get a calendar event", description = "One event with its attendees.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The event",
                    content = @Content(schema = @Schema(implementation = CalendarEventDTO.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Event or workspace not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{eventId}")
    CalendarEventDTO getEvent(@PathVariable UUID workspaceId, @PathVariable UUID eventId) {
        return calendarService.getEvent(workspaceId, eventId);
    }

    /** PATCH /api/v1/workspaces/{id}/events/{eventId}: changes or cancels an event. */
    @Operation(summary = "Update or cancel a calendar event",
            description = "Changes the given fields, or cancels with cancelled=true. Only the creator, or an owner or "
                    + "admin, may change it.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event updated",
                    content = @Content(schema = @Schema(implementation = CalendarEventDTO.class))),
            @ApiResponse(responseCode = "400",
                    description = "Invalid request, a start in the past, an end before the start, or an attendee who is not a member",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "You did not create this event and are not an admin",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Event or workspace not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "The event is cancelled",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{eventId}")
    CalendarEventDTO updateEvent(@PathVariable UUID workspaceId, @PathVariable UUID eventId,
            @Valid @RequestBody UpdateCalendarEventRequest request) {
        return calendarService.updateEvent(workspaceId, eventId, request);
    }
}
