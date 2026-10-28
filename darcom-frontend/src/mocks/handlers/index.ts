import { adminHandlers } from './admin.handlers';
import { authHandlers } from './auth.handlers';
import { healthHandlers } from './health.handlers';
import { userHandlers } from './user.handlers';
import { listingHandlers } from './listing.handlers';

export const handlers = [
  ...authHandlers,
  ...userHandlers,
  ...adminHandlers,
  ...healthHandlers,
  ...listingHandlers,
];
