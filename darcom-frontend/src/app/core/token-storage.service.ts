import { Injectable } from '@angular/core';
import { LoginResponse, SessionUser } from './models';

const ACCESS_KEY = 'darcom.accessToken';
const REFRESH_KEY = 'darcom.refreshToken';
const USER_KEY = 'darcom.user';

function hasStorage(): boolean {
  return typeof window !== 'undefined' && typeof localStorage !== 'undefined';
}

@Injectable({
  providedIn: 'root',
})
export class TokenStorageService {
  getAccessToken(): string | null {
    if (!hasStorage()) {
      return null;
    }
    return localStorage.getItem(ACCESS_KEY);
  }

  getRefreshToken(): string | null {
    if (!hasStorage()) {
      return null;
    }
    return localStorage.getItem(REFRESH_KEY);
  }

  getUser(): SessionUser | null {
    if (!hasStorage()) {
      return null;
    }
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) {
      return null;
    }
    try {
      const parsed: unknown = JSON.parse(raw);
      if (
        typeof parsed === 'object' &&
        parsed !== null &&
        'id' in parsed &&
        'email' in parsed &&
        'role' in parsed &&
        'verified' in parsed
      ) {
        return parsed as SessionUser;
      }
      return null;
    } catch {
      return null;
    }
  }

  setSession(login: LoginResponse): void {
    if (!hasStorage()) {
      return;
    }
    localStorage.setItem(ACCESS_KEY, login.accessToken);
    localStorage.setItem(REFRESH_KEY, login.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(login.user));
  }

  setAccessToken(token: string): void {
    if (!hasStorage()) {
      return;
    }
    localStorage.setItem(ACCESS_KEY, token);
  }

  clear(): void {
    if (!hasStorage()) {
      return;
    }
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
  }
}
