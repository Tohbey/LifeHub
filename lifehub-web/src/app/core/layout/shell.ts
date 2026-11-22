import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { describeError } from '../api/errors';
import { AuthService } from '../auth/auth.service';
import { NotificationStream } from '../notifications/notification-stream.service';
import { WorkspaceService } from '../workspace/workspace.service';
import { Avatar } from '../../shared/avatar';
import { Icon } from '../../shared/icon';

interface NavItem { path: string; label: string; icon: string; exact?: boolean; adminOnly?: boolean; }

/**
 * The signed-in frame: top bar with the workspace switcher, the Add menu and the notification bell, a sidebar on wide screens
 * and a bottom bar on phones. Loads the user and workspaces before showing any page.
 */
@Component({
  selector: 'lh-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icon, Avatar],
  template: `
    <aside class="sidebar">
      <a routerLink="/" class="brand"><span class="brand-mark"><lh-icon name="home" [size]="18" /></span> LifeHub</a>
      <nav aria-label="Main">
        @for (group of groups; track group.title) {
          <p class="nav-group">{{ group.title }}</p>
          @for (item of group.items; track item.path) {
            @if (!item.adminOnly || ws.isAdmin()) {
              <a [routerLink]="item.path" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: !!item.exact }">
                <lh-icon [name]="item.icon" />
                <span>{{ item.label }}</span>
                @if (item.path === '/notifications' && stream.unread() > 0) { <span class="nav-count">{{ stream.unread() }}</span> }
              </a>
            }
          }
        }
      </nav>
      <div class="sidebar-user">
        <lh-avatar [name]="displayName()" [size]="36" />
        <div class="who">
          <strong>{{ displayName() }}</strong>
          <span>{{ ws.me()?.email || auth.claims()?.email || 'No email set' }}</span>
        </div>
        <button class="icon-button" (click)="signOut()" aria-label="Sign out" title="Sign out"><lh-icon name="logout" [size]="18" /></button>
      </div>
    </aside>

    <header class="topbar">
      <a routerLink="/" class="brand mobile-only"><span class="brand-mark"><lh-icon name="home" [size]="16" /></span></a>
      @if (ws.workspaces().length) {
        <label class="switcher">
          <lh-avatar class="switcher-avatar" [name]="ws.current()?.type === 'PERSONAL' ? displayName() : ws.current()?.name" [size]="28" />
          <span class="sr-only">Workspace</span>
          <!-- [selected] on each option, not [value] on the select: a newly created or joined workspace's option
               does not exist yet when the value would be set, and the browser would fall back to the first. -->
          <select (change)="switchTo($any($event.target).value)">
            @for (w of ws.workspaces(); track w.id) {
              <option [value]="w.id" [selected]="w.id === ws.current()?.id">{{ w.type === 'PERSONAL' ? 'Personal' : w.name }} ({{ w.role.toLowerCase() }})</option>
            }
          </select>
        </label>
      }
      <span class="spacer"></span>
      <details class="quick-add" #quickAdd>
        <summary class="button"><lh-icon name="plus" [size]="18" /> <span class="add-label">Add</span></summary>
        <nav class="quick-add-menu" aria-label="Add" (click)="quickAdd.open = false">
          <a routerLink="/finance"><lh-icon name="wallet" [size]="18" /> Expense or income</a>
          <a routerLink="/tasks"><lh-icon name="tasks" [size]="18" /> Task</a>
          <a routerLink="/calendar"><lh-icon name="calendar" [size]="18" /> Event</a>
          <a routerLink="/shopping"><lh-icon name="cart" [size]="18" /> Shopping item</a>
          <a routerLink="/meals"><lh-icon name="meal" [size]="18" /> Meal or recipe</a>
          <a routerLink="/notes"><lh-icon name="note" [size]="18" /> Note or link</a>
        </nav>
      </details>
      <span class="live" [class.on]="stream.connected()" [title]="stream.connected() ? 'Live updates are on' : 'Live updates are reconnecting'">
        <span class="live-dot"></span><span class="live-text">{{ stream.connected() ? 'Live' : 'Offline' }}</span>
      </span>
      <a routerLink="/notifications" class="bell" [attr.aria-label]="stream.unread() + ' unread notifications'">
        <lh-icon name="bell" />
        @if (stream.unread() > 0) { <span class="badge">{{ stream.unread() > 99 ? '99+' : stream.unread() }}</span> }
      </a>
      <a routerLink="/profile" class="topbar-avatar" aria-label="Profile"><lh-avatar [name]="displayName()" [size]="32" /></a>
    </header>

    <main class="content">
      @if (error()) {
        <div class="card error-card">
          <div class="empty-icon danger"><lh-icon name="alert" [size]="28" /></div>
          <h2>Could not load your account</h2>
          <p>{{ error() }}</p>
          <button class="button" (click)="load()">Try again</button>
        </div>
      } @else if (ready()) {
        <router-outlet />
      } @else {
        <div class="loading"><span class="spinner"></span> Loading your workspaces…</div>
      }
    </main>

    <nav class="bottombar" aria-label="Main (mobile)">
      @for (item of mobileNav; track item.path) {
        <a [routerLink]="item.path" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: !!item.exact }">
          <lh-icon [name]="item.icon" [size]="22" /><span>{{ item.label }}</span>
        </a>
      }
    </nav>
  `,
})
export class Shell implements OnInit, OnDestroy {
  protected readonly auth = inject(AuthService);
  protected readonly ws = inject(WorkspaceService);
  protected readonly stream = inject(NotificationStream);

