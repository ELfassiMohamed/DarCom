# Plan: Link the Angular frontend to the DarCom backend through nginx (Docker)

> This file is written for an AI coding agent (opencode). Execute the phases in order.
> Do not skip the "Acceptance" checks. Do not ask questions; where something is unknown,
> detect it from the repo or use the stated assumption and record it in `NOTES.md`.

---

## 0. Context

| Item | Value |
|---|---|
| Frontend | Fresh Angular project (blank), repo root = Angular project root |
| Backend | Jakarta EE 10 REST API, already working, **must not be modified** |
| Backend base URL (host machine) | `http://localhost:8080/darcom-backend/api` |
| Backend context path (assumption) | `darcom-backend` (the single place to change it is `nginx/default.conf` + `proxy.conf.json`) |
| Dev OS | Windows 10, PowerShell, Docker Desktop (WSL 2). Use `curl.exe`, not `curl`. |
| Public entry point | `http://localhost:3000` (nginx). Port 3000 is required: the backend emails verification links to `http://localhost:3000/verify-email?token=...` and its CORS allows `http://localhost:3000`. |

### Target architecture

```
Browser ──► nginx (Docker, host port 3000) ─┬─ /api/*        ──► http://host.docker.internal:8080/darcom-backend/api/*
                                            └─ everything else ──► Angular static build (index.html fallback)
```

Angular only ever calls **relative** URLs starting with `/api`. This makes the browser
same-origin, so no CORS configuration is needed.

### Backend API contract (summary, all JSON, UTF-8)

Error envelope on every 4xx/5xx:
`{ "timestamp", "status", "error": "<CODE>", "message", "path" }`

| Method + path | Auth | Request | Success |
|---|---|---|---|
| `POST /api/auth/register` | public | `{email, password, fullName, role}` role = `HOST` or `VISITOR` only | `201` `{id,email,fullName,role,verified,createdAt}` |
| `POST /api/auth/login` | public | `{email, password}` | `200` `{accessToken, refreshToken, expiresIn, user:{id,email,role,verified}}` |
| `POST /api/auth/refresh` | public | `{refreshToken}` | `200` `{accessToken, expiresIn}` (no refresh-token rotation) |
| `POST /api/auth/logout` | Bearer | `{refreshToken}` | `204` empty body |
| `POST /api/auth/verify-email` | public | `{token}` | `200` full user object |
| `POST /api/auth/resend-verification` | public | `{email}` | `204` always |
| `POST /api/auth/forgot-password` | public | `{email}` | `204` always |
| `POST /api/auth/reset-password` | public | `{token, newPassword}` | `204` |
| `GET /api/users/me` | Bearer | - | `200` `{id,email,fullName,phone,role,verified,blocked,createdAt}` |
| `PUT /api/users/me` | Bearer | `{fullName, phone?}` | `200` user |
| `GET /api/health` | public | - | `200` `{"status":"UP"}` |

Error codes the UI must handle: `VALIDATION_ERROR`, `INVALID_CREDENTIALS`, `ACCOUNT_BLOCKED`,
`EMAIL_ALREADY_EXISTS`, `UNAUTHENTICATED`, `INVALID_OR_EXPIRED_REFRESH_TOKEN`,
`INVALID_OR_EXPIRED_VERIFICATION_TOKEN`, `TOO_MANY_REQUESTS`, `INTERNAL_ERROR`.

Password policy (register + reset): 8-72 characters, at least one lowercase, one uppercase,
one digit, one special character.

### Rules for the agent

- Never edit or add backend code or backend `.env` values.
- Never hardcode `localhost:8080` or any absolute backend URL in Angular code. Use `/api` only.
- Use the Angular version already in `package.json`. Use standalone components, `inject()`,
  functional interceptors/guards, and reactive forms. No NgModules.
- Do not add UI libraries (no Angular Material, Bootstrap, Tailwind). Plain CSS, minimal styling.
- Do not add dependencies at all unless a step says so.
- Keep TypeScript strict-mode clean. No `any`.
- Do not commit secrets. Do not create any `.env` file.

