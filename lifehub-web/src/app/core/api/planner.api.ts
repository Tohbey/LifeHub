import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { params } from './http-params';
import { Page } from './models';

export type TaskStatus = 'OPEN' | 'DONE';
export interface Task {
  id: string; title: string; description: string | null; assigneeUserId: string | null; dueAt: string | null;
  status: TaskStatus; completedAt: string | null; completedByUserId: string | null; createdByUserId: string; createdAt: string;
}
export type Frequency = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY';
/** How an event repeats; {@code until} is the last day (YYYY-MM-DD) an occurrence may start, or null. */
export interface Recurrence { frequency: Frequency; interval: number; until: string | null; }
export interface CalendarEvent {
  id: string; title: string; description: string | null; location: string | null; startsAt: string; endsAt: string;
  allDay: boolean; reminderMinutesBefore: number | null; status: 'ACTIVE' | 'CANCELLED'; attendeeUserIds: string[];
  createdByUserId: string; recurrence: Recurrence | null;
}
export interface UpdateTask {
  title?: string; description?: string; assigneeUserId?: string; unassign?: boolean; dueAt?: string;
  clearDueAt?: boolean; status?: TaskStatus;
}

/** planner: tasks and calendar events of one workspace. */
@Injectable({ providedIn: 'root' })
export class PlannerApi {
  private readonly http = inject(HttpClient);

  /** GET /workspaces/{id}/tasks. */
  tasks(ws: string, status: TaskStatus | '', mine: boolean, page = 0): Promise<Page<Task>> {
    return firstValueFrom(this.http.get<Page<Task>>(`/api/v1/workspaces/${ws}/tasks`,
      { params: params({ status, mine, page, size: 50 }) }));
  }

  /** POST /workspaces/{id}/tasks. */
  createTask(ws: string, body: { title: string; description?: string; assigneeUserId?: string | null; dueAt?: string | null }): Promise<Task> {
    return firstValueFrom(this.http.post<Task>(`/api/v1/workspaces/${ws}/tasks`, body));
  }

  /** PATCH /workspaces/{id}/tasks/{taskId}. */
  updateTask(ws: string, id: string, body: UpdateTask): Promise<Task> {
    return firstValueFrom(this.http.patch<Task>(`/api/v1/workspaces/${ws}/tasks/${id}`, body));
  }

  /** DELETE /workspaces/{id}/tasks/{taskId}: the creator, or an owner or admin. */
  deleteTask(ws: string, id: string): Promise<unknown> {
    return firstValueFrom(this.http.delete(`/api/v1/workspaces/${ws}/tasks/${id}`));
  }

  /** GET /workspaces/{id}/events overlapping [from, to); a recurring event comes once per occurrence. */
  events(ws: string, from?: string, to?: string, includeCancelled = false): Promise<Page<CalendarEvent>> {
    return firstValueFrom(this.http.get<Page<CalendarEvent>>(`/api/v1/workspaces/${ws}/events`,
      { params: params({ from, to, includeCancelled, size: 100 }) }));
  }

  /** POST /workspaces/{id}/events. */
  createEvent(ws: string, body: {
    title: string; description?: string; location?: string; startsAt: string; endsAt: string; allDay: boolean;
    reminderMinutesBefore?: number | null; attendeeUserIds: string[]; recurrence?: Recurrence | null;
  }): Promise<CalendarEvent> {
    return firstValueFrom(this.http.post<CalendarEvent>(`/api/v1/workspaces/${ws}/events`, body));
  }

  /** PATCH /workspaces/{id}/events/{eventId}; {cancelled: true} cancels. */
  updateEvent(ws: string, id: string, body: Record<string, unknown>): Promise<CalendarEvent> {
    return firstValueFrom(this.http.patch<CalendarEvent>(`/api/v1/workspaces/${ws}/events/${id}`, body));
  }
}
