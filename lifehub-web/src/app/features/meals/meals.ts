import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Ingredient, MEAL_SLOTS, MealPlanEntry, MealSlot, MealsApi, Recipe } from '../../core/api/meals.api';
import { ShoppingApi, ShoppingList } from '../../core/api/shopping.api';
import { WorkspaceService } from '../../core/workspace/workspace.service';
import { Icon } from '../../shared/icon';
import { runWithToast } from '../../shared/run';
import { ToastService } from '../../shared/toast.service';

const SLOT_NAMES: Record<MealSlot, string> = { BREAKFAST: 'Breakfast', LUNCH: 'Lunch', DINNER: 'Dinner', SNACK: 'Snack' };

/** shopping-meals: the week's meal plan, and the household's recipes. */
@Component({
  selector: 'lh-meals',
  imports: [FormsModule, Icon],
  template: `
    <header class="page-head"><div class="grow"><h1>Meals</h1><p class="subtitle">Plan the week and keep the recipes you cook.</p></div></header>

    <div class="tabs" role="tablist">
      <button role="tab" [class.active]="tab() === 'plan'" [attr.aria-selected]="tab() === 'plan'" (click)="tab.set('plan')">Week plan</button>
      <button role="tab" [class.active]="tab() === 'recipes'" [attr.aria-selected]="tab() === 'recipes'" (click)="tab.set('recipes')">Recipes</button>
    </div>

    @if (tab() === 'plan') {
      <section class="card">
        <h2>Plan a meal</h2>
        <form class="form" (ngSubmit)="plan()">
          <div class="row">
            <label>Day
              <select name="day" [(ngModel)]="day">
                @for (d of days(); track d.iso) { <option [value]="d.iso">{{ d.label }}</option> }
              </select>
            </label>
            <label>Meal
              <select name="slot" [(ngModel)]="slot">
                @for (s of slots; track s) { <option [value]="s">{{ slotName(s) }}</option> }
              </select>
            </label>
            <label class="grow">Recipe
              <select name="recipe" [(ngModel)]="recipeId">
                <option [ngValue]="null">Something else (type it below)</option>
                @for (r of recipes(); track r.id) { <option [ngValue]="r.id">{{ r.name }}</option> }
              </select>
            </label>
          </div>
          <div class="row">
            @if (!recipeId) {
              <label class="grow">What are you eating? <input name="title" maxlength="120" [(ngModel)]="title" placeholder="Takeaway" /></label>
            }
            <label class="grow">Note (optional) <input name="note" maxlength="300" [(ngModel)]="note" placeholder="Bob cooks" /></label>
          </div>
          <button class="button" [disabled]="busy() || (!recipeId && !title.trim())">Add to plan</button>
        </form>
      </section>

      <section class="card">
        <div class="row">
          <h2 class="grow">Week of {{ days()[0].label }}</h2>
          <button class="button ghost" (click)="moveWeek(-1)">Previous week</button>
          <button class="button ghost" (click)="moveWeek(0)">This week</button>
          <button class="button ghost" (click)="moveWeek(1)">Next week</button>
        </div>
        <table class="table week">
          <thead><tr><th>Day</th>@for (s of slots; track s) { <th>{{ slotName(s) }}</th> }</tr></thead>
          <tbody>
            @for (d of days(); track d.iso) {
              <tr [class.today]="d.iso === todayIso">
                <th scope="row">{{ d.label }}</th>
                @for (s of slots; track s) {
                  <td>
                    @for (m of mealsFor(d.iso, s); track m.id) {
                      <span class="meal">
                        <span>{{ m.title }}@if (m.note) { <span class="muted small"> ({{ m.note }})</span> }</span>
                        <button class="icon-button" (click)="remove(m)" [attr.aria-label]="'Remove ' + m.title + ' on ' + d.label">
                          <lh-icon name="close" [size]="16" /></button>
                      </span>
                    }
                  </td>
                }
              </tr>
            }
          </tbody>
        </table>
      </section>
    } @else {
      <section class="card">
        <h2>{{ editingId ? 'Edit recipe' : 'Add a recipe' }}</h2>
        <form class="form" (ngSubmit)="saveRecipe()">
          <div class="row">
            <label class="grow">Name <input name="rname" required maxlength="120" [(ngModel)]="draft.name" placeholder="Jollof rice" /></label>
            <label>Serves <input name="servings" type="number" min="1" max="100" class="short" [(ngModel)]="draft.servings" /></label>
          </div>
          <fieldset>
            <legend>Ingredients</legend>
            @for (i of draft.ingredients; track $index; let n = $index) {
              <div class="row">
                <label>Quantity <input [name]="'q' + n" maxlength="40" class="short" [(ngModel)]="i.quantity" placeholder="500 g" /></label>
                <label class="grow">Ingredient {{ n + 1 }} <input [name]="'i' + n" maxlength="120" [(ngModel)]="i.name" placeholder="Rice" /></label>
                <button type="button" class="button ghost" (click)="draft.ingredients.splice(n, 1)" [attr.aria-label]="'Remove ingredient ' + (n + 1)">Remove</button>
              </div>
            }
            <button type="button" class="button ghost" (click)="draft.ingredients.push({ name: '', quantity: '' })">Add ingredient</button>
          </fieldset>
          <label>Method <textarea name="instructions" rows="4" maxlength="10000" [(ngModel)]="draft.instructions"></textarea></label>
          <label>Source link (optional) <input name="source" type="url" maxlength="500" [(ngModel)]="draft.sourceUrl" placeholder="https://" /></label>
          <div class="row">
            <button class="button" [disabled]="busy()">{{ editingId ? 'Save recipe' : 'Add recipe' }}</button>
            @if (editingId) { <button type="button" class="button ghost" (click)="resetDraft()">Cancel</button> }
          </div>
        </form>
      </section>

      <section class="card">
        <div class="row">
          <h2 class="grow">Recipes</h2>
          <label>Search <input name="search" [(ngModel)]="search" (ngModelChange)="loadRecipes()" placeholder="Name" /></label>
        </div>
        <ul class="list">
          @for (r of recipes(); track r.id) {
            <li class="recipe">
              <div class="grow">
                <strong>{{ r.name }}</strong>
                <span class="muted small">@if (r.servings) { Serves {{ r.servings }}, } {{ r.ingredients.length }} ingredients</span>
                @if (r.ingredients.length) {
                  <p class="small">{{ ingredientLine(r.ingredients) }}</p>
                }
                @if (r.sourceUrl) { <a class="small" [href]="r.sourceUrl" target="_blank" rel="noopener noreferrer">Source</a> }
              </div>
              @if (lists().length && r.ingredients.length) {
                <label class="sr-only" [for]="'list-' + r.id">Shopping list for {{ r.name }}</label>
                <select [id]="'list-' + r.id" [(ngModel)]="targetList[r.id]" [name]="'list-' + r.id">
                  @for (l of lists(); track l.id) { <option [value]="l.id">{{ l.name }}</option> }
                </select>
                <button class="button ghost" (click)="addToList(r)">Add to list</button>
              }
              <button class="button ghost" (click)="edit(r)">Edit</button>
              <button class="button ghost danger" (click)="deleteRecipe(r)" [attr.aria-label]="'Delete ' + r.name">Delete</button>
            </li>
          } @empty { <li class="muted">No recipes yet. Add the meals you cook most.</li> }
        </ul>
      </section>
    }
  `,
  styles: `
    .week th[scope="row"] { white-space: nowrap; }
    .week tr.today th[scope="row"] { box-shadow: inset 4px 0 0 var(--marigold); padding-left: 16px; }
    .week td { vertical-align: top; min-width: 140px; }
    .meal { display: flex; align-items: center; justify-content: space-between; gap: 4px; padding: 2px 0; }
    .meal .icon-button { width: 28px; height: 28px; }
    .recipe { flex-wrap: wrap; }
    .recipe .small { margin: 4px 0 0; }
  `,
})
export class Meals implements OnInit {
  private readonly ws = inject(WorkspaceService);
  private readonly api = inject(MealsApi);
  private readonly shopping = inject(ShoppingApi);
  private readonly toasts = inject(ToastService);

