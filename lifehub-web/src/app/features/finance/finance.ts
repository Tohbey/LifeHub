import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  Budget, Category, CreateTransaction, EntryType, FinanceApi, FinanceSummary, Split, Transaction,
} from '../../core/api/finance.api';
import { Page } from '../../core/api/models';
import { ReferenceDataStore } from '../../core/api/reference.api';
import { WorkspaceService } from '../../core/workspace/workspace.service';
import { dateTime, money, toInstant, toLocalInput } from '../../shared/format';
import { ToastService } from '../../shared/toast.service';

type Tab = 'transactions' | 'categories' | 'budgets' | 'summary';

/** A split row in the form: whether the member takes part, and their share. */
interface SplitRow { userId: string; included: boolean; amount: number | null; }

/** finance for the current workspace: transactions (with splits), categories, budgets and the summary. */
@Component({
  selector: 'lh-finance',
  imports: [FormsModule],
  template: `
    <header class="page-head"><div class="grow"><h1>Finance</h1><p class="subtitle">Income, spending and shared expenses for this workspace.</p></div></header>
    <div class="tabs" role="tablist">
      @for (t of tabs; track t) {
        <button role="tab" [class.active]="tab() === t" (click)="open(t)">{{ t.charAt(0).toUpperCase() + t.slice(1) }}</button>
      }
    </div>

    @switch (tab()) {
      @case ('transactions') {
        <section class="card">
          <h2>{{ editingId ? 'Edit transaction' : 'Record a transaction' }}</h2>
          <form class="form" (ngSubmit)="saveTransaction()">
            <div class="row">
              <label>Type
                <select name="type" [(ngModel)]="form.type" [disabled]="!!editingId">
                  <option value="EXPENSE">Expense</option><option value="INCOME">Income</option>
                </select>
              </label>
              <label class="grow">Description <input name="description" required maxlength="200" [(ngModel)]="form.description" /></label>
            </div>
            <div class="row">
              <label>Amount <input name="amount" type="number" step="0.01" min="0.01" required [(ngModel)]="form.amount" (ngModelChange)="splitEqually(false)" /></label>
              <label>Currency
                <select name="currency" required [(ngModel)]="form.currency">
                  @for (c of ref.currencies(); track c.code) { <option [value]="c.code">{{ ref.currencyLabel(c) }}</option> }
                </select>
              </label>
              <label>Category
                <select name="categoryId" [(ngModel)]="form.categoryId">
                  <option [ngValue]="null">None</option>
                  @for (c of categoriesFor(form.type); track c.id) { <option [ngValue]="c.id">{{ c.name }}</option> }
                </select>
              </label>
              <label>Paid by
                <select name="payer" [(ngModel)]="form.payerUserId">
                  @for (m of ws.members(); track m.userId) { <option [ngValue]="m.userId">{{ ws.name(m.userId) }}</option> }
                </select>
              </label>
              <label>When <input name="occurredAt" type="datetime-local" required [(ngModel)]="form.occurredAt" /></label>
            </div>
            @if (form.type === 'EXPENSE' && ws.members().length > 1) {
              <div class="toggle-row">
                <label class="checkbox"><input type="checkbox" name="shared" [(ngModel)]="shared" (ngModelChange)="splitEqually(true)" /> Shared expense</label>
                <span class="hint">Split it between household members.</span>
              </div>
              @if (shared) {
              <fieldset>
                <legend>Who pays what</legend>
                  @for (row of splitRows; track row.userId) {
                    <div class="row">
                      <label class="checkbox"><input type="checkbox" [name]="'inc-' + row.userId" [(ngModel)]="row.included" (ngModelChange)="splitEqually(true)" /> {{ ws.name(row.userId) }}</label>
                      <input type="number" step="0.01" class="short" [name]="'amt-' + row.userId" [(ngModel)]="row.amount" [disabled]="!row.included" />
                    </div>
                  }
                  <p class="hint">Shares add up to {{ splitSum() }} of {{ form.amount ?? 0 }}.
                    <button type="button" class="button ghost" (click)="splitEqually(true)">Split equally</button></p>
              </fieldset>
              }
            }
            <div class="row">
              <button class="button" [disabled]="busy()">{{ editingId ? 'Save changes' : 'Record' }}</button>
              @if (editingId) { <button type="button" class="button ghost" (click)="resetForm()">Cancel</button> }
            </div>
          </form>
        </section>

        <section class="card">
          <div class="row">
            <h2 class="grow">Transactions</h2>
            <button class="button ghost" (click)="exportCsv()" [disabled]="busy()">Export CSV (this month)</button>
            <select [(ngModel)]="filterType" (ngModelChange)="loadTransactions(0)" name="filterType" aria-label="Filter by type">
              <option value="">All types</option><option value="EXPENSE">Expenses</option><option value="INCOME">Income</option>
            </select>
            <select [(ngModel)]="filterCategory" (ngModelChange)="loadTransactions(0)" name="filterCategory" aria-label="Filter by category">
              <option value="">All categories</option>
              @for (c of categories(); track c.id) { <option [value]="c.id">{{ c.name }}</option> }
            </select>
          </div>
          <table class="table">
            <thead><tr><th>When</th><th>Description</th><th>Category</th><th>Paid by</th><th class="num">Amount</th><th></th></tr></thead>
            <tbody>
              @for (t of transactions()?.items ?? []; track t.id) {
                <tr>
                  <td>{{ dateTime(t.occurredAt) }}</td>
                  <td>{{ t.description }}
                    @if (t.splits.length) {
                      <div class="muted small">Split: @for (s of t.splits; track s.userId) { {{ ws.name(s.userId) }} {{ money(s.amount, t.currency) }}{{ $last ? '' : ', ' }} }</div>
                    }
                  </td>
                  <td>{{ categoryName(t.categoryId) }}</td>
                  <td>{{ ws.name(t.payerUserId) }}</td>
                  <td class="num" [class.positive]="t.type === 'INCOME'">{{ t.type === 'INCOME' ? '+' : '−' }}{{ money(t.amount, t.currency) }}</td>
                  <td class="actions">
                    <button class="button ghost" (click)="edit(t)">Edit</button>
                    <button class="button ghost danger" (click)="remove(t)">Delete</button>
                  </td>
                </tr>
              } @empty { <tr><td colspan="6" class="muted">No transactions.</td></tr> }
            </tbody>
          </table>
          @if ((transactions()?.totalPages ?? 0) > 1) {
            <div class="pager">
              <button class="button ghost" [disabled]="page === 0" (click)="loadTransactions(page - 1)">Previous</button>
              <span>Page {{ page + 1 }} of {{ transactions()?.totalPages }}</span>
              <button class="button ghost" [disabled]="page + 1 >= (transactions()?.totalPages ?? 0)" (click)="loadTransactions(page + 1)">Next</button>
            </div>
          }
        </section>
      }

      @case ('categories') {
        <section class="card">
          <h2>Categories</h2>
          <form class="form inline" (ngSubmit)="createCategory()">
            <label>Type <select name="catType" [(ngModel)]="newCategoryType"><option value="EXPENSE">Expense</option><option value="INCOME">Income</option></select></label>
            <label>Name <input name="catName" required maxlength="60" [(ngModel)]="newCategoryName" /></label>
            <button class="button" [disabled]="busy()">Add</button>
          </form>
          <table class="table">
            <thead><tr><th>Name</th><th>Type</th><th></th></tr></thead>
            <tbody>
              @for (c of categories(); track c.id) {
                <tr>
                  <td>
                    @if (renamingId === c.id) {
                      <input [name]="'rename-' + c.id" [(ngModel)]="renameTo" maxlength="60" aria-label="New name" (keydown.enter)="saveRename(c)" />
                    } @else { {{ c.name }} }
                  </td>
                  <td>{{ c.type.toLowerCase() }}</td>
                  <td class="actions">
                    @if (ws.isAdmin()) {
                      @if (renamingId === c.id) {
                        <button class="button ghost" (click)="saveRename(c)">Save</button>
                        <button class="button ghost" (click)="renamingId = null">Cancel</button>
                      } @else {
                        <button class="button ghost" (click)="startRename(c)">Rename</button>
                        <button class="button ghost danger" (click)="deleteCategory(c)">Delete</button>
                      }
                    }
                  </td>
                </tr>
              }
              @empty { <tr><td colspan="3" class="muted">No categories yet.</td></tr> }
            </tbody>
          </table>
        </section>
      }

      @case ('budgets') {
        <section class="card">
          <h2>Monthly budgets</h2>
          @if (ws.isAdmin()) {
            <form class="form inline" (ngSubmit)="createBudget()">
              <label>Category
                <select name="budgetCategory" required [(ngModel)]="newBudget.categoryId">
                  @for (c of categoriesFor('EXPENSE'); track c.id) { <option [ngValue]="c.id">{{ c.name }}</option> }
                </select>
              </label>
              <label>Monthly limit <input name="limit" type="number" step="0.01" min="0.01" required [(ngModel)]="newBudget.monthlyLimit" /></label>
              <label>Currency
                <select name="budgetCurrency" required [(ngModel)]="newBudget.currency">
                  @for (c of ref.currencies(); track c.code) { <option [value]="c.code">{{ ref.currencyLabel(c) }}</option> }
                </select>
              </label>
              <button class="button" [disabled]="busy()">Set budget</button>
            </form>
          } @else { <p class="hint">Only owners and admins can set budgets.</p> }
          @for (b of budgets(); track b.id) {
            <div class="budget">
              <div class="row"><strong class="grow">{{ categoryName(b.categoryId) }}</strong>
                @if (editingBudgetId === b.id) {
                  <input type="number" step="0.01" min="0.01" class="short" [name]="'limit-' + b.id" [(ngModel)]="newLimit"
                         aria-label="New monthly limit" (keydown.enter)="saveLimit(b)" />
                  <button class="button ghost" (click)="saveLimit(b)">Save</button>
                  <button class="button ghost" (click)="editingBudgetId = null">Cancel</button>
                } @else {
                  <span>{{ money(b.spent, b.currency) }} of {{ money(b.monthlyLimit, b.currency) }}</span>
                  @if (ws.isAdmin()) {
                    <button class="button ghost" (click)="editingBudgetId = b.id; newLimit = b.monthlyLimit">Change limit</button>
                    <button class="button ghost danger" (click)="deleteBudget(b)">Delete</button>
                  }
                }
              </div>
              <div class="bar"><span [style.width.%]="percent(b)" [class.over]="b.spent >= b.monthlyLimit" [class.warn]="percent(b) >= 80"></span></div>
              <p class="muted small">{{ b.periodStart }} to {{ b.periodEnd }} (the household's local month)</p>
            </div>
          } @empty { <p class="muted">No budgets yet.</p> }
        </section>
      }

      @case ('summary') {
        <section class="card">
          <h2>Summary</h2>
          <form class="form inline" (ngSubmit)="loadSummary()">
            <label>From <input name="from" type="datetime-local" [(ngModel)]="summaryFrom" /></label>
            <label>To <input name="to" type="datetime-local" [(ngModel)]="summaryTo" /></label>
            <button class="button">Show</button>
          </form>
          @if (summary(); as s) {
            <p class="muted">{{ dateTime(s.from) }} – {{ dateTime(s.to) }}</p>
            @for (c of s.currencies; track c.currency) {
              <dl class="stats">
                <div><dt>Income</dt><dd>{{ money(c.income, c.currency) }}</dd></div>
                <div><dt>Spent</dt><dd>{{ money(c.expense, c.currency) }}</dd></div>
                <div><dt>Net</dt><dd [class.negative]="c.net < 0">{{ money(c.net, c.currency) }}</dd></div>
              </dl>
              <table class="table">
                <thead><tr><th>Category</th><th class="num">Spent</th></tr></thead>
                <tbody>
                  @for (row of c.expenseByCategory; track row.name) { <tr><td>{{ row.name }}</td><td class="num">{{ money(row.amount, c.currency) }}</td></tr> }
                </tbody>
              </table>
            } @empty { <p class="muted">No transactions in this period.</p> }
          }
        </section>
      }
    }
  `,
})
export class Finance implements OnInit {
  protected readonly ws = inject(WorkspaceService);
  protected readonly ref = inject(ReferenceDataStore);
  private readonly api = inject(FinanceApi);
  private readonly toasts = inject(ToastService);

