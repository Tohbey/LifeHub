import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Bookmark, Note, NotesApi, TagCount, Visibility } from '../../core/api/notes.api';
import { WorkspaceService } from '../../core/workspace/workspace.service';
import { dateTime } from '../../shared/format';
import { runWithToast } from '../../shared/run';
import { ToastService } from '../../shared/toast.service';

/** notes: notes and saved links, personal or shared with the household, filtered by tag or search. */
@Component({
  selector: 'lh-notes',
  imports: [FormsModule],
  template: `
    <header class="page-head"><div class="grow"><h1>Notes</h1>
      <p class="subtitle">The Wi-Fi password, the boiler's history, the links everyone keeps asking for.</p></div></header>

    <div class="tabs" role="tablist">
      <button role="tab" [class.active]="tab() === 'notes'" [attr.aria-selected]="tab() === 'notes'" (click)="switchTo('notes')">Notes</button>
      <button role="tab" [class.active]="tab() === 'links'" [attr.aria-selected]="tab() === 'links'" (click)="switchTo('links')">Links</button>
    </div>

    <div class="filters">
      <label>Search <input name="q" [(ngModel)]="q" (ngModelChange)="load()" placeholder="{{ tab() === 'notes' ? 'Title or text' : 'Title or link' }}" /></label>
      @if (tagsHere().length) {
        <div class="chips" role="group" aria-label="Filter by tag">
          <button type="button" class="chip" [class.on]="!tag()" [attr.aria-pressed]="!tag()" (click)="filterTag(null)">All</button>
          @for (t of tagsHere(); track t.tag) {
            <button type="button" class="chip" [class.on]="tag() === t.tag" [attr.aria-pressed]="tag() === t.tag" (click)="filterTag(t.tag)">
              {{ t.tag }} <span class="count">{{ tab() === 'notes' ? t.notes : t.bookmarks }}</span></button>
          }
        </div>
      }
    </div>

    @if (tab() === 'notes') {
      <section class="card">
        <h2>{{ editing ? 'Edit note' : 'New note' }}</h2>
        <form class="form" (ngSubmit)="saveNote()">
          <label>Title <input name="title" required maxlength="150" [(ngModel)]="note.title" placeholder="Wi-Fi" /></label>
          <label>Text <textarea name="body" rows="4" maxlength="20000" [(ngModel)]="note.body"></textarea></label>
          <div class="row">
            <label class="grow">Tags (comma separated) <input name="tags" [(ngModel)]="note.tags" placeholder="home, admin" /></label>
            <label class="checkbox"><input type="checkbox" name="pinned" [(ngModel)]="note.pinned" /> Pin to the top</label>
            @if (isHousehold()) { <label class="checkbox"><input type="checkbox" name="shared" [(ngModel)]="note.shared" /> Share with the household</label> }
          </div>
          <div class="row">
            <button class="button" [disabled]="busy() || !note.title.trim()">{{ editing ? 'Save' : 'Add note' }}</button>
            @if (editing) { <button type="button" class="button ghost" (click)="reset()">Cancel</button> }
          </div>
        </form>
      </section>

      <div class="notes">
        @for (n of notes(); track n.id) {
          <article class="note" [class.pinned]="n.pinned" [attr.aria-label]="n.title">
            <header>
              <h3>{{ n.title }}</h3>
              @if (n.pinned) { <span class="tag">Pinned</span> }
              @if (isHousehold() && n.visibility === 'PERSONAL') { <span class="tag muted">Only you</span> }
            </header>
            @if (n.body) { <p class="body">{{ n.body }}</p> }
            <footer>
              @for (t of n.tags; track t) { <span class="tag muted">{{ t }}</span> }
              <span class="muted small grow">{{ ws.name(n.createdByUserId) }}, {{ dateTime(n.updatedAt) }}</span>
              <button class="button ghost" (click)="editNote(n)" [attr.aria-label]="'Edit ' + n.title">Edit</button>
              <button class="button ghost danger" (click)="deleteNote(n)" [attr.aria-label]="'Delete ' + n.title">Delete</button>
            </footer>
          </article>
        } @empty { <p class="muted">{{ q || tag() ? 'No notes match.' : 'No notes yet.' }}</p> }
      </div>
    } @else {
      <section class="card">
        <h2>{{ editing ? 'Edit link' : 'Save a link' }}</h2>
        <form class="form" (ngSubmit)="saveLink()">
          <div class="row">
            <label class="grow">Link <input name="url" type="url" required maxlength="2000" [(ngModel)]="link.url" placeholder="https://" /></label>
            <label class="grow">Title (optional) <input name="ltitle" maxlength="200" [(ngModel)]="link.title" /></label>
          </div>
          <label>Description <input name="description" maxlength="500" [(ngModel)]="link.description" /></label>
          <div class="row">
            <label class="grow">Tags (comma separated) <input name="ltags" [(ngModel)]="link.tags" placeholder="school" /></label>
            @if (isHousehold()) { <label class="checkbox"><input type="checkbox" name="lshared" [(ngModel)]="link.shared" /> Share with the household</label> }
          </div>
          <div class="row">
            <button class="button" [disabled]="busy() || !link.url.trim()">{{ editing ? 'Save' : 'Save link' }}</button>
            @if (editing) { <button type="button" class="button ghost" (click)="reset()">Cancel</button> }
          </div>
        </form>
      </section>

      <section class="card">
        <ul class="list">
          @for (b of bookmarks(); track b.id) {
            <li>
              <div class="grow">
                <a [href]="b.url" target="_blank" rel="noopener noreferrer"><strong>{{ b.title }}</strong></a>
                <span class="muted small"> {{ host(b.url) }}</span>
                @if (isHousehold() && b.visibility === 'PERSONAL') { <span class="tag muted">Only you</span> }
                @if (b.description) { <p class="small">{{ b.description }}</p> }
                <div>@for (t of b.tags; track t) { <span class="tag muted">{{ t }}</span> }</div>
              </div>
              <button class="button ghost" (click)="editLink(b)" [attr.aria-label]="'Edit ' + b.title">Edit</button>
              <button class="button ghost danger" (click)="deleteLink(b)" [attr.aria-label]="'Delete ' + b.title">Delete</button>
            </li>
          } @empty { <li class="muted">{{ q || tag() ? 'No links match.' : 'No links yet.' }}</li> }
        </ul>
      </section>
    }
  `,
  styles: `
    .filters { display: flex; flex-wrap: wrap; align-items: flex-end; gap: 16px; margin-bottom: 20px; }
    .filters label { display: flex; flex-direction: column; gap: 6px; font-weight: 700; font-size: .9375rem; }
    .chips { display: flex; flex-wrap: wrap; gap: 6px; }
    .chip { font: inherit; font-size: .875rem; padding: 6px 12px; border-radius: 999px; border: 1px solid var(--line-strong);
      background: transparent; color: var(--text); cursor: pointer; min-height: 36px; }
    .chip.on { background: var(--text); color: var(--bg); border-color: var(--text); }
    .chip .count { color: inherit; opacity: .8; margin-left: 2px; }
    .notes { columns: 2 320px; column-gap: 20px; }
    .note { break-inside: avoid; margin: 0 0 20px; padding: 18px 20px; background: var(--surface);
      border: 1px solid var(--line); border-radius: var(--radius); }
    .note.pinned { border-top: 4px solid var(--sage); }
    .note header { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 6px; }
    .note h3 { margin: 0; font-size: 1.125rem; flex: 1 1 auto; }
    .note .body { white-space: pre-wrap; overflow-wrap: anywhere; color: var(--text); }
    .note footer { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; margin-top: 10px; }
  `,
})
export class Notes implements OnInit {
  protected readonly ws = inject(WorkspaceService);
  private readonly api = inject(NotesApi);
  private readonly toasts = inject(ToastService);

