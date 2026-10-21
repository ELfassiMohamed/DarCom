export type Role = 'VISITOR' | 'HOST' | 'ADMIN';

export type RegisterRole = 'VISITOR' | 'HOST';

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
  role: RegisterRole;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface SessionUser {
  id: string;
  email: string;
  role: Role;
  verified: boolean;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: SessionUser;
}

export interface RefreshResponse {
  accessToken: string;
  expiresIn: number;
}

export interface User {
  id: string;
  email: string;
  fullName: string;
  phone: string | null;
  role: Role;
  verified: boolean;
  blocked: boolean;
  createdAt: string;
}

export interface RegisteredUser {
  id: string;
  email: string;
  fullName: string;
  role: Role;
  verified: boolean;
  createdAt: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

export interface AdminUserSummary {
  id: string;
  email: string;
  fullName: string;
  role: Role;
  verified: boolean;
  blocked: boolean;
  createdAt: string;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export type AdminSortField = 'createdAt' | 'email' | 'fullName' | 'role';

export type SortDir = 'asc' | 'desc';

export interface AdminUserQuery {
  page: number;
  size: number;
  sortField: AdminSortField;
  sortDir: SortDir;
  search?: string;
  role?: Role;
  verified?: boolean;
  blocked?: boolean;
}
