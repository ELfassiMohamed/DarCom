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

/** PUT /users/me replaces the profile; `phone: null` clears it. */
export interface UpdateProfileRequest {
  fullName: string;
  phone: string | null;
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

export interface ListingPhoto {
  id: string;
  url: string;
  sortOrder: number;
}

export interface ListingHost {
  id: string;
  fullName: string;
}

export interface Listing {
  id: string;
  host: ListingHost;
  title: string;
  description: string;
  city: string;
  address: string;
  pricePerNight: number;
  currency: string;
  mealsIncluded: boolean;
  activities: string[];
  status: 'ACTIVE' | 'HIDDEN' | 'REMOVED';
  photos: ListingPhoto[];
  createdAt: string;
  updatedAt: string | null;
}

export interface ListingQuery {
  city?: string;
  minPrice?: number;
  maxPrice?: number;
  mealsIncluded?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

export interface ListingPageResponse {
  items: Listing[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface ListingDetails {
  id: string;
  host: ListingHost;
  title: string;
  description: string;
  city: string;
  address: string;
  pricePerNight: number;
  currency: string;
  mealsIncluded: boolean;
  activities: string[];
  status: 'ACTIVE' | 'HIDDEN' | 'REMOVED';
  photos: ListingPhoto[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateListingRequest {
  title: string;
  description: string;
  city: string;
  address: string;
  pricePerNight: number;
  currency: string;
  mealsIncluded: boolean;
  activities: string[];
}

export interface UpdateListingRequest {
  title: string;
  description: string;
  city: string;
  address: string;
  pricePerNight: number;
  currency: string;
  mealsIncluded: boolean;
  activities: string[];
  status?: 'ACTIVE' | 'HIDDEN' | 'REMOVED';
}

export interface AdminListingQuery {
  role?: Role;
  verified?: boolean;
  blocked?: boolean;
  search?: string;
  page: number;
  size: number;
  sortField: AdminSortField;
  sortDir: SortDir;
}
