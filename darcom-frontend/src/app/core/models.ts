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
  id: number;
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
  id: number;
  email: string;
  fullName: string;
  phone: string | null;
  role: Role;
  verified: boolean;
  blocked: boolean;
  createdAt: string;
}

export interface RegisteredUser {
  id: number;
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
