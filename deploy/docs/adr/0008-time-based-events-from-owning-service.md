# 0008. Time-based events are raised by the owning service

- Status: Proposed
- Date: 2026-11-15

## Context

Some notifications depend on time rather than on a user action: overdue tasks, upcoming calendar events, expiring documents and warranties, budget periods. The README described both "the notification service runs a reminder scheduler" and "planner publishes `task.overdue`", which contradict each other. A reminder scheduler in `notification` would need data it does not own (due dates, expiry dates, reminder offsets).

## Decision

- The service that owns the data detects the time-based condition and publishes the event: `planner` publishes `task.overdue` and `calendar.event.reminder.due`, `document-inventory` publishes `document.expiring` and `warranty.expiring`.
- Each owning service runs its own scheduled scan (for example every few minutes for reminders, daily for expiry) and records what it has already announced, so a condition is published once.
- `notification` only consumes events, decides recipients, checks preferences and delivers. It has no business scheduler. Its only scheduled work is retrying failed deliveries.
- There is no generic `reminder.due` event.

## Consequences

- No service needs another service's data to send a reminder.
- Each owning service carries a small scheduler and an "already announced" record (for example a `reminder_sent_at` column).
- If a service runs more than one instance later, its scan needs a lock (for example ShedLock) so a condition is not announced twice. Consumers are idempotent anyway.