  protected readonly tab = signal<'notes' | 'links'>('notes');
  protected readonly busy = signal(false);
  protected readonly notes = signal<Note[]>([]);
  protected readonly bookmarks = signal<Bookmark[]>([]);
  protected readonly tags = signal<TagCount[]>([]);
  protected readonly tag = signal<string | null>(null);
  protected readonly dateTime = dateTime;
  protected readonly isHousehold = computed(() => this.ws.current()?.type === 'HOUSEHOLD');
  /** Only the tags used on the current tab. */
  protected readonly tagsHere = computed(() =>
    this.tags().filter((t) => (this.tab() === 'notes' ? t.notes : t.bookmarks) > 0));

  protected q = '';
  protected editing: string | null = null;
  protected note = this.emptyNote();
  protected link = this.emptyLink();

  ngOnInit(): void {
    void this.load();
  }

  protected switchTo(tab: 'notes' | 'links'): void {
    this.tab.set(tab);
    this.tag.set(null);
    this.reset();
    void this.load();
  }

  protected filterTag(tag: string | null): void {
    this.tag.set(tag);
    void this.load();
  }

  /** Loads the current tab's items and the tag counts. */
  protected async load(): Promise<void> {
    const filter = { q: this.q.trim() || undefined, tag: this.tag() ?? undefined };
    await runWithToast(this.busy, this.toasts, async () => {
      const [items, tags] = await Promise.all([
        this.tab() === 'notes' ? this.api.notes(this.id, filter) : this.api.bookmarks(this.id, filter),
        this.api.tags(this.id),
      ]);
      if (this.tab() === 'notes') {
        this.notes.set(items.items as Note[]);
      } else {
        this.bookmarks.set(items.items as Bookmark[]);
      }
      this.tags.set(tags);
    });
  }

