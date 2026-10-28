# DarCom Backend — API Documentation

Base URL: `http://localhost:8080/darcom-backend/api`
(`<context-path>` = deployed WAR name, e.g. `darcom-backend-0.0.1-SNAPSHOT` or `darcom` if renamed.)

- All JSON is UTF-8. IDs are UUIDs. Timestamps are ISO-8601 UTC (`2026-09-19T14:30:00Z`).
- Authenticated requests send: `Authorization: Bearer <accessToken>`
- Every 4xx/5xx body uses the error envelope:

```json
{
  "timestamp": "2026-10-12T13:00:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "email: must be a well-formed email address",
  "path": "/api/auth/register"
}
```

---

## 1. Auth

### `POST /auth/register` (public) → `201`

Creates a `HOST` or `VISITOR` account and sends a verification email (best-effort).
Email is normalized with `trim().toLowerCase()`.

Request:

```json
{
  "email": "host@example.com",
  "password": "Str0ng!Pass",
  "fullName": "Test Host",
  "role": "HOST"
}
```

Rules: valid email (max 255); password 8–72 bytes with ≥1 lowercase, ≥1 uppercase,
≥1 digit, ≥1 special char; `fullName` non-blank (max 255); `role` must be `HOST` or `VISITOR`
(anything else, including `ADMIN`, → `400`).

Success `201`:

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "email": "host@example.com",
  "fullName": "Test Host",
  "role": "HOST",
  "verified": false,
  "createdAt": "2026-10-12T13:00:00Z"
}
```

Errors: `400 VALIDATION_ERROR`, `409 EMAIL_ALREADY_EXISTS`.

---

### `POST /auth/login` (public) → `200`

Login works regardless of `verified`. Wrong password is checked **before** `blocked`.

Request:

```json
{
  "email": "host@example.com",
  "password": "Str0ng!Pass"
}
```

Success `200`:

```json
{
  "accessToken": "<jwt, 900s TTL>",
  "refreshToken": "<jwt, 7 days TTL>",
  "expiresIn": 900,
  "user": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "email": "host@example.com",
    "role": "HOST",
    "verified": false
  }
}
```

Errors: `400 VALIDATION_ERROR`, `401 INVALID_CREDENTIALS`, `403 ACCOUNT_BLOCKED`.

---

### `POST /auth/refresh` (public) → `200`

Returns a new access token only (no rotation).

Request:

```json
{ "refreshToken": "<refresh jwt>" }
```

Success `200`:

```json
{
  "accessToken": "<new jwt>",
  "expiresIn": 900
}
```

Errors: `400 VALIDATION_ERROR`, `401 INVALID_OR_EXPIRED_REFRESH_TOKEN`
(same code for bad signature, expired, unknown, or blocked-user tokens).

---

### `POST /auth/logout` (Bearer) → `204`

Revokes one refresh token, scoped to the caller. Idempotent (unknown token still → `204`).

Request:

```json
{ "refreshToken": "<refresh jwt>" }
```

Success: `204` with empty body.
Errors: `400 VALIDATION_ERROR`, `401 UNAUTHENTICATED`.

---

### `POST /auth/verify-email` (public) → `200`

Single-use token from the verification email (valid 24h by default).

Request:

```json
{ "token": "<raw token from email>" }
```

Success `200` (same shape as `GET /users/me`):

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "email": "host@example.com",
  "fullName": "Test Host",
  "phone": null,
  "role": "HOST",
  "verified": true,
  "blocked": false,
  "createdAt": "2026-10-12T13:00:00Z"
}
```

Errors: `400 VALIDATION_ERROR`, `400 INVALID_OR_EXPIRED_VERIFICATION_TOKEN`.

---

### `POST /auth/resend-verification` (public) → `204`

Sends a fresh verification email (invalidates the previous token).
Always `204` — unknown emails, already-verified and blocked accounts get the same response.

Request:

```json
{ "email": "host@example.com" }
```

