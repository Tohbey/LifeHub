import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { describeError } from '../../core/api/errors';
import { Budget, FinanceApi, FinanceSummary } from '../../core/api/finance.api';
import { CalendarEvent, PlannerApi, Task } from '../../core/api/planner.api';
import { ShoppingApi, ShoppingList } from '../../core/api/shopping.api';
import { NotificationStream } from '../../core/notifications/notification-stream.service';
import { WorkspaceService } from '../../core/workspace/workspace.service';
import { Avatar } from '../../shared/avatar';
import { dateTime, money } from '../../shared/format';
import { Icon } from '../../shared/icon';

/** One section's state: data, or the API error that stopped it. */
interface Card<T> { data?: T; error?: string; }

/** One mark on the day strip: an event, a task due today, or an overdue task. */
interface StripItem { id: string; kind: 'event' | 'due'; title: string; time: string; at: number; link: string; }

/** One line under "Needs attention". */
interface Attention { id: string; tone: 'warn' | 'over'; text: string; link: string; }

const HOUR = 3600000;

/**
 * The home page. Its signature is the day strip: today's events and the tasks due today on one timeline, with
 * overdue tasks named on a line above it. Below it, what needs attention, this month's money, open tasks, what is
 * coming up and the shopping lists. Each section calls its own service, so one that is down only breaks itself.
 */
