package com.lifehub.notification.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.lifehub.notification.model.entity.NotificationType;
import com.lifehub.notification.model.event.ConsumedEvents;
import com.lifehub.starter.events.EventEnvelope;
import com.lifehub.starter.events.EventReader;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

/**
 * Decides who is told about an event, and what the notification says. Pure: it reads the event and the
 * {@link Directory} and returns drafts; saving, preferences and delivery happen elsewhere.
 *
 * <p>Two rules hold for every event: the person who caused it is never notified about it, and only active
 * members of the event's workspace are notified (so a private appointment never reaches a household). The
 * exceptions are invitations and removals, whose recipient is by definition not a member.
 */
@Component
@RequiredArgsConstructor
public class NotificationRules {

    private final EventReader reader;

    /** Returns the notifications this event should produce. Events this service does not notify about give none. */
    public List<NotificationDraft> draftsFor(EventEnvelope<JsonNode> event, Directory directory) {
        UUID ws = event.workspaceId();
        UUID actor = event.actorUserId();
        return switch (event.type()) {
            case "member.invited" -> memberInvited(event, directory);
            case "member.added" -> memberAdded(event, directory, actor);
            case "member.removed" -> memberRemoved(event, directory);
            case "member.role.changed" -> memberRoleChanged(event, directory, actor);
            case "transaction.created" -> transactionCreated(event, directory, ws, actor);
            case "budget.threshold.exceeded" -> budgetExceeded(event, directory, ws);
            case "task.assigned" -> taskAssigned(event, directory, ws, actor);
            case "task.completed" -> taskCompleted(event, directory, ws, actor);
            case "task.overdue" -> taskOverdue(event, directory, ws);
            case "calendar.event.created", "calendar.event.updated", "calendar.event.cancelled" ->
                    calendarChanged(event, directory, ws, actor);
            case "calendar.event.reminder.due" -> calendarReminder(event, directory, ws);
            case "shopping.item.added", "shopping.item.purchased" -> shoppingChanged(event, directory, ws, actor);
            default -> List.of();
        };
    }

    /** Tells an existing LifeHub user they were invited. Unknown emails get nothing: there is no one to tell. */
    private List<NotificationDraft> memberInvited(EventEnvelope<JsonNode> event, Directory directory) {
        var p = reader.payload(event, ConsumedEvents.MemberInvited.class);
        if (p.invitedUserId() == null || p.invitedUserId().equals(event.actorUserId())) {
            return List.of();
        }
        String household = directory.workspaceName(p.householdId());
        return List.of(NotificationDraft.single(p.invitedUserId(), p.householdId(), NotificationType.HOUSEHOLD_INVITATION,
                "Invitation to " + household,
                directory.displayName(event.actorUserId()) + " invited you to join " + household
                        + ". Open the invite link you were sent to accept.",
                null));
    }

    /** Tells the other members that someone joined. */
    private List<NotificationDraft> memberAdded(EventEnvelope<JsonNode> event, Directory directory, UUID actor) {
        var p = reader.payload(event, ConsumedEvents.MemberAdded.class);
        String household = directory.workspaceName(p.householdId());
        String who = directory.displayName(p.userId());
        return toMembers(directory, p.householdId(), exclude(actor, p.userId()), NotificationType.HOUSEHOLD_MEMBERSHIP,
                who + " joined " + household, who + " is now a member of " + household + ".",
                "/households/" + p.householdId());
    }

    /** Tells a removed member they were removed. Leaving on your own produces nothing. */
    private List<NotificationDraft> memberRemoved(EventEnvelope<JsonNode> event, Directory directory) {
        var p = reader.payload(event, ConsumedEvents.MemberRemoved.class);
        if (!"REMOVED".equals(p.reason())) {
            return List.of();
        }
        String household = directory.workspaceName(p.householdId());
        return List.of(NotificationDraft.single(p.userId(), p.householdId(), NotificationType.HOUSEHOLD_MEMBERSHIP,
                "Removed from " + household, "You are no longer a member of " + household + ".", null));
    }

