package com.lifehub.notification.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.lifehub.notification.model.dto.NotificationDTO;
import com.lifehub.notification.model.dto.UnreadCountDTO;
import com.lifehub.notification.model.dto.UpdateNotificationRequest;
import com.lifehub.notification.service.NotificationService;
import com.lifehub.notification.service.NotificationStreams;
import com.lifehub.starter.membership.WorkspaceAccess;
import com.lifehub.starter.web.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** The signed-in user's notifications. The user always comes from the token, never from the request. */
@RestController
@Tag(name = "Notifications", description = "The signed-in user's notifications and their live stream.")
@RequestMapping("/api/v1/me/notifications")
@RequiredArgsConstructor
class NotificationController {

    private final NotificationService notificationService;
    private final NotificationStreams streams;
    private final WorkspaceAccess access;

    /** GET /api/v1/me/notifications: newest first; {@code ?unread=true} for unread only. */
    @Operation(summary = "List my notifications",
            description = "Your notifications, newest first. Use unread=true for unread ones only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "One page of notifications"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "You have not signed in to LifeHub yet",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    PageResponse<NotificationDTO> getNotifications(@RequestParam(defaultValue = "false") boolean unread,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return notificationService.getNotifications(access.currentUserId(), unread, pageable);
    }

    /** GET /api/v1/me/notifications/unread-count: the number for the badge. */
    @Operation(summary = "Count my unread notifications", description = "The number for the badge.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The unread count",
                    content = @Content(schema = @Schema(implementation = UnreadCountDTO.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "You have not signed in to LifeHub yet",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/unread-count")
    UnreadCountDTO countUnread() {
        return notificationService.countUnread(access.currentUserId());
    }

    /** PATCH /api/v1/me/notifications/{id}: marks one read or unread. */
    @Operation(summary = "Mark a notification read or unread", description = "Only works on your own notifications.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notification updated",
                    content = @Content(schema = @Schema(implementation = NotificationDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Notification not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{notificationId}")
    NotificationDTO updateNotification(@PathVariable UUID notificationId,
            @Valid @RequestBody UpdateNotificationRequest request) {
        return notificationService.markRead(access.currentUserId(), notificationId, request.read());
    }

    /** POST /api/v1/me/notifications/read-all: marks all read. */
    @Operation(summary = "Mark all my notifications read", description = "Returns the new unread count, which is 0.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All marked read",
                    content = @Content(schema = @Schema(implementation = UnreadCountDTO.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "You have not signed in to LifeHub yet",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/read-all")
    UnreadCountDTO markAllRead() {
        return notificationService.markAllRead(access.currentUserId());
    }

    /**
     * GET /api/v1/me/notifications/stream: server-sent events, one "notification" event per new notification.
     * Needs the bearer token in the Authorization header, so the PWA uses a fetch-based SSE client.
     */
    @Operation(summary = "Stream my new notifications",
            description = "Server-sent events: one \"notification\" event per new notification, and a heartbeat "
                    + "comment every 25 seconds. The stream closes after 30 minutes; reconnect and read the list to "
                    + "catch up. Needs the bearer token in the Authorization header, so use a fetch-based SSE "
                    + "client rather than EventSource.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "An open event stream", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "You have not signed in to LifeHub yet",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "429", description = "Too many open streams for your account",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503",
                    description = "Membership could not be checked right now; try again shortly",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream() {
        return streams.open(access.currentUserId());
    }
}