---

## Phase 1: Inspect the project

1. Read `package.json`, `angular.json`, `src/app/app.config.ts`, `src/app/app.routes.ts`.
2. Record in `NOTES.md`:
   - Angular version
   - project name (`<PROJECT_NAME>`)
   - the build `outputPath` and builder (`@angular/build:application` / `@angular-devkit/build-angular:application` puts files in `dist/<PROJECT_NAME>/browser/`; the older `browser` builder puts them directly in `dist/<PROJECT_NAME>/`)
3. Determine `<DIST_SUBPATH>`: the folder under `dist/` that contains `index.html`
   (e.g. `<PROJECT_NAME>/browser`). Used in Phase 6.

**Acceptance:** `NOTES.md` exists and states Angular version, project name, and `<DIST_SUBPATH>`.

---

## Phase 2: API foundation (`src/app/core/`)

Create these files.

### `api.config.ts`
```ts
export const API_URL = '/api';
```

### `models.ts`
Typed interfaces matching the contract above: `Role = 'VISITOR' | 'HOST' | 'ADMIN'`,
`RegisterRequest`, `LoginRequest`, `LoginResponse`, `RefreshResponse`, `User`
(the full `/users/me` shape), `SessionUser` (`{id,email,role,verified}`), and
`ApiError` (`{timestamp,status,error,message,path}`).

### `token-storage.service.ts`
- Injectable, `providedIn: 'root'`.
- Stores `accessToken`, `refreshToken`, and `SessionUser` in `localStorage` under keys prefixed `darcom.`.
- Methods: `getAccessToken()`, `getRefreshToken()`, `setSession(login: LoginResponse)`,
  `setAccessToken(token: string)`, `getUser()`, `clear()`.
- Wrap `JSON.parse` in try/catch and return `null` on corrupt data.

### `error-messages.ts`
Export `toUserMessage(err: unknown): string`.
- If `err` is an `HttpErrorResponse` whose `error` looks like `ApiError`, map by `error.error` code:
  - `INVALID_CREDENTIALS` -> "Wrong email or password."
  - `ACCOUNT_BLOCKED` -> "This account has been blocked."
  - `EMAIL_ALREADY_EXISTS` -> "An account with this email already exists."
  - `TOO_MANY_REQUESTS` -> "Too many attempts. Try again in an hour."
  - `INVALID_OR_EXPIRED_VERIFICATION_TOKEN` -> "This verification link is invalid or has expired."
  - `VALIDATION_ERROR` -> show `error.message` as is
  - default -> `error.message`
- If `status === 0` -> "Cannot reach the server."
- Otherwise -> "Something went wrong."

### `auth.service.ts`
Injectable using `HttpClient` and `TokenStorageService`. Methods (all use `API_URL`):
- `register(body: RegisterRequest): Observable<User-like>`
- `login(email, password)`: on success call `tokens.setSession(res)`; also expose a `currentUser` signal initialised from storage.
- `refresh(): Observable<RefreshResponse>`: POST `/auth/refresh` with the stored refresh token; on success call `tokens.setAccessToken`.
- `logout()`: POST `/auth/logout` with the stored refresh token, and **always** (even on error) call `tokens.clear()` and reset `currentUser`.
- `verifyEmail(token)`, `resendVerification(email)`, `forgotPassword(email)`, `resetPassword(token, newPassword)`.
- `me(): Observable<User>` -> `GET /users/me`.
- `isLoggedIn()` -> boolean based on presence of tokens.

### `auth.interceptor.ts` (functional `HttpInterceptorFn`)
Behaviour:
1. Only touch requests whose URL starts with `/api`.
2. Do **not** attach a token to these public paths: `/api/auth/login`, `/api/auth/register`,
   `/api/auth/refresh`, `/api/auth/verify-email`, `/api/auth/resend-verification`,
   `/api/auth/forgot-password`, `/api/auth/reset-password`.
