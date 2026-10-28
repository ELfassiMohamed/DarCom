import type { AdminUserSummary, Role, SessionUser, User } from '../app/core/models';

/**
 * In-memory stand-in for the Jakarta EE backend. Seeded data is deterministic
 * so tests and manual development start from the same state.
 */

export const DEV_PASSWORD = 'DevPassword123!';

export interface MockUser {
  id: string;
  email: string;
  fullName: string;
  phone: string | null;
  role: Role;
  verified: boolean;
  blocked: boolean;
  createdAt: string;
  password: string;
  listingCount: number;
  reportCount: number;
}

export interface MockListing {
  id: string;
  host: MockUser;
  title: string;
  description: string;
  city: string;
  address: string;
  pricePerNight: number;
  currency: string;
  mealsIncluded: boolean;
  activities: string[];
  status: 'ACTIVE' | 'HIDDEN' | 'REMOVED';
  photos: MockListingPhoto[];
  createdAt: string;
  updatedAt: string | null;
}

export interface MockListingPhoto {
  id: string;
  url: string;
  sortOrder: number;
}

export interface MockUserDetail extends User {
  listingCount: number;
  reportCount: number;
}

interface TokenRecord {
  userId: string;
  expiresAt: number;
  used: boolean;
}

interface RefreshRecord {
  userId: string;
  expiresAt: number;
}

const SEED_USERS: readonly MockUser[] = [
  {
    id: '00000000-0000-4000-8000-000000000001',
    email: 'visitor@darcom.test',
    fullName: 'Val Visitor',
    phone: '+212600000001',
    role: 'VISITOR',
    verified: true,
    blocked: false,
    createdAt: '2026-01-15T09:00:00Z',
    password: DEV_PASSWORD,
    listingCount: 0,
    reportCount: 0,
  },
  {
    id: '00000000-0000-4000-8000-000000000002',
    email: 'host@darcom.test',
    fullName: 'Hana Host',
    phone: '+212600000002',
    role: 'HOST',
    verified: true,
    blocked: false,
    createdAt: '2026-02-01T10:30:00Z',
    password: DEV_PASSWORD,
    listingCount: 3,
    reportCount: 0,
  },
  {
    id: '00000000-0000-4000-8000-000000000003',
    email: 'admin@darcom.test',
    fullName: 'Adam Admin',
    phone: '+212600000003',
    role: 'ADMIN',
    verified: true,
    blocked: false,
    createdAt: '2026-01-01T08:00:00Z',
    password: DEV_PASSWORD,
    listingCount: 0,
    reportCount: 0,
  },
  {
    id: '00000000-0000-4000-8000-000000000004',
    email: 'blocked@darcom.test',
    fullName: 'Blake Blocked',
    phone: '+212600000004',
    role: 'VISITOR',
    verified: true,
    blocked: true,
    createdAt: '2026-03-10T12:00:00Z',
    password: DEV_PASSWORD,
    listingCount: 0,
    reportCount: 0,
  },
  {
    id: '00000000-0000-4000-8000-000000000005',
    email: 'unverified@darcom.test',
    fullName: 'Uma Unverified',
    phone: null,
    role: 'VISITOR',
    verified: false,
    blocked: false,
    createdAt: '2026-04-01T14:15:00Z',
    password: DEV_PASSWORD,
    listingCount: 0,
    reportCount: 0,
  },
  {
    id: '00000000-0000-4000-8000-000000000006',
    email: 'host2@darcom.test',
    fullName: 'Omar Host',
    phone: '+212600000006',
    role: 'HOST',
    verified: true,
    blocked: false,
    createdAt: '2026-02-15T09:45:00Z',
    password: DEV_PASSWORD,
    listingCount: 2,
    reportCount: 1,
  },
  {
    id: '00000000-0000-4000-8000-000000000007',
    email: 'host3@darcom.test',
    fullName: 'Sofia Stay',
    phone: '+212600000007',
    role: 'HOST',
    verified: false,
    blocked: false,
    createdAt: '2026-03-05T16:20:00Z',
    password: DEV_PASSWORD,
    listingCount: 1,
    reportCount: 0,
  },
  {
    id: '00000000-0000-4000-8000-000000000008',
    email: 'visitor2@darcom.test',
    fullName: 'Victor Visitor',
    phone: '+212600000008',
    role: 'VISITOR',
    verified: true,
    blocked: false,
    createdAt: '2026-03-20T11:00:00Z',
    password: DEV_PASSWORD,
    listingCount: 0,
    reportCount: 0,
  },
  {
    id: '00000000-0000-4000-8000-000000000009',
    email: 'visitor3@darcom.test',
    fullName: 'Nora Nomad',
    phone: '+212600000009',
    role: 'VISITOR',
    verified: true,
    blocked: false,
    createdAt: '2026-04-10T07:30:00Z',
    password: DEV_PASSWORD,
    listingCount: 0,
    reportCount: 0,
  },
  {
    id: '00000000-0000-4000-8000-000000000010',
    email: 'blockedhost@darcom.test',
    fullName: 'Bruno Blocked',
    phone: '+212600000010',
    role: 'HOST',
    verified: true,
    blocked: true,
    createdAt: '2026-04-15T13:10:00Z',
    password: DEV_PASSWORD,
    listingCount: 4,
    reportCount: 2,
  },
  {
    id: '00000000-0000-4000-8000-000000000011',
    email: 'host4@darcom.test',
    fullName: 'Leila Lodge',
    phone: '+212600000011',
    role: 'HOST',
    verified: true,
    blocked: false,
    createdAt: '2026-05-01T10:00:00Z',
    password: DEV_PASSWORD,
    listingCount: 5,
    reportCount: 0,
  },
  {
    id: '00000000-0000-4000-8000-000000000012',
    email: 'visitor4@darcom.test',
    fullName: 'Sam Sojourn',
    phone: null,
    role: 'VISITOR',
    verified: false,
    blocked: false,
    createdAt: '2026-05-10T18:45:00Z',
    password: DEV_PASSWORD,
    listingCount: 0,
    reportCount: 0,
  },
];

