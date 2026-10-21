import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, throwError } from 'rxjs';
import { API_URL } from './api.config';
import {
  LoginResponse,
  RefreshResponse,
  RegisterRequest,
  RegisteredUser,
  SessionUser,
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

  isLoggedIn(): boolean {
    return this.tokens.getAccessToken() !== null && this.tokens.getRefreshToken() !== null;
  }

  clearSession(): void {
    this.tokens.clear();
    this.currentUser.set(null);
  }
}
