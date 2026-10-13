import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { toUserMessage } from '../../core/error-messages';
import { User } from '../../core/models';

@Component({
  selector: 'app-home',
  standalone: true,
  template: `
    <div class="auth">
      <div class="container single">
        <h2 class="title">Welcome</h2>
        @if (loading()) {
          <p>Loading...</p>
        } @else if (errorMsg()) {
          <p class="form-error" role="alert">{{ errorMsg() }}</p>
        } @else if (user()) {
          <dl class="profile">
            <div class="profile-row">
              <dt>Email</dt>
              <dd>{{ user()?.email }}</dd>
            </div>
            <div class="profile-row">
              <dt>Full name</dt>
              <dd>{{ user()?.fullName }}</dd>
            </div>
            <div class="profile-row">
              <dt>Role</dt>
              <dd><span class="pill">{{ user()?.role }}</span></dd>
            </div>
            <div class="profile-row">
              <dt>Verified</dt>
              <dd>
                @if (user()?.verified) {
                  <span class="pill ok">verified</span>
                } @else {
                  <span class="pill warn">not verified</span>
                }
              </dd>
            </div>
          </dl>
        }
        <button type="button" class="btn" (click)="onLogout()" [disabled]="loggingOut()">Logout</button>
      </div>
    </div>
  `,
  styleUrls: ['../auth/auth-shell.css'],
})
export class HomeComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly user = signal<User | null>(null);
  readonly loading = signal(true);
  readonly errorMsg = signal<string | null>(null);
  readonly loggingOut = signal(false);

  ngOnInit(): void {
    this.auth.me().subscribe({
      next: (u) => {
        this.user.set(u);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.errorMsg.set(toUserMessage(err));
      },
    });
  }

  onLogout(): void {
    if (this.loggingOut()) {
      return;
    }
    this.loggingOut.set(true);
    this.auth.logout().subscribe({
      next: () => {
        this.loggingOut.set(false);
        void this.router.navigate(['/login']);
      },
      error: () => {
        this.loggingOut.set(false);
        void this.router.navigate(['/login']);
      },
    });
  }
}
