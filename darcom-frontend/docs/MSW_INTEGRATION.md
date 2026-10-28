# MSW Integration and Mock Data

This document explains how the DarCom frontend runs without the Jakarta EE
backend, how requests flow through Mock Service Worker (MSW), and what data the
mock API uses.

## 1. Purpose

MSW lets the Angular application use its real HTTP layer while the backend is
stopped. It does **not** replace `AuthService`, guards, interceptors, or
components. The app still calls:

```text
/api/auth/login
/api/users/me
/api/admin/users
...
```

MSW intercepts those requests in the browser and returns backend-shaped
responses.

This is important because the following production code is exercised
unchanged:

- `AuthService`
- `AdminService`
- `authInterceptor` (Bearer token, 401 refresh/retry)
- `authGuard` and `adminGuard`
- `TokenStorageService` (`darcom.accessToken`, `darcom.refreshToken`, `darcom.user`)
- error-message mapping in `src/app/core/error-messages.ts`
- admin pagination, filtering, sorting, block, and unblock UI

## 2. Ports and commands

| Command | URL | API source | Output |
|---|---|---|---|
| `npm start` | `http://localhost:4200` | Real backend via `proxy.conf.json` | Dev server |
| `npm run start:mock` | `http://localhost:4300` | MSW | Dev server |
| `npm run build` | — | Real backend | `dist/darcom-frontend/browser` |
| `npm run build:mock` | — | MSW | `dist/darcom-frontend-mock/browser` |
| nginx | `http://localhost:3000` | Real backend | Serves production build |

Port `3000` is reserved for nginx. Do not run an Angular dev server there.

The real backend is expected at:

```text
http://localhost:8080/darcom-backend/api/...
```

`proxy.conf.json` maps `/api` to `http://localhost:8080/darcom-backend`.

## 3. How mock mode starts

`angular.json` defines a `mock` build configuration:

```json
{
  "mock": {
    "browser": "src/main.mock.ts",
    "outputPath": "dist/darcom-frontend-mock",
    "fileReplacements": [
      {
        "replace": "src/environments/environment.ts",
        "with": "src/environments/environment.mock.ts"
      }
    ]
  }
}
```

The mock entry point is `src/main.mock.ts`:

```ts
import { bootstrap } from './bootstrap';
import { startMockApi } from './mocks/browser';

startMockApi()
  .then(() => bootstrap())
  .catch((err) => console.error(err));
```

Startup sequence:

```text
npm run start:mock
  -> Angular uses src/main.mock.ts
  -> environment.mock.ts sets useMocks = true
  -> startMockApi() starts MSW
  -> MSW registers public/mockServiceWorker.js
  -> Angular bootstrap() runs
  -> HttpClient calls /api/**
  -> mock service worker intercepts the request
  -> matching handler builds a response
  -> Angular receives the response normally
```

The production entry point is `src/main.ts`:

```ts
import { bootstrap } from './bootstrap';

bootstrap().catch((err) => console.error(err));
```

It does not import MSW.

## 4. Production separation

Mock code is kept out of production in four ways:

1. Production uses `src/main.ts`; mock mode uses `src/main.mock.ts`.
2. The production build excludes `mockServiceWorker.js` from copied assets.
3. `environment.ts` has `useMocks: false`; only `environment.mock.ts` sets it to
   `true`.
4. Mock builds write to `dist/darcom-frontend-mock`, not the production
   `dist/darcom-frontend` directory.

Nginx must serve only:

```text
C:/Dev/Eclipse/DarCom/darcom-frontend/dist/darcom-frontend/browser
```

The production bundle contains no `startMockApi`, no `msw/browser`, and no
`mockServiceWorker.js`.

If port 3000 previously served the mock build, unregister the old service
worker once:

```text
DevTools -> Application -> Service Workers -> Unregister
DevTools -> Application -> Storage -> Clear site data
```

Then hard reload. An installed service worker can outlive the files on disk.

## 5. Source layout

