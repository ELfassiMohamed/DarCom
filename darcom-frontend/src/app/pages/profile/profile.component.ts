import { Component, OnInit, computed, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '../../core/auth.service';
import { toUserMessage } from '../../core/error-messages';
import { Role, User } from '../../core/models';
import { MegaNavComponent } from '../../shared/mega-nav/mega-nav.component';

type NotificationId = 'productUpdates' | 'weeklyDigest' | 'mentions';
type NotificationState = Record<NotificationId, boolean>;

interface NotificationOption {
  id: NotificationId;
  label: string;
  description: string;
  defaultOn: boolean;
}

const NOTIFICATION_OPTIONS: readonly NotificationOption[] = [
  {
    id: 'productUpdates',
    label: 'Product updates',
    description: 'News about features and improvements.',
    defaultOn: true,
  },
  {
    id: 'weeklyDigest',
    label: 'Weekly digest',
    description: 'A summary of your activity every Monday.',
    defaultOn: false,
  },
  {
    id: 'mentions',
    label: 'Mentions',
    description: 'Get notified when someone mentions you.',
    defaultOn: true,
  },
];

const NOTIFICATIONS_KEY = 'darcom.notification-preferences';
const PHONE_PATTERN = /^\+[1-9]\d{6,14}$/;

/** Required, but whitespace-only names are rejected (the backend trims too). */
function nonBlank(control: AbstractControl): ValidationErrors | null {
  const value: unknown = control.value;
  return typeof value === 'string' && value.trim().length > 0 ? null : { required: true };
}

function defaultNotificationState(): NotificationState {
  const state: NotificationState = {
    productUpdates: true,
    weeklyDigest: false,
    mentions: true,
  };
  for (const option of NOTIFICATION_OPTIONS) {
    state[option.id] = option.defaultOn;
  }
  return state;
}

/** Device-local notification preferences; the API has no notification endpoints yet. */
function readNotificationState(): NotificationState {
  const state = defaultNotificationState();
  if (typeof localStorage === 'undefined') {
    return state;
  }
  try {
    const raw = localStorage.getItem(NOTIFICATIONS_KEY);
    if (!raw) {
      return state;
    }
    const parsed: unknown = JSON.parse(raw);
    if (typeof parsed !== 'object' || parsed === null) {
      return state;
    }
    const record = parsed as Record<string, unknown>;
    for (const option of NOTIFICATION_OPTIONS) {
      const value = record[option.id];
      if (typeof value === 'boolean') {
        state[option.id] = value;
      }
    }
  } catch {
    // Corrupted or unavailable storage: fall back to defaults.
  }
  return state;
}

/**
 * Profile settings, adapted from `profile-cards.txt` (the shadcn settings card).
 * Full name and phone use the real `GET/PUT /users/me` API; notification
 * preferences are device-local because the backend has no notification model.
 */
@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [ReactiveFormsModule, MegaNavComponent],
  template: `
    <app-mega-nav />

    <main class="min-h-[calc(100vh-84px)] w-full bg-zinc-50/60 px-4 py-10 sm:py-14">
      <div class="mx-auto flex w-full max-w-2xl flex-col gap-6">
        <header>
          <h1 class="text-2xl font-bold tracking-tight text-zinc-900 sm:text-3xl">
            Profile settings
          </h1>
          <p class="mt-1 text-sm text-zinc-500">
            Manage your account details and notification preferences.
          </p>
        </header>

        @if (loading()) {
          <section class="rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm">
            <p class="text-sm text-zinc-500">Loading your profile…</p>
          </section>
        } @else if (loadError()) {
          <section class="rounded-2xl border border-red-200 bg-red-50 p-6">
            <p class="text-sm font-medium text-red-800" role="alert">{{ loadError() }}</p>
            <button
              type="button"
              class="mt-4 inline-flex h-9 items-center justify-center rounded-lg border border-red-200 bg-white px-4 text-sm font-semibold text-red-700 transition-colors hover:bg-red-100"
              (click)="reload()"
            >
              Try again
            </button>
          </section>
        } @else if (user(); as u) {
          <form class="flex flex-col gap-6" [formGroup]="form" (ngSubmit)="onSave()" novalidate>
            <section class="overflow-hidden rounded-2xl border border-zinc-200 bg-white shadow-sm">
              <div class="border-b border-zinc-100 px-6 py-5">
                <h2 class="text-base font-semibold text-zinc-900">Profile</h2>
                <p class="mt-0.5 text-sm text-zinc-500">
                  This information is shown on your account.
                </p>
              </div>

              <div class="flex flex-col gap-5 px-6 py-6">
                <div class="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between sm:gap-8">
                  <div class="sm:pt-1">
                    <label class="auth-label" for="profile-full-name">Full name</label>
                    <p class="text-sm text-zinc-500">The name shown across your workspace.</p>
                  </div>
                  <div class="w-full sm:w-56">
                    <input
                      id="profile-full-name"
                      class="auth-input"
                      type="text"
                      formControlName="fullName"
                      autocomplete="name"
                      maxlength="255"
                    />
                    @if (form.controls.fullName.touched && form.controls.fullName.invalid) {
                      <p class="auth-field-error">
                        @if (form.controls.fullName.hasError('required')) {
                          Full name is required.
                        } @else {
                          Full name must be 255 characters or fewer.
                        }
                      </p>
                    }
                  </div>
                </div>

                <hr class="border-zinc-100" />

                <div class="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between sm:gap-8">
                  <div class="sm:pt-1">
                    <label class="auth-label" for="profile-email">Email</label>
                    <p class="text-sm text-zinc-500">Used to sign in and receive account notices.</p>
                  </div>
                  <div class="w-full sm:w-56">
                    <input
                      id="profile-email"
                      class="auth-input"
                      type="email"
                      [value]="u.email"
                      readonly
                    />
                    <p class="mt-1 text-xs text-zinc-400">
                      Changing your sign-in email is not available yet.
                    </p>
                  </div>
                </div>

                <hr class="border-zinc-100" />

                <div class="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between sm:gap-8">
                  <div class="sm:pt-1">
                    <label class="auth-label" for="profile-phone">Phone</label>
                    <p class="text-sm text-zinc-500">Optional contact number for your account.</p>
                  </div>
                  <div class="w-full sm:w-56">
                    <input
                      id="profile-phone"
                      class="auth-input"
                      type="tel"
                      formControlName="phone"
                      autocomplete="tel"
                      placeholder="+212612345678"
                    />
                    @if (form.controls.phone.touched && form.controls.phone.invalid) {
                      <p class="auth-field-error">
                        Use international format, e.g. +212612345678.
                      </p>
                    }
                    <p class="mt-1 text-xs text-zinc-400">
                      Include the country code. Leave empty to remove it.
                    </p>
                  </div>
                </div>

                <hr class="border-zinc-100" />

                <div class="grid gap-4 rounded-xl bg-zinc-50 px-4 py-4 sm:grid-cols-3">
                  <div>
                    <p class="text-xs font-medium uppercase tracking-wide text-zinc-400">Role</p>
                    <p class="mt-1 text-sm font-semibold text-zinc-800">
                      {{ roleLabel(u.role) }}
                    </p>
                  </div>
                  <div>
                    <p class="text-xs font-medium uppercase tracking-wide text-zinc-400">Status</p>
                    <p class="mt-1">
                      @if (u.verified) {
                        <span
                          class="inline-flex rounded-full bg-green-50 px-2.5 py-0.5 text-xs font-semibold text-green-700"
                        >
                          Verified
                        </span>
                      } @else {
                        <span
                          class="inline-flex rounded-full bg-amber-50 px-2.5 py-0.5 text-xs font-semibold text-amber-700"
                        >
                          Not verified
                        </span>
                      }
                    </p>
                  </div>
                  <div>
                    <p class="text-xs font-medium uppercase tracking-wide text-zinc-400">
                      Member since
                    </p>
                    <p class="mt-1 text-sm font-semibold text-zinc-800">
                      {{ formatDate(u.createdAt) }}
                    </p>
                  </div>
                </div>
              </div>
            </section>

            <section class="overflow-hidden rounded-2xl border border-zinc-200 bg-white shadow-sm">
              <div class="border-b border-zinc-100 px-6 py-5">
                <h2 class="text-base font-semibold text-zinc-900">Notifications</h2>
                <p class="mt-0.5 text-sm text-zinc-500">Choose what you want to hear about.</p>
              </div>

              <div class="flex flex-col gap-4 px-6 py-6">
                @for (option of notificationOptions; track option.id; let first = $first) {
                  @if (!first) {
                    <hr class="border-zinc-100" />
                  }
                  <div class="flex items-start justify-between gap-6">
                    <div class="min-w-0">
                      <p
                        [id]="'notif-' + option.id + '-label'"
                        class="text-sm font-medium text-zinc-900"
                      >
                        {{ option.label }}
                      </p>
                      <p class="mt-0.5 text-sm text-zinc-500">{{ option.description }}</p>
                    </div>
                    <button
                      type="button"
                      role="switch"
                      [attr.aria-checked]="notifications()[option.id]"
                      [attr.aria-labelledby]="'notif-' + option.id + '-label'"
                      (click)="toggleNotification(option.id)"
                      class="relative inline-flex h-6 w-11 shrink-0 items-center rounded-full border-2 border-transparent transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 focus-visible:ring-offset-2"
                      [class.bg-indigo-600]="notifications()[option.id]"
                      [class.bg-zinc-200]="!notifications()[option.id]"
                    >
                      <span
                        class="inline-block h-5 w-5 rounded-full bg-white shadow transition-transform"
                        [class.translate-x-5]="notifications()[option.id]"
                        [class.translate-x-0]="!notifications()[option.id]"
                      ></span>
                    </button>
                  </div>
                }
                <p class="text-xs text-zinc-400">
                  Notification preferences are saved on this device.
                </p>
              </div>
            </section>

            @if (saved()) {
              <p class="auth-alert-ok" role="status">Your changes have been saved.</p>
            }
            @if (saveError()) {
              <p class="auth-alert-error" role="alert">{{ saveError() }}</p>
            }

            <div
              class="flex flex-col-reverse items-stretch gap-3 sm:flex-row sm:items-center sm:justify-end"
            >
              <button
                type="button"
                class="inline-flex h-10 items-center justify-center rounded-lg border border-zinc-200 bg-white px-5 text-sm font-semibold text-zinc-700 transition-colors hover:bg-zinc-50 disabled:cursor-not-allowed disabled:opacity-50"
                [disabled]="!canReset()"
                (click)="onReset()"
              >
                Reset
              </button>
              <button
                type="submit"
                class="inline-flex h-10 items-center justify-center rounded-lg bg-indigo-600 px-5 text-sm font-semibold text-white transition-colors hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-50"
                [disabled]="!canSave()"
              >
                {{ saving() ? 'Saving…' : 'Save changes' }}
              </button>
            </div>
          </form>
        }
      </div>
    </main>
  `,
})
export class ProfileComponent implements OnInit {
  private readonly auth = inject(AuthService);

