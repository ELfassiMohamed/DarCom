# Mock API (MSW)

The frontend can run without the Jakarta EE backend by serving the API from
[Mock Service Worker](https://mswjs.io/).

For the full architecture, complete seed-data reference, request flows, and
troubleshooting, see
[`docs/MSW_INTEGRATION.md`](../../docs/MSW_INTEGRATION.md).

## Start

```bash
npm run start:mock
```

Open `http://localhost:4300`. A `MOCK API` badge is shown in the bottom-left
corner. Requests to `/api/**` are intercepted in the browser; no proxy and no
backend are used.

The normal command still targets the real backend and runs on port 4200:

```bash
npm start
```

Port `3000` is reserved for nginx in this project.

## Seeded accounts

All seeded accounts use the password `DevPassword123!`.

| Email | Role | State |
|---|---|---|
| `visitor@darcom.test` | VISITOR | verified |
| `host@darcom.test` | HOST | verified |
| `admin@darcom.test` | ADMIN | verified |
| `blocked@darcom.test` | VISITOR | blocked |
| `unverified@darcom.test` | VISITOR | not verified |
| `host2@darcom.test` ... `host4@darcom.test` | HOST | mixed |
| `visitor2@darcom.test` ... `visitor4@darcom.test` | VISITOR | mixed |

The admin list has 12 users, which is enough to exercise pagination at the
page size of 10 used by the admin screen.

## Covered endpoints

All endpoints documented in `darcom-backend/API_DOCS.md`:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `POST /api/auth/verify-email`
- `POST /api/auth/resend-verification`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`
- `GET /api/users/me`
- `PUT /api/users/me`
- `GET /api/admin/users`
- `GET /api/admin/users/{id}`
- `PATCH /api/admin/users/{id}/block`
- `PATCH /api/admin/users/{id}/unblock`
- `GET /api/health`

Responses follow the documented shapes, including the
`{ timestamp, status, error, message, path }` error envelope.

## Behavior notes

- Tokens are JWT-shaped and include `sub`, `typ`, `iss`, `iat`, and `exp`.
  The frontend never verifies the signature; only the backend does.
- Access tokens expire after 15 minutes, refresh tokens after 7 days.
- `POST /api/auth/refresh` issues a new access token without rotation, matching
  the backend.
- Registering, resending verification, or requesting a password reset logs the
  generated token to the browser console. Use it on `/verify-email?token=...`
  or `/reset-password?token=...`.
- State is in memory and resets on a full page reload. Seed accounts are
  restored automatically.
- The mock does not simulate the 5-emails-per-hour rate limit.

## Tests

`src/test-setup.ts` starts the same handlers in Node through `setupServer`.
Tests fail on an unmocked `/api` request:

```ts
server.listen({
  onUnhandledRequest(request, print) {
    if (new URL(request.url).pathname.startsWith('/api')) {
      print.error();
    }
  },
});
```

## Adding an endpoint

1. Add a handler in `src/mocks/handlers/*.handlers.ts`.
2. Register it in `src/mocks/handlers/index.ts`.
3. Add a fixture or behavior to `src/mocks/db.ts` if it needs state.
4. Add a case to `src/mocks/mocks.spec.ts` if it is business-critical.

Keep the mock in sync with `darcom-backend/API_DOCS.md`. When the backend gains
an OpenAPI spec, these handlers can be generated or type-checked against it.

## Production safety

The mock build uses a separate entry point (`src/main.mock.ts`) and a separate
output directory (`dist/darcom-frontend-mock`) configured in `angular.json`.
The production build uses `src/main.ts`, writes to
`dist/darcom-frontend/browser`, excludes `mockServiceWorker.js` from assets, and
does not include MSW in the bundle.

Nginx must serve only `dist/darcom-frontend/browser`. Never point it at the mock
output directory.
