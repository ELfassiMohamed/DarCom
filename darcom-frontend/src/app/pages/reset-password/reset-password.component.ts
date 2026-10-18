import { Component, OnInit, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthCardComponent } from '../../shared/auth-card/auth-card.component';
import { PasswordToggleComponent } from '../../shared/password-toggle/password-toggle.component';
import { AuthService } from '../../core/auth.service';
import { toUserMessage } from '../../core/error-messages';
import { passwordPolicyValidator } from '../register/register.component';

const matchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const group = control as FormGroup<{
    newPassword: FormControl<string>;
    confirm: FormControl<string>;
  }>;
  const pw: unknown = group.controls.newPassword.value;
  const confirm: unknown = group.controls.confirm.value;
  if (
    typeof pw === 'string' &&
    typeof confirm === 'string' &&
    confirm.length > 0 &&
    pw !== confirm
  ) {
    return { mismatch: true };
  }
  return null;
};

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, AuthCardComponent, PasswordToggleComponent],
  template: `
    <app-auth-card tagline="Pick a strong new password to keep your stays and bookings secure.">
      <h1 class="text-2xl md:text-3xl font-bold mb-1 text-gray-800">Set a new password</h1>
      <p class="text-gray-500 mb-8">Choose a strong password for your account</p>

      @if (missing()) {
        <p class="auth-alert-error">This reset link is invalid or has expired.</p>
        <p class="text-center mt-6">
          <a class="auth-link" routerLink="/forgot-password">Request a new link</a>
        </p>
      } @else if (done()) {
        <p class="auth-alert-ok">Your password has been reset.</p>
        <p class="text-center mt-6">
          <a class="auth-link" routerLink="/login">Go to login</a>
        </p>
      } @else {
        <form class="space-y-5" [formGroup]="form" (ngSubmit)="onSubmit()" novalidate>
          <div>
            <label class="auth-label" for="reset-new-password">
              New password <span class="auth-required">*</span>
            </label>
            <div class="relative">
              <input
                id="reset-new-password"
                class="auth-input pr-10"
                [type]="showPassword() ? 'text' : 'password'"
                placeholder="Enter your new password"
                formControlName="newPassword"
                autocomplete="new-password"
              />
              <app-password-toggle
                [visible]="showPassword()"
                (toggle)="showPassword.set(!showPassword())"
              />
            </div>
            @if (form.controls.newPassword.touched && form.controls.newPassword.invalid) {
              <p class="auth-field-error">
                8-72 characters, with at least one lowercase, one uppercase, one digit and one
                special character.
              </p>
            }
          </div>

          <div>
            <label class="auth-label" for="reset-confirm-password">
              Confirm password <span class="auth-required">*</span>
            </label>
            <div class="relative">
              <input
                id="reset-confirm-password"
                class="auth-input pr-10"
                [type]="showConfirm() ? 'text' : 'password'"
                placeholder="Repeat your new password"
                formControlName="confirm"
                autocomplete="new-password"
              />
              <app-password-toggle
                [visible]="showConfirm()"
                (toggle)="showConfirm.set(!showConfirm())"
              />
            </div>
            @if (form.touched && form.hasError('mismatch')) {
              <p class="auth-field-error">Passwords do not match.</p>
            }
          </div>

          @if (errorMsg()) {
            <p class="auth-alert-error" role="alert">{{ errorMsg() }}</p>
          }

          <button type="submit" class="auth-btn" [disabled]="form.invalid || saving()">
            {{ saving() ? 'Saving...' : 'Set password' }}
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
export class ResetPasswordComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);

  readonly missing = signal(false);
  readonly done = signal(false);
  readonly saving = signal(false);
  readonly errorMsg = signal<string | null>(null);
  readonly showPassword = signal(false);
  readonly showConfirm = signal(false);

  private token: string | null = null;

  readonly form = new FormGroup(
    {
      newPassword: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required, passwordPolicyValidator],
      }),
      confirm: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    },
    { validators: [matchValidator] },
  );

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.missing.set(true);
      return;
    }
    this.token = token;
  }

  onSubmit(): void {
    if (this.missing() || this.form.invalid || this.saving() || !this.token) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.errorMsg.set(null);
    const { newPassword } = this.form.getRawValue();
    this.auth.resetPassword(this.token, newPassword).subscribe({
      next: () => {
        this.saving.set(false);
        this.done.set(true);
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.errorMsg.set(toUserMessage(err));
      },
    });
  }
}
