import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthCardComponent } from '../../shared/auth-card/auth-card.component';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, AuthCardComponent],
  template: `
    <app-auth-card tagline="We'll send a reset link to your email so you can get back in.">
      <h1 class="text-2xl md:text-3xl font-bold mb-1 text-gray-800">Forgot password?</h1>
      <p class="text-gray-500 mb-8">Enter your account email</p>

      @if (sent()) {
        <p class="auth-alert-ok">If the account exists, a reset link was sent to your email.</p>
        <p class="text-center mt-6">
          <a class="auth-link" routerLink="/login">Back to login</a>
        </p>
      } @else {
        <form class="space-y-5" [formGroup]="form" (ngSubmit)="onSubmit()" novalidate>
          <div>
            <label class="auth-label" for="forgot-email">
              Email <span class="auth-required">*</span>
            </label>
            <input
              id="forgot-email"
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

          <button type="submit" class="auth-btn" [disabled]="form.invalid || sending()">
            {{ sending() ? 'Sending...' : 'Send reset link' }}
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
            <a class="auth-link" routerLink="/login">Back to login</a>
          </div>
        </form>
      }
    </app-auth-card>
  `,
})
export class ForgotPasswordComponent {
  private readonly auth = inject(AuthService);

  readonly sending = signal(false);
  readonly sent = signal(false);

  readonly form = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email],
    }),
  });

  onSubmit(): void {
    if (this.form.invalid || this.sending()) {
      this.form.markAllAsTouched();
      return;
    }
    this.sending.set(true);
    const { email } = this.form.getRawValue();
    this.auth.forgotPassword(email).subscribe({
      next: () => {
        this.sending.set(false);
        this.sent.set(true);
      },
      error: () => {
        // Backend always returns 204: show the same message on error too.
        this.sending.set(false);
        this.sent.set(true);
      },
    });
  }
}