  protected readonly tabs: Tab[] = ['transactions', 'categories', 'budgets', 'summary'];
  protected readonly tab = signal<Tab>('transactions');
  protected readonly transactions = signal<Page<Transaction> | null>(null);
  protected readonly categories = signal<Category[]>([]);
  protected readonly budgets = signal<Budget[]>([]);
  protected readonly summary = signal<FinanceSummary | null>(null);
  protected readonly busy = signal(false);
  protected readonly money = money;
  protected readonly dateTime = dateTime;

  protected form = this.emptyForm();
  protected editingId: string | null = null;
  protected shared = false;
  protected splitRows: SplitRow[] = [];
  protected filterType: EntryType | '' = '';
  protected filterCategory = '';
  protected page = 0;
  protected newCategoryType: EntryType = 'EXPENSE';
  protected newCategoryName = '';
  protected newBudget = { categoryId: null as string | null, monthlyLimit: null as number | null, currency: this.ws.defaultCurrency() };
  protected summaryFrom = '';
  protected renamingId: string | null = null;
  protected editingBudgetId: string | null = null;
  protected newLimit: number | null = null;
  protected renameTo = '';
  protected summaryTo = '';

  /** Loads categories (needed by every tab) and the transactions. */
  async ngOnInit(): Promise<void> {
    await this.run(async () => {
      await Promise.all([this.ref.load(), this.loadCategories()]);
      await this.loadTransactions(0);
    });
  }

