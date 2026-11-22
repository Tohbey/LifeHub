import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';
import { Shell } from './core/layout/shell';
import { Callback } from './features/callback/callback';

/** Feature pages load on demand; everything except /callback needs a signed-in user. */
export const routes: Routes = [
  { path: 'callback', component: Callback },
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard) },
      { path: 'finance', loadComponent: () => import('./features/finance/finance').then((m) => m.Finance) },
      { path: 'tasks', loadComponent: () => import('./features/planner/tasks').then((m) => m.Tasks) },
      { path: 'calendar', loadComponent: () => import('./features/planner/calendar').then((m) => m.Calendar) },
      { path: 'shopping', loadComponent: () => import('./features/shopping/shopping-lists').then((m) => m.ShoppingLists) },
      { path: 'shopping/:listId', loadComponent: () => import('./features/shopping/shopping-list').then((m) => m.ShoppingListPage) },
      { path: 'meals', loadComponent: () => import('./features/meals/meals').then((m) => m.Meals) },
      { path: 'notes', loadComponent: () => import('./features/notes/notes').then((m) => m.Notes) },
      { path: 'household', loadComponent: () => import('./features/household/household').then((m) => m.HouseholdPage) },
      { path: 'invitations/:token', loadComponent: () => import('./features/household/accept-invitation').then((m) => m.AcceptInvitation) },
      { path: 'notifications', loadComponent: () => import('./features/notifications/notifications').then((m) => m.Notifications) },
      { path: 'audit', loadComponent: () => import('./features/audit/audit').then((m) => m.Audit) },
      { path: 'profile', loadComponent: () => import('./features/profile/profile').then((m) => m.Profile) },
      { path: 'verify-email/:token', loadComponent: () => import('./features/profile/verify-email').then((m) => m.VerifyEmail) },
    ],
  },
  { path: '**', redirectTo: '' },
];