  readonly user = this.auth.profile;
  readonly notificationOptions = NOTIFICATION_OPTIONS;

  readonly form = new FormGroup({
    fullName: new FormControl('', {
      nonNullable: true,
      validators: [nonBlank, Validators.maxLength(255)],
    }),
    phone: new FormControl('', {
      nonNullable: true,
      validators: [Validators.pattern(PHONE_PATTERN)],
    }),
  });

  readonly loading = signal(this.auth.profile() === null);
  readonly loadError = signal<string | null>(null);
  readonly saving = signal(false);
  readonly saved = signal(false);
  readonly saveError = signal<string | null>(null);
  readonly dirty = signal(false);
  readonly valid = signal(this.form.valid);
  readonly notifications = signal<NotificationState>(readNotificationState());
  readonly notificationsDirty = signal(false);

  readonly canSave = computed(
    () => this.valid() && (this.dirty() || this.notificationsDirty()) && !this.saving(),
  );
  readonly canReset = computed(
    () => (this.dirty() || this.notificationsDirty()) && !this.saving(),
  );

  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.dirty.set(this.form.dirty);
      this.valid.set(this.form.valid);
      this.saved.set(false);
      this.saveError.set(null);
    });

    this.form.statusChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.valid.set(this.form.valid);
    });
  }

  ngOnInit(): void {
    const cached = this.auth.profile();
    if (cached) {
      this.applyUser(cached);
      return;
    }
    this.loadProfile();
  }

  reload(): void {
    this.loadProfile(true);
  }

  onSave(): void {
    if (!this.canSave()) {
      this.form.markAllAsTouched();
      return;
    }

    const fullName = this.form.controls.fullName.value.trim();
    const phoneValue = this.form.controls.phone.value.trim();
    const phone = phoneValue.length > 0 ? phoneValue : null;

    this.saving.set(true);
    this.saved.set(false);
    this.saveError.set(null);

    this.auth.updateProfile({ fullName, phone }).subscribe({
      next: (user) => {
        this.applyUser(user);
        this.saveNotifications();
        this.notificationsDirty.set(false);
        this.saving.set(false);
        this.saved.set(true);
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.saveError.set(toUserMessage(err));
      },
    });
  }

  onReset(): void {
    const user = this.auth.profile();
    if (user) {
      this.applyUser(user);
    }
    this.notifications.set(readNotificationState());
    this.notificationsDirty.set(false);
    this.saved.set(false);
    this.saveError.set(null);
  }

  toggleNotification(id: NotificationId): void {
    this.notifications.update((state) => ({ ...state, [id]: !state[id] }));
    this.notificationsDirty.set(true);
    this.saved.set(false);
  }

  roleLabel(role: Role): string {
    return role.charAt(0) + role.slice(1).toLowerCase();
  }

  formatDate(value: string): string {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return '—';
    }
    return new Intl.DateTimeFormat(undefined, {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    }).format(date);
  }

  private loadProfile(force = false): void {
    this.loading.set(true);
    this.loadError.set(null);

    this.auth.loadProfile(force).subscribe({
      next: (user) => {
        this.applyUser(user);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.loadError.set(toUserMessage(err));
        this.loading.set(false);
      },
    });
  }

  private applyUser(user: User): void {
    this.form.reset(
      { fullName: user.fullName ?? '', phone: user.phone ?? '' },
      { emitEvent: false },
    );
    this.dirty.set(false);
    this.valid.set(this.form.valid);
  }

  private saveNotifications(): void {
    if (typeof localStorage === 'undefined') {
      return;
    }
    try {
      localStorage.setItem(NOTIFICATIONS_KEY, JSON.stringify(this.notifications()));
    } catch {
      // Storage can be unavailable in private mode; the toggles still work for this session.
    }
  }
}
