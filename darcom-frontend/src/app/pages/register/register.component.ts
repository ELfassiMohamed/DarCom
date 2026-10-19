import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthCardComponent } from '../../shared/auth-card/auth-card.component';
import { PasswordToggleComponent } from '../../shared/password-toggle/password-toggle.component';
import { AuthService } from '../../core/auth.service';
import { toUserMessage } from '../../core/error-messages';
import { RegisterRole } from '../../core/models';

export const passwordPolicyValidator: ValidatorFn = (
  control: AbstractControl,
): ValidationErrors | null => {
  const value: unknown = control.value;
  if (typeof value !== 'string' || value.length === 0) {
    return { passwordPolicy: true };
  }
  if (value.length < 8 || value.length > 72) {
    return { passwordPolicy: true };
  }
  const hasLower = /[a-z]/.test(value);
  const hasUpper = /[A-Z]/.test(value);
  const hasDigit = /[0-9]/.test(value);
  const hasSpecial = /[^A-Za-z0-9]/.test(value);
  if (!hasLower || !hasUpper || !hasDigit || !hasSpecial) {
    return { passwordPolicy: true };
  }
  return null;
};

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, AuthCardComponent, PasswordToggleComponent],
  template: `
    <app-auth-card
      tagline="Create an account to book your next stay or start hosting and welcome visitors."
    >
      @if (created()) {
        <h1 class="text-2xl md:text-3xl font-bold mb-1 text-gray-800">Check your email</h1>
        <p class="text-gray-500 mb-8">Your account has been created.</p>

        <p class="auth-alert-ok">Account created. Check your email to verify it.</p>
        <p class="text-center mt-6">
          <a class="auth-link" routerLink="/login">Go to login</a>
        </p>
      } @else {
        <h1 class="text-2xl md:text-3xl font-bold mb-1 text-gray-800">Create your account</h1>
        <p class="text-gray-500 mb-8">Sign up to start booking or hosting</p>

        <form class="space-y-5" [formGroup]="form" (ngSubmit)="onSubmit()" novalidate>
          <div>
            <label class="auth-label" for="register-fullname">
              Full name <span class="auth-required">*</span>
            </label>
            <input
              id="register-fullname"
              class="auth-input"
              type="text"
              placeholder="Enter your full name"
              formControlName="fullName"
              autocomplete="name"
            />
            @if (form.controls.fullName.touched && form.controls.fullName.invalid) {
              <p class="auth-field-error">Full name is required.</p>
            }
          </div>

          <div>
            <label class="auth-label" for="register-email">
              Email <span class="auth-required">*</span>
            </label>
            <input
              id="register-email"
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
            <label class="auth-label" for="register-password">
              Password <span class="auth-required">*</span>
            </label>
            <div class="relative">
              <input
                id="register-password"
                class="auth-input pr-10"
                [type]="showPassword() ? 'text' : 'password'"
                placeholder="Create a password"
                formControlName="password"
                autocomplete="new-password"
              />
              <app-password-toggle
                [visible]="showPassword()"
                (toggle)="showPassword.set(!showPassword())"
              />
            </div>
            @if (form.controls.password.touched && form.controls.password.invalid) {
              <p class="auth-field-error">
                8-72 characters, with at least one lowercase, one uppercase, one digit and one
                special character.
              </p>
            }
          </div>

          <div>
            <label class="auth-label" for="register-role">
              Account type <span class="auth-required">*</span>
            </label>
            <select
              id="register-role"
              class="auth-input block cursor-pointer"
              formControlName="role"
            >
              <option value="VISITOR">Visitor - looking for a stay</option>
              <option value="HOST">Host - sharing my space</option>
            </select>
          </div>

          @if (errorMsg()) {
            <p class="auth-alert-error" role="alert">{{ errorMsg() }}</p>
          }

          <button type="submit" class="auth-btn" [disabled]="form.invalid || loading()">
            {{ loading() ? 'Creating account...' : 'Create account' }}
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

          <div class="text-center mt-6">
            <p class="text-sm text-gray-500">
              Already have an account? <a class="auth-link" routerLink="/login">Sign in</a>
            </p>
          </div>
        </form>
      }
    </app-auth-card>
  `,
})
export class RegisterComponent {
  private readonly auth = inject(AuthService);

  readonly loading = signal(false);
  readonly errorMsg = signal<string | null>(null);
  readonly created = signal(false);
  readonly showPassword = signal(false);

  readonly form = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email],
    }),
    fullName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, passwordPolicyValidator],
    }),
    role: new FormControl<RegisterRole>('VISITOR', {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });

  onSubmit(): void {
    if (this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.errorMsg.set(null);
    const raw = this.form.getRawValue();
    this.auth
      .register({
        email: raw.email,
        password: raw.password,
        fullName: raw.fullName,
        role: raw.role,
      })
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.created.set(true);
        },
        error: (err: unknown) => {
          this.loading.set(false);
          this.errorMsg.set(toUserMessage(err));
        },
      });
  }
}
