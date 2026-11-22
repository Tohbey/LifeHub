import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CalendarEvent, Frequency, PlannerApi } from '../../core/api/planner.api';
import { WorkspaceService } from '../../core/workspace/workspace.service';
import { dateTime, toInstant, toLocalInput } from '../../shared/format';
import { runWithToast } from '../../shared/run';
import { ToastService } from '../../shared/toast.service';

/** planner calendar for the current workspace: add events with attendees and reminders, cancel them. */
@Component({
  selector: 'lh-calendar',
  imports: [FormsModule],
  template: `
    <header class="page-head"><div class="grow"><h1>Calendar</h1><p class="subtitle">Appointments, who is going, and reminders.</p></div></header>
    <section class="card">
      <h2>New event</h2>
      <form class="form" (ngSubmit)="create()">
        <div class="row">
          <label class="grow">Title <input name="title" required maxlength="200" [(ngModel)]="title" /></label>
          <label>Where <input name="location" maxlength="200" [(ngModel)]="location" /></label>
        </div>
        <div class="row">
          <label>Starts <input name="startsAt" type="datetime-local" required [min]="minStart()" [(ngModel)]="startsAt" /></label>
          <label>Ends <input name="endsAt" type="datetime-local" required [(ngModel)]="endsAt" /></label>
          <label class="checkbox"><input type="checkbox" name="allDay" [(ngModel)]="allDay" /> All day</label>
          <label>Remind
            <select name="reminder" [(ngModel)]="reminder">
              <option [ngValue]="null">No reminder</option><option [ngValue]="0">At the start</option>
              <option [ngValue]="15">15 min before</option><option [ngValue]="60">1 hour before</option>
              <option [ngValue]="1440">1 day before</option>
            </select>
          </label>
        </div>
        <div class="row">
          <label>Repeat
            <select name="frequency" [(ngModel)]="frequency">
              <option [ngValue]="null">Does not repeat</option>
              <option value="DAILY">Daily</option><option value="WEEKLY">Weekly</option>
              <option value="MONTHLY">Monthly</option><option value="YEARLY">Yearly</option>
            </select>
          </label>
          @if (frequency) {
            <label>Every <input name="interval" type="number" min="1" max="99" class="short" [(ngModel)]="interval" /></label>
            <label>Until (optional) <input name="until" type="date" [min]="startsAt.slice(0, 10)" [(ngModel)]="until" /></label>
          }
        </div>
        @if (startInPast()) { <p class="hint error-text">An event cannot start in the past.</p> }
        @if (ws.members().length > 1) {
          <fieldset>
            <legend>Attendees</legend>
            @for (m of ws.members(); track m.userId) {
              <label class="checkbox"><input type="checkbox" [name]="'att-' + m.userId" [checked]="attendees.has(m.userId)" (change)="toggleAttendee(m.userId)" /> {{ ws.name(m.userId) }}</label>
            }
          </fieldset>
        }
        <button class="button" [disabled]="busy() || startInPast()">Add event</button>
      </form>
    </section>

    <section class="card">
      <div class="row">
        <h2 class="grow">Coming up</h2>
        <label>From <input name="from" type="datetime-local" [(ngModel)]="from" (change)="load()" /></label>
        <label>To <input name="to" type="datetime-local" [(ngModel)]="to" (change)="load()" /></label>
        <label class="checkbox"><input type="checkbox" name="cancelled" [(ngModel)]="includeCancelled" (ngModelChange)="load()" /> Show cancelled</label>
      </div>
      <table class="table">
        <thead><tr><th>When</th><th>Event</th><th>Attendees</th><th>Reminder</th><th></th></tr></thead>
        <tbody>
          @for (e of events(); track e.id) {
            <tr [class.done]="e.status === 'CANCELLED'">
              <td>{{ dateTime(e.startsAt) }}<div class="muted small">to {{ dateTime(e.endsAt) }}</div></td>
              <td>{{ e.title }} @if (e.status === 'CANCELLED') { <span class="tag">cancelled</span> }
                @if (e.recurrence) { <span class="tag">{{ repeatLabel(e) }}</span> }
                @if (e.location) { <div class="muted small">{{ e.location }}</div> }</td>
              <td>@for (a of e.attendeeUserIds; track a) { {{ ws.name(a) }}{{ $last ? '' : ', ' }} } @empty { <span class="muted">–</span> }</td>
              <td>{{ e.reminderMinutesBefore === null ? '–' : e.reminderMinutesBefore + ' min before' }}</td>
              <td class="actions">
                @if (e.status === 'ACTIVE') { <button class="button ghost danger" (click)="cancel(e)">Cancel</button> }
              </td>
            </tr>
          } @empty { <tr><td colspan="5" class="muted">Nothing in this period.</td></tr> }
        </tbody>
      </table>
    </section>
  `,
})
export class Calendar implements OnInit {
  protected readonly ws = inject(WorkspaceService);
  private readonly api = inject(PlannerApi);
  private readonly toasts = inject(ToastService);

