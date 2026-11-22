import { Component, OnInit, inject, signal } from '@angular/core';
import { JsonPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { AuditApi, AuditEntry } from '../../core/api/audit.api';
import { Page } from '../../core/api/models';
import { WorkspaceService } from '../../core/workspace/workspace.service';
import { dateTime } from '../../shared/format';
import { runWithToast } from '../../shared/run';
import { Avatar } from '../../shared/avatar';
import { ToastService } from '../../shared/toast.service';

/** The audit log of the current workspace (audit service). Owners and admins only; members get 403. */
@Component({
  selector: 'lh-audit',
  imports: [FormsModule, JsonPipe, Avatar],
  template: `
    <header class="page-head"><div class="grow"><h1>Audit log</h1><p class="subtitle">Every recorded event in this workspace. Visible to owners and admins.</p></div></header>
    <section class="card">
      <div class="row">
        <label class="grow">Event type <input name="type" [(ngModel)]="type" placeholder="for example transaction.created" (change)="load(0)" /></label>
        <button class="button ghost" (click)="load(0)">Refresh</button>
      </div>
      <table class="table">
        <thead><tr><th>When</th><th>Event</th><th>By</th><th>Payload</th></tr></thead>
        <tbody>
          @for (e of page()?.items ?? []; track e.eventId) {
            <tr>
              <td>{{ dateTime(e.occurredAt) }}</td>
              <td><code>{{ e.type }}</code> <span class="muted small">v{{ e.version }}</span></td>
              <td>
                @if (e.actorUserId) {
                  <span class="person"><lh-avatar [name]="actor(e)" [size]="28" />{{ actor(e) }}</span>
                } @else { <span class="tag muted">system</span> }
              </td>
              <td>
                @if (peopleIn(e).length) {
                  <div class="muted small">People: {{ peopleIn(e).join(', ') }}</div>
                }
                <details><summary>show</summary><pre>{{ withNames(e) | json }}</pre></details>
              </td>
            </tr>
          } @empty { <tr><td colspan="4" class="muted">No events recorded yet.</td></tr> }
        </tbody>
      </table>
      @if ((page()?.totalPages ?? 0) > 1) {
        <div class="pager">
          <button class="button ghost" [disabled]="current === 0" (click)="load(current - 1)">Newer</button>
          <span>Page {{ current + 1 }} of {{ page()?.totalPages }}</span>
          <button class="button ghost" [disabled]="current + 1 >= (page()?.totalPages ?? 0)" (click)="load(current + 1)">Older</button>
        </div>
      }
    </section>
  `,
})
export class Audit implements OnInit {
  protected readonly ws = inject(WorkspaceService);
  private readonly api = inject(AuditApi);
  private readonly toasts = inject(ToastService);

  protected readonly page = signal<Page<AuditEntry> | null>(null);
  protected readonly busy = signal(false);
  protected readonly dateTime = dateTime;
  protected type = '';
  protected current = 0;

  /** Loads the newest page. */
  ngOnInit(): void {
    void this.load(0);
  }

  /** Who did it: the name the audit service knows, a current member's name, or "Unnamed user". */
  protected actor(e: AuditEntry): string {
    if (e.actorUserId === this.ws.me()?.id) {
      return 'You';
    }
    return e.actorName || this.memberName(e.actorUserId) || 'Unnamed user';
  }

  /** The names of everyone the event mentions, other than the actor. */
  protected peopleIn(e: AuditEntry): string[] {
    return Object.entries(e.people ?? {})
      .filter(([id]) => id !== e.actorUserId)
      .map(([id, name]) => (id === this.ws.me()?.id ? 'You' : name));
  }

  /** The payload with every known user ID shown as "Name (id)", so the JSON reads in names. */
  protected withNames(e: AuditEntry): unknown {
    const names: Record<string, string> = { ...e.people };
    for (const m of this.ws.members()) {
      if (m.displayName && !names[m.userId]) {
        names[m.userId] = m.displayName;
      }
    }
    const replace = (value: unknown): unknown => {
      if (typeof value === 'string') {
        return names[value] ? `${names[value]} (${value})` : value;
      }
      if (Array.isArray(value)) {
        return value.map(replace);
      }
      if (value && typeof value === 'object') {
        return Object.fromEntries(Object.entries(value).map(([k, v]) => [k, replace(v)]));
      }
      return value;
    };
    return replace(e.payload);
  }

  /** A current member's display name, if they have one. */
  private memberName(userId: string | null): string | null {
    return this.ws.members().find((m) => m.userId === userId)?.displayName ?? null;
  }

  /** GET /audit, optionally of one event type. */
  protected async load(page: number): Promise<void> {
    this.current = page;
    await runWithToast(this.busy, this.toasts, async () =>
      this.page.set(await this.api.entries(this.ws.current()!.id, this.type.trim(), page)));
  }
}
