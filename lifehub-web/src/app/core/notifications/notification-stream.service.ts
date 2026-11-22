import { Injectable, inject, signal } from '@angular/core';

import { AppNotification, NotificationApi } from '../api/notification.api';
import { AuthService } from '../auth/auth.service';
import { ToastService } from '../../shared/toast.service';

/**
 * Keeps the unread badge current and shows new notifications as they arrive, over the server-sent events
 * stream. Uses fetch rather than EventSource, because EventSource cannot send the bearer token.
 * Reconnects with backoff; the server closes streams after 30 minutes.
 */
@Injectable({ providedIn: 'root' })
export class NotificationStream {
  private readonly auth = inject(AuthService);
  private readonly api = inject(NotificationApi);
  private readonly toasts = inject(ToastService);

  readonly unread = signal(0);
  readonly connected = signal(false);
  /** The last notification received live, so open pages can refresh. */
  readonly latest = signal<AppNotification | null>(null);

  private controller?: AbortController;
  private retryMs = 2000;

  /** Reads the unread count and opens the stream. Safe to call more than once. */
  async start(): Promise<void> {
    if (this.controller) {
      return;
    }
    await this.refreshCount();
    this.controller = new AbortController();
    void this.connect(this.controller.signal);
  }

  /** Closes the stream, for example on sign-out. */
  stop(): void {
    this.controller?.abort();
    this.controller = undefined;
    this.connected.set(false);
  }

  /** Re-reads the unread count from the API. */
  async refreshCount(): Promise<void> {
    try {
      this.unread.set((await this.api.unreadCount()).unread);
    } catch {
      // The badge is not worth an error message; the notifications page shows real errors.
    }
  }

  /** Opens one stream and reads it until it ends, then schedules a reconnect. */
  private async connect(signal: AbortSignal): Promise<void> {
    try {
      const token = await this.auth.accessToken();
      const response = await fetch('/api/v1/me/notifications/stream', {
        headers: { Authorization: `Bearer ${token}`, Accept: 'text/event-stream' },
        signal,
      });
      if (!response.ok || !response.body) {
        throw new Error(`stream answered ${response.status}`);
      }
      this.connected.set(true);
      this.retryMs = 2000;
      await this.read(response.body, signal);
    } catch (error) {
      if (signal.aborted) {
        return;
      }
      console.warn('Notification stream closed', error);
    }
    this.connected.set(false);
    if (!signal.aborted) {
      setTimeout(() => void this.connect(signal), this.retryMs);
      this.retryMs = Math.min(this.retryMs * 2, 30000);
      void this.refreshCount();
    }
  }

  /** Parses server-sent events: blank-line separated blocks of "event:" and "data:" lines. */
  private async read(body: ReadableStream<Uint8Array>, signal: AbortSignal): Promise<void> {
    const reader = body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    while (!signal.aborted) {
      const { value, done } = await reader.read();
      if (done) {
        return;
      }
      buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n');
      let end: number;
      while ((end = buffer.indexOf('\n\n')) >= 0) {
        this.dispatch(buffer.slice(0, end));
        buffer = buffer.slice(end + 2);
      }
    }
  }

  /** Handles one event block; only "notification" events carry data. */
  private dispatch(block: string): void {
    const notification = parseNotificationEvent(block);
    if (!notification) {
      return;
    }
    this.latest.set(notification);
    void this.refreshCount();
    this.toasts.info(`${notification.title}: ${notification.body}`);
  }
}

/**
 * Parses one server-sent event block ("event:" and "data:" lines). Returns the notification for a "notification"
 * event, or null for anything else (comments, heartbeats, other event names).
 */
export function parseNotificationEvent(block: string): AppNotification | null {
  let event = 'message';
  const data: string[] = [];
  for (const line of block.split('\n')) {
    if (line.startsWith('event:')) {
      event = line.slice(6).trim();
    } else if (line.startsWith('data:')) {
      data.push(line.slice(5).trimStart());
    }
  }
  if (event !== 'notification' || data.length === 0) {
    return null;
  }
  return JSON.parse(data.join('\n')) as AppNotification;
}