  /** Switches tab and loads what it shows. */
  protected async open(tab: Tab): Promise<void> {
    this.tab.set(tab);
    await this.run(async () => {
      if (tab === 'budgets') {
        this.budgets.set((await this.api.budgets(this.id)).items);
      } else if (tab === 'summary') {
        await this.loadSummary();
      } else if (tab === 'transactions') {
        await this.loadTransactions(this.page);
      }
    });
  }

  /** GET /transactions with the filters. */
  protected async loadTransactions(page: number): Promise<void> {
    this.page = page;
    await this.run(async () => this.transactions.set(
      await this.api.transactions(this.id, { type: this.filterType, categoryId: this.filterCategory, page })));
  }

  /** POST or PATCH /transactions from the form. */
  protected async saveTransaction(): Promise<void> {
    const splits: Split[] = this.shared
      ? this.splitRows.filter((r) => r.included).map((r) => ({ userId: r.userId, amount: Number(r.amount ?? 0) }))
      : [];
    const body: CreateTransaction = {
      type: this.form.type, description: this.form.description, amount: Number(this.form.amount),
      currency: this.form.currency.toUpperCase(), categoryId: this.form.categoryId, payerUserId: this.form.payerUserId,
      occurredAt: toInstant(this.form.occurredAt)!, splits,
    };
    await this.run(async () => {
      if (this.editingId) {
        await this.api.updateTransaction(this.id, this.editingId, { ...body, clearCategory: !body.categoryId });
        this.toasts.success('Transaction updated.');
      } else {
        await this.api.createTransaction(this.id, body);
        this.toasts.success(splits.length ? 'Shared expense recorded; the others are notified.' : 'Transaction recorded.');
      }
      this.resetForm();
      await this.loadTransactions(this.page);
    });
  }

