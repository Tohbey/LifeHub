import { Component, OnInit, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { AppNotification, NotificationApi } from '../../core/api/notification.api';
import { Page } from '../../core/api/models';
import { NotificationStream } from '../../core/notifications/notification-stream.service';
import { WorkspaceService } from '../../core/workspace/workspace.service';
import { dateTime } from '../../shared/format';
import { runWithToast } from '../../shared/run';
import { ToastService } from '../../shared/toast.service';

/** The user's notifications (notification service). New ones appear live from the stream. */
@Component({
  selector: 'lh-notifications',
  imports: [FormsModule],
  template: `
    <header class="page-head"><div class="grow"><h1>Notifications</h1><p class="subtitle">Everything that happened while you were away.</p></div></header>
    <section class="card">
      <div class="row">
        <p class="grow">{{ stream.unread() }} unread · live updates {{ stream.connected() ? 'connected' : 'reconnecting…' }}</p>
        <label class="checkbox"><input type="checkbox" name="unread" [(ngModel)]="unreadOnly" (ngModelChange)="load(0)" /> Unread only</label>
        <button class="button ghost" (click)="markAll()" [disabled]="busy()">Mark all read</button>
      </div>
      <ul class="list notifications">
        @for (n of page()?.items ?? []; track n.id) {
          <li [class.unread]="!n.read">
            <div class="grow clickable" (click)="open(n)">
              <strong>{{ n.title }}</strong> @if (n.count > 1) { <span class="tag">×{{ n.count }}</span> }
              <div>{{ n.body }}</div>
              <div class="muted small">{{ dateTime(n.createdAt) }} · {{ workspaceName(n.workspaceId) }} · {{ n.type }}</div>
            </div>
            <button class="button ghost" (click)="toggle(n)">{{ n.read ? 'Mark unread' : 'Mark read' }}</button>
          </li>
        } @empty { <li class="muted">No notifications.</li> }
      </ul>
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
export class Notifications implements OnInit {
  protected readonly stream = inject(NotificationStream);
  private readonly ws = inject(WorkspaceService);
  private readonly api = inject(NotificationApi);
  private readonly toasts = inject(ToastService);
  private readonly router = inject(Router);

  protected readonly page = signal<Page<AppNotification> | null>(null);
  protected readonly busy = signal(false);
  protected readonly dateTime = dateTime;
  protected unreadOnly = false;
  protected current = 0;

  /** Reloads the first page whenever a notification arrives live. */
  constructor() {
    effect(() => {
      if (this.stream.latest() && this.current === 0) {
        void this.load(0);
      }
    });
  }

  /** Loads the newest page. */
  ngOnInit(): void {
    void this.load(0);
  }

  /** GET /me/notifications. */
  protected async load(page: number): Promise<void> {
    this.current = page;
    await runWithToast(this.busy, this.toasts, async () => this.page.set(await this.api.list(this.unreadOnly, page)));
  }

  /** PATCH /me/notifications/{id}. */
  protected async toggle(n: AppNotification): Promise<void> {
    await runWithToast(this.busy, this.toasts, async () => {
      await this.api.markRead(n.id, !n.read);
      await this.stream.refreshCount();
      await this.load(this.current);
    });
  }

  /** POST /me/notifications/read-all. */
  protected async markAll(): Promise<void> {
    await runWithToast(this.busy, this.toasts, async () => {
      await this.api.markAllRead();
      await this.stream.refreshCount();
      await this.load(this.current);
    });
  }

  /** Marks it read and opens what it points at, switching workspace when needed. */
  protected async open(n: AppNotification): Promise<void> {
    if (!n.read) {
      await this.toggle(n);
    }
    if (!n.link) {
      return;
    }
    if (n.workspaceId && n.workspaceId !== this.ws.current()?.id && this.ws.workspaces().some((w) => w.id === n.workspaceId)) {
      await this.ws.select(n.workspaceId);
    }
    const target = n.link.replace(/^\/finance\/.*/, '/finance').replace(/^\/planner\/tasks\/.*/, '/tasks')
      .replace(/^\/planner\/events\/.*/, '/calendar').replace(/^\/shopping\/lists\//, '/shopping/')
      .replace(/^\/households\/.*/, '/household');
    await this.router.navigateByUrl(target);
  }

  /** The workspace's name, if the user still belongs to it. */
  protected workspaceName(id: string | null): string {
    const w = this.ws.workspaces().find((x) => x.id === id);
    return w ? (w.type === 'PERSONAL' ? 'Personal' : w.name) : '–';
  }
}
