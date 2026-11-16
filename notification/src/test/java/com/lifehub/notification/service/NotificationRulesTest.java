package com.lifehub.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.lifehub.notification.model.entity.NotificationType;
import com.lifehub.starter.events.EventEnvelope;
import com.lifehub.starter.events.EventReader;
import com.lifehub.starter.id.Ids;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class NotificationRulesTest {

    private final JsonMapper json = JsonMapper.builder().build();
    private final NotificationRules rules = new NotificationRules(new EventReader(json));

    private final UUID household = Ids.newId();
    private final UUID alice = Ids.newId();
    private final UUID bob = Ids.newId();
    private final UUID carol = Ids.newId();
    private final UUID outsider = Ids.newId();
    private final FakeDirectory directory = new FakeDirectory();

    {
        directory.members.put(alice, "OWNER");
        directory.members.put(bob, "MEMBER");
        directory.members.put(carol, "MEMBER");
        directory.names.put(alice, "Alice");
    }

    @Test
    void sharedExpenseGoesToOtherSplitParticipantsOnly() {
        var drafts = drafts("transaction.created", alice, """
                {"transactionId":"%s","description":"Electricity","amount":"120.00","currency":"GBP",
                 "splits":[{"userId":"%s","amount":"60.00"},{"userId":"%s","amount":"60.00"},{"userId":"%s","amount":"0.00"}]}
                """.formatted(Ids.newId(), alice, bob, outsider));

        assertThat(drafts).extracting(NotificationDraft::userId).containsExactly(bob);
        assertThat(drafts.get(0).type()).isEqualTo(NotificationType.SHARED_EXPENSE);
        assertThat(drafts.get(0).body()).isEqualTo("Alice added Electricity (120.00 GBP). Your share is 60.00 GBP.");
    }

    @Test
    void personalExpenseNotifiesNobody() {
        assertThat(drafts("transaction.created", alice, """
                {"transactionId":"%s","description":"Lunch","amount":"9.50","currency":"GBP","splits":[]}
                """.formatted(Ids.newId()))).isEmpty();
    }

    @Test
    void taskAssignedToSomeoneElseNotifiesTheAssignee() {
        var drafts = drafts("task.assigned", alice, """
                {"taskId":"%s","title":"Pay bill","assigneeUserId":"%s"}""".formatted(Ids.newId(), bob));

        assertThat(drafts).extracting(NotificationDraft::userId).containsExactly(bob);
        assertThat(drafts.get(0).body()).isEqualTo("Alice assigned you \"Pay bill\".");
    }

    @Test
    void assigningYourselfOrAnOutsiderNotifiesNobody() {
        assertThat(drafts("task.assigned", alice, """
                {"taskId":"%s","title":"x","assigneeUserId":"%s"}""".formatted(Ids.newId(), alice))).isEmpty();
        assertThat(drafts("task.assigned", alice, """
                {"taskId":"%s","title":"x","assigneeUserId":"%s"}""".formatted(Ids.newId(), outsider))).isEmpty();
    }

    @Test
    void overdueTaskWithoutAssigneeGoesToEveryMember() {
        var drafts = drafts("task.overdue", null, """
                {"taskId":"%s","title":"Bins","assigneeUserId":null}""".formatted(Ids.newId()));

        assertThat(drafts).extracting(NotificationDraft::userId).containsExactlyInAnyOrder(alice, bob, carol);
    }

    @Test
    void calendarEventOnlyReachesAttendeesWhoAreMembersAndNotTheActor() {
        var drafts = drafts("calendar.event.created", alice, """
                {"calendarEventId":"%s","title":"Parents' evening","startsAt":"2026-11-25T17:30:00Z",
                 "attendeeUserIds":["%s","%s","%s"]}""".formatted(Ids.newId(), alice, bob, outsider));

        assertThat(drafts).extracting(NotificationDraft::userId).containsExactly(bob);
    }

    @Test
    void shoppingUpdatesAreGroupedPerList() {
        UUID list = Ids.newId();
        var drafts = drafts("shopping.item.added", alice, """
                {"shoppingListId":"%s","listName":"Weekly shop","itemId":"%s","name":"Milk","quantity":"2"}
                """.formatted(list, Ids.newId()));

        assertThat(drafts).extracting(NotificationDraft::userId).containsExactlyInAnyOrder(bob, carol);
        assertThat(drafts.get(0).groupKey()).isEqualTo("shopping:" + list + ":added");
        assertThat(drafts.get(0).groupedBodyFormat().formatted(3)).isEqualTo("3 items added to Weekly shop.");
    }

    @Test
    void invitationReachesAnExistingUserOnly() {
        assertThat(drafts("member.invited", alice, """
                {"householdId":"%s","invitedUserId":"%s","role":"MEMBER"}""".formatted(household, outsider)))
                .extracting(NotificationDraft::userId).containsExactly(outsider);
        assertThat(drafts("member.invited", alice, """
                {"householdId":"%s","invitedUserId":null,"role":"MEMBER"}""".formatted(household))).isEmpty();
    }

    @Test
    void removalTellsTheRemovedMemberButLeavingTellsNobody() {
        assertThat(drafts("member.removed", alice, """
                {"householdId":"%s","userId":"%s","reason":"REMOVED"}""".formatted(household, outsider)))
                .extracting(NotificationDraft::userId).containsExactly(outsider);
        assertThat(drafts("member.removed", bob, """
                {"householdId":"%s","userId":"%s","reason":"LEFT"}""".formatted(household, bob))).isEmpty();
    }

    @Test
    void unknownEventsProduceNothing() {
        assertThat(drafts("document.uploaded", alice, "{}")).isEmpty();
    }

    /** Runs the rules for an event of this type in the test household. */
    private List<NotificationDraft> drafts(String type, UUID actor, String payload) {
        JsonNode node = json.readTree(payload);
        return rules.draftsFor(new EventEnvelope<>(Ids.newId(), type, 1, Instant.now(), household, actor, node), directory);
    }

    /** One household whose members and names are set by the test. */
    private final class FakeDirectory implements Directory {

        final Map<UUID, String> members = new LinkedHashMap<>();
        final Map<UUID, String> names = new HashMap<>();

        /** Every workspace is called "Home". */
        @Override
        public String workspaceName(UUID workspaceId) {
            return "Home";
        }

        /** The test household's members; other workspaces are empty. */
        @Override
        public Map<UUID, String> activeMembers(UUID workspaceId) {
            return household.equals(workspaceId) ? members : Map.of();
        }

        /** The name set by the test, or "Someone". */
        @Override
        public String displayName(UUID userId) {
            return names.getOrDefault(userId, "Someone");
        }
    }
}
