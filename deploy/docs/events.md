# Event catalogue

The single source of truth for every Kafka event in LifeHub: topic, type, version, producer, consumers and payload. If an event is not listed here, it must not be published. Change this file in the same commit as the code that produces or consumes the change.

Background: [ADR 0006](adr/0006-transactional-outbox.md) (outbox and idempotency), [ADR 0004](adr/0004-uuid-identifiers.md) (IDs), [ADR 0008](adr/0008-time-based-events-from-owning-service.md) (time-based events).

## Envelope

Every event uses this envelope, defined once in `lifehub-starter`:

```json
{
  "eventId": "0193a4b2-7c1e-7d3a-9f00-2b6c1e4d8a10",
  "type": "task.assigned",
  "version": 1,
  "occurredAt": "2026-11-11T09:15:00Z",
  "workspaceId": "0193a4a0-1b2c-7e4f-8a00-5d6e7f809a1b",
  "actorUserId": "0193a49f-0c1d-7a2b-9c00-3e4f5a6b7c8d",
  "payload": {
    "taskId": "0193a4b2-6f00-7b1c-8d00-1a2b3c4d5e6f",
    "assigneeUserId": "0193a4a1-2c3d-7f50-9b00-6e7f8091a2b3",
    "title": "Pay electricity bill"
  }
}
```

| Field | Type | Rules |
|---|---|---|
| `eventId` | UUID | UUIDv7, generated when the outbox row is written. Consumers dedupe on it. |
| `type` | string | `noun.verb` in the past tense, lowercase, dot separated. Listed below. |
| `version` | integer | Starts at 1. Bump only for a breaking change to `payload`. |
| `occurredAt` | ISO 8601 UTC | When the business change happened, not when it was published. |
| `workspaceId` | UUID or null | The workspace the change belongs to. Null only for events with no workspace (`user.created`). |
| `actorUserId` | UUID or null | The LifeHub user who caused the change. Null for system-raised events (schedulers). |
| `payload` | object | Event specific, documented below. |

### Kafka record

| Part | Value |
|---|---|
| Key | `workspaceId`, or the user ID for events with no workspace. Guarantees order per workspace. |
| Value | The envelope, as UTF-8 JSON. |
| Headers | `traceparent` (W3C trace context) for correlation, `type` and `version` duplicated for routing without parsing. |

### Payload rules

- IDs are UUID strings ([ADR 0004](adr/0004-uuid-identifiers.md)).
- Money is a **string** decimal plus a currency, for example `"amount": "120.00", "currency": "GBP"`. Never a JSON number, which may be parsed as a float.
- Timestamps are ISO 8601 UTC. Dates without a time (expiry dates) are `YYYY-MM-DD`.
- Include what consumers need to act and to render a notification (IDs, a title or name), so they do not have to call back the producer. Do not include file contents, tokens, notes bodies or anything a consumer has no reason to store.
- Adding an optional field is not a breaking change. Removing a field, renaming it, or changing its type or meaning is, and needs a new `version`. Consumers ignore unknown fields.
- When a version is bumped, the producer publishes the new version only after every consumer handles it.

## Topics

| Topic | Producer | Partitions | Retention |
|---|---|---|---|
| `identity.events` | identity-workspace | 3 | 14 days |
| `finance.events` | finance | 3 | 14 days |
| `task.events` | planner | 3 | 14 days |
| `calendar.events` | planner | 3 | 14 days |
| `shopping.events` | shopping-meals | 3 | 14 days |
| `document.events` | document-inventory | 3 | 14 days |
| `<topic>.retry` | consumers | 1 | 7 days |
| `<topic>.dlt` | consumers | 1 | 30 days |

