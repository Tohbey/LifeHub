import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { params } from './http-params';
import { Page } from './models';

export type EntryType = 'INCOME' | 'EXPENSE';
export interface Category { id: string; type: EntryType; name: string; }
export interface Split { userId: string; amount: number; }
export interface Transaction {
  id: string; type: EntryType; description: string; amount: number; currency: string; categoryId: string | null;
  payerUserId: string; occurredAt: string; createdByUserId: string; splits: Split[];
}
export interface CreateTransaction {
  type: EntryType; description: string; amount: number; currency: string; categoryId?: string | null;
  payerUserId?: string | null; occurredAt: string; splits?: Split[];
}
export interface Budget {
  id: string; categoryId: string; monthlyLimit: number; currency: string; periodStart: string; periodEnd: string; spent: number;
}
export interface FinanceSummary {
  from: string; to: string;
  currencies: {
    currency: string; income: number; expense: number; net: number;
    expenseByCategory: { categoryId: string | null; name: string; amount: number }[];
  }[];
}

/** finance: transactions, categories, budgets and summaries of one workspace. */
@Injectable({ providedIn: 'root' })
export class FinanceApi {
  private readonly http = inject(HttpClient);

  /** GET /workspaces/{id}/transactions with optional filters. */
  transactions(ws: string, filter: { type?: EntryType | ''; categoryId?: string; page?: number }): Promise<Page<Transaction>> {
    return firstValueFrom(this.http.get<Page<Transaction>>(`/api/v1/workspaces/${ws}/transactions`,
      { params: params({ type: filter.type, categoryId: filter.categoryId, page: filter.page ?? 0, size: 20 }) }));
  }

  /** POST /workspaces/{id}/transactions. */
  createTransaction(ws: string, body: CreateTransaction): Promise<Transaction> {
    return firstValueFrom(this.http.post<Transaction>(`/api/v1/workspaces/${ws}/transactions`, body));
  }

  /** PATCH /workspaces/{id}/transactions/{transactionId}. */
  updateTransaction(ws: string, id: string, body: Partial<CreateTransaction> & { clearCategory?: boolean }): Promise<Transaction> {
    return firstValueFrom(this.http.patch<Transaction>(`/api/v1/workspaces/${ws}/transactions/${id}`, body));
  }

  /** DELETE /workspaces/{id}/transactions/{transactionId}. */
  deleteTransaction(ws: string, id: string): Promise<unknown> {
    return firstValueFrom(this.http.delete(`/api/v1/workspaces/${ws}/transactions/${id}`));
  }

  /** GET /workspaces/{id}/finance/categories. */
  categories(ws: string): Promise<Page<Category>> {
    return firstValueFrom(this.http.get<Page<Category>>(`/api/v1/workspaces/${ws}/finance/categories`, { params: params({ size: 100 }) }));
  }

  /** POST /workspaces/{id}/finance/categories. */
  createCategory(ws: string, body: { type: EntryType; name: string }): Promise<Category> {
    return firstValueFrom(this.http.post<Category>(`/api/v1/workspaces/${ws}/finance/categories`, body));
  }

  /** PATCH /workspaces/{id}/finance/categories/{categoryId}: renames a category. Owners and admins. */
  renameCategory(ws: string, id: string, name: string): Promise<Category> {
    return firstValueFrom(this.http.patch<Category>(`/api/v1/workspaces/${ws}/finance/categories/${id}`, { name }));
  }

  /** DELETE /workspaces/{id}/finance/categories/{categoryId}: deletes an unused category. Owners and admins. */
  deleteCategory(ws: string, id: string): Promise<unknown> {
    return firstValueFrom(this.http.delete(`/api/v1/workspaces/${ws}/finance/categories/${id}`));
  }

  /** PATCH /workspaces/{id}/budgets/{budgetId}: changes the monthly limit. Owners and admins. */
  updateBudget(ws: string, id: string, monthlyLimit: number): Promise<Budget> {
    return firstValueFrom(this.http.patch<Budget>(`/api/v1/workspaces/${ws}/budgets/${id}`, { monthlyLimit }));
  }

  /** DELETE /workspaces/{id}/budgets/{budgetId}. Owners and admins. */
  deleteBudget(ws: string, id: string): Promise<unknown> {
    return firstValueFrom(this.http.delete(`/api/v1/workspaces/${ws}/budgets/${id}`));
  }

  /** GET /workspaces/{id}/finance/export: the CSV file and its name. This month when no range is given. */
  async exportCsv(ws: string, from?: string, to?: string): Promise<{ fileName: string; blob: Blob }> {
    const response = await firstValueFrom(this.http.get(`/api/v1/workspaces/${ws}/finance/export`,
      { params: params({ from, to }), responseType: 'blob', observe: 'response' }));
    const disposition = response.headers.get('Content-Disposition') ?? '';
    const fileName = /filename="?([^";]+)"?/.exec(disposition)?.[1] ?? 'lifehub-transactions.csv';
    return { fileName, blob: response.body! };
  }

  /** GET /workspaces/{id}/budgets. */
  budgets(ws: string): Promise<Page<Budget>> {
    return firstValueFrom(this.http.get<Page<Budget>>(`/api/v1/workspaces/${ws}/budgets`, { params: params({ size: 100 }) }));
  }

  /** POST /workspaces/{id}/budgets: owners and admins. */
  createBudget(ws: string, body: { categoryId: string; monthlyLimit: number; currency: string }): Promise<Budget> {
    return firstValueFrom(this.http.post<Budget>(`/api/v1/workspaces/${ws}/budgets`, body));
  }

  /** GET /workspaces/{id}/finance/summary, this month when no range is given. */
  summary(ws: string, from?: string, to?: string): Promise<FinanceSummary> {
    return firstValueFrom(this.http.get<FinanceSummary>(`/api/v1/workspaces/${ws}/finance/summary`, { params: params({ from, to }) }));
  }
}