@Component({
  selector: 'lh-dashboard',
  imports: [RouterLink, Icon, Avatar],
  template: `
    <header class="page-head">
      <div class="grow">
        <h1>{{ greeting() }}{{ ws.me()?.displayName ? ', ' + ws.me()?.displayName : '' }}</h1>
        <p class="subtitle">{{ householdLine() }}</p>
      </div>
      <div class="member-stack" aria-hidden="true">
        @for (m of ws.members().slice(0, 5); track m.userId) { <lh-avatar [name]="m.displayName" [size]="34" /> }
      </div>
    </header>

    <section class="day" aria-labelledby="day-title">
      <div class="day-head">
        <h2 id="day-title">{{ today }}</h2>
        @if (strip().length) {
          <span class="day-legend" aria-hidden="true">
            <span><i class="dot event"></i> Event</span><span><i class="dot due"></i> Due today</span>
          </span>
        }
      </div>
      @if (overdue().length) {
        <p class="overdue-line"><i class="dot overdue"></i> <strong>Overdue:</strong>
          @for (t of overdue(); track t.id; let last = $last) { <a routerLink="/tasks">{{ t.title }}</a>@if (!last) {, } }
        </p>
      }
      @if (events().error) { <p class="notice"><lh-icon name="alert" [size]="16" /> {{ events().error }}</p> }
      @if (strip().length) {
        <div class="strip">
          <div class="track" aria-hidden="true">
            @if (nowX() !== null) { <span class="now" [style.--x.%]="nowX()"><span>Now</span></span> }
          </div>
          <div class="ruler" aria-hidden="true">
            @for (h of hours(); track h.label) { <span class="hour" [style.--x.%]="h.x">{{ h.label }}</span> }
          </div>
          <ol class="marks">
            @for (item of strip(); track item.id; let i = $index) {
              <li [class]="item.kind" [class.below]="i % 2 === 1" [style.--x.%]="position(item.at)">
                <a [routerLink]="item.link">
                  <span class="mark-time">{{ item.time }}</span>
                  <span class="mark-title">{{ item.title }}</span>
                </a>
              </li>
            }
          </ol>
        </div>
      } @else if (events().data && tasks().data && !overdue().length) {
        <p class="day-empty">Nothing scheduled or due today. <a routerLink="/calendar">Plan something</a></p>
      }
    </section>

    <div class="board">
      <section class="band attention" aria-labelledby="attention-title">
        <h2 id="attention-title">Needs attention</h2>
        @if (attention().length) {
          <ul class="plain">
            @for (a of attention(); track a.id) {
              <li class="inline"><i class="dot" [class.due]="a.tone === 'warn'" [class.overdue]="a.tone === 'over'"></i>
                <a [routerLink]="a.link" class="grow">{{ a.text }}</a></li>
            }
          </ul>
        } @else {
          <p class="all-clear"><lh-icon name="check" [size]="18" /> Nothing is waiting on you.</p>
        }
      </section>

      <section class="band" aria-labelledby="money-title">
        <h2 id="money-title"><a routerLink="/finance">This month</a></h2>
        @if (summary().error) { <p class="notice"><lh-icon name="alert" [size]="16" /> {{ summary().error }}</p> }
        @for (c of summary().data?.currencies ?? []; track c.currency) {
          <dl class="stats">
            <div><dt>Spent</dt><dd>{{ money(c.expense, c.currency) }}</dd></div>
            <div><dt>Income</dt><dd>{{ money(c.income, c.currency) }}</dd></div>
            <div><dt>Left over</dt><dd [class.negative]="c.net < 0">{{ money(c.net, c.currency) }}</dd></div>
          </dl>
        } @empty {
          @if (summary().data) { <p class="muted">No money in or out yet this month. <a routerLink="/finance">Record an expense</a></p> }
        }
        @for (b of budgetTotals(); track b.currency) {
          <p class="small budget-line">{{ money(b.spent, b.currency) }} of {{ money(b.limit, b.currency) }} budgeted</p>
          <div class="bar" role="img" [attr.aria-label]="b.percent + '% of the ' + b.currency + ' budgets used'">
            <span [class.warn]="b.percent >= 80 && b.percent < 100" [class.over]="b.percent >= 100" [style.width.%]="b.percent > 100 ? 100 : b.percent"></span>
          </div>
        }
      </section>
    </div>

    <div class="board three">
      <section class="band" aria-labelledby="tasks-title">
        <h2 id="tasks-title"><a routerLink="/tasks">Open tasks</a></h2>
        @if (tasks().error) { <p class="notice"><lh-icon name="alert" [size]="16" /> {{ tasks().error }}</p> }
        <ul class="plain">
          @for (t of (tasks().data ?? []).slice(0, 5); track t.id) {
            <li><span class="title">{{ t.title }}</span>
              <span class="muted small">{{ t.dueAt ? 'Due ' + dateTime(t.dueAt) : 'No due date' }}@if (t.assigneeUserId) {, for {{ ws.name(t.assigneeUserId) }}}</span></li>
          }
        </ul>
        @if (tasks().data?.length === 0) { <p class="muted">Nothing open. <a routerLink="/tasks">Add a task</a></p> }
      </section>

      <section class="band" aria-labelledby="coming-title">
        <h2 id="coming-title"><a routerLink="/calendar">Coming up</a></h2>
        <ul class="plain">
          @for (e of comingUp(); track e.id + e.startsAt) {
            <li><span class="title">{{ e.title }}</span>
              <span class="muted small">{{ dateTime(e.startsAt) }}@if (e.location) {, {{ e.location }}}</span></li>
          }
        </ul>
        @if (events().data && comingUp().length === 0) { <p class="muted">Nothing in the next 7 days.</p> }
      </section>

      <section class="band" aria-labelledby="shopping-title">
        <h2 id="shopping-title"><a routerLink="/shopping">Shopping</a></h2>
        @if (lists().error) { <p class="notice"><lh-icon name="alert" [size]="16" /> {{ lists().error }}</p> }
        <ul class="plain">
          @for (l of lists().data ?? []; track l.id) {
            <li class="inline"><a [routerLink]="['/shopping', l.id]" class="grow">{{ l.name }}</a> <span class="tag muted">{{ l.openItems }} to buy</span></li>
          }
        </ul>
        @if (lists().data?.length === 0) { <p class="muted">No lists yet. <a routerLink="/shopping">Start one</a></p> }
      </section>
    </div>
  `,
  styles: `
    .member-stack { display: flex; }
    .member-stack lh-avatar { margin-left: -8px; border: 2px solid var(--bg); border-radius: 50%; }

    /* The day strip: the one bold element on the page. */
    .day { margin: 0 -32px 32px; padding: 24px 32px 28px; background: var(--shelf); }
    .day-head { display: flex; align-items: baseline; gap: 16px; flex-wrap: wrap; margin-bottom: 8px; }
    .day-head h2 { font-size: 1.75rem; margin: 0; letter-spacing: -0.02em; }
    .day-legend { display: flex; gap: 16px; font-size: .875rem; color: var(--text-2); }
    .day-legend span { display: inline-flex; align-items: center; gap: 6px; }
    .day-empty { margin: 8px 0 0; color: var(--text-2); }
    .dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: var(--sage); flex: none; }
    .dot.event { background: var(--sage); }
    .dot.due { background: var(--marigold); }
    .dot.overdue { background: var(--brick); }

    .overdue-line { display: flex; flex-wrap: wrap; align-items: center; gap: 4px 8px; margin: 4px 0 12px; color: var(--brick); }
    .overdue-line a { color: var(--brick); }
    /* Lanes: labels above the track (0-74), the track at 74, labels below (74-140), the hour ruler at 148. */
    .strip { position: relative; height: 172px; margin: 8px 8px 0; }
    .track { position: absolute; left: 0; right: 0; top: 72px; height: 4px; background: var(--line-strong); border-radius: 2px; }
    .ruler { position: absolute; left: 0; right: 0; top: 150px; }
    .hour { position: absolute; left: var(--x); transform: translateX(-50%); font-size: .8125rem; color: var(--text-2); font-variant-numeric: tabular-nums; white-space: nowrap; }
    .hour::before { content: ""; position: absolute; left: 50%; top: -12px; height: 8px; border-left: 1px solid var(--line-strong); }
    .now { position: absolute; left: var(--x); top: -8px; height: 20px; width: 2px; background: var(--text); }
    .now span { position: absolute; top: 22px; left: 6px; font-size: .75rem; font-weight: 700; }
    .marks { list-style: none; margin: 0; padding: 0; }
    .marks li { position: absolute; left: var(--x); top: 0; height: 74px; max-width: 180px; padding: 0 0 0 12px; border-left: 2px solid var(--line-strong); }
    .marks li::after { content: ""; position: absolute; left: -9px; top: 59px; width: 16px; height: 16px; border-radius: 50%; background: var(--sage); border: 3px solid var(--shelf); }
    .marks li.below { top: 74px; height: 64px; padding-top: 22px; }
    .marks li.below::after { top: -8px; }
    .marks li.due::after { background: var(--marigold); }
    .marks a { display: flex; flex-direction: column; color: var(--text); text-decoration: none; line-height: 1.25; }
    .marks a:hover .mark-title { text-decoration: underline; }
    .mark-time { font-size: .8125rem; color: var(--text-2); font-variant-numeric: tabular-nums; }
    .mark-title { font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

    /* The rest: bands separated by rules and space, not a wall of cards. */
    .board { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 0 40px; margin-bottom: 32px; }
    .board.three { grid-template-columns: repeat(3, minmax(0, 1fr)); }
    .band { border-top: 2px solid var(--text); padding-top: 14px; min-width: 0; }
    .band h2 { font-size: 1.25rem; }
    .attention .plain li { padding: 12px 0; }
    .all-clear { display: flex; align-items: center; gap: 8px; color: var(--sage); font-weight: 700; }
    .budget-line { margin: 8px 0 0; color: var(--text-2); }

    @media (max-width: 860px) {
      .day { margin: 0 -16px 28px; padding: 20px 16px; }
      .day-head h2 { font-size: 1.375rem; }
      /* On phones the strip reads as a timed list, top to bottom. */
      .strip { height: auto; margin: 0; }
      .track, .ruler { display: none; }
      .marks li, .marks li.below { position: relative; left: auto; top: auto; height: auto; max-width: none; padding: 8px 0 8px 22px; border-left: 2px solid var(--line-strong); margin-left: 7px; }
      .marks li::after, .marks li.below::after { left: -9px; top: 12px; }
      .marks a { flex-direction: row; gap: 10px; align-items: baseline; }
      .mark-time { min-width: 52px; }
      .mark-title { white-space: normal; }
      .board, .board.three { grid-template-columns: minmax(0, 1fr); gap: 28px; }
    }
  `,
})
export class Dashboard implements OnInit {
  protected readonly ws = inject(WorkspaceService);
  protected readonly stream = inject(NotificationStream);
  private readonly finance = inject(FinanceApi);
  private readonly planner = inject(PlannerApi);
  private readonly shopping = inject(ShoppingApi);

