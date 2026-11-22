import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ShoppingApi, ShoppingItem, ShoppingList } from '../../core/api/shopping.api';
import { WorkspaceService } from '../../core/workspace/workspace.service';
import { runWithToast } from '../../shared/run';
import { ToastService } from '../../shared/toast.service';

/** One shopping list: add items, tick them off, remove them. Item endpoints are keyed by list or item ID. */
@Component({
  selector: 'lh-shopping-list',
  imports: [FormsModule, RouterLink],
  template: `
    <header class="page-head"><div class="grow">
      <p class="eyebrow"><a routerLink="/shopping">← All lists</a></p>
      <h1>{{ list()?.name ?? 'Shopping list' }}</h1>
      <p class="subtitle">{{ openCount() }} to buy · {{ items().length - openCount() }} bought</p>
    </div></header>
    <section class="card">
      <form class="form inline" (ngSubmit)="add()">
        <label class="grow">Item <input name="name" required maxlength="120" [(ngModel)]="name" placeholder="Milk" /></label>
        <label>Quantity <input name="quantity" maxlength="40" class="short" [(ngModel)]="quantity" placeholder="2" /></label>
        <label>Note <input name="note" maxlength="300" [(ngModel)]="note" /></label>
        <button class="button" [disabled]="busy() || list()?.archived">Add</button>
      </form>
      @if (list()?.archived) { <p class="hint">This list is archived. Restore it to add items.</p> }
    </section>
    <section class="card">
      <div class="row">
        <h2 class="grow">Items</h2>
        <label class="checkbox"><input type="checkbox" name="open" [(ngModel)]="openOnly" (ngModelChange)="load()" /> Hide bought</label>
      </div>
      <ul class="list">
        @for (i of items(); track i.id) {
          <li [class.done]="i.purchased">
            <input type="checkbox" [checked]="i.purchased" (change)="toggle(i)" [attr.aria-label]="'Bought ' + i.name" />
            <span class="grow">{{ i.name }} @if (i.quantity) { <span class="muted">× {{ i.quantity }}</span> }
              @if (i.note) { <span class="muted small">· {{ i.note }}</span> }
              <span class="muted small">· added by {{ ws.name(i.addedByUserId) }}@if (i.purchased) {, bought by {{ ws.name(i.purchasedByUserId) }} }</span>
            </span>
            <button class="button ghost danger" (click)="remove(i)">Remove</button>
          </li>
        } @empty { <li class="muted">Nothing on this list.</li> }
      </ul>
    </section>
  `,
})
export class ShoppingListPage implements OnInit {
  protected readonly ws = inject(WorkspaceService);
  private readonly api = inject(ShoppingApi);
  private readonly toasts = inject(ToastService);

  /** The list ID from /shopping/:listId. */
  readonly listId = input.required<string>();
  protected readonly list = signal<ShoppingList | null>(null);
  protected readonly items = signal<ShoppingItem[]>([]);
  protected readonly busy = signal(false);
  protected name = '';
  protected quantity = '';
  protected note = '';
  protected openOnly = false;

  /** How many items on the page are still to buy. */
  protected openCount(): number {
    return this.items().filter((i) => !i.purchased).length;
  }

  /** Loads the list and its items. */
  ngOnInit(): void {
    void runWithToast(this.busy, this.toasts, async () => {
      this.list.set(await this.api.list(this.ws.current()!.id, this.listId()));
      await this.loadItems();
    });
  }

  /** GET /shopping-lists/{id}/items. */
  protected async load(): Promise<void> {
    await runWithToast(this.busy, this.toasts, () => this.loadItems());
  }

  /** POST /shopping-lists/{id}/items. */
  protected async add(): Promise<void> {
    await runWithToast(this.busy, this.toasts, async () => {
      await this.api.addItem(this.listId(), { name: this.name, quantity: this.quantity || undefined, note: this.note || undefined });
      this.name = '';
      this.quantity = '';
      this.note = '';
      await this.loadItems();
    });
  }

  /** PATCH /shopping-items/{id} with purchased flipped. */
  protected async toggle(item: ShoppingItem): Promise<void> {
    await runWithToast(this.busy, this.toasts, async () => {
      await this.api.updateItem(item.id, { purchased: !item.purchased });
      await this.loadItems();
    });
  }

  /** DELETE /shopping-items/{id}. */
  protected async remove(item: ShoppingItem): Promise<void> {
    await runWithToast(this.busy, this.toasts, async () => {
      await this.api.removeItem(item.id);
      await this.loadItems();
    });
  }

  /** Reads the items. */
  private async loadItems(): Promise<void> {
    this.items.set((await this.api.items(this.listId(), this.openOnly)).items);
  }
}
