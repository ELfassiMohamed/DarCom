/** Validation helpers that mirror the rules documented in darcom-backend/API_DOCS.md. */

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_RE = /^\+[1-9]\d{6,14}$/;

export function normalizeEmail(email: string): string {
  return email.trim().toLowerCase();
}

export function isValidEmail(email: string): boolean {
  return email.length <= 255 && EMAIL_RE.test(email);
}

export function passwordPolicyError(password: string): string | null {
  if (password.length < 8 || password.length > 72) {
    return 'password: must be between 8 and 72 characters';
  }
  if (!/[a-z]/.test(password)) {
    return 'password: must contain a lowercase letter';
  }
  if (!/[A-Z]/.test(password)) {
    return 'password: must contain an uppercase letter';
  }
  if (!/[0-9]/.test(password)) {
    return 'password: must contain a digit';
  }
  if (!/[^A-Za-z0-9]/.test(password)) {
    return 'password: must contain a special character';
  }
  return null;
}

export function isValidPhone(phone: string): boolean {
  return PHONE_RE.test(phone);
}