  protected readonly events = signal<CalendarEvent[]>([]);
  protected readonly busy = signal(false);
  protected readonly dateTime = dateTime;
  protected title = '';
  protected location = '';
  protected startsAt = toLocalInput(new Date(Date.now() + 3600000));
  protected endsAt = toLocalInput(new Date(Date.now() + 7200000));
  protected allDay = false;
  protected reminder: number | null = 15;
  protected attendees = new Set<string>();
  protected from = toLocalInput(new Date());
  protected to = toLocalInput(new Date(Date.now() + 31 * 86400000));
  protected includeCancelled = false;
  protected frequency: Frequency | null = null;
  protected interval = 1;
  protected until = '';

  /** Includes the caller as an attendee by default, then loads the next 31 days. */
  ngOnInit(): void {
    const me = this.ws.me()?.id;
    if (me) {
      this.attendees.add(me);
    }
    void this.load();
  }

  /** GET /events for the range. */
  protected async load(): Promise<void> {
    await runWithToast(this.busy, this.toasts, async () => this.events.set(
      (await this.api.events(this.id, toInstant(this.from), toInstant(this.to), this.includeCancelled)).items));
  }

  /** POST /events. */
  protected async create(): Promise<void> {
    await runWithToast(this.busy, this.toasts, async () => {
      await this.api.createEvent(this.id, {
        title: this.title, location: this.location || undefined, startsAt: toInstant(this.startsAt)!,
        endsAt: toInstant(this.endsAt)!, allDay: this.allDay, reminderMinutesBefore: this.reminder,
        attendeeUserIds: [...this.attendees],
        recurrence: this.frequency ? { frequency: this.frequency, interval: Number(this.interval) || 1, until: this.until || null } : null,
      });
      this.title = '';
      this.location = '';
      this.frequency = null;
      this.until = '';
      this.toasts.success('Event added; attendees are notified.');
      await this.load();
    });
  }

  /** PATCH /events/{id} with cancelled: true. */
  protected async cancel(e: CalendarEvent): Promise<void> {
    if (!confirm(`Cancel "${e.title}"?`)) {
      return;
    }
    await runWithToast(this.busy, this.toasts, async () => {
      await this.api.updateEvent(this.id, e.id, { cancelled: true });
      this.toasts.success('Event cancelled.');
      await this.load();
    });
  }

  /** The earliest start the picker offers: now, to the minute. */
  protected minStart(): string {
    return toLocalInput(new Date());
  }

  /** True when the chosen start is already past; the API refuses those too. */
  protected startInPast(): boolean {
    return !!this.startsAt && new Date(this.startsAt).getTime() < Date.now() - 60_000;
  }

  /** "Repeats weekly", "Every 2 weeks until 2026-12-31" and so on. */
  protected repeatLabel(e: CalendarEvent): string {
    const r = e.recurrence!;
    const unit = { DAILY: 'day', WEEKLY: 'week', MONTHLY: 'month', YEARLY: 'year' }[r.frequency];
    const base = r.interval === 1 ? `Repeats ${r.frequency.toLowerCase()}` : `Every ${r.interval} ${unit}s`;
    return r.until ? `${base} until ${r.until}` : base;
  }

  /** Adds or removes an attendee. */
  protected toggleAttendee(userId: string): void {
    if (!this.attendees.delete(userId)) {
      this.attendees.add(userId);
    }
  }

  /** The current workspace's ID. */
  private get id(): string {
    return this.ws.current()!.id;
  }
}
