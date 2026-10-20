import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { toUserMessage } from '../../core/error-messages';
import { User } from '../../core/models';
import { MegaNavComponent } from '../../shared/mega-nav/mega-nav.component';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [RouterLink, MegaNavComponent],
  template: `
    <app-mega-nav />
    <div class="auth profile-page">
      <div class="container single">
        <h2 class="title">Your profile</h2>

        @if (loading()) {
          <p>Loading...</p>
        } @else if (errorMsg()) {
          <p class="form-error" role="alert">{{ errorMsg() }}</p>
        } @else if (user(); as u) {
          <dl class="profile">
            <div class="profile-row">
              <dt>Email</dt>
              <dd>{{ u.email }}</dd>
            </div>
            <div class="profile-row">
              <dt>Full name</dt>
              <dd>{{ u.fullName }}</dd>
            </div>
            <div class="profile-row">
              <dt>Phone</dt>
              <dd>{{ u.phone || '—' }}</dd>
            </div>
            <div class="profile-row">
              <dt>Role</dt>
              <dd><span class="pill">{{ u.role }}</span></dd>
            </div>
            <div class="profile-row">
              <dt>Verified</dt>
              <dd>
                @if (u.verified) {
                  <span class="pill ok">verified</span>
                } @else {
                  <span class="pill warn">not verified</span>
                }
              </dd>
            </div>
            <div class="profile-row">
              <dt>Member since</dt>
              <dd>{{ formatDate(u.createdAt) }}</dd>
            </div>
          </dl>
        }

        <p class="alt"><a routerLink="/home">Back to home</a></p>
      </div>
    </div>
  `,
  styleUrls: ['../auth/auth-shell.css'],
  styles: `
    .profile-page {
      min-height: calc(100vh - 84px);
    }
  `,
})
export class ProfileComponent implements OnInit {
  private readonly auth = inject(AuthService);

  readonly user = signal<User | null>(null);
  readonly loading = signal(true);
  readonly errorMsg = signal<string | null>(null);

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

  formatDate(value: string): string {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }
    return date.toLocaleDateString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }
}
