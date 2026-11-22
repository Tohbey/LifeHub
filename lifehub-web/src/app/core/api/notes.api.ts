import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { params } from './http-params';
import { Page } from './models';

export type Visibility = 'PERSONAL' | 'SHARED';
export interface Note {
  id: string; workspaceId: string; title: string; body: string | null; visibility: Visibility; pinned: boolean;
  tags: string[]; createdByUserId: string; createdAt: string; updatedAt: string;
}
export interface Bookmark {
  id: string; workspaceId: string; url: string; title: string; description: string | null; visibility: Visibility;
  tags: string[]; createdByUserId: string; createdAt: string; updatedAt: string;
}
export interface TagCount { tag: string; notes: number; bookmarks: number; }
export interface SaveNote { title: string; body?: string | null; visibility: Visibility; pinned?: boolean; tags: string[]; }
export interface SaveBookmark { url: string; title?: string | null; description?: string | null; visibility: Visibility; tags: string[]; }

/** notes: personal and shared notes, saved links and their tags. */
@Injectable({ providedIn: 'root' })
export class NotesApi {
  private readonly http = inject(HttpClient);

  /** GET /workspaces/{id}/notes: pinned first; q searches, tag filters. */
  notes(ws: string, filter: { q?: string; tag?: string } = {}): Promise<Page<Note>> {
    return firstValueFrom(this.http.get<Page<Note>>(`/api/v1/workspaces/${ws}/notes`, { params: params({ ...filter, size: 100 }) }));
  }

  /** POST /workspaces/{id}/notes. */
  createNote(ws: string, body: SaveNote): Promise<Note> {
    return firstValueFrom(this.http.post<Note>(`/api/v1/workspaces/${ws}/notes`, body));
  }

  /** PUT /workspaces/{id}/notes/{noteId}. */
  replaceNote(ws: string, id: string, body: SaveNote): Promise<Note> {
    return firstValueFrom(this.http.put<Note>(`/api/v1/workspaces/${ws}/notes/${id}`, body));
  }

  /** DELETE /workspaces/{id}/notes/{noteId}. */
  deleteNote(ws: string, id: string): Promise<unknown> {
    return firstValueFrom(this.http.delete(`/api/v1/workspaces/${ws}/notes/${id}`));
  }

  /** GET /workspaces/{id}/bookmarks. */
  bookmarks(ws: string, filter: { q?: string; tag?: string } = {}): Promise<Page<Bookmark>> {
    return firstValueFrom(this.http.get<Page<Bookmark>>(`/api/v1/workspaces/${ws}/bookmarks`, { params: params({ ...filter, size: 100 }) }));
  }

  /** POST /workspaces/{id}/bookmarks. */
  createBookmark(ws: string, body: SaveBookmark): Promise<Bookmark> {
    return firstValueFrom(this.http.post<Bookmark>(`/api/v1/workspaces/${ws}/bookmarks`, body));
  }

  /** PUT /workspaces/{id}/bookmarks/{bookmarkId}. */
  replaceBookmark(ws: string, id: string, body: SaveBookmark): Promise<Bookmark> {
    return firstValueFrom(this.http.put<Bookmark>(`/api/v1/workspaces/${ws}/bookmarks/${id}`, body));
  }

  /** DELETE /workspaces/{id}/bookmarks/{bookmarkId}. */
  deleteBookmark(ws: string, id: string): Promise<unknown> {
    return firstValueFrom(this.http.delete(`/api/v1/workspaces/${ws}/bookmarks/${id}`));
  }

  /** GET /workspaces/{id}/tags. */
  tags(ws: string): Promise<TagCount[]> {
    return firstValueFrom(this.http.get<TagCount[]>(`/api/v1/workspaces/${ws}/tags`));
  }
}