function seedUsers(): MockUser[] {
  return SEED_USERS.map((user) => ({ ...user }));
}

export class MockDatabase {
  users: MockUser[] = [];
  listings: MockListing[] = [];
  readonly refreshTokens = new Map<string, RefreshRecord>();
  readonly verificationTokens = new Map<string, TokenRecord>();
  readonly resetTokens = new Map<string, TokenRecord>();

  constructor() {
    this.reset();
  }

  reset(): void {
    this.users = seedUsers();
    this.listings = [];
    this.refreshTokens.clear();
    this.verificationTokens.clear();
    this.resetTokens.clear();
  }

  findUserById(id: string): MockUser | undefined {
    return this.users.find((user) => user.id === id);
  }

  findUserByEmail(email: string): MockUser | undefined {
    return this.users.find((user) => user.email === email);
  }

  addUser(user: MockUser): void {
    this.users.push(user);
  }

  updateUser(id: string, patch: Partial<MockUser>): MockUser | undefined {
    const user = this.findUserById(id);
    if (!user) {
      return undefined;
    }
    Object.assign(user, patch);
    return user;
  }

  issueVerificationToken(user: MockUser): string {
    return this.issueToken(this.verificationTokens, 'EMAIL_VERIFICATION', user, 24 * 60 * 60 * 1000);
  }

  issueResetToken(user: MockUser): string {
    return this.issueToken(this.resetTokens, 'PASSWORD_RESET', user, 60 * 60 * 1000);
  }

  verifyEmail(rawToken: string): MockUser | null {
    return this.consumeToken(this.verificationTokens, rawToken, (user) => {
      user.verified = true;
    });
  }

  resetPassword(rawToken: string, newPassword: string): MockUser | null {
    return this.consumeToken(this.resetTokens, rawToken, (user) => {
      user.password = newPassword;
      this.revokeRefreshTokens(user.id);
    });
  }

  saveRefreshToken(rawToken: string, userId: string, ttlSeconds: number): void {
    this.refreshTokens.set(rawToken, {
      userId,
      expiresAt: Date.now() + ttlSeconds * 1000,
    });
  }

