import { Page, Request, Route } from '@playwright/test';

import { ALICE, HOME, MEMBERS, PERSONAL, page } from './data';

/** One API call the UI made, for assertions. */
export interface RecordedRequest {
  method: string;
  path: string;
  query: URLSearchParams;
  body: any;
  authorization: string | undefined;
}

/** What a handler answers: a status and a JSON body (or raw text for event streams). */
export interface MockResponse {
  status?: number;
  body?: unknown;
  contentType?: string;
  raw?: string;
  headers?: Record<string, string>;
}

type Handler = (request: RecordedRequest, match: RegExpMatchArray) => MockResponse | Promise<MockResponse>;

/**
 * An in-browser fake of the LifeHub API behind lifehub-app. Every /api/** call is answered here, recorded, and
 * never reaches the network. Defaults cover what each page needs to render; a spec overrides any endpoint with
 * {@link on}, and the last registered handler wins.
 */
export class MockBackend {
  readonly requests: RecordedRequest[] = [];
  private readonly handlers: { method: string; path: RegExp; handler: Handler }[] = [];

  me = { ...ALICE, emailVerified: false };
  pendingInvitations: any[] = [];
  workspaces: any[] = [PERSONAL, HOME];
  members = [...MEMBERS];
  transactions: any[] = [];
  categories: any[] = [{ id: 'cat-food', type: 'EXPENSE', name: 'Food' }, { id: 'cat-salary', type: 'INCOME', name: 'Salary' }];
  tasks: any[] = [];
  lists: any[] = [{ id: 'list-1', workspaceId: HOME.id, name: 'Weekly shop', archived: false, openItems: 0, createdAt: '2026-11-10T09:00:00Z' }];
  items: any[] = [];
  notifications: any[] = [];
  recipes: any[] = [];
  meals: any[] = [];
  notes: any[] = [];
  bookmarks: any[] = [];
  unread = 0;
  private nextId = 1;

