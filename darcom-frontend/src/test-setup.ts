import { afterAll, afterEach, beforeAll } from 'vitest';
import { resetMockDb } from './mocks/db';
import { server } from './mocks/node';

beforeAll(() => {
  server.listen({
    onUnhandledRequest(request, print) {
      if (new URL(request.url).pathname.startsWith('/api')) {
        print.error();
      }
    },
  });
});

afterEach(() => {
  server.resetHandlers();
  resetMockDb();
});

afterAll(() => {
  server.close();
});