| Path | Responsibility |
|---|---|
| `public/mockServiceWorker.js` | Generated MSW browser worker |
| `src/main.mock.ts` | Starts MSW, then bootstraps Angular |
| `src/mocks/browser.ts` | Browser worker setup and seed-account banner/log |
| `src/mocks/node.ts` | Node server for Vitest |
| `src/mocks/db.ts` | In-memory users, tokens, and seed data |
| `src/mocks/jwt.ts` | JWT-shaped access/refresh tokens |
| `src/mocks/http.ts` | Error envelope, auth checks, JSON helpers |
| `src/mocks/validation.ts` | Email, password, and phone rules |
| `src/mocks/handlers/auth.handlers.ts` | Auth endpoints |
| `src/mocks/handlers/user.handlers.ts` | Profile endpoints |
| `src/mocks/handlers/admin.handlers.ts` | Admin list/detail/block/unblock |
| `src/mocks/handlers/health.handlers.ts` | Health endpoint |
| `src/mocks/handlers/index.ts` | Handler registry |
| `src/mocks/mocks.spec.ts` | MSW API tests |
| `src/test-setup.ts` | Starts MSW Node server for tests |

Handler paths use a wildcard origin:

```ts
export function apiPath(path: string): string {
  return `*/api${path}`;
}
```

This makes the same handlers work in the browser (`localhost:4300`) and in
Vitest, regardless of the test origin.

## 6. API coverage

All endpoints documented in `darcom-backend/API_DOCS.md` are implemented:

### Authentication

| Method | Path | Behavior |
|---|---|---|
| `POST` | `/api/auth/register` | Creates an unverified user and logs a verification token |
| `POST` | `/api/auth/login` | Returns access token, refresh token, expiry, and session user |
| `POST` | `/api/auth/refresh` | Validates the refresh token and returns a new access token |
| `POST` | `/api/auth/logout` | Revokes the caller's refresh token |
| `POST` | `/api/auth/verify-email` | Consumes a verification token and returns the user |
| `POST` | `/api/auth/resend-verification` | Always 204; logs a new token for eligible users |
| `POST` | `/api/auth/forgot-password` | Always 204; logs a reset token for eligible users |
| `POST` | `/api/auth/reset-password` | Consumes a reset token, changes the password, revokes refresh tokens |

### Profile

| Method | Path | Behavior |
|---|---|---|
| `GET` | `/api/users/me` | Returns the authenticated user |
| `PUT` | `/api/users/me` | Replaces `fullName` and `phone`; omitted/null phone clears it |

### Admin

| Method | Path | Behavior |
|---|---|---|
| `GET` | `/api/admin/users` | Search, filter, sort, and paginate users |
| `GET` | `/api/admin/users/{id}` | Returns detail plus `listingCount` and `reportCount` |
| `PATCH` | `/api/admin/users/{id}/block` | Requires a non-blank reason; revokes refresh tokens |
| `PATCH` | `/api/admin/users/{id}/unblock` | Clears blocked status |

### Health

| Method | Path | Behavior |
|---|---|---|
| `GET` | `/api/health` | Returns `{ "status": "UP" }` |

## 7. Authentication model

The mock issues JWT-shaped tokens:

```json
{
  "sub": "00000000-0000-4000-8000-000000000003",
  "role": "ADMIN",
  "typ": "access",
  "iss": "darcom-mock",
  "iat": 1790000000,
  "exp": 1790000900
}
```

Refresh tokens add a `jti` and use `typ: "refresh"`.

Important details:

- Access token TTL: **15 minutes (900 seconds)**.
- Refresh token TTL: **7 days**.
- The signature is a placeholder (`mock-signature`). The Angular client never
  verifies it; the real backend does.
- `authInterceptor` attaches the access token as `Authorization: Bearer ...`.
- Protected handlers load the current user from the mock database on every
  request, so blocking a user takes effect immediately.
- A missing/invalid access token returns `401 UNAUTHENTICATED`.
- A blocked user returns `403 ACCOUNT_BLOCKED`, including on Bearer use.
- A non-admin calling an admin endpoint returns `403 FORBIDDEN`.
- Refresh validates the token shape, expiry, stored refresh record, and user
  state. Invalid refresh returns `401 INVALID_OR_EXPIRED_REFRESH_TOKEN`.
- Logout is idempotent and only deletes a refresh token owned by the caller.
- Refresh does not rotate the refresh token, matching the backend contract.

## 8. Mock data

Data lives in `src/mocks/db.ts`. It is **in memory** and resets on a full page
reload. Seed accounts are restored automatically.

All seeded accounts use:

```text
Password: DevPassword123!
```

### Seed users

IDs follow the pattern `00000000-0000-4000-8000-00000000000N`.