  /** Registers the default behaviour of every endpoint the UI calls. */
  constructor() {
    this.on('GET', /^\/api\/v1\/me$/, () => ({ body: this.me }));
    this.on('PATCH', /^\/api\/v1\/me$/, (r) => ({ body: (this.me = { ...this.me, ...r.body }) }));
    this.on('GET', /^\/api\/v1\/me\/workspaces$/, () => ({ body: page(this.workspaces) }));
    this.on('GET', /^\/api\/v1\/households\/([^/]+)\/members$/, () => ({ body: page(this.members) }));
    this.on('GET', /^\/api\/v1\/households\/([^/]+)\/invitations$/, () => ({ body: page(this.pendingInvitations) }));
    this.on('DELETE', /^\/api\/v1\/households\/[^/]+\/invitations\/([^/]+)$/, (_r, m) => {
      this.pendingInvitations = this.pendingInvitations.filter((i) => i.id !== m[1]);
      return { status: 204 };
    });
    this.on('POST', /^\/api\/v1\/me\/email-verification$/, () => ({ status: 202 }));
    this.on('POST', /^\/api\/v1\/me\/email-verification\/([^/]+)$/, () => ({ body: (this.me = { ...this.me, emailVerified: true }) }));
    this.on('GET', /^\/api\/v1\/households\/([^/]+)$/, (_r, m) => {
      const ws = this.workspaces.find((w) => w.id === m[1]);
      return { body: { id: m[1], name: ws?.name, defaultCurrency: 'GBP', timezone: 'Europe/London', createdAt: '2026-11-01T09:00:00Z', yourRole: ws?.role } };
    });

    this.on('GET', /^\/api\/v1\/reference-data$/, () => ({
      body: {
        currencies: [
          { code: 'GBP', name: 'British Pound', symbol: '£', common: true },
          { code: 'EUR', name: 'Euro', symbol: '€', common: true },
          { code: 'NGN', name: 'Nigerian Naira', symbol: 'NGN', common: true },
          { code: 'JPY', name: 'Japanese Yen', symbol: 'JP¥', common: false },
        ],
        timezones: [
          { id: 'Europe/London', offset: '+00:00', common: true },
          { id: 'Africa/Lagos', offset: '+01:00', common: true },
          { id: 'Europe/Lisbon', offset: '+00:00', common: false },
        ],
      },
    }));
    this.on('GET', /^\/api\/v1\/me\/notifications\/unread-count$/, () => ({ body: { unread: this.unread } }));
    this.on('GET', /^\/api\/v1\/me\/notifications$/, () => ({ body: page(this.notifications) }));
    this.on('POST', /^\/api\/v1\/me\/notifications\/read-all$/, () => {
      this.notifications = this.notifications.map((n) => ({ ...n, read: true }));
      this.unread = 0;
      return { body: { unread: 0 } };
    });
    // The live stream: answered with "unavailable" unless a spec sends events with streamEvents().
    this.on('GET', /^\/api\/v1\/me\/notifications\/stream$/, () => ({ status: 503, body: {} }));
    this.on('GET', /^\/api\/v1\/me\/notification-preferences$/, () => ({ body: [] }));

    this.on('GET', /^\/api\/v1\/workspaces\/[^/]+\/finance\/summary$/, () => ({
      body: { from: '2026-11-01T00:00:00Z', to: '2026-12-01T00:00:00Z', currencies: [] },
    }));
    this.on('GET', /^\/api\/v1\/workspaces\/[^/]+\/finance\/categories$/, () => ({ body: page(this.categories) }));
    this.on('GET', /^\/api\/v1\/workspaces\/[^/]+\/transactions$/, () => ({ body: page(this.transactions) }));
    this.on('POST', /^\/api\/v1\/workspaces\/[^/]+\/transactions$/, (r) => {
      const t = { id: this.id('tx'), createdByUserId: this.me.id, payerUserId: this.me.id, splits: [], ...r.body };
      this.transactions.unshift(t);
      return { status: 201, body: t };
    });
    this.on('GET', /^\/api\/v1\/workspaces\/[^/]+\/budgets$/, () => ({ body: page([]) }));

    this.on('GET', /^\/api\/v1\/workspaces\/[^/]+\/tasks$/, (r) => ({
      body: page(this.tasks.filter((t) => !r.query.get('status') || t.status === r.query.get('status'))),
    }));
    this.on('POST', /^\/api\/v1\/workspaces\/[^/]+\/tasks$/, (r) => {
      const t = { id: this.id('task'), status: 'OPEN', description: null, dueAt: null, completedAt: null,
        completedByUserId: null, createdByUserId: this.me.id, createdAt: new Date().toISOString(), ...r.body };
      this.tasks.push(t);
      return { status: 201, body: t };
    });
    this.on('PATCH', /^\/api\/v1\/workspaces\/[^/]+\/tasks\/([^/]+)$/, (r, m) => {
      const t = this.tasks.find((x) => x.id === m[1]);
      Object.assign(t, r.body, r.body.status === 'DONE' ? { completedByUserId: this.me.id, completedAt: new Date().toISOString() } : {});
      return { body: t };
    });
    this.on('DELETE', /^\/api\/v1\/workspaces\/[^/]+\/tasks\/([^/]+)$/, (_r, m) => {
      this.tasks = this.tasks.filter((t) => t.id !== m[1]);
      return { status: 204 };
    });
    this.on('GET', /^\/api\/v1\/workspaces\/[^/]+\/events$/, () => ({ body: page([]) }));
    this.on('GET', /^\/api\/v1\/workspaces\/[^/]+\/finance\/export$/, () => ({
      contentType: 'text/csv', raw: 'Date,Type,Description\r\n2026-11-09 10:00,EXPENSE,Electricity\r\n',
      headers: { 'Content-Disposition': 'attachment; filename="lifehub-transactions-2026-11-01-to-2026-11-30.csv"' },
    }));

    this.on('GET', /^\/api\/v1\/workspaces\/[^/]+\/shopping-lists$/, () => ({ body: page(this.lists) }));
    this.on('GET', /^\/api\/v1\/workspaces\/[^/]+\/shopping-lists\/([^/]+)$/, (_r, m) => ({ body: this.lists.find((l) => l.id === m[1]) }));
    this.on('GET', /^\/api\/v1\/shopping-lists\/([^/]+)\/items$/, (r) => ({
      body: page(this.items.filter((i) => r.query.get('open') !== 'true' || !i.purchased)),
    }));
    this.on('POST', /^\/api\/v1\/shopping-lists\/([^/]+)\/items$/, (r, m) => {
      const i = { id: this.id('item'), shoppingListId: m[1], quantity: null, note: null, purchased: false, purchasedAt: null,
        purchasedByUserId: null, addedByUserId: this.me.id, createdAt: new Date().toISOString(), ...r.body };
      this.items.push(i);
      return { status: 201, body: i };
    });
    this.on('PATCH', /^\/api\/v1\/shopping-items\/([^/]+)$/, (r, m) => {
      const i = this.items.find((x) => x.id === m[1]);
      Object.assign(i, r.body, r.body.purchased ? { purchasedByUserId: this.me.id } : {});
      return { body: i };
    });

    this.on('GET', /^\/api\/v1\/workspaces\/[^/]+\/audit$/, () => ({ body: page([]) }));

    // shopping-meals: recipes and the meal plan.
    this.on('GET', /^\/api\/v1\/workspaces\/[^/]+\/recipes$/, (r) => ({
      body: page(this.recipes.filter((x) => !r.query.get('q') || x.name.toLowerCase().includes(r.query.get('q')!.toLowerCase()))),
    }));
    this.on('POST', /^\/api\/v1\/workspaces\/([^/]+)\/recipes$/, (r, m) => {
      const x = { id: this.id('recipe'), workspaceId: m[1], servings: null, instructions: null, sourceUrl: null,
        createdByUserId: this.me.id, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(), ...r.body };
      this.recipes.push(x);
      return { status: 201, body: x };
    });
    this.on('PUT', /^\/api\/v1\/workspaces\/[^/]+\/recipes\/([^/]+)$/, (r, m) => {
      const x = this.recipes.find((y) => y.id === m[1]);
      Object.assign(x, r.body);
      return { body: x };
    });
    this.on('DELETE', /^\/api\/v1\/workspaces\/[^/]+\/recipes\/([^/]+)$/, (_r, m) => {
      this.recipes = this.recipes.filter((y) => y.id !== m[1]);
      return { status: 204 };
    });
    this.on('POST', /^\/api\/v1\/workspaces\/[^/]+\/recipes\/([^/]+)\/shopping-list$/, (r, m) => {
      const recipe = this.recipes.find((y) => y.id === m[1]);
      const added = recipe.ingredients.map((i: any) => ({ id: this.id('item'), shoppingListId: r.body.shoppingListId, name: i.name,
        quantity: i.quantity, note: `For ${recipe.name}`, purchased: false, addedByUserId: this.me.id, createdAt: new Date().toISOString() }));
      this.items.push(...added);
      return { status: 201, body: added };
    });
    this.on('GET', /^\/api\/v1\/workspaces\/[^/]+\/meals$/, (r) => ({
      body: this.meals.filter((x) => x.date >= r.query.get('from')! && x.date <= r.query.get('to')!),
    }));
    this.on('POST', /^\/api\/v1\/workspaces\/([^/]+)\/meals$/, (r, m) => {
      const recipe = this.recipes.find((y) => y.id === r.body.recipeId);
      const x = { id: this.id('meal'), workspaceId: m[1], recipeId: null, note: null, createdByUserId: this.me.id,
        createdAt: new Date().toISOString(), ...r.body, title: recipe ? recipe.name : r.body.title };
      this.meals.push(x);
      return { status: 201, body: x };
    });
    this.on('DELETE', /^\/api\/v1\/workspaces\/[^/]+\/meals\/([^/]+)$/, (_r, m) => {
      this.meals = this.meals.filter((y) => y.id !== m[1]);
      return { status: 204 };
    });

    // notes: notes, bookmarks and their tags (lower-cased and de-duplicated, like the API).
    for (const [kind, prefix] of [['notes', 'note'], ['bookmarks', 'link']] as const) {
      this.on('GET', new RegExp(`^/api/v1/workspaces/[^/]+/${kind}$`), (r) => ({
        body: page(this[kind].filter((x: any) => (!r.query.get('tag') || x.tags.includes(r.query.get('tag')))
          && (!r.query.get('q') || JSON.stringify(x).toLowerCase().includes(r.query.get('q')!.toLowerCase())))),
      }));
      this.on('POST', new RegExp(`^/api/v1/workspaces/([^/]+)/${kind}$`), (r, m) => {
        const x = { id: this.id(prefix), workspaceId: m[1], pinned: false, body: null, description: null,
          createdByUserId: this.me.id, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(),
          ...r.body, title: r.body.title ?? (r.body.url ? new URL(r.body.url).host : ''), tags: tagsOf(r.body.tags) };
        this[kind].unshift(x);
        return { status: 201, body: x };
      });
      this.on('PUT', new RegExp(`^/api/v1/workspaces/[^/]+/${kind}/([^/]+)$`), (r, m) => {
        const x = this[kind].find((y: any) => y.id === m[1]);
        Object.assign(x, r.body, { tags: tagsOf(r.body.tags) });
        return { body: x };
      });
      this.on('DELETE', new RegExp(`^/api/v1/workspaces/[^/]+/${kind}/([^/]+)$`), (_r, m) => {
        (this as any)[kind] = this[kind].filter((y: any) => y.id !== m[1]);
        return { status: 204 };
      });
    }
    this.on('GET', /^\/api\/v1\/workspaces\/[^/]+\/tags$/, () => {
      const counts = new Map<string, { tag: string; notes: number; bookmarks: number }>();
      for (const [kind, list] of [['notes', this.notes], ['bookmarks', this.bookmarks]] as const) {
        for (const t of list.flatMap((x: any) => x.tags)) {
          const c = counts.get(t) ?? { tag: t, notes: 0, bookmarks: 0 };
          c[kind]++;
          counts.set(t, c);
        }
      }
      return { body: [...counts.values()].sort((a, b) => a.tag.localeCompare(b.tag)) };
    });
  }

