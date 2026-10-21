import { HttpResponse, http } from 'msw';
import { db } from '../db';
import { apiError, readJsonBody, requireUser, stringValue } from '../http';
import { isValidPhone } from '../validation';
import { apiPath } from './paths';

export const userHandlers = [
  http.get(apiPath('/users/me'), ({ request }) => {
    const auth = requireUser(request);
    if (!auth.ok) {
      return auth.response;
    }

    return HttpResponse.json(db.toUserDto(auth.user));
  }),

  http.put(apiPath('/users/me'), async ({ request }) => {
    const auth = requireUser(request);
    if (!auth.ok) {
      return auth.response;
    }

    const body = await readJsonBody(request);
    if (!body) {
      return apiError(400, 'VALIDATION_ERROR', 'Request body is required', request);
    }

    const fullName = stringValue(body, 'fullName')?.trim() ?? '';
    if (!fullName) {
      return apiError(400, 'VALIDATION_ERROR', 'fullName: must not be blank', request);
    }
    if (fullName.length > 255) {
      return apiError(
        400,
        'VALIDATION_ERROR',
        'fullName: size must be between 0 and 255',
        request,
      );
    }

    // PUT replaces the profile: an omitted/null phone clears it.
    const rawPhone = body['phone'];
    let phone: string | null = null;
    if (rawPhone !== undefined && rawPhone !== null) {
      if (typeof rawPhone !== 'string' || !isValidPhone(rawPhone)) {
        return apiError(
          400,
          'VALIDATION_ERROR',
          'phone: must be a valid E.164 phone number',
          request,
        );
      }
      phone = rawPhone;
    }

    const updated = db.updateUser(auth.user.id, { fullName, phone });
    if (!updated) {
      return apiError(404, 'USER_NOT_FOUND', 'User not found', request);
    }

    return HttpResponse.json(db.toUserDto(updated));
  }),
];