3. For all other `/api` requests, add `Authorization: Bearer <accessToken>` when a token exists
   (this includes `/api/auth/logout`).
4. On a `401` response from a non-public request that has a refresh token:
   - Call `AuthService.refresh()` **once**, even if several requests fail at the same time
     (share a single in-flight refresh with `shareReplay`/a `BehaviorSubject` gate).
   - On success, retry the original request with the new token.
   - On failure (`INVALID_OR_EXPIRED_REFRESH_TOKEN` or any error), clear the session and navigate to `/login`.
5. On `403` with `error.error === 'ACCOUNT_BLOCKED'`, clear the session and navigate to `/login`.
6. Never try to refresh for the public paths above (avoids infinite loops).

### `auth.guard.ts`
`CanActivateFn`: allow if `isLoggedIn()`, otherwise `router.createUrlTree(['/login'])`.

### Wire up `app.config.ts`
```ts
provideRouter(routes),
provideHttpClient(withInterceptors([authInterceptor])),
```

**Acceptance:** `ng build` succeeds with zero TypeScript errors.

---

## Phase 3: Pages (`src/app/pages/`)

Keep each page small: standalone component, reactive form, inline error text, a disabled
submit button while the request is in flight. Use `toUserMessage()` for errors.

| Route | Component | Behaviour |
|---|---|---|
| `/login` | `LoginComponent` | Email + password form. On success navigate to `/home`. |
| `/register` | `RegisterComponent` | Email, password, full name, role `<select>` with only `VISITOR` and `HOST`. Client-side password validator matching the policy. On `201` show: "Account created. Check your email to verify it." and a link to `/login`. |
| `/verify-email` | `VerifyEmailComponent` | See below. |
| `/home` | `HomeComponent` (guarded) | Calls `GET /users/me` and shows email, full name, role, and `verified` status. Includes a Logout button. This page proves the Bearer token flow works end to end. |
| `/` | redirect | Redirect to `/home`. |
| `**` | redirect | Redirect to `/home`. |

### `VerifyEmailComponent` (critical: this is the target of the emailed link)

The email link has the form `http://localhost:3000/verify-email?token=<raw-token>`.

1. Read `token` from `ActivatedRoute` query params (`queryParamMap.get('token')`).
2. If it is missing, show the "invalid link" state without calling the API.
3. Otherwise call `AuthService.verifyEmail(token)` **exactly once** on init
   (guard against a double call; do not call it again on re-render).
4. States:
   - `loading`: "Verifying your email..."
   - `success`: "Your email is verified." with a link to `/login`
   - `error`: show `toUserMessage(err)`, plus a small form to request a new link by email
     (calls `resendVerification(email)` and then shows "If the account exists, a new link was sent.").
5. This route must be **public** (no guard).

Add all routes in `app.routes.ts` (lazy-load with `loadComponent`). `/home` uses `authGuard`.

**Acceptance:** `ng build` succeeds. All five routes exist.

---

## Phase 4: Dev workflow with `ng serve` (no Docker)

Create `proxy.conf.json` at the repo root:

```json
{
  "/api": {
    "target": "http://localhost:8080/darcom-backend",
    "secure": false,
    "changeOrigin": false,
    "logLevel": "info"
  }
}
```

(`/api/x` becomes `http://localhost:8080/darcom-backend/api/x`, so no `pathRewrite` is needed.)

In `package.json` set the `start` script to:

```json
"start": "ng serve --port 3000 --proxy-config proxy.conf.json"
```

Port 3000 is intentional so the browser `Origin` matches the backend's allowed origins.
Note in `NOTES.md` that `npm start` and the nginx container both use port 3000 and cannot run at the same time.

**Acceptance:** `npm start` serves Angular at `http://localhost:3000/` without errors.

---

## Phase 5: nginx in Docker

Create the following at the repo root.

### `nginx/default.conf`

