import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { params } from './http-params';
import { Page } from './models';

export interface AuditEntry {
  eventId: string; type: string; version: number; occurredAt: string; actorUserId: string | null;
  /** The actor's display name, or null for system events and users who never set one. */
  actorName: string | null;
  payload: unknown;
  /** User ID to display name, for every user the event mentions whose name is known. */
  people: Record<string, string>;
}

/** audit: the workspace's event log, for owners and admins. */
@Injectable({ providedIn: 'root' })
export class AuditApi {
  private readonly http = inject(HttpClient);

  /** GET /workspaces/{id}/audit, newest first. */
  entries(ws: string, type: string, page = 0): Promise<Page<AuditEntry>> {
    return firstValueFrom(this.http.get<Page<AuditEntry>>(`/api/v1/workspaces/${ws}/audit`,
      { params: params({ type, page, size: 50 }) }));
  }
}
