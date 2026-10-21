# DarcomFrontend

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 21.2.0.

## Development server

To start a local development server against the Jakarta EE backend, run:

```bash
npm start
```

Open `http://localhost:4200/`. The dev server proxies `/api` to
`http://localhost:8080/darcom-backend` via `proxy.conf.json`.

Port `3000` is reserved for nginx, so the Angular dev servers do not use it.

To run the frontend **without the backend**, using the MSW mock API:

```bash
npm run start:mock
```

Open `http://localhost:4300/`. See
[`docs/MSW_INTEGRATION.md`](docs/MSW_INTEGRATION.md) for the integration
architecture, complete seed data, endpoint behavior, tests, and troubleshooting.
The shorter quick reference is in [`src/mocks/README.md`](src/mocks/README.md).

## Nginx and production builds

```bash
npm run build
```

This writes the real-backend build to `dist/darcom-frontend/browser`. Nginx
should serve that directory; it contains no MSW code and no
`mockServiceWorker.js`.

The mock build is deliberately written to a different directory:

```bash
npm run build:mock
# dist/darcom-frontend-mock/browser
```

Never point nginx at `dist/darcom-frontend-mock`. It is only for previewing the
mock version.

If you previously opened the mock build on the same origin (for example
`http://localhost:3000`), unregister its service worker once in DevTools:
**Application → Service Workers → Unregister**, then clear site data and reload.
Otherwise the old MSW worker can keep intercepting `/api` requests.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
