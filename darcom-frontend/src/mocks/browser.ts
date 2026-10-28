import { setupWorker } from 'msw/browser';
import { DEV_PASSWORD } from './db';
import { handlers } from './handlers';

export const worker = setupWorker(...handlers);

function warnOnUnmockedApi(request: Request, print: { warning: () => void }): void {
  const url = new URL(request.url);
  if (url.pathname.startsWith('/api')) {
    print.warning();
  }
}

export async function startMockApi(): Promise<void> {
  await worker.start({
    onUnhandledRequest: warnOnUnmockedApi,
    serviceWorker: {
      url: '/mockServiceWorker.js',
    },
  });

  console.info(
    [
      '[MSW] Mock API enabled.',
      'Seeded accounts (all use password "' + DEV_PASSWORD + '"):',
      '  visitor@darcom.test  (VISITOR, verified)',
      '  host@darcom.test     (HOST, verified)',
      '  admin@darcom.test    (ADMIN, verified)',
      '  blocked@darcom.test  (blocked)',
      '  unverified@darcom.test (not verified)',
    ].join('\n'),
  );
}