  protected readonly summary = signal<Card<FinanceSummary>>({});
  protected readonly budgets = signal<Card<Budget[]>>({});
  protected readonly tasks = signal<Card<Task[]>>({});
  protected readonly events = signal<Card<CalendarEvent[]>>({});
  protected readonly lists = signal<Card<ShoppingList[]>>({});
  protected readonly money = money;
  protected readonly dateTime = dateTime;
  protected readonly today = new Date().toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long' });

  private readonly dayStart = startOfDay(new Date()).getTime();
  private readonly dayEnd = this.dayStart + 24 * HOUR;

  /** "Good morning", "Good afternoon" or "Good evening". */
  protected readonly greeting = computed(() => {
    const hour = new Date().getHours();
    return hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';
  });

  /** Which workspace this is, your role in it and how many people share it. */
  protected readonly householdLine = computed(() => {
    const current = this.ws.current();
    if (current?.type === 'PERSONAL') {
      return 'Your personal workspace';
    }
    const count = this.ws.members().length;
    return `${current?.name}: you are ${current?.role?.toLowerCase()}, ${count} ${count === 1 ? 'member' : 'members'}`;
  });

  /** Today's events and the tasks due today, in time order. */
  protected readonly strip = computed<StripItem[]>(() => {
    const items: StripItem[] = [];
    for (const e of this.events().data ?? []) {
      const at = Date.parse(e.startsAt);
      if (at < this.dayEnd && Date.parse(e.endsAt) > this.dayStart && e.status !== 'CANCELLED') {
        items.push({ id: `e-${e.id}-${at}`, kind: 'event', title: e.title, link: '/calendar',
          at: Math.max(at, this.dayStart), time: e.allDay || at < this.dayStart ? 'All day' : clock(at) });
      }
    }
    for (const t of this.tasks().data ?? []) {
      const at = t.dueAt ? Date.parse(t.dueAt) : NaN;
      if (at >= this.dayStart && at < this.dayEnd) {
        items.push({ id: `t-${t.id}`, kind: 'due', title: t.title, time: clock(at), at, link: '/tasks' });
      }
    }
    return items.sort((a, b) => a.at - b.at);
  });

