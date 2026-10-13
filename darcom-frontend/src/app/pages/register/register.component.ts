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
import { AuthService } from '../../core/auth.service';
import { toUserMessage } from '../../core/error-messages';
import { RegisterRole } from '../../core/models';

export const passwordPolicyValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
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
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth">
      <div class="container sign-up-mode tall">
        <div class="forms-container">
          <div class="signin-signup">
            <form class="sign-up-form" [formGroup]="form" (ngSubmit)="onSubmit()" novalidate>
              <h2 class="title">Sign up</h2>
              @if (created()) {
                <p class="form-ok">Account created. Check your email to verify it.</p>
                <p class="alt"><a routerLink="/login">Go to login</a></p>
              } @else {
                <div class="input-field">
                  <span class="ico">&#128100;</span>
                  <input type="text" placeholder="Full name" formControlName="fullName" autocomplete="name" />
                </div>
                @if (form.controls.fullName.touched && form.controls.fullName.invalid) {
                  <p class="field-error">Full name is required.</p>
                }
                <div class="input-field">
                  <span class="ico">&#64;</span>
                  <input type="email" placeholder="Email" formControlName="email" autocomplete="email" />
                </div>
                @if (form.controls.email.touched && form.controls.email.invalid) {
                  <p class="field-error">Enter a valid email address.</p>
                }
                <div class="input-field">
                  <span class="ico">&#128274;</span>
                  <input
                    type="password"
                    placeholder="Password"
                    formControlName="password"
                    autocomplete="new-password"
                  />
                </div>
                @if (form.controls.password.touched && form.controls.password.invalid) {
                  <p class="field-error">
                    8-72 characters, with at least one lowercase, one uppercase, one digit and one
                    special character.
                  </p>
                }
                <div class="input-field">
                  <span class="ico">&#8746;</span>
                  <select formControlName="role" aria-label="Role">
                    <option value="VISITOR">VISITOR</option>
                    <option value="HOST">HOST</option>
                  </select>
                </div>
                @if (errorMsg()) {
                  <p class="form-error" role="alert">{{ errorMsg() }}</p>
                }
                <input
                  type="submit"
                  value="Sign up"
                  class="btn"
                  [disabled]="form.invalid || loading()"
                />
                <p class="alt">Already have an account? <a routerLink="/login">Sign in</a></p>
              }
            </form>
          </div>
        </div>

        <div class="panels-container">
          <div class="panel left-panel">
            <div class="content">
              <h3>New here?</h3>
              <p>Join us today and discover a world of possibilities. Create your account in seconds!</p>
              <button type="button" class="btn transparent" routerLink="/register">Sign up</button>
            </div>
          </div>
          <div class="panel right-panel">
            <div class="content">
              <h3>One of us?</h3>
              <p>Welcome back! Sign in to continue your journey with us.</p>
              <button type="button" class="btn transparent" routerLink="/login">Sign in</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['../auth/auth-shell.css'],
})
export class RegisterComponent {
  private readonly auth = inject(AuthService);

  readonly loading = signal(false);
  readonly errorMsg = signal<string | null>(null);
  readonly created = signal(false);

  readonly form = new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    fullName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, passwordPolicyValidator],
    }),
    role: new FormControl<RegisterRole>('VISITOR', { nonNullable: true, validators: [Validators.required] }),
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
      .register({ email: raw.email, password: raw.password, fullName: raw.fullName, role: raw.role })
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
