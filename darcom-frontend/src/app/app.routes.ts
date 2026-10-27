import { Routes } from '@angular/router';
import { adminGuard } from './core/admin.guard';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () => import('./pages/register/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'verify-email',
    loadComponent: () =>
      import('./pages/verify-email/verify-email.component').then((m) => m.VerifyEmailComponent),
  },
  {
    path: 'forgot-password',
    loadComponent: () =>
      import('./pages/forgot-password/forgot-password.component').then((m) => m.ForgotPasswordComponent),
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import('./pages/reset-password/reset-password.component').then((m) => m.ResetPasswordComponent),
  },
  {
    path: 'home',
    loadComponent: () => import('./pages/home/home.component').then((m) => m.HomeComponent),
    canActivate: [authGuard],
  },
  {
    path: 'listings',
    loadComponent: () => import('./pages/listings/listing-list.component').then((m) => m.ListingListComponent),
  },
  {
    path: 'listings/:id',
    loadComponent: () => import('./pages/listings/listing-detail.component').then((m) => m.ListingDetailComponent),
  },
  {
    path: 'profile',
    loadComponent: () => import('./pages/profile/profile.component').then((m) => m.ProfileComponent),
    canActivate: [authGuard],
  },
  {
    path: 'admin/users',
    loadComponent: () =>
      import('./pages/admin-users/admin-users.component').then((m) => m.AdminUsersComponent),
    canActivate: [authGuard, adminGuard],
  },
  {
    path: 'admin/reports',
    loadComponent: () =>
      import('./pages/admin-placeholder/admin-placeholder.component').then(
        (m) => m.AdminPlaceholderComponent,
      ),
    canActivate: [authGuard, adminGuard],
    data: {
      title: 'Reports',
      description:
        'User reports and flagged content will be reviewed from this page once the reporting API is available.',
    },
  },
  {
    path: 'admin/logs',
    loadComponent: () =>
      import('./pages/admin-placeholder/admin-placeholder.component').then(
        (m) => m.AdminPlaceholderComponent,
      ),
    canActivate: [authGuard, adminGuard],
    data: {
      title: 'Logs',
      description:
        'Account and moderation activity will be listed here once the audit-log API is available.',
    },
  },
  {
    path: '',
    loadComponent: () =>
      import('./pages/landing/landing.component').then((m) => m.LandingComponent),
    pathMatch: 'full',
  },
  { path: '**', redirectTo: 'home' },
];