import { describe, expect, it } from 'vitest';
import { DEV_PASSWORD } from './db';

const API = 'http://localhost/api';

interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: {
    id: string;
    email: string;
    role: string;
    verified: boolean;
  };
}

interface RefreshResponse {
  accessToken: string;
  expiresIn: number;
}

interface ApiError {
  status: number;
  error: string;
  message: string;
  path: string;
}

async function login(email: string): Promise<LoginResponse> {
  const response = await fetch(`${API}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password: DEV_PASSWORD }),
  });
  expect(response.status).toBe(200);
  return (await response.json()) as LoginResponse;
}

describe('MSW API', () => {
  it('logs in and refreshes a token', async () => {
    const session = await login('admin@darcom.test');
    expect(session.user.role).toBe('ADMIN');
    expect(session.accessToken).toContain('.');

    const response = await fetch(`${API}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: session.refreshToken }),
    });
    expect(response.status).toBe(200);

    const refreshed = (await response.json()) as RefreshResponse;
    expect(refreshed.accessToken).toBeTruthy();
    expect(refreshed.expiresIn).toBe(900);
  });

  it('rejects protected endpoints without a bearer token', async () => {
    const response = await fetch(`${API}/users/me`);
    expect(response.status).toBe(401);

    const body = (await response.json()) as ApiError;
    expect(body.error).toBe('UNAUTHENTICATED');
    expect(body.path).toBe('/api/users/me');
  });

  it('lists, filters, and pages admin users', async () => {
    const session = await login('admin@darcom.test');
    const response = await fetch(`${API}/admin/users?role=HOST&size=2&sort=email,asc`, {
      headers: { Authorization: `Bearer ${session.accessToken}` },
    });
    expect(response.status).toBe(200);

    const body = (await response.json()) as {
      items: Array<{ role: string; email: string }>;
      page: number;
      size: number;
      totalItems: number;
      totalPages: number;
    };

    expect(body.page).toBe(0);
    expect(body.size).toBe(2);
    expect(body.items).toHaveLength(2);
    expect(body.items.every((user) => user.role === 'HOST')).toBe(true);
    expect(body.totalItems).toBeGreaterThan(2);
    expect(body.totalPages).toBeGreaterThan(1);
  });

  it('reads and updates the current profile', async () => {
    const session = await login('visitor@darcom.test');
    const headers = {
      Authorization: `Bearer ${session.accessToken}`,
      'Content-Type': 'application/json',
    };

    const read = await fetch(`${API}/users/me`, { headers });
    expect(read.status).toBe(200);
    expect(((await read.json()) as { fullName: string }).fullName).toBe('Val Visitor');

    const update = await fetch(`${API}/users/me`, {
      method: 'PUT',
      headers,
      body: JSON.stringify({ fullName: 'Val Updated', phone: null }),
    });
    expect(update.status).toBe(200);

    const updated = (await update.json()) as { fullName: string; phone: string | null };
    expect(updated.fullName).toBe('Val Updated');
    expect(updated.phone).toBeNull();
  });

  it('blocks and unblocks a user', async () => {
    const session = await login('admin@darcom.test');
    const headers = {
      Authorization: `Bearer ${session.accessToken}`,
      'Content-Type': 'application/json',
    };

    const block = await fetch(`${API}/admin/users/00000000-0000-4000-8000-000000000001/block`, {
      method: 'PATCH',
      headers,
      body: JSON.stringify({ reason: 'Mock test' }),
    });
    expect(block.status).toBe(200);
    expect(((await block.json()) as { blocked: boolean }).blocked).toBe(true);

    const unblock = await fetch(
      `${API}/admin/users/00000000-0000-4000-8000-000000000001/unblock`,
      {
        method: 'PATCH',
        headers,
        body: '{}',
      },
    );
    expect(unblock.status).toBe(200);
    expect(((await unblock.json()) as { blocked: boolean }).blocked).toBe(false);
  });
});
