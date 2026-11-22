import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { params } from './http-params';
import { Page } from './models';
import { ShoppingItem } from './shopping.api';

export type MealSlot = 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK';
export const MEAL_SLOTS: MealSlot[] = ['BREAKFAST', 'LUNCH', 'DINNER', 'SNACK'];
export interface Ingredient { name: string; quantity: string | null; }
export interface Recipe {
  id: string; workspaceId: string; name: string; servings: number | null; instructions: string | null;
  sourceUrl: string | null; ingredients: Ingredient[]; createdByUserId: string; createdAt: string; updatedAt: string;
}
export interface SaveRecipe {
  name: string; servings?: number | null; instructions?: string | null; sourceUrl?: string | null; ingredients: Ingredient[];
}
export interface MealPlanEntry {
  id: string; workspaceId: string; date: string; slot: MealSlot; recipeId: string | null; title: string;
  note: string | null; createdByUserId: string; createdAt: string;
}

/** shopping-meals: recipes and the meal plan of a workspace. */
@Injectable({ providedIn: 'root' })
export class MealsApi {
  private readonly http = inject(HttpClient);

  /** GET /workspaces/{id}/recipes, by name; q filters by name. */
  recipes(ws: string, q?: string): Promise<Page<Recipe>> {
    return firstValueFrom(this.http.get<Page<Recipe>>(`/api/v1/workspaces/${ws}/recipes`, { params: params({ q, size: 100 }) }));
  }

  /** POST /workspaces/{id}/recipes. */
  createRecipe(ws: string, body: SaveRecipe): Promise<Recipe> {
    return firstValueFrom(this.http.post<Recipe>(`/api/v1/workspaces/${ws}/recipes`, body));
  }

  /** PUT /workspaces/{id}/recipes/{recipeId}: replaces it, ingredients included. */
  replaceRecipe(ws: string, id: string, body: SaveRecipe): Promise<Recipe> {
    return firstValueFrom(this.http.put<Recipe>(`/api/v1/workspaces/${ws}/recipes/${id}`, body));
  }

  /** DELETE /workspaces/{id}/recipes/{recipeId}. */
  deleteRecipe(ws: string, id: string): Promise<unknown> {
    return firstValueFrom(this.http.delete(`/api/v1/workspaces/${ws}/recipes/${id}`));
  }

  /** POST /workspaces/{id}/recipes/{recipeId}/shopping-list: every ingredient onto a list. */
  addToList(ws: string, id: string, shoppingListId: string): Promise<ShoppingItem[]> {
    return firstValueFrom(this.http.post<ShoppingItem[]>(`/api/v1/workspaces/${ws}/recipes/${id}/shopping-list`, { shoppingListId }));
  }

  /** GET /workspaces/{id}/meals?from=&to= (YYYY-MM-DD, both included). */
  plan(ws: string, from: string, to: string): Promise<MealPlanEntry[]> {
    return firstValueFrom(this.http.get<MealPlanEntry[]>(`/api/v1/workspaces/${ws}/meals`, { params: params({ from, to }) }));
  }

  /** POST /workspaces/{id}/meals: a recipe, or a free-text title. */
  addMeal(ws: string, body: { date: string; slot: MealSlot; recipeId?: string | null; title?: string; note?: string }): Promise<MealPlanEntry> {
    return firstValueFrom(this.http.post<MealPlanEntry>(`/api/v1/workspaces/${ws}/meals`, body));
  }

  /** DELETE /workspaces/{id}/meals/{entryId}. */
  removeMeal(ws: string, id: string): Promise<unknown> {
    return firstValueFrom(this.http.delete(`/api/v1/workspaces/${ws}/meals/${id}`));
  }
}
