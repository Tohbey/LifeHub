import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { params } from './http-params';
import { Page, WorkspaceRole } from './models';

export interface User { id: string; displayName: string | null; email: string | null; emailVerified: boolean; createdAt: string; }
export interface Workspace { id: string; type: 'PERSONAL' | 'HOUSEHOLD'; name: string; role: WorkspaceRole; }
export interface Household { id: string; name: string; defaultCurrency: string; timezone: string; createdAt: string; yourRole: WorkspaceRole; }
export interface Member { userId: string; displayName: string | null; role: WorkspaceRole; joinedAt: string; }
export interface Invitation { id: string; email: string; role: WorkspaceRole; expiresAt: string; token: string; }
export interface PendingInvitation { id: string; email: string; role: WorkspaceRole; invitedByUserId: string; createdAt: string; expiresAt: string; }

/** identity-workspace: profile, workspaces, households, members and invitations. */
@Injectable({ providedIn: 'root' })
export class IdentityApi {
  private readonly http = inject(HttpClient);

  /** GET /me: the signed-in user, created on first call. */
  me(): Promise<User> {
    return firstValueFrom(this.http.get<User>('/api/v1/me'));
  }

  /** PATCH /me: changes display name and/or email; an empty string clears a field. */
  updateMe(body: { displayName?: string; email?: string }): Promise<User> {
    return firstValueFrom(this.http.patch<User>('/api/v1/me', body));
  }

  /** GET /me/workspaces: the workspaces the user can switch between. */
  workspaces(size = 100): Promise<Page<Workspace>> {
    return firstValueFrom(this.http.get<Page<Workspace>>('/api/v1/me/workspaces', { params: params({ size }) }));
  }

  /** POST /households: creates a household with the caller as owner. */
  createHousehold(body: { name: string; defaultCurrency?: string; timezone?: string }): Promise<Household> {
    return firstValueFrom(this.http.post<Household>('/api/v1/households', body));
  }

  /** GET /households/{id}. */
  household(id: string): Promise<Household> {
    return firstValueFrom(this.http.get<Household>(`/api/v1/households/${id}`));
  }

  /** PATCH /households/{id}: owners and admins. */
  updateHousehold(id: string, body: { name?: string; defaultCurrency?: string; timezone?: string }): Promise<Household> {
    return firstValueFrom(this.http.patch<Household>(`/api/v1/households/${id}`, body));
  }

  /** GET /households/{id}/members. */
  members(id: string, size = 100): Promise<Page<Member>> {
    return firstValueFrom(this.http.get<Page<Member>>(`/api/v1/households/${id}/members`, { params: params({ size }) }));
  }

  /** POST /households/{id}/invitations: returns the one-time token. */
  invite(id: string, body: { email: string; role: WorkspaceRole }): Promise<Invitation> {
    return firstValueFrom(this.http.post<Invitation>(`/api/v1/households/${id}/invitations`, body));
  }

  /** GET /households/{id}/invitations: pending invitations, newest first. Owners and admins. */
  invitations(id: string): Promise<Page<PendingInvitation>> {
    return firstValueFrom(this.http.get<Page<PendingInvitation>>(`/api/v1/households/${id}/invitations`, { params: params({ size: 100 }) }));
  }

  /** DELETE /households/{id}/invitations/{invitationId}: revokes a pending invitation. */
  revokeInvitation(id: string, invitationId: string): Promise<unknown> {
    return firstValueFrom(this.http.delete(`/api/v1/households/${id}/invitations/${invitationId}`));
  }

  /** POST /me/email-verification: mails a verification link to the profile email. */
  requestEmailVerification(): Promise<unknown> {
    return firstValueFrom(this.http.post('/api/v1/me/email-verification', null));
  }

  /** POST /me/email-verification/{token}: uses a link from the email. */
  confirmEmail(token: string): Promise<User> {
    return firstValueFrom(this.http.post<User>(`/api/v1/me/email-verification/${encodeURIComponent(token)}`, null));
  }

  /** PATCH /households/{id}/members/{userId}: owner only; OWNER hands over ownership. */
  changeRole(id: string, userId: string, role: WorkspaceRole): Promise<Member> {
    return firstValueFrom(this.http.patch<Member>(`/api/v1/households/${id}/members/${userId}`, { role }));
  }

  /** DELETE /households/{id}/members/{userId}: removes a member, or leaves with your own ID. */
  removeMember(id: string, userId: string): Promise<unknown> {
    return firstValueFrom(this.http.delete(`/api/v1/households/${id}/members/${userId}`));
  }

  /** POST /invitations/{token}/accept. */
  acceptInvitation(token: string): Promise<Workspace> {
    return firstValueFrom(this.http.post<Workspace>(`/api/v1/invitations/${encodeURIComponent(token)}/accept`, null));
  }
}