  protected readonly tab = signal<'plan' | 'recipes'>('plan');
  protected readonly busy = signal(false);
  protected readonly slots = MEAL_SLOTS;
  protected readonly recipes = signal<Recipe[]>([]);
  protected readonly lists = signal<ShoppingList[]>([]);
  protected readonly meals = signal<MealPlanEntry[]>([]);
  protected readonly weekStart = signal(monday(new Date()));
  protected readonly todayIso = iso(new Date());

  protected day = this.todayIso;
  protected slot: MealSlot = 'DINNER';
  protected recipeId: string | null = null;
  protected title = '';
  protected note = '';
  protected search = '';
  protected editingId: string | null = null;
  protected draft = emptyDraft();
  protected targetList: Record<string, string> = {};

  /** The seven days of the shown week, Monday first. */
  protected readonly days = computed(() => Array.from({ length: 7 }, (_, i) => {
    const d = new Date(this.weekStart());
    d.setDate(d.getDate() + i);
    return { iso: iso(d), label: d.toLocaleDateString(undefined, { weekday: 'short', day: 'numeric', month: 'short' }) };
  }));

  ngOnInit(): void {
    void this.loadWeek();
    void this.loadRecipes();
    void runWithToast(this.busy, this.toasts, async () => {
      const lists = (await this.shopping.lists(this.id, false)).items;
      this.lists.set(lists);
    });
  }

  protected slotName(slot: MealSlot): string {
    return SLOT_NAMES[slot];
  }

