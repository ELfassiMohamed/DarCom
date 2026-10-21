import { HttpResponse, http } from 'msw';
import type { Role } from '../../app/core/models';
import {
  ACCESS_TTL_SECONDS,
  REFRESH_TTL_SECONDS,
  createAccessToken,
  createRefreshToken,
  parseToken,
} from '../jwt';
import { db, type MockUser } from '../db';
import { apiError, noContent, readJsonBody, requireUser, stringValue } from '../http';
import { isValidEmail, normalizeEmail, passwordPolicyError } from '../validation';
import { apiPath } from './paths';

function validationError(message: string, request: Request): Response {
  return apiError(400, 'VALIDATION_ERROR', message, request);
}

function invalidRefresh(request: Request): Response {
  return apiError(
    401,
    'INVALID_OR_EXPIRED_REFRESH_TOKEN',
    'Invalid or expired refresh token',
    request,
  );
}

function toRegisterResponse(user: MockUser) {
  return {
    id: user.id,
    email: user.email,
    fullName: user.fullName,
    role: user.role,
    verified: user.verified,
    createdAt: user.createdAt,
  };
}

export const authHandlers = [
  http.post(apiPath('/auth/register'), async ({ request }) => {
    const body = await readJsonBody(request);
    if (!body) {
      return validationError('Request body is required', request);
    }

    const rawEmail = stringValue(body, 'email');
    const password = stringValue(body, 'password');
    const rawFullName = stringValue(body, 'fullName');
    const role = stringValue(body, 'role');

    if (!rawEmail) {
      return validationError('email: must not be blank', request);
    }
    const email = normalizeEmail(rawEmail);
    if (!isValidEmail(email)) {
      return validationError('email: must be a well-formed email address', request);
    }
    const fullName = rawFullName?.trim() ?? '';
    if (!fullName) {
      return validationError('fullName: must not be blank', request);
    }
    if (fullName.length > 255) {
      return validationError('fullName: size must be between 0 and 255', request);
    }
    if (!password) {
      return validationError('password: must not be blank', request);
    }
    const policyError = passwordPolicyError(password);
    if (policyError) {
      return validationError(policyError, request);
    }
    if (role !== 'VISITOR' && role !== 'HOST') {
      return validationError('role: must be VISITOR or HOST', request);
    }

    if (db.findUserByEmail(email)) {
      return apiError(409, 'EMAIL_ALREADY_EXISTS', 'Email is already registered', request);
    }

    const user: MockUser = {
      id: crypto.randomUUID(),
      email,
      fullName,
      phone: null,
      role: role as Role,
      verified: false,
      blocked: false,
      createdAt: new Date().toISOString(),
      password,
      listingCount: 0,
      reportCount: 0,
    };
    db.addUser(user);
    db.issueVerificationToken(user);

    return HttpResponse.json(toRegisterResponse(user), { status: 201 });
  }),

  http.post(apiPath('/auth/login'), async ({ request }) => {
    const body = await readJsonBody(request);
    if (!body) {
      return validationError('Request body is required', request);
    }

    const rawEmail = stringValue(body, 'email');
    const password = stringValue(body, 'password');
    if (!rawEmail || !password) {
      return validationError('email and password are required', request);
    }

    const user = db.findUserByEmail(normalizeEmail(rawEmail));

    // The backend checks the password before checking blocked status.
    if (!user || user.password !== password) {
      return apiError(401, 'INVALID_CREDENTIALS', 'Invalid email or password', request);
    }
    if (user.blocked) {
      return apiError(403, 'ACCOUNT_BLOCKED', 'Account is blocked', request);
    }

    const accessToken = createAccessToken(user.id, user.role);
    const refreshToken = createRefreshToken(user.id);
    db.saveRefreshToken(refreshToken, user.id, REFRESH_TTL_SECONDS);

    return HttpResponse.json({
      accessToken,
      refreshToken,
      expiresIn: ACCESS_TTL_SECONDS,
      user: db.toSessionUser(user),
    });
  }),

  http.post(apiPath('/auth/refresh'), async ({ request }) => {
    const body = await readJsonBody(request);
    if (!body) {
      return validationError('Request body is required', request);
    }

    const refreshToken = stringValue(body, 'refreshToken');
    if (!refreshToken) {
      return validationError('refreshToken: must not be blank', request);
    }

    const payload = parseToken(refreshToken);
    if (!payload || payload.typ !== 'refresh') {
      return invalidRefresh(request);
    }

    const user = db.findUserById(payload.sub);
    if (!user || user.blocked || !db.hasRefreshToken(refreshToken, user.id)) {
      return invalidRefresh(request);
    }

    return HttpResponse.json({
      accessToken: createAccessToken(user.id, user.role),
      expiresIn: ACCESS_TTL_SECONDS,
    });
  }),

  http.post(apiPath('/auth/logout'), async ({ request }) => {
    const auth = requireUser(request);
    if (!auth.ok) {
      return auth.response;
    }

    const body = await readJsonBody(request);
    if (!body) {
      return validationError('Request body is required', request);
    }

    const refreshToken = stringValue(body, 'refreshToken');
    if (!refreshToken) {
      return validationError('refreshToken: must not be blank', request);
    }

    // Idempotent and scoped to the caller: unknown or foreign tokens are still 204.
    if (db.hasRefreshToken(refreshToken, auth.user.id)) {
      db.deleteRefreshToken(refreshToken);
    }
    return noContent();
  }),

  http.post(apiPath('/auth/verify-email'), async ({ request }) => {
    const body = await readJsonBody(request);
    if (!body) {
      return validationError('Request body is required', request);
    }

    const token = stringValue(body, 'token');
    if (!token) {
      return validationError('token: must not be blank', request);
    }

    const user = db.verifyEmail(token);
    if (!user) {
      return apiError(
        400,
        'INVALID_OR_EXPIRED_VERIFICATION_TOKEN',
        'Invalid or expired verification token',
        request,
      );
    }

    return HttpResponse.json(db.toUserDto(user));
  }),

  http.post(apiPath('/auth/resend-verification'), async ({ request }) => {
    const body = await readJsonBody(request);
    if (!body) {
      return validationError('Request body is required', request);
    }

    const rawEmail = stringValue(body, 'email');
    if (!rawEmail || !isValidEmail(normalizeEmail(rawEmail))) {
      return validationError('email: must be a well-formed email address', request);
    }

    const user = db.findUserByEmail(normalizeEmail(rawEmail));
    if (user && !user.verified && !user.blocked) {
      db.issueVerificationToken(user);
    }

    // Always 204, even for unknown/already-verified addresses.
    return noContent();
  }),

  http.post(apiPath('/auth/forgot-password'), async ({ request }) => {
    const body = await readJsonBody(request);
    if (!body) {
      return validationError('Request body is required', request);
    }

    const rawEmail = stringValue(body, 'email');
    if (!rawEmail || !isValidEmail(normalizeEmail(rawEmail))) {
      return validationError('email: must be a well-formed email address', request);
    }

    const user = db.findUserByEmail(normalizeEmail(rawEmail));
    if (user && !user.blocked) {
      db.issueResetToken(user);
    }

    // Always 204, even for unknown/blocked addresses.
    return noContent();
  }),

  http.post(apiPath('/auth/reset-password'), async ({ request }) => {
    const body = await readJsonBody(request);
    if (!body) {
      return validationError('Request body is required', request);
    }

    const token = stringValue(body, 'token');
    const newPassword = stringValue(body, 'newPassword');
    if (!token) {
      return validationError('token: must not be blank', request);
    }
    if (!newPassword) {
      return validationError('newPassword: must not be blank', request);
    }
    const policyError = passwordPolicyError(newPassword);
    if (policyError) {
      return validationError(policyError, request);
    }

    const user = db.resetPassword(token, newPassword);
    if (!user) {
      return apiError(
        400,
        'INVALID_OR_EXPIRED_RESET_TOKEN',
        'Invalid or expired reset token',
        request,
      );
    }

    return noContent();
  }),
];
