import { HttpResponse } from 'msw';
import type { ApiError, Role } from '../app/core/models';
import { db, type MockUser } from './db';
import { parseToken } from './jwt';

export function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

export async function readJsonBody(request: Request): Promise<Record<string, unknown> | null> {
  try {
    const value: unknown = await request.json();
    return isRecord(value) ? value : null;
  } catch {
    return null;
  }
}

export function stringValue(body: Record<string, unknown>, key: string): string | null {
  const value = body[key];
  return typeof value === 'string' ? value : null;
}

export function apiError(
  status: number,
  error: string,
  message: string,
  request: Request,
): Response {
  const body: ApiError = {
    timestamp: new Date().toISOString().replace(/\.\d{3}Z$/, 'Z'),
    status,
    error,
    message,
    path: new URL(request.url).pathname,
  };
  return HttpResponse.json(body, { status });
}

export function noContent(): Response {
  return new HttpResponse(null, { status: 204 });
}

export type AuthResult = { ok: true; user: MockUser } | { ok: false; response: Response };

/**
 * Mirrors AuthenticationFilter + @RolesAllowed: missing/invalid token → 401,
 * blocked user → 403 ACCOUNT_BLOCKED, wrong role → 403 FORBIDDEN.
 */
export function requireUser(request: Request, roles?: readonly Role[]): AuthResult {
  const header = request.headers.get('Authorization');
  if (!header?.startsWith('Bearer ')) {
    return {
      ok: false,
      response: apiError(401, 'UNAUTHENTICATED', 'Missing or invalid Authorization header', request),
    };
  }

  const payload = parseToken(header.slice('Bearer '.length).trim());
  if (!payload || payload.typ !== 'access') {
    return {
      ok: false,
      response: apiError(401, 'UNAUTHENTICATED', 'Invalid or expired token', request),
    };
  }

  const user = db.findUserById(payload.sub);
  if (!user) {
    return {
      ok: false,
      response: apiError(401, 'UNAUTHENTICATED', 'User no longer exists', request),
    };
  }

  if (user.blocked) {
    return {
      ok: false,
      response: apiError(403, 'ACCOUNT_BLOCKED', 'Account is blocked', request),
    };
  }

  if (roles && !roles.includes(user.role)) {
    return {
      ok: false,
      response: apiError(403, 'FORBIDDEN', 'Forbidden', request),
    };
  }

  return { ok: true, user };
}