  /** Answers {@code method path} with this handler from now on. */
  on(method: string, path: RegExp, handler: Handler): this {
    this.handlers.push({ method, path, handler });
    return this;
  }

  /** Answers the notification stream with these events, once; later reconnects get "unavailable". */
  streamEvents(...notifications: unknown[]): this {
    let sent = false;
    return this.on('GET', /^\/api\/v1\/me\/notifications\/stream$/, () => {
      if (sent) {
        return { status: 503, body: {} };
      }
      sent = true;
      const events = notifications.map((n: any) => `event:notification\nid:${n.id}\ndata:${JSON.stringify(n)}\n\n`);
      return { contentType: 'text/event-stream', raw: `:connected\n\n${events.join('')}` };
    });
  }

  /** The recorded calls matching a method and path. */
  calls(method: string, path: RegExp): RecordedRequest[] {
    return this.requests.filter((r) => r.method === method && path.test(r.path));
  }

  /** Routes every /api/** call of the page to this backend. */
  async install(page: Page): Promise<void> {
    await page.route('**/api/**', (route) => this.handle(route));
  }

  /** Answers one intercepted call with the last matching handler, or 404 when none matches. */
  private async handle(route: Route): Promise<void> {
    const request = route.request();
    const recorded = record(request);
    this.requests.push(recorded);
    const found = [...this.handlers].reverse()
      .map((h) => ({ h, match: h.method === recorded.method ? recorded.path.match(h.path) : null }))
      .find((x) => x.match);
    if (!found) {
      await route.fulfill({ status: 404, json: { title: 'Not Found', status: 404, detail: `No mock for ${recorded.method} ${recorded.path}` } });
      return;
    }
    const response = await found.h.handler(recorded, found.match!);
    if (response.raw !== undefined) {
      await route.fulfill({ status: response.status ?? 200, contentType: response.contentType ?? 'text/plain', body: response.raw,
        headers: response.headers });
      return;
    }
    if (response.status === 204 || response.status === 202) {
      await route.fulfill({ status: response.status, headers: response.headers });
      return;
    }
    await route.fulfill({ status: response.status ?? 200, contentType: 'application/json', body: JSON.stringify(response.body ?? null),
      headers: response.headers });
  }

  /** A new fake ID with a prefix. */
  private id(prefix: string): string {
    return `${prefix}-${this.nextId++}`;
  }
}

/** Tags as the notes service stores them: trimmed, lower case, unique, sorted. */
function tagsOf(tags: string[] | undefined): string[] {
  return [...new Set((tags ?? []).map((t) => t.trim().toLowerCase()).filter(Boolean))].sort();
}

/** Turns a Playwright request into a {@link RecordedRequest}. */
function record(request: Request): RecordedRequest {
  const url = new URL(request.url());
  let body: any = null;
  try {
    body = request.postDataJSON();
  } catch {
    body = request.postData();
  }
  return { method: request.method(), path: url.pathname, query: url.searchParams, body, authorization: request.headers()['authorization'] };
}