| # | Email | Full name | Role | Verified | Blocked | Created | Listings | Reports |
|---|---|---|---|---|---|---|---|---|
| 1 | `visitor@darcom.test` | Val Visitor | VISITOR | yes | no | 2026-01-15 | 0 | 0 |
| 2 | `host@darcom.test` | Hana Host | HOST | yes | no | 2026-02-01 | 3 | 0 |
| 3 | `admin@darcom.test` | Adam Admin | ADMIN | yes | no | 2026-01-01 | 0 | 0 |
| 4 | `blocked@darcom.test` | Blake Blocked | VISITOR | yes | yes | 2026-03-10 | 0 | 0 |
| 5 | `unverified@darcom.test` | Uma Unverified | VISITOR | no | no | 2026-04-01 | 0 | 0 |
| 6 | `host2@darcom.test` | Omar Host | HOST | yes | no | 2026-02-15 | 2 | 1 |
| 7 | `host3@darcom.test` | Sofia Stay | HOST | no | no | 2026-03-05 | 1 | 0 |
| 8 | `visitor2@darcom.test` | Victor Visitor | VISITOR | yes | no | 2026-03-20 | 0 | 0 |
| 9 | `visitor3@darcom.test` | Nora Nomad | VISITOR | yes | no | 2026-04-10 | 0 | 0 |
| 10 | `blockedhost@darcom.test` | Bruno Blocked | HOST | yes | yes | 2026-04-15 | 4 | 2 |
| 11 | `host4@darcom.test` | Leila Lodge | HOST | yes | no | 2026-05-01 | 5 | 0 |
| 12 | `visitor4@darcom.test` | Sam Sojourn | VISITOR | no | no | 2026-05-10 | 0 | 0 |

Phone numbers are present for most seeds; `unverified@darcom.test` and
`visitor4@darcom.test` have `null` phones.

Twelve users allow the admin screen to exercise pagination with its page size of
10.

### User shape

```ts
interface MockUser {
  id: string;              // UUID
  email: string;
  fullName: string;
  phone: string | null;    // E.164
  role: 'VISITOR' | 'HOST' | 'ADMIN';
  verified: boolean;
  blocked: boolean;
  createdAt: string;       // ISO-8601 UTC
  password: string;        // never returned by the API
  listingCount: number;    // detailed admin response only
  reportCount: number;     // detailed admin response only
}
```

API responses omit `password` and, except for the admin detail endpoint, omit
`listingCount`/`reportCount`.

### New registrations

Registering creates a random UUID user with:

- `verified: false`
- `blocked: false`
- `listingCount: 0`
- `reportCount: 0`

Because state is in memory, a newly registered user is lost on a full page
reload. Use the seeded accounts for repeatable testing.

## 9. Verification and reset tokens

No email is sent in mock mode. Tokens are printed to the browser console:

```text
[MSW] EMAIL_VERIFICATION token for user@example.com: mock-email_verification-...
[MSW] PASSWORD_RESET token for user@example.com: mock-password_reset-...
```

Use them at:

```text
/verify-email?token=...
/reset-password?token=...
```

Rules:

- Verification token TTL: 24 hours.
- Reset token TTL: 1 hour.
- Issuing a new token invalidates the previous token for that user.
- Tokens are single-use.
- Reset revokes all refresh tokens for the user.

To exercise verification for the seeded unverified account, submit the resend
form with `unverified@darcom.test`, then copy the token from the console.

## 10. Admin list behavior

`GET /api/admin/users` supports:

| Query | Rules |
|---|---|
| `search` | Case-insensitive match on email or full name |
| `role` | `VISITOR`, `HOST`, or `ADMIN` |
| `verified` | `true` or `false` |
| `blocked` | `true` or `false` |
| `page` | 0-based, default `0` |
| `size` | Default `20`, capped at `100` |
| `sort` | `createdAt`, `email`, `fullName`, or `role`, plus `asc`/`desc` |

Examples:

```text
/api/admin/users?role=HOST&blocked=false&page=0&size=10&sort=email,asc
/api/admin/users?search=host&verified=true&sort=createdAt,desc
```

The response envelope matches the backend:

```json
{
  "items": [],
  "page": 0,
  "size": 10,
  "totalItems": 12,
  "totalPages": 2
}
```

## 11. Error responses

Errors use the same envelope as the backend:

```json
{
  "timestamp": "2026-10-21T12:00:00Z",
  "status": 401,
  "error": "UNAUTHENTICATED",
  "message": "Missing or invalid Authorization header",
  "path": "/api/users/me"
}
```

