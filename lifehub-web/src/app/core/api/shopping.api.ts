import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { params } from './http-params';
import { Page } from './models';

export interface ShoppingList { id: string; workspaceId: string; name: string; archived: boolean; openItems: number; createdAt: string; }
export interface ShoppingItem {
  id: string; shoppingListId: string; name: string; quantity: string | null; note: string | null; purchased: boolean;
  purchasedAt: string | null; purchasedByUserId: string | null; addedByUserId: string; createdAt: string;
}

/** shopping-meals: lists of a workspace, and their items (keyed by list or item ID). */
@Injectable({ providedIn: 'root' })
export class ShoppingApi {
  private readonly http = inject(HttpClient);

  /** GET /workspaces/{id}/shopping-lists. */
  lists(ws: string, includeArchived: boolean): Promise<Page<ShoppingList>> {
    return firstValueFrom(this.http.get<Page<ShoppingList>>(`/api/v1/workspaces/${ws}/shopping-lists`,
      { params: params({ includeArchived, size: 100 }) }));
  }

  /** GET /workspaces/{id}/shopping-lists/{listId}. */
  list(ws: string, id: string): Promise<ShoppingList> {
    return firstValueFrom(this.http.get<ShoppingList>(`/api/v1/workspaces/${ws}/shopping-lists/${id}`));
  }

  /** POST /workspaces/{id}/shopping-lists. */
  createList(ws: string, name: string): Promise<ShoppingList> {
    return firstValueFrom(this.http.post<ShoppingList>(`/api/v1/workspaces/${ws}/shopping-lists`, { name }));
  }

  /** PATCH /workspaces/{id}/shopping-lists/{listId}. */
  updateList(ws: string, id: string, body: { name?: string; archived?: boolean }): Promise<ShoppingList> {
    return firstValueFrom(this.http.patch<ShoppingList>(`/api/v1/workspaces/${ws}/shopping-lists/${id}`, body));
  }

  /** GET /shopping-lists/{listId}/items. */
  items(listId: string, open: boolean): Promise<Page<ShoppingItem>> {
    return firstValueFrom(this.http.get<Page<ShoppingItem>>(`/api/v1/shopping-lists/${listId}/items`,
      { params: params({ open, size: 100 }) }));
  }

  /** POST /shopping-lists/{listId}/items. */
  addItem(listId: string, body: { name: string; quantity?: string; note?: string }): Promise<ShoppingItem> {
    return firstValueFrom(this.http.post<ShoppingItem>(`/api/v1/shopping-lists/${listId}/items`, body));
  }

  /** PATCH /shopping-items/{itemId}; {purchased: true} ticks it off. */
  updateItem(id: string, body: { name?: string; quantity?: string; note?: string; purchased?: boolean }): Promise<ShoppingItem> {
    return firstValueFrom(this.http.patch<ShoppingItem>(`/api/v1/shopping-items/${id}`, body));
  }

  /** DELETE /shopping-items/{itemId}. */
  removeItem(id: string): Promise<unknown> {
    return firstValueFrom(this.http.delete(`/api/v1/shopping-items/${id}`));
  }
}