    /** Tells a member their role changed, unless they changed it themselves. */
    private List<NotificationDraft> memberRoleChanged(EventEnvelope<JsonNode> event, Directory directory, UUID actor) {
        var p = reader.payload(event, ConsumedEvents.MemberRoleChanged.class);
        if (p.userId().equals(actor) || !directory.activeMembers(p.householdId()).containsKey(p.userId())) {
            return List.of();
        }
        String household = directory.workspaceName(p.householdId());
        return List.of(NotificationDraft.single(p.userId(), p.householdId(), NotificationType.HOUSEHOLD_MEMBERSHIP,
                "Your role in " + household + " changed",
                "You are now " + article(p.newRole()) + " " + p.newRole().toLowerCase() + " of " + household + ".",
                "/households/" + p.householdId()));
    }

    /** Tells split participants, other than the person who added it, about a shared expense and their share. */
    private List<NotificationDraft> transactionCreated(EventEnvelope<JsonNode> event, Directory directory, UUID ws,
            UUID actor) {
        var p = reader.payload(event, ConsumedEvents.TransactionCreated.class);
        if (p.splits() == null || p.splits().isEmpty()) {
            return List.of();
        }
        Map<UUID, String> members = directory.activeMembers(ws);
        String who = directory.displayName(actor);
        List<NotificationDraft> drafts = new ArrayList<>();
        for (ConsumedEvents.Split split : p.splits()) {
            if (split.userId().equals(actor) || !members.containsKey(split.userId())) {
                continue;
            }
            drafts.add(NotificationDraft.single(split.userId(), ws, NotificationType.SHARED_EXPENSE,
                    "New shared expense: " + p.description(),
                    who + " added " + p.description() + " (" + p.amount() + " " + p.currency() + "). Your share is "
                            + split.amount() + " " + p.currency() + ".",
                    "/finance/transactions/" + p.transactionId()));
        }
        return drafts;
    }

    /** Tells every member of the workspace that a budget crossed a threshold. */
    private List<NotificationDraft> budgetExceeded(EventEnvelope<JsonNode> event, Directory directory, UUID ws) {
        var p = reader.payload(event, ConsumedEvents.BudgetThresholdExceeded.class);
        String title = p.thresholdPercent() >= 100 ? "Budget exceeded" : "Budget at " + p.thresholdPercent() + "%";
        return toMembers(directory, ws, Set.of(), NotificationType.BUDGET_ALERT, title,
                "Spent " + p.spent() + " of " + p.limit() + " " + p.currency() + " this period.",
                "/finance/budgets/" + p.budgetId());
    }

    /** Tells the new assignee, unless they assigned the task to themselves. */
    private List<NotificationDraft> taskAssigned(EventEnvelope<JsonNode> event, Directory directory, UUID ws,
            UUID actor) {
        var p = reader.payload(event, ConsumedEvents.TaskAssigned.class);
        if (p.assigneeUserId() == null || p.assigneeUserId().equals(actor)
                || !directory.activeMembers(ws).containsKey(p.assigneeUserId())) {
            return List.of();
        }
        return List.of(NotificationDraft.single(p.assigneeUserId(), ws, NotificationType.TASK_ASSIGNED,
                "Task assigned: " + p.title(), directory.displayName(actor) + " assigned you \"" + p.title() + "\".",
                "/planner/tasks/" + p.taskId()));
    }

    /** Tells the other members that a task was done. */
    private List<NotificationDraft> taskCompleted(EventEnvelope<JsonNode> event, Directory directory, UUID ws,
            UUID actor) {
        var p = reader.payload(event, ConsumedEvents.TaskCompleted.class);
        UUID by = p.completedByUserId() != null ? p.completedByUserId() : actor;
        return toMembers(directory, ws, exclude(actor, by), NotificationType.TASK_COMPLETED,
                "Task done: " + p.title(), directory.displayName(by) + " completed \"" + p.title() + "\".",
                "/planner/tasks/" + p.taskId());
    }

    /** Tells the assignee, or every member when the task has no assignee, that a task is overdue. */
    private List<NotificationDraft> taskOverdue(EventEnvelope<JsonNode> event, Directory directory, UUID ws) {
        var p = reader.payload(event, ConsumedEvents.TaskOverdue.class);
        Map<UUID, String> members = directory.activeMembers(ws);
        Collection<UUID> recipients = p.assigneeUserId() != null ? List.of(p.assigneeUserId()) : members.keySet();
        return recipients.stream()
                .filter(members::containsKey)
                .map(user -> NotificationDraft.single(user, ws, NotificationType.TASK_OVERDUE,
                        "Overdue: " + p.title(), "\"" + p.title() + "\" is past its due time.",
                        "/planner/tasks/" + p.taskId()))
                .toList();
    }