  /** POST or PUT /notes. */
  protected async saveNote(): Promise<void> {
    const body = { title: this.note.title.trim(), body: this.note.body.trim() || null, pinned: this.note.pinned,
      visibility: this.visibility(this.note.shared), tags: splitTags(this.note.tags) };
    await runWithToast(this.busy, this.toasts, async () => {
      if (this.editing) {
        await this.api.replaceNote(this.id, this.editing, body);
      } else {
        await this.api.createNote(this.id, body);
      }
      this.toasts.success(this.editing ? 'Note saved.' : 'Note added.');
      this.reset();
      await this.load();
    });
  }

  protected editNote(n: Note): void {
    this.editing = n.id;
    this.note = { title: n.title, body: n.body ?? '', pinned: n.pinned, shared: n.visibility === 'SHARED', tags: n.tags.join(', ') };
  }

  protected async deleteNote(n: Note): Promise<void> {
    if (!confirm(`Delete the note "${n.title}"?`)) {
      return;
    }
    await runWithToast(this.busy, this.toasts, async () => {
      await this.api.deleteNote(this.id, n.id);
      this.toasts.success('Note deleted.');
      await this.load();
    });
  }

  /** POST or PUT /bookmarks. */
  protected async saveLink(): Promise<void> {
    const body = { url: this.link.url.trim(), title: this.link.title.trim() || null,
      description: this.link.description.trim() || null, visibility: this.visibility(this.link.shared), tags: splitTags(this.link.tags) };
    await runWithToast(this.busy, this.toasts, async () => {
      if (this.editing) {
        await this.api.replaceBookmark(this.id, this.editing, body);
      } else {
        await this.api.createBookmark(this.id, body);
      }
      this.toasts.success('Link saved.');
      this.reset();
      await this.load();
    });
  }

  protected editLink(b: Bookmark): void {
    this.editing = b.id;
    this.link = { url: b.url, title: b.title, description: b.description ?? '', shared: b.visibility === 'SHARED', tags: b.tags.join(', ') };
  }

  protected async deleteLink(b: Bookmark): Promise<void> {
    if (!confirm(`Delete the link "${b.title}"?`)) {
      return;
    }
    await runWithToast(this.busy, this.toasts, async () => {
      await this.api.deleteBookmark(this.id, b.id);
      this.toasts.success('Link deleted.');
      await this.load();
    });
  }

  protected reset(): void {
    this.editing = null;
    this.note = this.emptyNote();
    this.link = this.emptyLink();
  }

  protected host(url: string): string {
    try {
      return new URL(url).host;
    } catch {
      return '';
    }
  }

  /** In a personal workspace everything is personal; in a household it follows the checkbox. */
  private visibility(shared: boolean): Visibility {
    return this.isHousehold() && shared ? 'SHARED' : 'PERSONAL';
  }

  private emptyNote() {
    return { title: '', body: '', pinned: false, shared: true, tags: '' };
  }

  private emptyLink() {
    return { url: '', title: '', description: '', shared: true, tags: '' };
  }

  private get id(): string {
    return this.ws.current()!.id;
  }
}

/** "Home, admin,, home" becomes ["Home", "admin", "home"]; the API lower-cases and removes duplicates. */
export function splitTags(text: string): string[] {
  return text.split(',').map((t) => t.trim()).filter(Boolean).slice(0, 10);
}
