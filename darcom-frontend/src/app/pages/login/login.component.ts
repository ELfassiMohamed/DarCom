import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthCardComponent } from '../../shared/auth-card/auth-card.component';
import { PasswordToggleComponent } from '../../shared/password-toggle/password-toggle.component';
import { AuthService } from '../../core/auth.service';
import { toUserMessage } from '../../core/error-messages';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, AuthCardComponent, PasswordToggleComponent],
  template: `
    <app-auth-card
      tagline="Sign in to browse stays, manage bookings, and connect with hosts worldwide."
    >
      <h1 class="text-2xl md:text-3xl font-bold mb-1 text-gray-800">Welcome back</h1>
      <p class="text-gray-500 mb-8">Sign in to your account</p>

      <form class="space-y-5" [formGroup]="form" (ngSubmit)="onSubmit()" novalidate>
        <div>
          <label class="auth-label" for="login-email">
            Email <span class="auth-required">*</span>
          </label>
          <input
            id="login-email"
            class="auth-input"
            type="email"
            placeholder="Enter your email address"
            formControlName="email"
            autocomplete="email"
          />
          @if (form.controls.email.touched && form.controls.email.invalid) {
            <p class="auth-field-error">Enter a valid email address.</p>
          }
        </div>

        <div>
          <label class="auth-label" for="login-password">
            Password <span class="auth-required">*</span>
          </label>
          <div class="relative">
            <input
              id="login-password"
              class="auth-input pr-10"
              [type]="showPassword() ? 'text' : 'password'"
              placeholder="Enter your password"
              formControlName="password"
              autocomplete="current-password"
            />
            <app-password-toggle
              [visible]="showPassword()"
              (toggle)="showPassword.set(!showPassword())"
            />
          </div>
          @if (form.controls.password.touched && form.controls.password.invalid) {
            <p class="auth-field-error">Password is required.</p>
          }
        </div>

        @if (errorMsg()) {
          <p class="auth-alert-error" role="alert">{{ errorMsg() }}</p>
        }

        <button type="submit" class="auth-btn" [disabled]="form.invalid || loading()">
          {{ loading() ? 'Signing in...' : 'Sign in' }}
          <svg
            class="h-4 w-4"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <path d="M5 12h14" />
            <path d="m12 5 7 7-7 7" />
          </svg>
        </button>

        <div class="text-center mt-6 space-y-2">
          <p><a class="auth-link" routerLink="/forgot-password">Forgot password?</a></p>
          <p class="text-sm text-gray-500">
            Don't have an account? <a class="auth-link" routerLink="/register">Sign up</a>
          </p>
        </div>
      </form>
    </app-auth-card>
  `,
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly errorMsg = signal<string | null>(null);
  readonly showPassword = signal(false);

  readonly form = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email],
    }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  onSubmit(): void {
    if (this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.errorMsg.set(null);
    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: () => {
        this.loading.set(false);
        void this.router.navigate(['/home']);
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.errorMsg.set(toUserMessage(err));
      },
    });
  }
}