  /** Open tasks whose due time was before today. */
  protected readonly overdue = computed(() =>
    (this.tasks().data ?? []).filter((t) => t.dueAt && Date.parse(t.dueAt) < this.dayStart));

  /** The hours the strip shows: 07:00 to 23:00, stretched to fit anything earlier or later. */
  private readonly range = computed(() => {
    const timed = this.strip().map((i) => i.at);
    const first = Math.min(this.dayStart + 7 * HOUR, ...timed);
    const last = Math.max(this.dayStart + 23 * HOUR, ...timed);
    return { from: floorHour(first), to: Math.min(this.dayEnd, floorHour(last) + HOUR) };
  });

  /** Hour labels every three hours along the track. */
  protected readonly hours = computed(() => {
    const { from, to } = this.range();
    const labels: { label: string; x: number }[] = [];
    for (let t = from; t <= to; t += 3 * HOUR) {
      labels.push({ label: clock(t), x: this.position(t) });
    }
    return labels;
  });

  /** Where "now" sits on the track, or null outside the shown hours. */
  protected readonly nowX = computed(() => {
    const now = Date.now();
    const { from, to } = this.range();
    return now >= from && now <= to ? this.position(now) : null;
  });

  /** Budgets at 80% or more, overdue tasks and unread notifications. Empty means nothing is waiting. */
  protected readonly attention = computed<Attention[]>(() => {
    const out: Attention[] = [];
    for (const b of this.budgets().data ?? []) {
      const percent = b.monthlyLimit > 0 ? Math.round((b.spent / b.monthlyLimit) * 100) : 0;
      if (percent >= 80) {
        out.push({ id: `b-${b.id}`, tone: percent >= 100 ? 'over' : 'warn', link: '/finance',
          text: `${this.categoryName(b.categoryId)} budget is at ${percent}% (${money(b.spent, b.currency)} of ${money(b.monthlyLimit, b.currency)})` });
      }
    }
    const overdue = this.overdue().length;
    if (overdue) {
      out.push({ id: 'overdue', tone: 'over', link: '/tasks', text: `${overdue} ${overdue === 1 ? 'task is' : 'tasks are'} overdue` });
    }
    const unread = this.stream.unread();
    if (unread) {
      out.push({ id: 'unread', tone: 'warn', link: '/notifications', text: `${unread} unread ${unread === 1 ? 'notification' : 'notifications'}` });
    }
    return out;
  });

