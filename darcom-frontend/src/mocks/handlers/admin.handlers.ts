import { HttpResponse, http } from 'msw';
import type {
  AdminSortField,
  AdminUserSummary,
  PageResponse,
  Role,
  SortDir,
} from '../../app/core/models';
import { db, type MockUser } from '../db';
import { apiError, readJsonBody, requireUser, stringValue } from '../http';
import { apiPath } from './paths';

const DEFAULT_PAGE_SIZE = 20;
const MAX_PAGE_SIZE = 100;
const SORT_FIELDS: readonly AdminSortField[] = ['createdAt', 'email', 'fullName', 'role'];

interface ListQuery {
  role?: Role;
  verified?: boolean;
  blocked?: boolean;
  search?: string;
  page: number;
  size: number;
  sortField: AdminSortField;
  sortDir: SortDir;
}

function parseFlag(
  value: string | null,
  name: string,
  request: Request,
): { value: boolean | undefined } | { error: Response } {
  if (value === null || value.trim() === '') {
    return { value: undefined };
  }
  const normalized = value.trim().toLowerCase();
  if (normalized === 'true') {
    return { value: true };
  }
  if (normalized === 'false') {
    return { value: false };
  }
  return {
    error: apiError(
      400,
      'VALIDATION_ERROR',
      `${name}: must be true or false`,
      request,
    ),
  };
}

function parseInteger(
  value: string | null,
  name: string,
  fallback: number,
  min: number,
  request: Request,
): { value: number } | { error: Response } {
  if (value === null || value.trim() === '') {
    return { value: fallback };
  }
  if (!/^-?\d+$/.test(value.trim())) {
    return {
      error: apiError(400, 'VALIDATION_ERROR', `Invalid ${name}: must be an integer`, request),
    };
  }
  const parsed = Number.parseInt(value, 10);
  if (parsed < min) {
    return {
      error: apiError(
        400,
        'VALIDATION_ERROR',
        `${name} must be >= ${min}`,
        request,
      ),
    };
  }
  return { value: parsed };
}

function parseListQuery(url: URL, request: Request): { query: ListQuery } | { error: Response } {
  const params = url.searchParams;

  const rawRole = params.get('role');
  let role: Role | undefined;
  if (rawRole !== null && rawRole.trim() !== '') {
    const normalizedRole = rawRole.trim().toUpperCase();
    if (normalizedRole !== 'VISITOR' && normalizedRole !== 'HOST' && normalizedRole !== 'ADMIN') {
      return {
        error: apiError(
          400,
          'VALIDATION_ERROR',
          'Invalid role. Allowed values: VISITOR, HOST, ADMIN',
          request,
        ),
      };
    }
    role = normalizedRole;
  }

  const verified = parseFlag(params.get('verified'), 'verified', request);
  if ('error' in verified) {
    return { error: verified.error };
  }
  const blocked = parseFlag(params.get('blocked'), 'blocked', request);
  if ('error' in blocked) {
    return { error: blocked.error };
  }

  const page = parseInteger(params.get('page'), 'page', 0, 0, request);
  if ('error' in page) {
    return { error: page.error };
  }
  const size = parseInteger(params.get('size'), 'size', DEFAULT_PAGE_SIZE, 1, request);
  if ('error' in size) {
    return { error: size.error };
  }

  const rawSort = params.get('sort');
  let sortField: AdminSortField = 'createdAt';
  let sortDir: SortDir = 'desc';
  if (rawSort !== null && rawSort.trim() !== '') {
    const [field, rawDirection, ...rest] = rawSort.split(',');
    const direction = rawDirection?.trim().toLowerCase();
    if (
      rest.length > 0 ||
      !SORT_FIELDS.includes(field as AdminSortField) ||
      (direction !== 'asc' && direction !== 'desc')
    ) {
      return {
        error: apiError(
          400,
          'VALIDATION_ERROR',
          'Invalid sort. Allowed fields: createdAt, email, fullName, role with direction asc|desc',
          request,
        ),
      };
    }
    sortField = field as AdminSortField;
    sortDir = direction;
  }

  const search = params.get('search')?.trim();

  return {
    query: {
      ...(role ? { role } : {}),
      ...(verified.value !== undefined ? { verified: verified.value } : {}),
      ...(blocked.value !== undefined ? { blocked: blocked.value } : {}),
      ...(search ? { search } : {}),
      page: page.value,
      size: Math.min(size.value, MAX_PAGE_SIZE),
      sortField,
      sortDir,
    },
  };
}

