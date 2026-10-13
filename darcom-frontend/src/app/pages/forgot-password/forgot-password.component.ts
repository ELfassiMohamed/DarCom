import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth">
      <div class="container single">
        <h2 class="title">Reset password</h2>
        @if (sent()) {
          <p class="form-ok">If the account exists, a reset link was sent to your email.</p>
          <p class="alt"><a routerLink="/login">Back to login</a></p>
        } @else {
          <p>Enter your account email. We'll send you a reset link.</p>
          <form [formGroup]="form" (ngSubmit)="onSubmit()" novalidate>
            <div class="input-field">
              <span class="ico">&#64;</span>
              <input type="email" placeholder="Email" formControlName="email" autocomplete="email" />
            </div>
            @if (form.controls.email.touched && form.controls.email.invalid) {
              <p class="field-error">Enter a valid email address.</p>
            }
            <input
              type="submit"
              value="Send reset link"
              class="btn"
              [disabled]="form.invalid || sending()"
            />
          </form>
          <p class="alt"><a routerLink="/login">Back to login</a></p>
        }
      </div>
    </div>
  `,
  styleUrls: ['../auth/auth-shell.css'],
})
export class ForgotPasswordComponent {
  private readonly auth = inject(AuthService);

  readonly sending = signal(false);
  readonly sent = signal(false);

  readonly form = new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
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