  hasRefreshToken(rawToken: string, userId: string): boolean {
    const record = this.refreshTokens.get(rawToken);
    if (!record || record.userId !== userId) {
      return false;
    }
    if (record.expiresAt <= Date.now()) {
      this.refreshTokens.delete(rawToken);
      return false;
    }
    return true;
  }

  deleteRefreshToken(rawToken: string): void {
    this.refreshTokens.delete(rawToken);
  }

  revokeRefreshTokens(userId: string): void {
    for (const [token, record] of this.refreshTokens) {
      if (record.userId === userId) {
        this.refreshTokens.delete(token);
      }
    }
  }

  toSessionUser(user: MockUser): SessionUser {
    return {
      id: user.id,
      email: user.email,
      role: user.role,
      verified: user.verified,
    };
  }

  toUserDto(user: MockUser): User {
    return {
      id: user.id,
      email: user.email,
      fullName: user.fullName,
      phone: user.phone,
      role: user.role,
      verified: user.verified,
      blocked: user.blocked,
      createdAt: user.createdAt,
    };
  }

  toSummary(user: MockUser): AdminUserSummary {
    return {
      id: user.id,
      email: user.email,
      fullName: user.fullName,
      role: user.role,
      verified: user.verified,
      blocked: user.blocked,
      createdAt: user.createdAt,
    };
  }

  toDetail(user: MockUser): MockUserDetail {
    return {
      ...this.toUserDto(user),
      listingCount: user.listingCount,
      reportCount: user.reportCount,
    };
  }

  addListing(listing: MockListing): void {
    this.listings.push(listing);
  }

  findListingById(id: string): MockListing | undefined {
    return this.listings.find((l) => l.id === id);
  }

  updateListing(id: string, patch: Partial<MockListing>): MockListing | undefined {
    const listing = this.findListingById(id);
    if (!listing) {
      return undefined;
    }
    Object.assign(listing, patch);
    return listing;
  }

  toListingDto(listing: MockListing): ListingDetails {
    return {
      id: listing.id,
      host: {
        id: listing.host.id,
        fullName: listing.host.fullName,
      },
      title: listing.title,
      description: listing.description,
      city: listing.city,
      address: listing.address,
      pricePerNight: listing.pricePerNight,
      currency: listing.currency,
      mealsIncluded: listing.mealsIncluded,
      activities: listing.activities,
      status: listing.status,
      photos: listing.photos.map((p) => ({
        id: p.id,
        url: p.url,
        sortOrder: p.sortOrder,
      })),
      createdAt: listing.createdAt,
      updatedAt: listing.updatedAt,
    };
  }

  toListingSummary(listing: MockListing): Listing {
    return {
      id: listing.id,
      host: {
        id: listing.host.id,
        fullName: listing.host.fullName,
      },
      title: listing.title,
      description: listing.description,
      city: listing.city,
      pricePerNight: listing.pricePerNight,
      currency: listing.currency,
      mainPhotoUrl: listing.photos?.[0]?.url || null,
      hostName: null,
    };
  }

  private issueToken(
    store: Map<string, TokenRecord>,
    purpose: 'EMAIL_VERIFICATION' | 'PASSWORD_RESET',
    user: MockUser,
    ttlMs: number,
  ): string {
    for (const [token, record] of store) {
      if (record.userId === user.id) {
        store.delete(token);
      }
    }

    const rawToken = `mock-${purpose.toLowerCase()}-${crypto.randomUUID()}`;
    store.set(rawToken, {
      userId: user.id,
      expiresAt: Date.now() + ttlMs,
      used: false,
    });

    // No email is sent in mock mode. The token is logged so the verify/reset
    // pages can be exercised end-to-end from the console.
    console.info(`[MSW] ${purpose} token for ${user.email}: ${rawToken}`);
    return rawToken;
  }

  private consumeToken(
    store: Map<string, TokenRecord>,
    rawToken: string,
    apply: (user: MockUser) => void,
  ): MockUser | null {
    const record = store.get(rawToken);
    if (!record || record.used || record.expiresAt <= Date.now()) {
      return null;
    }

    const user = this.findUserById(record.userId);
    if (!user) {
      return null;
    }

    record.used = true;
    apply(user);
    return user;
  }
}

export const db = new MockDatabase();

export function resetMockDb(): void {
  db.reset();
}