  /** Budget limits and spending added up per currency, for the one bar under "This month". */
  protected readonly budgetTotals = computed(() => {
    const totals = new Map<string, { currency: string; spent: number; limit: number; percent: number }>();
    for (const b of this.budgets().data ?? []) {
      const t = totals.get(b.currency) ?? { currency: b.currency, spent: 0, limit: 0, percent: 0 };
      t.spent += b.spent;
      t.limit += b.monthlyLimit;
      t.percent = t.limit > 0 ? Math.round((t.spent / t.limit) * 100) : 0;
      totals.set(b.currency, t);
    }
    return [...totals.values()];
  });

  /** Events after today, within the next 7 days. */
  protected readonly comingUp = computed(() =>
    (this.events().data ?? []).filter((e) => Date.parse(e.startsAt) >= this.dayEnd && e.status !== 'CANCELLED').slice(0, 5));

  private readonly categoryNames = signal(new Map<string, string>());

  /** Loads every section at once. */
  ngOnInit(): void {
    const id = this.ws.current()!.id;
    void load(this.summary, () => this.finance.summary(id));
    void load(this.budgets, async () => (await this.finance.budgets(id)).items);
    void load(this.tasks, async () => (await this.planner.tasks(id, 'OPEN', false)).items);
    void load(this.events, async () => (await this.planner.events(id, new Date(this.dayStart).toISOString(),
      new Date(this.dayStart + 8 * 24 * HOUR).toISOString())).items);
    void load(this.lists, async () => (await this.shopping.lists(id, false)).items.slice(0, 5));
    this.finance.categories(id).then((page) => this.categoryNames.set(new Map(page.items.map((c) => [c.id, c.name]))),
      () => undefined);
  }

  /** Where a time sits along the track, as a percentage. */
  protected position(at: number): number {
    const { from, to } = this.range();
    return Math.min(100, Math.max(0, ((at - from) / (to - from)) * 100));
  }

  private categoryName(id: string): string {
    return this.categoryNames().get(id) ?? 'A';
  }
}

/** Runs a call and stores either its result or its error in the section. */
async function load<T>(card: ReturnType<typeof signal<Card<T>>>, call: () => Promise<T>): Promise<void> {
  try {
    card.set({ data: await call() });
  } catch (error) {
    card.set({ error: describeError(error) });
  }
}

function startOfDay(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate());
}

function floorHour(at: number): number {
  const d = new Date(at);
  d.setMinutes(0, 0, 0);
  return d.getTime();
}

/** "08:30" in the viewer's own clock. */
function clock(at: number): string {
  return new Date(at).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
}