function sortValue(user: MockUser, field: AdminSortField): string {
  return field === 'role' ? user.role : user[field];
}

export const adminHandlers = [
  http.get(apiPath('/admin/users'), ({ request }) => {
    const auth = requireUser(request, ['ADMIN']);
    if (!auth.ok) {
      return auth.response;
    }

    const parsed = parseListQuery(new URL(request.url), request);
    if ('error' in parsed) {
      return parsed.error;
    }
    const { query } = parsed;
    const search = query.search?.toLowerCase();

    const filtered = db.users.filter((user) => {
      if (query.role && user.role !== query.role) {
        return false;
      }
      if (query.verified !== undefined && user.verified !== query.verified) {
        return false;
      }
      if (query.blocked !== undefined && user.blocked !== query.blocked) {
        return false;
      }
      if (
        search &&
        !user.email.toLowerCase().includes(search) &&
        !user.fullName.toLowerCase().includes(search)
      ) {
        return false;
      }
      return true;
    });

    filtered.sort((a, b) => {
      const left = sortValue(a, query.sortField);
      const right = sortValue(b, query.sortField);
      const comparison = left < right ? -1 : left > right ? 1 : 0;
      return query.sortDir === 'asc' ? comparison : -comparison;
    });

    const start = query.page * query.size;
    const items = filtered.slice(start, start + query.size);
    const response: PageResponse<AdminUserSummary> = {
      items: items.map((user) => db.toSummary(user)),
      page: query.page,
      size: query.size,
      totalItems: filtered.length,
      totalPages: Math.ceil(filtered.length / query.size),
    };

    return HttpResponse.json(response);
  }),

  http.get(apiPath('/admin/users/:id'), ({ request, params }) => {
    const auth = requireUser(request, ['ADMIN']);
    if (!auth.ok) {
      return auth.response;
    }

    const user = db.findUserById(String(params['id']));
    if (!user) {
      return apiError(404, 'USER_NOT_FOUND', 'User not found', request);
    }

    return HttpResponse.json(db.toDetail(user));
  }),

  http.patch(apiPath('/admin/users/:id/block'), async ({ request, params }) => {
    const auth = requireUser(request, ['ADMIN']);
    if (!auth.ok) {
      return auth.response;
    }

    const body = await readJsonBody(request);
    if (!body) {
      return apiError(400, 'VALIDATION_ERROR', 'Request body is required', request);
    }
    const reason = stringValue(body, 'reason')?.trim() ?? '';
    if (!reason) {
      return apiError(400, 'VALIDATION_ERROR', 'reason: must not be blank', request);
    }
    if (reason.length > 500) {
      return apiError(
        400,
        'VALIDATION_ERROR',
        'reason: size must be between 0 and 500',
        request,
      );
    }

    const target = db.findUserById(String(params['id']));
    if (!target) {
      return apiError(404, 'USER_NOT_FOUND', 'User not found', request);
    }
    if (target.role === 'ADMIN') {
      return apiError(409, 'CANNOT_BLOCK_ADMIN', 'Admin accounts cannot be blocked', request);
    }
    if (target.blocked) {
      return apiError(409, 'ALREADY_BLOCKED', 'User is already blocked', request);
    }

    db.updateUser(target.id, { blocked: true });
    db.revokeRefreshTokens(target.id);
    return HttpResponse.json(db.toUserDto(target));
  }),

  http.patch(apiPath('/admin/users/:id/unblock'), ({ request, params }) => {
    const auth = requireUser(request, ['ADMIN']);
    if (!auth.ok) {
      return auth.response;
    }

    const target = db.findUserById(String(params['id']));
    if (!target) {
      return apiError(404, 'USER_NOT_FOUND', 'User not found', request);
    }
    if (!target.blocked) {
      return apiError(409, 'NOT_BLOCKED', 'User is not blocked', request);
    }

    db.updateUser(target.id, { blocked: false });
    return HttpResponse.json(db.toUserDto(target));
  }),
];
