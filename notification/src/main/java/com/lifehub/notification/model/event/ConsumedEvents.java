package com.lifehub.notification.model.event;

import java.util.List;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * The payloads this service reads, with only the fields it uses. Field names follow deploy/docs/events.md;
 * unknown fields are ignored, so producers can add fields freely.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConsumedEvents {

    public static final List<String> TOPICS =
            List.of("identity.events", "finance.events", "task.events", "calendar.events", "shopping.events");

    public record UserCreated(UUID userId, UUID personalWorkspaceId, String displayName) {
    }

    public record UserProfileUpdated(UUID userId, String displayName, String email) {
    }

    public record HouseholdCreated(UUID householdId, String name, UUID ownerUserId) {
    }

    public record HouseholdUpdated(UUID householdId, String name) {
    }

    public record MemberInvited(UUID householdId, UUID invitedUserId, String role) {
    }

    public record MemberAdded(UUID householdId, UUID userId, String role) {
    }

    public record MemberRemoved(UUID householdId, UUID userId, String reason) {
    }

    public record MemberRoleChanged(UUID householdId, UUID userId, String newRole) {
    }

    public record Split(UUID userId, String amount) {
    }

    public record TransactionCreated(UUID transactionId, String description, String amount, String currency,
            List<Split> splits) {
    }

    public record BudgetThresholdExceeded(UUID budgetId, String limit, String spent, String currency,
            int thresholdPercent) {
    }

    public record TaskAssigned(UUID taskId, String title, UUID assigneeUserId) {
    }

    public record TaskCompleted(UUID taskId, String title, UUID completedByUserId) {
    }

    public record TaskOverdue(UUID taskId, String title, UUID assigneeUserId) {
    }

    public record CalendarEventChanged(UUID calendarEventId, String title, String startsAt,
            List<UUID> attendeeUserIds) {
    }

    public record ShoppingItemChanged(UUID shoppingListId, String listName, String name) {
    }
}