Implemented error codes include:

- `INVALID_CREDENTIALS`
- `ACCOUNT_BLOCKED`
- `INVALID_OR_EXPIRED_REFRESH_TOKEN`
- `INVALID_OR_EXPIRED_VERIFICATION_TOKEN`
- `INVALID_OR_EXPIRED_RESET_TOKEN`
- `EMAIL_ALREADY_EXISTS`
- `ALREADY_BLOCKED`
- `NOT_BLOCKED`
- `CANNOT_BLOCK_ADMIN`
- `USER_NOT_FOUND`
- `VALIDATION_ERROR`
- `UNAUTHENTICATED`
- `FORBIDDEN`

## 12. Validation rules

The mock mirrors the documented contract:

- Email: well-formed, maximum 255 characters, normalized to lowercase.
- Password: 8–72 characters, at least one lowercase, uppercase, digit, and
  special character.
- Registration role: `VISITOR` or `HOST` only.
- Full name: non-blank, maximum 255 characters.
- Phone: E.164, for example `+212600000001`; null clears it.
- Block reason: non-blank, maximum 500 characters.
- Invalid pagination, filters, or sort values return `400 VALIDATION_ERROR`.

## 13. Testing

`src/test-setup.ts` starts the same handlers in Node:

```ts
import { setupServer } from 'msw/node';
import { handlers } from './handlers';

export const server = setupServer(...handlers);
```

The setup:

- starts the server before the suite,
- resets runtime handlers and the mock database after each test,
- closes the server after the suite,
- fails tests on an unmocked `/api` request.

`src/mocks/mocks.spec.ts` currently covers:

- login and token refresh,
- protected endpoints rejecting missing Bearer tokens,
- admin list filtering/pagination,
- block and unblock.

Run:

```bash
npm test
```

`onUnhandledRequest` is intentionally strict for `/api` paths so a newly added
frontend endpoint cannot silently pass without a handler.

## 14. Adding a new endpoint

1. Add the handler to the appropriate file in `src/mocks/handlers/`.
2. Register it in `src/mocks/handlers/index.ts`.
3. Add state or seed data to `src/mocks/db.ts` if required.
4. Add a test to `src/mocks/mocks.spec.ts` if the endpoint is business-critical.
5. Keep the response shape aligned with `darcom-backend/API_DOCS.md`.

Example:

```ts
http.get(apiPath('/example'), ({ request }) => {
  const auth = requireUser(request);
  if (!auth.ok) {
    return auth.response;
  }
  return HttpResponse.json({ ok: true });
});
```

Use `apiError(...)` from `src/mocks/http.ts` for error responses so the envelope
stays consistent.

## 15. Known limitations

- Data is in memory and resets on reload.
- Registered users and password changes do not survive a reload.
- The 5-emails-per-hour rate limit is not simulated.
- Email delivery is not simulated; tokens are logged to the console.
- Mock JWTs are not cryptographically signed.
- The mock does not reproduce database transactions or concurrency behavior.
- Admin block hides host listings in the real backend; there are no listing
  entities in the mock yet, so only the user's blocked flag and refresh-token
  revocation are simulated.
- The frontend currently does not call `PUT /users/me`,
  `GET /admin/users/{id}`, or `GET /health`; they are mocked for completeness.

## 16. Troubleshooting

### `MOCK API` badge appears on port 3000

Nginx is serving the mock output, or the browser still has the mock service
worker registered.

Check the served `index.html`:

- `main.js` means the mock build is being served.
- `main-<HASH>.js` means the production build is being served.

Ensure nginx root points to `dist/darcom-frontend/browser`, not
`dist/darcom-frontend-mock/browser`.

Then unregister the old MSW service worker and clear site data for
`http://localhost:3000`.

### `mockServiceWorker.js` returns 404

This is correct in production. It means MSW is not installed.

### Requests still hit MSW after switching to nginx

The old service worker is still controlling the page. Unregister it in DevTools
or test in a private window.

### Mock login fails with a real backend account

Mock accounts exist only in the mock database. Use the seeded accounts listed
above when running `npm run start:mock`.

### Port already in use

- Mock dev server: `http://localhost:4300`
- Real dev server: `http://localhost:4200`
- Nginx: `http://localhost:3000`
- Backend: `http://localhost:8080`

Stop the process using the conflicting port or change the relevant script.
