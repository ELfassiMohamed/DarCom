# NOTES.md — DarCom Frontend / Backend link (nginx Docker)

## Detected values (Phase 1)
- Angular version: 21.2.0 (`@angular/* ^21.2.0`, `@angular/build ^21.2.0`, TypeScript ~5.9.2, rxjs ~7.8.0)
- Project name (`<PROJECT_NAME>`): `darcom-frontend`
- Builder: `@angular/build:application` (in `angular.json` projects.darcom-frontend.architect.build)
- Build `outputPath`: not set explicitly in `angular.json` → Angular application builder default = `dist/<PROJECT_NAME>/`
- `<DIST_SUBPATH>`: `darcom-frontend/browser` (confirmed: `ng build` produces `dist/darcom-frontend/browser/index.html`).
- SSR disabled for static nginx serving: removed `server`, `outputMode: server`, and `ssr.entry` from `angular.json` build options (scaffold default was SSR with `outputMode: server`, which produced only `browser/index.csr.html` and no `browser/index.html`). `src/main.server.ts`, `src/server.ts`, `app.config.server.ts` remain on disk but are no longer used by the build. `app.routes.server.ts` wildcard set to `RenderMode.Client` (was `Prerender`) in case SSR is re-enabled later.

## Assumptions (from plan.md)
- Backend base URL (host): `http://localhost:8080/darcom-backend/api`
- Backend context path: `darcom-backend` (single place to change: `nginx/default.conf` + `proxy.conf.json`)
- Public entry point: `http://localhost:3000` (nginx host port 3000; also `ng serve` port 3000)
- Angular only calls relative `/api` URLs (no absolute backend URL in code).

## Dev workflow notes (Phase 4)
- Route `/admin/users` (lazy, `authGuard + adminGuard`; non-admin → `/home`, logged-out → `/login`).
- `AdminService.list` maps to `GET /api/admin/users` (`page` 0-based, `size=10`,
  `sort=field,dir` with fields `createdAt|email|fullName|role`, optional `role|verified|blocked|search`).
  Block → `PATCH /api/admin/users/{id}/block {reason}` (reason required, max 500);
  unblock → `PATCH .../unblock`.
- UI is a plain-CSS port of `Users-List-Datatable.txt` (no shadcn/radix/Tailwind deps added):
  card + toolbar (search w/ 400ms debounce, role/verified/status filters),
  checkbox selection + select-all, sortable User/Role/Joined headers,
  avatar initials, role/verified/status pills, actions menu (Block with inline
  reason form / Unblock), pagination ("Showing X–Y of Z").
- Mock-only columns from the txt (`Last Login`, `Two-Step`, Edit/View/Delete,
  Unsplash avatars) were dropped: the API provides no such fields; actions map
  to the real block/unblock endpoints. `ADMIN` rows cannot be blocked (backend 409).
- Home shows a "Manage users" link when `user.role === 'ADMIN'`.
- `npm start` = `ng serve --port 3000 --proxy-config proxy.conf.json`
- `npm start` and the nginx container both use host port 3000 → cannot run at the same time. Stop one before starting the other (`docker compose down` or stop `ng serve`).

## Admin users page (added on request, was out of scope in plan.md)
- Route `/admin/users` (lazy, `authGuard + adminGuard`; non-admin → `/home`, logged-out → `/login`).
- `AdminService.list` maps to `GET /api/admin/users` (`page` 0-based, `size=10`,
  `sort=field,dir` with fields `createdAt|email|fullName|role`, optional `role|verified|blocked|search`).
  Block → `PATCH /api/admin/users/{id}/block {reason}` (reason required, max 500);
  unblock → `PATCH .../unblock`.
- UI is a plain-CSS port of `Users-List-Datatable.txt` (no shadcn/radix/Tailwind deps added):
  card + toolbar (search w/ 400ms debounce, role/verified/status filters),
  checkbox selection + select-all, sortable User/Role/Joined headers,
  avatar initials, role/verified/status pills, actions menu (Block with inline
  reason form / Unblock), pagination ("Showing X–Y of Z").
- Mock-only columns from the txt (`Last Login`, `Two-Step`, Edit/View/Delete,
  Unsplash avatars) were dropped: the API provides no such fields; actions map
  to the real block/unblock endpoints. `ADMIN` rows cannot be blocked (backend 409).
- Home shows a "Manage users" link when `user.role === 'ADMIN'`.

## Troubleshooting (Phase 7)
See plan.md Phase 7 table — copied here for convenience:
| Symptom | Likely cause / fix |
|---|---|
| `502 Bad Gateway` on `/api/...` | nginx cannot reach backend. Confirm `http://localhost:8080/darcom-backend/api/health` works in browser; check Windows Firewall on port 8080. |
| `404` on `/api/...` | Wrong context path in `proxy_pass` (and `proxy.conf.json`). Use actual deployed WAR name. |
| Blank page / `404` for `main-*.js` | Wrong `root` path: `<DIST_SUBPATH>` does not match real build output. |
| `/verify-email` returns nginx 404 on refresh | `try_files ... /index.html` missing or not inside `location /`. |
| `403` / CORS-style error from backend | Origin not allowed. Only `http://localhost:3000` and `http://localhost:5173` are in `CORS_ALLOWED_ORIGINS`. If port changes, update backend `.env` (`CORS_ALLOWED_ORIGINS`, `APP_PUBLIC_URL`). Backend must not be modified by this plan. |
| Port 3000 already in use | Stop `ng serve` or run `docker compose down`. |
| Config change not applied | `docker compose restart nginx` |
