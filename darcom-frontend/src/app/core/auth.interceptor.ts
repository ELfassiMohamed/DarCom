import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandlerFn,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, shareReplay, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { TokenStorageService } from './token-storage.service';
import { RefreshResponse } from './models';

const PUBLIC_PATHS: readonly string[] = [
  '/api/auth/login',
  '/api/auth/register',
  '/api/auth/refresh',
  '/api/auth/verify-email',
  '/api/auth/resend-verification',
  '/api/auth/forgot-password',
  '/api/auth/reset-password',
];

function isApiRequest(url: string): boolean {
  return url.startsWith('/api');
}

function isPublicRequest(url: string): boolean {
  const path = url.split('?')[0].split('#')[0];
  return PUBLIC_PATHS.some((p) => path === p || path.startsWith(p + '/'));
}

function getApiErrorCode(err: unknown): string | null {
  if (err instanceof HttpErrorResponse) {
    const body: unknown = err.error;
    if (typeof body === 'object' && body !== null && 'error' in body) {
      const code: unknown = (body as Record<string, unknown>)['error'];
      return typeof code === 'string' ? code : null;
    }
  }
  return null;
}

let refreshInFlight$: Observable<RefreshResponse> | null = null;

function singleFlightRefresh(auth: AuthService): Observable<RefreshResponse> {
  if (!refreshInFlight$) {
    refreshInFlight$ = auth.refresh().pipe(
      shareReplay(1),
      finalize(() => {
        refreshInFlight$ = null;
      }),
    );
  }
  return refreshInFlight$;
}

export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
): Observable<HttpEvent<unknown>> => {
  if (!isApiRequest(req.url)) {
    return next(req);
  }

  const isPublic = isPublicRequest(req.url);
  const tokens = inject(TokenStorageService);

  let outgoing = req;
  if (!isPublic) {
    const accessToken = tokens.getAccessToken();
    if (accessToken) {
      outgoing = req.clone({
        setHeaders: { Authorization: `Bearer ${accessToken}` },
      });
    }
  }

  // Public requests: never attach token, never attempt refresh.
  if (isPublic) {
    return next(outgoing);
  }

  const auth = inject(AuthService);
  const router = inject(Router);

  return next(outgoing).pipe(
    catchError((err: unknown) => {
      if (!(err instanceof HttpErrorResponse)) {
        return throwError(() => err);
      }

      if (err.status === 403 && getApiErrorCode(err) === 'ACCOUNT_BLOCKED') {
        auth.clearSession();
        void router.navigate(['/login']);
        return throwError(() => err);
      }

      if (err.status !== 401) {
        return throwError(() => err);
      }

      const refreshToken = tokens.getRefreshToken();
      if (!refreshToken) {
        auth.clearSession();
        void router.navigate(['/login']);
        return throwError(() => err);
      }

      return singleFlightRefresh(auth).pipe(
        switchMap((res) => {
          const retried = req.clone({
            setHeaders: { Authorization: `Bearer ${res.accessToken}` },
          });
          return next(retried);
        }),
        catchError((refreshErr: unknown) => {
          auth.clearSession();
          void router.navigate(['/login']);
          return throwError(() => refreshErr);
        }),
      );
    }),
  );
};