  /** Fills the form with a transaction to change it. */
  protected edit(t: Transaction): void {
    this.editingId = t.id;
    this.form = {
      type: t.type, description: t.description, amount: t.amount, currency: t.currency, categoryId: t.categoryId,
      payerUserId: t.payerUserId, occurredAt: toLocalInput(t.occurredAt),
    };
    this.shared = t.splits.length > 0;
    this.splitRows = this.ws.members().map((m) => {
      const split = t.splits.find((s) => s.userId === m.userId);
      return { userId: m.userId, included: !!split, amount: split?.amount ?? null };
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  /** DELETE /transactions/{id}. */
  protected async remove(t: Transaction): Promise<void> {
    if (!confirm(`Delete "${t.description}"?`)) {
      return;
    }
    await this.run(async () => {
      await this.api.deleteTransaction(this.id, t.id);
      this.toasts.success('Transaction deleted.');
      await this.loadTransactions(this.page);
    });
  }

  /** Clears the form back to a new expense paid by the caller, now. */
  protected resetForm(): void {
    this.form = this.emptyForm();
    this.editingId = null;
    this.shared = false;
    this.splitRows = this.ws.members().map((m) => ({ userId: m.userId, included: true, amount: null }));
  }

  /**
   * Shares the amount equally between the included members, putting the rounding remainder on the first.
   * When {@code force} is false it only runs if the split is already on.
   */
  protected splitEqually(force: boolean): void {
    if (!force && !this.shared) {
      return;
    }
    if (!this.splitRows.length) {
      this.splitRows = this.ws.members().map((m) => ({ userId: m.userId, included: true, amount: null }));
    }
    const included = this.splitRows.filter((r) => r.included);
    const cents = Math.round(Number(this.form.amount ?? 0) * 100);
    if (!included.length || !cents) {
      return;
    }
    const each = Math.floor(cents / included.length);
    included.forEach((row, i) => (row.amount = (each + (i === 0 ? cents - each * included.length : 0)) / 100));
  }

  /** POST /finance/categories. */
  protected async createCategory(): Promise<void> {
    await this.run(async () => {
      await this.api.createCategory(this.id, { type: this.newCategoryType, name: this.newCategoryName });
      this.newCategoryName = '';
      await this.loadCategories();
      this.toasts.success('Category added.');
    });
  }

  /** POST /budgets. */
  protected async createBudget(): Promise<void> {
    await this.run(async () => {
      await this.api.createBudget(this.id, {
        categoryId: this.newBudget.categoryId!, monthlyLimit: Number(this.newBudget.monthlyLimit),
        currency: this.newBudget.currency.toUpperCase(),
      });
      this.budgets.set((await this.api.budgets(this.id)).items);
      this.toasts.success('Budget set.');
    });
  }

  /** Starts renaming a category inline. */
  protected startRename(c: Category): void {
    this.renamingId = c.id;
    this.renameTo = c.name;
  }

  /** PATCH /finance/categories/{id}. */
  protected async saveRename(c: Category): Promise<void> {
    await this.run(async () => {
      await this.api.renameCategory(this.id, c.id, this.renameTo);
      this.renamingId = null;
      await this.loadCategories();
      this.toasts.success('Category renamed.');
    });
  }

  /** DELETE /finance/categories/{id}; the API refuses a category still in use. */
  protected async deleteCategory(c: Category): Promise<void> {
    if (!confirm(`Delete the category "${c.name}"?`)) {
      return;
    }
    await this.run(async () => {
      await this.api.deleteCategory(this.id, c.id);
      await this.loadCategories();
      this.toasts.success('Category deleted.');
    });
  }

  /** PATCH /budgets/{id} with the new monthly limit. */
  protected async saveLimit(b: Budget): Promise<void> {
    await this.run(async () => {
      await this.api.updateBudget(this.id, b.id, Number(this.newLimit));
      this.editingBudgetId = null;
      this.budgets.set((await this.api.budgets(this.id)).items);
      this.toasts.success('Budget limit changed.');
    });
  }

  /** DELETE /budgets/{id}. */
  protected async deleteBudget(b: Budget): Promise<void> {
    if (!confirm(`Delete the budget for ${this.categoryName(b.categoryId)}? Spending is not affected.`)) {
      return;
    }
    await this.run(async () => {
      await this.api.deleteBudget(this.id, b.id);
      this.budgets.set((await this.api.budgets(this.id)).items);
      this.toasts.success('Budget deleted.');
    });
  }

  /** GET /finance/export and saves the CSV file. */
  protected async exportCsv(): Promise<void> {
    await this.run(async () => {
      const { fileName, blob } = await this.api.exportCsv(this.id);
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;
      link.click();
      URL.revokeObjectURL(url);
      this.toasts.success(`Downloaded ${fileName}.`);
    });
  }

  /** GET /finance/summary for the chosen range, this month when empty. */
  protected async loadSummary(): Promise<void> {
    await this.run(async () =>
      this.summary.set(await this.api.summary(this.id, toInstant(this.summaryFrom), toInstant(this.summaryTo))));
  }

  /** Categories of one type, for the selects. */
  protected categoriesFor(type: EntryType): Category[] {
    return this.categories().filter((c) => c.type === type);
  }

  /** A category's name, or "–". */
  protected categoryName(id: string | null): string {
    return this.categories().find((c) => c.id === id)?.name ?? '–';
  }

  /** How full a budget is, capped at 100 for the bar. */
  protected percent(b: Budget): number {
    return Math.min(100, Math.round((b.spent / b.monthlyLimit) * 100));
  }

  /** The sum of the included shares, for the hint under the split. */
  protected splitSum(): string {
    return this.splitRows.filter((r) => r.included).reduce((sum, r) => sum + Number(r.amount ?? 0), 0).toFixed(2);
  }

  /** GET /finance/categories. */
  private async loadCategories(): Promise<void> {
    this.categories.set((await this.api.categories(this.id)).items);
  }

  /** A blank form: an expense paid by the caller, now, in the workspace's default currency. */
  private emptyForm() {
    return {
      type: 'EXPENSE' as EntryType, description: '', amount: null as number | null, currency: this.ws.defaultCurrency(),
      categoryId: null as string | null, payerUserId: this.ws.me()?.id ?? null, occurredAt: toLocalInput(new Date()),
    };
  }

  /** The current workspace's ID. */
  private get id(): string {
    return this.ws.current()!.id;
  }

  /** Runs an action with the buttons disabled, showing any API error. */
  private async run(action: () => Promise<void>): Promise<void> {
    this.busy.set(true);
    try {
      await action();
    } catch (error) {
      this.toasts.error(error);
    } finally {
      this.busy.set(false);
    }
  }
}