  protected mealsFor(day: string, slot: MealSlot): MealPlanEntry[] {
    return this.meals().filter((m) => m.date === day && m.slot === slot);
  }

  /** Moves the plan a week back or on; 0 returns to this week. */
  protected moveWeek(step: number): void {
    const start = step === 0 ? monday(new Date()) : new Date(this.weekStart());
    start.setDate(start.getDate() + 7 * step);
    this.weekStart.set(start);
    this.day = this.days()[0].iso;
    void this.loadWeek();
  }

  /** POST /meals. */
  protected async plan(): Promise<void> {
    await runWithToast(this.busy, this.toasts, async () => {
      await this.api.addMeal(this.id, { date: this.day, slot: this.slot, recipeId: this.recipeId,
        title: this.recipeId ? undefined : this.title.trim(), note: this.note.trim() || undefined });
      this.title = '';
      this.note = '';
      await this.loadWeek();
      this.toasts.success('Meal planned.');
    });
  }

  /** DELETE /meals/{id}. */
  protected async remove(meal: MealPlanEntry): Promise<void> {
    await runWithToast(this.busy, this.toasts, async () => {
      await this.api.removeMeal(this.id, meal.id);
      await this.loadWeek();
    });
  }

  /** GET /recipes, filtered by the search box. */
  protected async loadRecipes(): Promise<void> {
    await runWithToast(this.busy, this.toasts, async () => {
      this.recipes.set((await this.api.recipes(this.id, this.search.trim() || undefined)).items);
    });
  }

  /** POST or PUT /recipes; blank ingredient lines are dropped. */
  protected async saveRecipe(): Promise<void> {
    const body = {
      name: this.draft.name.trim(),
      servings: this.draft.servings || null,
      instructions: this.draft.instructions.trim() || null,
      sourceUrl: this.draft.sourceUrl.trim() || null,
      ingredients: this.draft.ingredients.filter((i) => i.name.trim())
        .map((i) => ({ name: i.name.trim(), quantity: i.quantity?.trim() || null })),
    };
    await runWithToast(this.busy, this.toasts, async () => {
      if (this.editingId) {
        await this.api.replaceRecipe(this.id, this.editingId, body);
      } else {
        await this.api.createRecipe(this.id, body);
      }
      this.toasts.success(this.editingId ? 'Recipe saved.' : 'Recipe added.');
      this.resetDraft();
      await this.loadRecipes();
    });
  }

  /** Loads a recipe into the form. */
  protected edit(r: Recipe): void {
    this.editingId = r.id;
    this.draft = { name: r.name, servings: r.servings, instructions: r.instructions ?? '', sourceUrl: r.sourceUrl ?? '',
      ingredients: r.ingredients.map((i) => ({ ...i })) };
  }

  protected resetDraft(): void {
    this.editingId = null;
    this.draft = emptyDraft();
  }

  /** DELETE /recipes/{id}; planned meals keep their title. */
  protected async deleteRecipe(r: Recipe): Promise<void> {
    if (!confirm(`Delete the recipe "${r.name}"? Meals already planned keep their name.`)) {
      return;
    }
    await runWithToast(this.busy, this.toasts, async () => {
      await this.api.deleteRecipe(this.id, r.id);
      await this.loadRecipes();
      this.toasts.success('Recipe deleted.');
    });
  }

  /** POST /recipes/{id}/shopping-list with the chosen list (the first one by default). */
  protected async addToList(r: Recipe): Promise<void> {
    const listId = this.targetList[r.id] ?? this.lists()[0].id;
    const list = this.lists().find((l) => l.id === listId);
    await runWithToast(this.busy, this.toasts, async () => {
      const added = await this.api.addToList(this.id, r.id, listId);
      this.toasts.success(`Added ${added.length} ingredients to ${list?.name}.`);
    });
  }

  /** "500 g Rice, 6 Tomatoes". */
  protected ingredientLine(ingredients: Ingredient[]): string {
    return ingredients.map((i) => (i.quantity ? `${i.quantity} ${i.name}` : i.name)).join(', ');
  }

  /** GET /meals for the shown week. */
  private async loadWeek(): Promise<void> {
    const days = this.days();
    await runWithToast(this.busy, this.toasts, async () => {
      this.meals.set(await this.api.plan(this.id, days[0].iso, days[6].iso));
    });
  }

  private get id(): string {
    return this.ws.current()!.id;
  }
}

function emptyDraft() {
  return { name: '', servings: null as number | null, instructions: '', sourceUrl: '', ingredients: [{ name: '', quantity: '' }] as Ingredient[] };
}

/** The Monday of the week containing this day, at midnight. */
function monday(d: Date): Date {
  const start = new Date(d.getFullYear(), d.getMonth(), d.getDate());
  start.setDate(start.getDate() - ((start.getDay() + 6) % 7));
  return start;
}

/** YYYY-MM-DD in the viewer's own calendar. */
function iso(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}