  protected readonly ready = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly groups: { title: string; items: NavItem[] }[] = [
    {
      title: 'Everyday',
      items: [
        { path: '/', label: 'Home', icon: 'home', exact: true },
        { path: '/finance', label: 'Finance', icon: 'wallet' },
        { path: '/tasks', label: 'Tasks', icon: 'tasks' },
        { path: '/calendar', label: 'Calendar', icon: 'calendar' },
        { path: '/shopping', label: 'Shopping', icon: 'cart' },
        { path: '/meals', label: 'Meals', icon: 'meal' },
      ],
    },
    {
      title: 'Household',
      items: [
        { path: '/notes', label: 'Notes', icon: 'note' },
        { path: '/household', label: 'Household', icon: 'users' },
        { path: '/notifications', label: 'Notifications', icon: 'bell' },
        { path: '/audit', label: 'Audit log', icon: 'scroll', adminOnly: true },
        { path: '/profile', label: 'Profile', icon: 'settings' },
      ],
    },
  ];
  /** The phone's bottom bar has room for five: the everyday pages except Meals (reached from the Add menu). */
  protected readonly mobileNav = this.groups[0].items.slice(0, 5);

  /** The name shown for the signed-in user. */
  protected readonly displayName = computed(() => this.ws.me()?.displayName || this.auth.claims()?.name || 'You');

  /** Loads the account, then starts live notifications. */
  ngOnInit(): void {
    void this.load();
  }

  /** Closes the notification stream when leaving the signed-in area. */
  ngOnDestroy(): void {
    this.stream.stop();
  }

  /** Loads the user and workspaces; shows the API's error if that fails. */
  protected async load(): Promise<void> {
    this.error.set(null);
    try {
      await this.ws.load();
      this.ready.set(true);
      void this.stream.start();
    } catch (error) {
      this.error.set(describeError(error));
    }
  }

  /** Switches workspace and redraws the page for it. */
  protected async switchTo(id: string): Promise<void> {
    this.ready.set(false);
    await this.ws.select(id);
    this.ready.set(true);
  }

  /** Signs out of Logto. */
  protected async signOut(): Promise<void> {
    this.stream.stop();
    await this.auth.signOut();
  }
}