Partition counts and retention are starting values for a single broker, set by the topic script in `deploy/kafka/`. Kafka is not the source of truth ([README, backups](../../README.md#backups)), so retention only needs to cover consumer downtime and replay.

Consumer group IDs are the consuming service's name, for example `notification` and `audit`.

## Events

Status: **Phase 1** events are needed for the foundation. Later events are proposals until their service is built, and may change freely until then.

Consumers: **N** = notification, **A** = audit. Audit consumes every event on every topic and stores the envelope as is.

### identity.events

| Type | v | Phase | Consumers | When |
|---|---|---|---|---|
| `user.created` | 1 | 1 | A | First login provisions a LifeHub user and personal workspace |
| `household.created` | 1 | 1 | A | A household and its shared workspace are created |
| `household.updated` | 1 | 1 | A | Household name or settings change |
| `member.invited` | 1 | 1 | N, A | An invitation is created |
| `member.added` | 1 | 1 | N, A | An invitation is accepted |
| `member.removed` | 1 | 1 | N, A | A member is removed or leaves |
| `member.role.changed` | 1 | 1 | N, A | A member's role changes |

```jsonc
// user.created (workspaceId: null, key: userId)
{ "userId": "…", "personalWorkspaceId": "…", "displayName": "Tobi" }

// household.created
{ "householdId": "…", "name": "Fafowora Home", "ownerUserId": "…" }

// household.updated
{ "householdId": "…", "name": "Fafowora Home", "changedFields": ["name"] }

// member.invited
{ "householdId": "…", "invitationId": "…", "invitedEmail": "sam@example.com",
  "invitedUserId": null, "role": "MEMBER", "expiresAt": "2026-11-22T09:15:00Z" }

// member.added
{ "householdId": "…", "userId": "…", "role": "MEMBER", "invitationId": "…" }

// member.removed
{ "householdId": "…", "userId": "…", "reason": "REMOVED" }   // REMOVED | LEFT

// member.role.changed
{ "householdId": "…", "userId": "…", "previousRole": "MEMBER", "newRole": "ADMIN" }
```

`invitedUserId` is set when the email already belongs to a LifeHub user, so notification can tell them in-app. Roles are `OWNER`, `ADMIN`, `MEMBER`.

### finance.events

| Type | v | Phase | Consumers | When |
|---|---|---|---|---|
| `transaction.created` | 1 | 2 | N, A | Income or expense recorded |
| `transaction.updated` | 1 | 2 | A | A transaction or its splits change |
| `transaction.deleted` | 1 | 2 | A | A transaction is soft deleted |
| `budget.threshold.exceeded` | 1 | 2 | N, A | Spending in a budget period crosses a threshold |

```jsonc
// transaction.created
{ "transactionId": "…", "type": "EXPENSE", "description": "Electricity bill",
  "amount": "120.00", "currency": "GBP", "categoryId": "…",
  "payerUserId": "…", "occurredAt": "2026-11-09T10:00:00Z",
  "splits": [ { "userId": "…", "amount": "60.00" }, { "userId": "…", "amount": "60.00" } ] }

// transaction.updated
{ "transactionId": "…", "changedFields": ["amount", "splits"],
  "amount": "130.00", "currency": "GBP",
  "splits": [ { "userId": "…", "amount": "65.00" }, { "userId": "…", "amount": "65.00" } ] }

// transaction.deleted
{ "transactionId": "…" }

// budget.threshold.exceeded (actorUserId: the user whose transaction crossed it)
{ "budgetId": "…", "categoryId": "…", "periodStart": "2026-11-01", "periodEnd": "2026-11-30",
  "limit": "300.00", "spent": "312.40", "currency": "GBP", "thresholdPercent": 100 }
```

`splits` is empty for personal transactions. Notification tells split participants other than the actor about shared expenses. `budget.threshold.exceeded` is published once per budget, period and threshold (for example 80 and 100).

### task.events

| Type | v | Phase | Consumers | When |
|---|---|---|---|---|
| `task.created` | 1 | 2 | A | A task is created |
| `task.assigned` | 1 | 2 | N, A | A task gets a new assignee |
| `task.completed` | 1 | 2 | N, A | A task is marked done |
| `task.overdue` | 1 | 2 | N, A | Planner's scheduler finds an open task past its due time |

```jsonc
// task.created
{ "taskId": "…", "title": "Pay electricity bill", "assigneeUserId": null, "dueAt": "2026-11-20T18:00:00Z" }

// task.assigned
{ "taskId": "…", "title": "Pay electricity bill", "assigneeUserId": "…",
  "previousAssigneeUserId": null, "dueAt": "2026-11-20T18:00:00Z" }

// task.completed
{ "taskId": "…", "title": "Pay electricity bill", "completedByUserId": "…" }

// task.overdue (actorUserId: null)
{ "taskId": "…", "title": "Pay electricity bill", "assigneeUserId": "…", "dueAt": "2026-11-20T18:00:00Z" }
```

### calendar.events

| Type | v | Phase | Consumers | When |
|---|---|---|---|---|
| `calendar.event.created` | 1 | 2 | N, A | An event is added to a calendar |
| `calendar.event.updated` | 1 | 2 | N, A | Time, title or attendees change |
| `calendar.event.cancelled` | 1 | 2 | N, A | An event is cancelled |
| `calendar.event.reminder.due` | 1 | 2 | N | Planner's scheduler reaches an event's reminder time |

```jsonc
// calendar.event.created
{ "calendarEventId": "…", "title": "Parents' evening", "startsAt": "2026-11-25T17:30:00Z",
  "endsAt": "2026-11-25T19:00:00Z", "allDay": false, "attendeeUserIds": ["…", "…"] }

// calendar.event.updated
{ "calendarEventId": "…", "title": "Parents' evening", "startsAt": "…", "endsAt": "…",
  "attendeeUserIds": ["…"], "changedFields": ["startsAt", "endsAt"] }

// calendar.event.cancelled
{ "calendarEventId": "…", "title": "Parents' evening", "startsAt": "…" }

// calendar.event.reminder.due (actorUserId: null)
{ "calendarEventId": "…", "title": "Parents' evening", "startsAt": "…", "attendeeUserIds": ["…"] }
```

Events in a personal workspace are published like any other. Notification only ever notifies members of the event's workspace, so a private appointment cannot leak to a household.

### shopping.events

| Type | v | Phase | Consumers | When |
|---|---|---|---|---|
| `shopping.item.added` | 1 | 2 | N, A | An item is added to a list |
| `shopping.item.purchased` | 1 | 2 | N, A | An item is ticked off |

```jsonc
// shopping.item.added
{ "shoppingListId": "…", "listName": "Weekly shop", "itemId": "…", "name": "Milk", "quantity": "2" }

// shopping.item.purchased
{ "shoppingListId": "…", "listName": "Weekly shop", "itemId": "…", "name": "Milk" }
```

Notification batches these per list (for example one notification per list per few minutes) rather than one per item. That is a notification concern and does not change the events.

### document.events

| Type | v | Phase | Consumers | When |
|---|---|---|---|---|
| `document.uploaded` | 1 | 3 | A | A document is stored |
| `document.deleted` | 1 | 3 | A | A document is soft deleted |
| `document.expiring` | 1 | 3 | N, A | A document's expiry date is within the reminder window |
| `warranty.expiring` | 1 | 3 | N, A | An inventory item's warranty is within the reminder window |

```jsonc
// document.uploaded
{ "documentId": "…", "title": "Passport", "contentType": "application/pdf", "sizeBytes": 482113, "expiresOn": "2031-03-02" }

// document.deleted
{ "documentId": "…" }

// document.expiring (actorUserId: null)
{ "documentId": "…", "title": "Passport", "expiresOn": "2031-03-02", "daysRemaining": 30 }

// warranty.expiring (actorUserId: null)
{ "inventoryItemId": "…", "name": "Washing machine", "warrantyExpiresOn": "2027-01-10", "daysRemaining": 30 }
```

The file itself never goes through Kafka.

### Not yet catalogued

`notes`, savings goals, recurring transactions and meal plans publish nothing yet. Add their events here before writing the producer.
