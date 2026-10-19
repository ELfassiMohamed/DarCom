import { Component, OnInit, inject, signal } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { toUserMessage } from '../../core/error-messages';

type VerifyState = 'loading' | 'success' | 'error' | 'missing';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, NgTemplateOutlet],
  template: `
    <div class="auth">
      <div class="container single">
        <h2 class="title">Verify email</h2>
        @switch (state()) {
          @case ('loading') {
            <p>Verifying your email...</p>
          }
          @case ('success') {
            <p class="form-ok">Your email is verified.</p>
            <p class="alt"><a routerLink="/login">Go to login</a></p>
          }
          @case ('missing') {
            <p class="form-error">This verification link is invalid or has expired.</p>
            <ng-container *ngTemplateOutlet="resendTpl"></ng-container>
          }
          @case ('error') {
            <p class="form-error" role="alert">{{ errorMsg() }}</p>
            <ng-container *ngTemplateOutlet="resendTpl"></ng-container>
          }
        }
        <ng-template #resendTpl>
          <form [formGroup]="resendForm" (ngSubmit)="onResend()" novalidate>
            <div class="input-field">
              <span class="ico">&#64;</span>
              <input type="email" placeholder="Email" formControlName="email" autocomplete="email" />
            </div>
            <input
              type="submit"
              value="Request a new link"
              class="btn"
              [disabled]="resendForm.invalid || resending()"
            />
          </form>
          @if (resent()) {
            <p class="form-ok">If the account exists, a new link was sent.</p>
          }
          <p class="alt"><a routerLink="/login">Back to login</a></p>
        </ng-template>
      </div>
    </div>
  `,
  styleUrls: ['../auth/auth-shell.css'],
})
export class VerifyEmailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);

  readonly state = signal<VerifyState>('loading');
  readonly errorMsg = signal<string | null>(null);
  readonly resending = signal(false);
  readonly resent = signal(false);

  readonly resendForm = new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
  });

  private called = false;

  ngOnInit(): void {
    if (this.called) {
      return;
    }
    this.called = true;

    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.state.set('missing');
      return;
    }
    this.state.set('loading');
    this.auth.verifyEmail(token).subscribe({
      next: () => this.state.set('success'),
      error: (err: unknown) => {
        this.errorMsg.set(toUserMessage(err));
        this.state.set('error');
      },
    });
  }

  onResend(): void {
    if (this.resendForm.invalid || this.resending()) {
      this.resendForm.markAllAsTouched();
      return;
    }
    this.resending.set(true);
    this.resent.set(false);
    const { email } = this.resendForm.getRawValue();
    this.auth.resendVerification(email).subscribe({
      next: () => {
        this.resending.set(false);
        this.resent.set(true);
      },
      error: () => {
        // Always show the same message (backend returns 204 always).
        this.resending.set(false);
        this.resent.set(true);
      },
    });
  }
}
