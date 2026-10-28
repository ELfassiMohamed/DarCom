import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  Observable,
  catchError,
  finalize,
  of,
  shareReplay,
  tap,
  throwError,
} from 'rxjs';
import { API_URL } from './api.config';
import {
  LoginResponse,
  RefreshResponse,
  RegisterRequest,
  RegisteredUser,
  SessionUser,
  UpdateProfileRequest,
  User,
} from './models';
import { TokenStorageService } from './token-storage.service';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokens = inject(TokenStorageService);

  readonly currentUser = signal<SessionUser | null>(this.tokens.getUser());

  /**
   * Full profile shared by the nav and the profile pages. Loaded lazily so the
   * navbar can show the user's name without each page fetching it again.
   */
  readonly profile = signal<User | null>(null);
  private profileRequest$: Observable<User> | null = null;

  register(body: RegisterRequest): Observable<RegisteredUser> {
    return this.http.post<RegisteredUser>(`${API_URL}/auth/register`, body);
  }

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${API_URL}/auth/login`, { email, password })
      .pipe(
        tap((res) => {
          this.tokens.setSession(res);
          this.currentUser.set(res.user);
          this.profile.set(null);
          this.profileRequest$ = null;
        }),
      );
  }

  refresh(): Observable<RefreshResponse> {
    const refreshToken = this.tokens.getRefreshToken();
    return this.http
      .post<RefreshResponse>(`${API_URL}/auth/refresh`, { refreshToken })
      .pipe(
        tap((res) => {
          this.tokens.setAccessToken(res.accessToken);
        }),
      );
  }

  logout(): Observable<void> {
    const refreshToken = this.tokens.getRefreshToken();
    return this.http.post<void>(`${API_URL}/auth/logout`, { refreshToken }).pipe(
      tap({
        next: () => this.clearSession(),
        error: () => this.clearSession(),
      }),
      catchError((err: unknown) => {
        this.clearSession();
        return throwError(() => err);
      }),
    );
  }

  verifyEmail(token: string): Observable<User> {
    return this.http.post<User>(`${API_URL}/auth/verify-email`, { token });
  }

  resendVerification(email: string): Observable<void> {
    return this.http.post<void>(`${API_URL}/auth/resend-verification`, { email });
  }

  forgotPassword(email: string): Observable<void> {
    return this.http.post<void>(`${API_URL}/auth/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${API_URL}/auth/reset-password`, { token, newPassword });
  }

  me(): Observable<User> {
    return this.http.get<User>(`${API_URL}/users/me`);
  }

  /**
   * Returns the cached profile when available. Concurrent callers share one
   * in-flight request, so the nav and a page can both ask without duplicating it.
   */
  loadProfile(force = false): Observable<User> {
    const cached = this.profile();
    if (!force && cached) {
      return of(cached);
    }
    if (!force && this.profileRequest$) {
      return this.profileRequest$;
    }

    const request$ = this.me().pipe(
      tap((user) => {
        // Ignore a late response if the session changed while it was in flight.
        if (this.currentUser()?.id === user.id) {
          this.profile.set(user);
        }
      }),
      finalize(() => {
        this.profileRequest$ = null;
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    this.profileRequest$ = request$;
    return request$;
  }

  updateProfile(body: UpdateProfileRequest): Observable<User> {
    return this.http.put<User>(`${API_URL}/users/me`, body).pipe(
      tap((user) => {
        if (this.currentUser()?.id === user.id) {
          this.profile.set(user);
        }
      }),
    );
  }

  isLoggedIn(): boolean {
    return this.tokens.getAccessToken() !== null && this.tokens.getRefreshToken() !== null;
  }

  clearSession(): void {
    this.tokens.clear();
    this.currentUser.set(null);
    this.profile.set(null);
    this.profileRequest$ = null;
  }
}
