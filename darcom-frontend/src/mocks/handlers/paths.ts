/**
 * MSW resolves relative paths against `location.origin` in the browser and
 * against a default origin in Node tests. Using an explicit wildcard makes the
 * same handlers match both environments regardless of the port.
 */
export function apiPath(path: string): string {
  return `*/api${path}`;
}
