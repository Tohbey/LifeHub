import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { params } from './http-params';
import { Page } from './models';

export type NotificationType =
  | 'HOUSEHOLD_INVITATION' | 'HOUSEHOLD_MEMBERSHIP' | 'SHARED_EXPENSE' | 'BUDGET_ALERT' | 'TASK_ASSIGNED'
  | 'TASK_COMPLETED' | 'TASK_OVERDUE' | 'CALENDAR_UPDATE' | 'CALENDAR_REMINDER' | 'SHOPPING_LIST_UPDATE';

export interface AppNotification {
  id: string; workspaceId: string | null; type: NotificationType; title: string; body: string;
  link: string | null; count: number; read: boolean; createdAt: string;
}
export interface NotificationPreference { type: NotificationType; inApp: boolean; email: boolean; }

/** notification: the user's notifications and settings. The live stream is in NotificationStream. */
@Injectable({ providedIn: 'root' })
export class NotificationApi {
  private readonly http = inject(HttpClient);

  /** GET /me/notifications, newest first. */
  list(unread: boolean, page = 0, size = 20): Promise<Page<AppNotification>> {
    return firstValueFrom(this.http.get<Page<AppNotification>>('/api/v1/me/notifications',
      { params: params({ unread, page, size }) }));
  }

  /** GET /me/notifications/unread-count. */
  unreadCount(): Promise<{ unread: number }> {
    return firstValueFrom(this.http.get<{ unread: number }>('/api/v1/me/notifications/unread-count'));
  }

  /** PATCH /me/notifications/{id}. */
  markRead(id: string, read: boolean): Promise<AppNotification> {
    return firstValueFrom(this.http.patch<AppNotification>(`/api/v1/me/notifications/${id}`, { read }));
  }

  /** POST /me/notifications/read-all. */
  markAllRead(): Promise<{ unread: number }> {
    return firstValueFrom(this.http.post<{ unread: number }>('/api/v1/me/notifications/read-all', null));
  }

  /** GET /me/notification-preferences. */
  preferences(): Promise<NotificationPreference[]> {
    return firstValueFrom(this.http.get<NotificationPreference[]>('/api/v1/me/notification-preferences'));
  }

  /** PUT /me/notification-preferences. */
  updatePreferences(preferences: NotificationPreference[]): Promise<NotificationPreference[]> {
    return firstValueFrom(this.http.put<NotificationPreference[]>('/api/v1/me/notification-preferences', { preferences }));
  }
}
