import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { PlannerApi, Task, TaskStatus } from '../../core/api/planner.api';
import { WorkspaceService } from '../../core/workspace/workspace.service';
import { dateTime, toInstant, toLocalInput } from '../../shared/format';
import { runWithToast } from '../../shared/run';
import { ToastService } from '../../shared/toast.service';

/** planner tasks for the current workspace: create, assign, complete and reopen. */
@Component({
  selector: 'lh-tasks',
  imports: [FormsModule],
  template: `
    <header class="page-head"><div class="grow"><h1>Tasks</h1><p class="subtitle">A shared to-do list. Assign, finish and keep track.</p></div></header>
    <section class="card">
      <h2>New task</h2>
      <form class="form" (ngSubmit)="create()">
        <div class="row">
          <label class="grow">Title <input name="title" required maxlength="200" [(ngModel)]="title" /></label>
          <label>Assign to
            <select name="assignee" [(ngModel)]="assignee">
              <option [ngValue]="null">Nobody</option>
              @for (m of ws.members(); track m.userId) { <option [ngValue]="m.userId">{{ ws.name(m.userId) }}</option> }
            </select>
          </label>
          <label>Due <input name="due" type="datetime-local" [min]="minDue()" [(ngModel)]="due" /></label>
        </div>
        <label>Notes <textarea name="description" rows="2" maxlength="2000" [(ngModel)]="description"></textarea></label>
        @if (dueInPast()) { <p class="hint error-text">The due date cannot be in the past.</p> }
        <button class="button" [disabled]="busy() || dueInPast()">Add task</button>
      </form>
    </section>

    <section class="card">
      <div class="row">
        <h2 class="grow">Task list</h2>
        <select name="status" aria-label="Filter by status" [(ngModel)]="status" (ngModelChange)="load()">
          <option value="OPEN">Open</option><option value="DONE">Done</option><option value="">All</option>
        </select>
        <label class="checkbox"><input type="checkbox" name="mine" [(ngModel)]="mine" (ngModelChange)="load()" /> Assigned to me</label>
      </div>
      <table class="table">
        <thead><tr><th></th><th>Task</th><th>Assignee</th><th>Due</th><th></th></tr></thead>
        <tbody>
          @for (t of tasks(); track t.id) {
            <tr [class.done]="t.status === 'DONE'" [class.overdue]="isOverdue(t)">
              <td><input type="checkbox" [checked]="t.status === 'DONE'" (change)="toggle(t)" [attr.aria-label]="'Complete ' + t.title" /></td>
              <td>{{ t.title }}
                @if (t.description) { <div class="muted small">{{ t.description }}</div> }
                @if (t.status === 'DONE') { <div class="muted small">Done by {{ ws.name(t.completedByUserId) }} {{ dateTime(t.completedAt) }}</div> }
              </td>
              <td>
                <select [ngModel]="t.assigneeUserId" (ngModelChange)="assign(t, $event)" [name]="'assign-' + t.id"
                        [attr.aria-label]="'Assignee of ' + t.title">
                  <option [ngValue]="null">Nobody</option>
                  @for (m of ws.members(); track m.userId) { <option [ngValue]="m.userId">{{ ws.name(m.userId) }}</option> }
                </select>
              </td>
              <td>{{ dateTime(t.dueAt) }}</td>
              <td class="actions">
                <span class="muted small">by {{ ws.name(t.createdByUserId) }}</span>
                @if (canDelete(t)) { <button class="button ghost danger" (click)="remove(t)" [attr.aria-label]="'Delete ' + t.title">Delete</button> }
              </td>
            </tr>
          } @empty { <tr><td colspan="5" class="muted">No tasks.</td></tr> }
        </tbody>
      </table>
    </section>
  `,
})
export class Tasks implements OnInit {
  protected readonly ws = inject(WorkspaceService);
  private readonly api = inject(PlannerApi);
  private readonly toasts = inject(ToastService);

  protected readonly tasks = signal<Task[]>([]);
  protected readonly busy = signal(false);
  protected readonly dateTime = dateTime;
  protected title = '';
  protected description = '';
  protected assignee: string | null = null;
  protected due = '';
  protected status: TaskStatus | '' = 'OPEN';
  protected mine = false;

  /** Loads the open tasks. */
  ngOnInit(): void {
    void this.load();
  }

  /** GET /tasks with the filters. */
  protected async load(): Promise<void> {
    await runWithToast(this.busy, this.toasts, async () =>
      this.tasks.set((await this.api.tasks(this.id, this.status, this.mine)).items));
  }

  /** POST /tasks. */
  protected async create(): Promise<void> {
    await runWithToast(this.busy, this.toasts, async () => {
      await this.api.createTask(this.id, {
        title: this.title, description: this.description || undefined, assigneeUserId: this.assignee, dueAt: toInstant(this.due),
      });
      this.title = '';
      this.description = '';
      this.due = '';
      this.toasts.success(this.assignee && this.assignee !== this.ws.me()?.id ? 'Task added; the assignee is notified.' : 'Task added.');
      await this.load();
    });
  }

  /** PATCH /tasks/{id} with status DONE or OPEN. */
  protected async toggle(t: Task): Promise<void> {
    await runWithToast(this.busy, this.toasts, async () => {
      await this.api.updateTask(this.id, t.id, { status: t.status === 'DONE' ? 'OPEN' : 'DONE' });
      await this.load();
    });
  }

  /** PATCH /tasks/{id} with a new assignee, or unassign. */
  protected async assign(t: Task, userId: string | null): Promise<void> {
    await runWithToast(this.busy, this.toasts, async () => {
      await this.api.updateTask(this.id, t.id, userId ? { assigneeUserId: userId } : { unassign: true });
      this.toasts.success(userId ? `Assigned to ${this.ws.name(userId)}.` : 'Unassigned.');
      await this.load();
    });
  }

  /** The creator, or an owner or admin, may delete a task; the API checks it too. */
  protected canDelete(t: Task): boolean {
    return t.createdByUserId === this.ws.me()?.id || this.ws.isAdmin();
  }

  /** DELETE /tasks/{id}. */
  protected async remove(t: Task): Promise<void> {
    if (!confirm(`Delete "${t.title}"?`)) {
      return;
    }
    await runWithToast(this.busy, this.toasts, async () => {
      await this.api.deleteTask(this.id, t.id);
      this.toasts.success('Task deleted.');
      await this.load();
    });
  }

  /** The earliest due time the picker offers: now, to the minute. */
  protected minDue(): string {
    return toLocalInput(new Date());
  }

  /** True when the chosen due time is already in the past; the API refuses those too. */
  protected dueInPast(): boolean {
    return !!this.due && new Date(this.due).getTime() < Date.now() - 60_000;
  }

  /** True for an open task past its due time. */
  protected isOverdue(t: Task): boolean {
    return t.status === 'OPEN' && !!t.dueAt && new Date(t.dueAt) < new Date();
  }

  /** The current workspace's ID. */
  private get id(): string {
    return this.ws.current()!.id;
  }
}
