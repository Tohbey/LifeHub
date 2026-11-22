import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ShoppingApi, ShoppingList } from '../../core/api/shopping.api';
import { WorkspaceService } from '../../core/workspace/workspace.service';
import { runWithToast } from '../../shared/run';
import { ToastService } from '../../shared/toast.service';

/** shopping-meals: the current workspace's lists. */
@Component({
  selector: 'lh-shopping-lists',
  imports: [FormsModule, RouterLink],
  template: `
    <header class="page-head"><div class="grow"><h1>Shopping</h1><p class="subtitle">Lists everyone can add to and tick off.</p></div></header>
    <section class="card">
      <form class="form inline" (ngSubmit)="create()">
        <label class="grow">New list <input name="name" required maxlength="100" [(ngModel)]="name" placeholder="Weekly shop" /></label>
        <button class="button" [disabled]="busy()">Create</button>
      </form>
    </section>
    <section class="card">
      <div class="row">
        <h2 class="grow">Lists</h2>
        <label class="checkbox"><input type="checkbox" name="archived" [(ngModel)]="includeArchived" (ngModelChange)="load()" /> Show archived</label>
      </div>
      <ul class="list">
        @for (l of lists(); track l.id) {
          <li>
            <a [routerLink]="['/shopping', l.id]" class="grow">{{ l.name }}</a>
            <span class="muted">{{ l.openItems }} to buy</span>
            @if (l.archived) { <span class="tag">archived</span> }
            <button class="button ghost" (click)="archive(l)">{{ l.archived ? 'Restore' : 'Archive' }}</button>
          </li>
        } @empty { <li class="muted">No lists yet.</li> }
      </ul>
    </section>
  `,
})
export class ShoppingLists implements OnInit {
  private readonly ws = inject(WorkspaceService);
  private readonly api = inject(ShoppingApi);
  private readonly toasts = inject(ToastService);

  protected readonly lists = signal<ShoppingList[]>([]);
  protected readonly busy = signal(false);
  protected name = '';
  protected includeArchived = false;

  /** Loads the lists in use. */
  ngOnInit(): void {
    void this.load();
  }

  /** GET /shopping-lists. */
  protected async load(): Promise<void> {
    await runWithToast(this.busy, this.toasts, async () =>
      this.lists.set((await this.api.lists(this.id, this.includeArchived)).items));
  }

  /** POST /shopping-lists. */
  protected async create(): Promise<void> {
    await runWithToast(this.busy, this.toasts, async () => {
      await this.api.createList(this.id, this.name);
      this.name = '';
      await this.load();
    });
  }

  /** PATCH /shopping-lists/{id} with archived flipped. */
  protected async archive(list: ShoppingList): Promise<void> {
    await runWithToast(this.busy, this.toasts, async () => {
      await this.api.updateList(this.id, list.id, { archived: !list.archived });
      await this.load();
    });
  }

  /** The current workspace's ID. */
  private get id(): string {
    return this.ws.current()!.id;
  }
}