```nginx
server {
    listen 80;
    server_name localhost;

    root /usr/share/nginx/html/<DIST_SUBPATH>;   # replace with the value found in Phase 1
    index index.html;

    gzip on;
    gzip_types text/plain text/css application/javascript application/json image/svg+xml;

    # Backend API -> Jakarta EE running on the Windows host
    location /api/ {
        proxy_pass http://host.docker.internal:8080/darcom-backend/api/;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 5s;
        proxy_read_timeout    60s;
    }

    # Never cache the app shell
    location = /index.html {
        add_header Cache-Control "no-cache";
    }

    # Hashed build assets can be cached aggressively
    location ~* \.(?:js|css|woff2?)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
        try_files $uri =404;
    }

    # Angular router fallback (makes /verify-email?token=... work on direct load/refresh)
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

Important: keep the trailing slash on **both** `location /api/` and the `proxy_pass` URL.

### `docker-compose.yml`

```yaml
services:
  nginx:
    image: nginx:alpine
    container_name: darcom-nginx
    ports:
      - "3000:80"
    extra_hosts:
      - "host.docker.internal:host-gateway"
    volumes:
      - ./nginx/default.conf:/etc/nginx/conf.d/default.conf:ro
      - ./dist:/usr/share/nginx/html:ro
    restart: unless-stopped
```

The whole `dist` folder is mounted (not `dist/<project>/browser`) because `ng build` deletes
and recreates its output folder, which would leave a single-folder mount stale.

**Acceptance:** `docker compose config` prints without errors.

---

## Phase 6: Build, run, verify

Run in PowerShell, from the repo root:

```powershell
ng build
docker compose up -d
docker compose logs nginx
```

Then verify each item (use `curl.exe`):

| # | Check | Expected |
|---|---|---|
| 1 | `curl.exe -i http://localhost:3000/` | `200`, HTML containing `<app-root>` |
| 2 | `curl.exe -i "http://localhost:3000/verify-email?token=abc"` | `200`, the same HTML (router fallback) |
| 3 | `curl.exe -i http://localhost:3000/does-not-exist-as-file.js` | `404` (not the HTML page) |

The backend is not running during this plan, so do **not** test `/api/...` calls. They will be tested manually later.

**Acceptance:** all three curl checks pass.

---

## Phase 7: Troubleshooting notes (put these in `NOTES.md`)

| Symptom | Likely cause / fix |
|---|---|
| `502 Bad Gateway` on `/api/...` | nginx cannot reach the backend. Confirm `http://localhost:8080/darcom-backend/api/health` works in the browser; check Windows Firewall on port 8080. |
| `404` on `/api/...` | Wrong context path in `proxy_pass` (and `proxy.conf.json`). Use the actual deployed WAR name. |
| Blank page / `404` for `main-*.js` | Wrong `root` path: `<DIST_SUBPATH>` does not match the real build output. |
| `/verify-email` returns nginx 404 on refresh | `try_files ... /index.html` is missing or not inside `location /`. |
| `403` / CORS-style error from backend | Origin is not allowed. Only `http://localhost:3000` and `http://localhost:5173` are in `CORS_ALLOWED_ORIGINS`. If the port changes, update the backend `.env` (`CORS_ALLOWED_ORIGINS`, `APP_PUBLIC_URL`). |
| Port 3000 already in use | Stop `ng serve` or run `docker compose down`. |
| Config change not applied | `docker compose restart nginx` |

---

## Out of scope (do not implement now)

- Forgot/reset-password pages (the service methods already exist)
- Profile editing (`PUT /users/me`) and the admin screens (`/admin/users`)
- HTTPS / production deployment
- Moving the JWT out of `localStorage` (a possible later hardening step)

## Final deliverable checklist

- [ ] `NOTES.md` with detected values and assumptions
- [ ] `src/app/core/*` (config, models, storage, error-messages, auth service, interceptor, guard)
- [ ] `src/app/pages/*` (login, register, verify-email, home) and routes
- [ ] `proxy.conf.json` and updated `start` script
- [ ] `nginx/default.conf` and `docker-compose.yml`
- [ ] All checks in Phase 6 pass
