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
import { AuthService } from '../../core/auth.service';
import { toUserMessage } from '../../core/error-messages';
import { passwordPolicyValidator } from '../register/register.component';

const matchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const group = control as FormGroup<{ newPassword: FormControl<string>; confirm: FormControl<string> }>;
  const pw: unknown = group.controls.newPassword.value;
  const confirm: unknown = group.controls.confirm.value;
  if (typeof pw === 'string' && typeof confirm === 'string' && confirm.length > 0 && pw !== confirm) {
    return { mismatch: true };
  }
  return null;
};

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth">
      <div class="container single">
        <h2 class="title">Set new password</h2>
        @if (missing()) {
          <p class="form-error">This reset link is invalid or has expired.</p>
          <p class="alt"><a routerLink="/forgot-password">Request a new link</a></p>
        } @else if (done()) {
          <p class="form-ok">Your password has been reset.</p>
          <p class="alt"><a routerLink="/login">Go to login</a></p>
        } @else {
          <form [formGroup]="form" (ngSubmit)="onSubmit()" novalidate>
            <div class="input-field">
              <span class="ico">&#128274;</span>
              <input
                type="password"
                placeholder="New password"
                formControlName="newPassword"
                autocomplete="new-password"
              />
            </div>
            @if (form.controls.newPassword.touched && form.controls.newPassword.invalid) {
              <p class="field-error">
                8-72 characters, with at least one lowercase, one uppercase, one digit and one
                special character.
              </p>
            }
            <div class="input-field">
              <span class="ico">&#128274;</span>
              <input
                type="password"
                placeholder="Confirm password"
                formControlName="confirm"
                autocomplete="new-password"
              />
            </div>
            @if (form.touched && form.hasError('mismatch')) {
              <p class="field-error">Passwords do not match.</p>
            }
            @if (errorMsg()) {
              <p class="form-error" role="alert">{{ errorMsg() }}</p>
            }
            <input
              type="submit"
              value="Set password"
              class="btn"
              [disabled]="form.invalid || saving()"
            />
          </form>
          <p class="alt"><a routerLink="/login">Back to login</a></p>
        }
      </div>
    </div>
  `,
  styleUrls: ['../auth/auth-shell.css'],
})
export class ResetPasswordComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);

  readonly missing = signal(false);
  readonly done = signal(false);
  readonly saving = signal(false);
  readonly errorMsg = signal<string | null>(null);

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