    /**
     * Tells attendees about a new, changed or cancelled event. A cancellation carries no attendee list, so
     * every member is told.
     */
    private List<NotificationDraft> calendarChanged(EventEnvelope<JsonNode> event, Directory directory, UUID ws,
            UUID actor) {
        var p = reader.payload(event, ConsumedEvents.CalendarEventChanged.class);
        String verb = switch (event.type()) {
            case "calendar.event.created" -> "added";
            case "calendar.event.cancelled" -> "cancelled";
            default -> "changed";
        };
        Map<UUID, String> members = directory.activeMembers(ws);
        Collection<UUID> recipients = p.attendeeUserIds() == null ? members.keySet() : p.attendeeUserIds();
        String title = "Event " + verb + ": " + p.title();
        String body = directory.displayName(actor) + " " + verb + " \"" + p.title() + "\"" + startsAt(p.startsAt()) + ".";
        return recipients.stream()
                .distinct()
                .filter(user -> members.containsKey(user) && !user.equals(actor))
                .map(user -> NotificationDraft.single(user, ws, NotificationType.CALENDAR_UPDATE, title, body,
                        "/planner/events/" + p.calendarEventId()))
                .toList();
    }

    /** Reminds attendees, or every member when nobody is listed, that an event is coming up. */
    private List<NotificationDraft> calendarReminder(EventEnvelope<JsonNode> event, Directory directory, UUID ws) {
        var p = reader.payload(event, ConsumedEvents.CalendarEventChanged.class);
        Map<UUID, String> members = directory.activeMembers(ws);
        Collection<UUID> recipients = p.attendeeUserIds() == null || p.attendeeUserIds().isEmpty()
                ? members.keySet() : p.attendeeUserIds();
        return recipients.stream()
                .distinct()
                .filter(members::containsKey)
                .map(user -> NotificationDraft.single(user, ws, NotificationType.CALENDAR_REMINDER,
                        "Coming up: " + p.title(), "\"" + p.title() + "\"" + startsAt(p.startsAt()) + ".",
                        "/planner/events/" + p.calendarEventId()))
                .toList();
    }

    /**
     * Tells the other members that a list changed. Grouped per list and kind of change, so a whole shop of
     * ticked-off items becomes one notification.
     */
    private List<NotificationDraft> shoppingChanged(EventEnvelope<JsonNode> event, Directory directory, UUID ws,
            UUID actor) {
        var p = reader.payload(event, ConsumedEvents.ShoppingItemChanged.class);
        boolean added = event.type().equals("shopping.item.added");
        String who = directory.displayName(actor);
        String title = p.listName() + " updated";
        String body = added ? who + " added " + p.name() + " to " + p.listName() + "."
                : who + " bought " + p.name() + " from " + p.listName() + ".";
        String groupKey = "shopping:" + p.shoppingListId() + (added ? ":added" : ":purchased");
        String grouped = added ? "%d items added to " + p.listName() + "." : "%d items bought from " + p.listName() + ".";
        String link = "/shopping/lists/" + p.shoppingListId();
        return directory.activeMembers(ws).keySet().stream()
                .filter(user -> !user.equals(actor))
                .map(user -> new NotificationDraft(user, ws, NotificationType.SHOPPING_LIST_UPDATE, title, body, link,
                        groupKey, grouped))
                .toList();
    }

    /** One draft per active member of the workspace, except the excluded users. */
    private static List<NotificationDraft> toMembers(Directory directory, UUID ws, Set<UUID> excluded,
            NotificationType type, String title, String body, String link) {
        return directory.activeMembers(ws).keySet().stream()
                .filter(user -> !excluded.contains(user))
                .map(user -> NotificationDraft.single(user, ws, type, title, body, link))
                .toList();
    }

    /** The non-null user IDs to leave out. */
    private static Set<UUID> exclude(UUID... users) {
        Set<UUID> set = new LinkedHashSet<>();
        for (UUID user : users) {
            if (user != null) {
                set.add(user);
            }
        }
        return set;
    }

    /** " (starts 2026-11-25T17:30:00Z)", or nothing when the time is unknown. */
    private static String startsAt(String startsAt) {
        return startsAt == null ? "" : " (starts " + startsAt + ")";
    }

    /** "an" for ADMIN and OWNER, "a" otherwise. */
    private static String article(String role) {
        return Objects.equals(role, "ADMIN") || Objects.equals(role, "OWNER") ? "an" : "a";
    }
}
