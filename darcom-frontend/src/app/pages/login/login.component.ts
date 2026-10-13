import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { toUserMessage } from '../../core/error-messages';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth">
      <div class="container">
        <div class="forms-container">
          <div class="signin-signup">
            <form class="sign-in-form" [formGroup]="form" (ngSubmit)="onSubmit()" novalidate>
              <h2 class="title">Sign in</h2>
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
                  autocomplete="current-password"
                />
              </div>
              @if (form.controls.password.touched && form.controls.password.invalid) {
                <p class="field-error">Password is required.</p>
              }
              @if (errorMsg()) {
                <p class="form-error" role="alert">{{ errorMsg() }}</p>
              }
              <input
                type="submit"
                value="Login"
                class="btn solid"
                [disabled]="form.invalid || loading()"
              />
              <p class="alt"><a routerLink="/forgot-password">Forgot password?</a></p>
              <p class="alt">No account? <a routerLink="/register">Sign up</a></p>
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
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly errorMsg = signal<string | null>(null);

  readonly form = new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
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