Errors: `400 VALIDATION_ERROR`, `429 TOO_MANY_REQUESTS` (max 5/hour per address).

---

### `POST /auth/forgot-password` (public) → `204`

Sends a password-reset email (valid 60min by default, invalidates previous reset tokens).
Always `204` — unknown and blocked accounts get the same response.

Request:

```json
{ "email": "host@example.com" }
```

Errors: `400 VALIDATION_ERROR`, `429 TOO_MANY_REQUESTS` (max 5/hour per address).

---

### `POST /auth/reset-password` (public) → `204`

Sets a new password. The token is single-use and all refresh sessions are revoked.

Request:

```json
{
  "token": "<raw token from email>",
  "newPassword": "N3w!StrongPass"
}
```

`newPassword` follows the same policy as registration.
Success: `204` with empty body.
Errors: `400 VALIDATION_ERROR`, `400 INVALID_OR_EXPIRED_RESET_TOKEN`.

---

## 2. Profile (Bearer)

### `GET /users/me` → `200`

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "email": "host@example.com",
  "fullName": "Test Host",
  "phone": "+212600000000",
  "role": "HOST",
  "verified": true,
  "blocked": false,
  "createdAt": "2026-10-12T13:00:00Z"
}
```

Errors: `401 UNAUTHENTICATED`, `404 USER_NOT_FOUND`.

---

### `PUT /users/me` → `200`

PUT = replace: an omitted/`null` `phone` **clears** it. Email, password, role,
`verified` and `blocked` cannot be changed here.

Request:

```json
{
  "fullName": "New Name",
  "phone": "+212611223344"
}
```

Rules: `fullName` non-blank (max 255); `phone` optional, E.164 format
(`+[1-9]` followed by 6–14 digits, e.g. `+212600000000`).

Success: `200` with the updated user (same shape as above).
Errors: `400 VALIDATION_ERROR`, `401 UNAUTHENTICATED`, `404 USER_NOT_FOUND`.

---

## 3. Admin users (Bearer + `ADMIN` role)

Wrong role → `403 FORBIDDEN`. Missing/invalid token → `401 UNAUTHENTICATED`.

### `GET /admin/users` → `200`

Query params (all optional): `role` (`VISITOR|HOST|ADMIN`), `verified` (`true|false`),
`blocked` (`true|false`), `search` (matches email or fullName, case-insensitive),
`page` (0-indexed, default `0`), `size` (default `20`, clamped to max `100`),
`sort` (`field,dir`, default `createdAt,desc`).

Example: `GET /admin/users?role=HOST&blocked=false&search=ali&page=0&size=20&sort=email,asc`

Sort fields whitelist: `createdAt`, `email`, `fullName`, `role` with direction `asc|desc`.

Success `200`:

```json
{
  "items": [
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "email": "host@example.com",
      "fullName": "Test Host",
      "role": "HOST",
      "verified": true,
      "blocked": false,
      "createdAt": "2026-10-12T13:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
```

Errors: `400 VALIDATION_ERROR` (bad `role`, `page < 0`, `size < 1`, non-integer
`page`/`size`/`verified`/`blocked`, bad `sort`), `401`, `403 FORBIDDEN`.

---

### `GET /admin/users/{id}` → `200`

Same as the user object plus counts (`listingCount` = listings hosted,
`reportCount` = reports naming this host).

Success `200`:

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "email": "host@example.com",
  "fullName": "Test Host",
  "phone": null,
  "role": "HOST",
  "verified": true,
  "blocked": false,
  "createdAt": "2026-10-12T13:00:00Z",
  "listingCount": 3,
  "reportCount": 1
}
```

Errors: `401`, `403 FORBIDDEN`, `404 USER_NOT_FOUND`.

---

### `PATCH /admin/users/{id}/block` → `200`

One transaction: sets `blocked=true`, revokes all refresh tokens, and hides the
host's `ACTIVE` listings (→ `HIDDEN`). `reason` is required but only logged, not stored.

Request:

```json
{ "reason": "Repeated fake listings" }
```

Rules: `reason` non-blank (max 500).
Success: `200` with the blocked user (same shape as `GET /users/me`, `blocked:true`).
Errors: `400 VALIDATION_ERROR`, `401`, `403 FORBIDDEN`, `404 USER_NOT_FOUND`,
`409 ALREADY_BLOCKED`, `409 CANNOT_BLOCK_ADMIN` (ADMIN accounts can never be blocked).

---

### `PATCH /admin/users/{id}/unblock` → `200`

Sets `blocked=false`. Does **not** restore hidden listings. No body required.

Success: `200` with the unblocked user.
Errors: `401`, `403 FORBIDDEN`, `404 USER_NOT_FOUND`, `409 NOT_BLOCKED`.

---

## 4. Listings

### `POST /listings` (Bearer, role `HOST`) → `201`

Creates a new listing. The host must have a verified email.

Request:

```json
{
  "title": "Cozy riad near the medina",
  "description": "Two-bedroom riad, 10 min walk to the stadium.",
  "city": "Tetouan",
  "address": "12 Rue de la Kasbah",
  "pricePerNight": 450.00,
  "currency": "MAD",
  "mealsIncluded": true,
  "activities": ["Airport pickup", "Guided medina tour"]
}
```

Success `201`:

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "host": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "fullName": "Youssef El Amrani"
  },
  "title": "Cozy riad near the medina",
  "description": "Two-bedroom riad, 10 min walk to the stadium.",
  "city": "Tetouan",
  "address": "12 Rue de la Kasbah",
  "pricePerNight": 450.00,
  "currency": "MAD",
  "mealsIncluded": true,
  "activities": ["Airport pickup", "Guided medina tour"],
  "status": "ACTIVE",
  "photos": [],
  "createdAt": "2026-10-12T13:00:00Z",
  "updatedAt": null
}
```

Errors: `400 VALIDATION_ERROR`, `403 EMAIL_NOT_VERIFIED`, `401 UNAUTHENTICATED`.

---

### `GET /listings` (public) → `200`

Public listing search with optional filters.

Query params:
- `city` - filter by city name (partial match)
- `minPrice` - minimum price per night
- `maxPrice` - maximum price per night
- `mealsIncluded` - `true` or `false`
- `page` (default: `0`)
- `size` (default: `20`)
- `sort` (default: `createdAt,desc`)

Success `200`:

```json
{
  "items": [
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "title": "Cozy riad near the medina",
      "city": "Tetouan",
      "pricePerNight": 450.00,
      "currency": "MAD",
      "mainPhotoUrl": "photo123.jpg",
      "hostName": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
```

---

### `GET /listings/mine` (Bearer, role `HOST`) → `200`

Lists all listings belonging to the authenticated host, regardless of status.

Success `200`:

```json
{
  "items": [
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "title": "Cozy riad near the medina",
      "city": "Tetouan",
      "pricePerNight": 450.00,
      "currency": "MAD",
      "mainPhotoUrl": "photo123.jpg",
      "hostName": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
```

---

### `GET /listings/{id}` (public) → `200`

Returns full listing details including photos and host summary.

Success `200`:

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "host": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "fullName": "Youssef El Amrani"
  },
  "title": "Cozy riad near the medina",
  "description": "Two-bedroom riad, 10 min walk to the stadium.",
  "city": "Tetouan",
  "address": "12 Rue de the Kasbah",
  "pricePerNight": 450.00,
  "currency": "MAD",
  "mealsIncluded": true,
  "activities": ["Airport pickup", "Guided medina tour"],
  "status": "ACTIVE",
  "photos": [
    {
      "id": "photo-uuid-1",
      "url": "photo123.jpg",
      "sortOrder": 0
    }
  ],
  "createdAt": "2026-10-12T13:00:00Z",
  "updatedAt": "2026-10-12T13:00:00Z"
}
```

Errors: `404 LISTING_NOT_FOUND`.

---

### `PUT /listings/{id}` (Bearer, role `HOST`, owner) → `200`

Updates a listing. Full replace - all fields must be provided.

Request: (same as `POST /listings`)

Success `200`: (same as `GET /listings/{id}`)

Errors: `400 VALIDATION_ERROR`, `403 NOT_OWNER`, `404 LISTING_NOT_FOUND`.

---

### `DELETE /listings/{id}` (Bearer, role `HOST`, owner) → `204`

Soft deletes a listing by setting status to `REMOVED`.

Success: `204` with empty body.

Errors: `403 NOT_OWNER`, `404 LISTING_NOT_FOUND`, `409 CANNOT_DELETE_WITH_ACTIVE_BOOKINGS`.

---

### `POST /listings/{id}/photos` (Bearer, role `HOST`, owner) → `201`

Uploads a photo for a listing.

**Content-Type:** `multipart/form-data` with field name `file`

**File requirements:**
- Allowed types: JPEG, PNG, WEBP
- Max size: 5MB

Success `201`:

```json
{
  "id": "photo-uuid-1",
  "url": "new-photo-uuid.jpg",
  "sortOrder": 1
}
```

Errors: `400 INVALID_FILE_TYPE`, `403 NOT_OWNER`, `404 LISTING_NOT_FOUND`.

---

### `DELETE /listings/{id}/photos/{photoId}` (Bearer, role `HOST`, owner) → `204`

Deletes a photo from a listing.

Success: `204` with empty body.

Errors: `403 NOT_OWNER`, `404 LISTING_NOT_FOUND`.

---

## 5. Health

### `GET /health` (public) → `200`

Service health's status, No DB call, no auth.

```json
{ "status": "UP" }
```

---

## 6. Error codes reference

| HTTP | Code | Meaning |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Bean/JSON/sort/pagination validation failure, missing body |
| 401 | `UNAUTHENTICATED` | Missing, invalid or expired access token |
| 401 | `INVALID_CREDENTIALS` | Wrong email/password |
| 401 | `INVALID_OR_EXPIRED_REFRESH_TOKEN` | Refresh rejected (any reason) |
| 400 | `INVALID_OR_EXPIRED_VERIFICATION_TOKEN` | Verify-email token bad/used/expired |
| 400 | `INVALID_OR_EXPIRED_RESET_TOKEN` | Reset token bad/used/expired |
| 403 | `FORBIDDEN` | Authenticated but wrong role |
| 403 | `ACCOUNT_BLOCKED` | Blocked account (login, or Bearer use) |
| 404 | `NOT_FOUND` | Unknown path |
| 404 | `USER_NOT_FOUND` | Unknown user id |
| 409 | `CANNOT_DELETE_WITH_ACTIVE_BOOKINGS` | Cannot delete listing with pending/confirmed bookings |
| 404 | `LISTING_NOT_FOUND` | Listing not found or already removed |
| 403 | `NOT_OWNER` | User is not the owner of the resource |
| 403 | `EMAIL_NOT_VERIFIED` | User email is not verified (required for listing creation) |
| 400 | `INVALID_FILE_TYPE` | Photo file type not allowed (only JPEG, PNG, WEBP) |
| 413 | `FILE_TOO_LARGE` | Photo file exceeds 5MB limit |
| 405 | `METHOD_NOT_ALLOWED` | Wrong HTTP method |
| 409 | `EMAIL_ALREADY_EXISTS` | Register with a taken email |
| 409 | `ALREADY_BLOCKED` / `NOT_BLOCKED` | Redundant block/unblock |
| 409 | `CANNOT_BLOCK_ADMIN` | Block target is an ADMIN |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | Non-JSON body |
| 429 | `TOO_MANY_REQUESTS` | Email rate limit exceeded |
| 500 | `INTERNAL_ERROR` | Unexpected failure (e.g. SMTP down) |
