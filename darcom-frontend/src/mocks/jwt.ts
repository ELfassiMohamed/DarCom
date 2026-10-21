import type { Role } from '../app/core/models';

/**
 * JWT-shaped tokens for the mock API.
 *
 * The real backend signs tokens with HS256, but the Angular client never
 * verifies the signature. It only sends the token back, so a structurally
 * correct token with `exp`/`typ`/`role` claims is enough to exercise the
 * real interceptor and guards.
 */

export const ACCESS_TTL_SECONDS = 15 * 60;
export const REFRESH_TTL_SECONDS = 7 * 24 * 60 * 60;
const ISSUER = 'darcom-mock';

export interface MockTokenPayload {
  sub: string;
  role?: Role;
  typ: 'access' | 'refresh';
  iss: string;
  iat: number;
  exp: number;
  jti?: string;
}

function encodeSegment(value: unknown): string {
  const json = JSON.stringify(value);
  return btoa(json).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
}

function decodeSegment(segment: string): unknown {
  const padded = segment
    .replace(/-/g, '+')
    .replace(/_/g, '/')
    .padEnd(Math.ceil(segment.length / 4) * 4, '=');
  return JSON.parse(atob(padded)) as unknown;
}

function nowSeconds(): number {
  return Math.floor(Date.now() / 1000);
}

function createToken(
  userId: string,
  typ: 'access' | 'refresh',
  ttlSeconds: number,
  role?: Role,
): string {
  const issuedAt = nowSeconds();
  const payload: MockTokenPayload = {
    sub: userId,
    typ,
    iss: ISSUER,
    iat: issuedAt,
    exp: issuedAt + ttlSeconds,
    ...(role ? { role } : {}),
    ...(typ === 'refresh' ? { jti: crypto.randomUUID() } : {}),
  };

  const header = { alg: 'HS256', typ: 'JWT' };
  return `${encodeSegment(header)}.${encodeSegment(payload)}.mock-signature`;
}

export function createAccessToken(userId: string, role: Role): string {
  return createToken(userId, 'access', ACCESS_TTL_SECONDS, role);
}

export function createRefreshToken(userId: string): string {
  return createToken(userId, 'refresh', REFRESH_TTL_SECONDS);
}

export function parseToken(token: string): MockTokenPayload | null {
  const parts = token.split('.');
  if (parts.length !== 3) {
    return null;
  }

  try {
    const payload = decodeSegment(parts[1]) as Partial<MockTokenPayload>;
    if (
      typeof payload.sub !== 'string' ||
      (payload.typ !== 'access' && payload.typ !== 'refresh') ||
      payload.iss !== ISSUER ||
      typeof payload.exp !== 'number' ||
      payload.exp <= nowSeconds()
    ) {
      return null;
    }
    return payload as MockTokenPayload;
  } catch {
    return null;
  }
}
