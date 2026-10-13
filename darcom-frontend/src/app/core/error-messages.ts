import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from './models';

function isApiError(value: unknown): value is ApiError {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const record = value as Record<string, unknown>;
  return (
    typeof record['error'] === 'string' &&
    typeof record['message'] === 'string' &&
    typeof record['status'] === 'number' &&
    typeof record['path'] === 'string' &&
    typeof record['timestamp'] === 'string'
  );
}

export function toUserMessage(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return 'Cannot reach the server.';
    }
    const body: unknown = err.error;
    if (isApiError(body)) {
      switch (body.error) {
        case 'INVALID_CREDENTIALS':
          return 'Wrong email or password.';
        case 'ACCOUNT_BLOCKED':
          return 'This account has been blocked.';
        case 'EMAIL_ALREADY_EXISTS':
          return 'An account with this email already exists.';
        case 'TOO_MANY_REQUESTS':
          return 'Too many attempts. Try again in an hour.';
        case 'INVALID_OR_EXPIRED_VERIFICATION_TOKEN':
          return 'This verification link is invalid or has expired.';
        case 'VALIDATION_ERROR':
          return body.message;
        default:
          return body.message;
      }
    }
    return 'Something went wrong.';
  }
  return 'Something went wrong.';
}
