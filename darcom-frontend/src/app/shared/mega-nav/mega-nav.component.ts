import {
  Component,
  ElementRef,
  HostListener,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter, map } from 'rxjs';
import { AuthService } from '../../core/auth.service';

interface MegaLink {
  label: string;
  description?: string;
  link: string;
  queryParams?: Record<string, string>;
  badge?: string;
}

interface MegaColumn {
  eyebrow: string;
  links: MegaLink[];
}

interface MegaMenu {
  columns: MegaColumn[];
  image: { src: string; alt: string };
  imageCaption?: string;
}

interface NavEntry {
  id: string;
  label: string;
  link?: string;
  /** Prefix used to highlight a dropdown trigger when one of its pages is open. */
  activePrefix?: string;
  menu?: MegaMenu;
}

/**
 * Admin space: user dashboard, reports and logs. Logout lives on the right side.
 * Edit this array to add / remove admin destinations.
 */
const ADMIN_NAV: NavEntry[] = [
  {
    id: 'dashboard',
    label: 'User dashboard',
    activePrefix: '/admin/users',
    menu: {
      columns: [
        {
          eyebrow: 'Manage',
          links: [
            {
              label: 'All users',
              description: 'Search, filter and sort accounts',
              link: '/admin/users',
            },
          ],
        },
        {
          eyebrow: 'Moderation',
          links: [
            {
              label: 'Blocked users',
              description: 'Review blocked accounts',
              link: '/admin/users',
              queryParams: { blocked: 'true' },
            },
            {
              label: 'Unverified',
              description: 'Pending email verification',
              link: '/admin/users',
              queryParams: { verified: 'false' },
            },
          ],
        },
      ],
      image: {
        src: 'https://images.unsplash.com/photo-1551288049-bebda4e38f71?auto=format&fit=crop&w=600&q=80',
        alt: 'Admin dashboard preview',
      },
      imageCaption: 'Manage accounts and monitor platform activity',
    },
  },
  { id: 'reports', label: 'Reports', link: '/admin/reports' },
  { id: 'logs', label: 'Logs', link: '/admin/logs' },
];

/** User space: home only; the account chip links to the profile page. */
const USER_NAV: NavEntry[] = [{ id: 'home', label: 'Home', link: '/home' }];

/**
 * Authenticated user-space navigation, ported from `mega-nav-menu.txt`.
 *
 * The React original relied on Next.js, framer-motion and react-icons; this
 * Angular version keeps the rounded floating bar, the mega dropdown with an
 * image preview and the mobile panel, using only Angular router + plain CSS.
 * Menu entries are role-aware: admins get the dashboard / reports / logs
 * workspace, everyone else gets home, with profile behind the account chip.
 */
@Component({
  selector: 'app-mega-nav',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="meganav">
      <div class="bar">
        <div class="shell">
          <div class="top">
            <a class="brand" [routerLink]="homeLink()" (click)="closeAll()" aria-label="DarCom home">
              Dar<span>Com</span>
            </a>

            <nav class="desktop" aria-label="User space">
              @for (entry of nav(); track entry.id) {
                @if (entry.menu) {
                  <div class="nav-item">
                    <button
                      type="button"
                      class="nav-btn"
                      [class.active]="openMenu() === entry.id || isEntryActive(entry)"
                      [attr.aria-expanded]="openMenu() === entry.id"
                      (click)="toggle(entry.id)"
                    >
                      <span>{{ entry.label }}</span>
                      <svg
                        class="chev"
                        [class.rot]="openMenu() === entry.id"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                        aria-hidden="true"
                      >
                        <path d="m6 9 6 6 6-6" />
                      </svg>
                    </button>
                  </div>
                } @else {
                  <a
                    class="nav-link"
                    [routerLink]="entry.link"
                    routerLinkActive="active"
                    (click)="closeAll()"
                  >
                    {{ entry.label }}
                  </a>
                }
              }
            </nav>

            <div class="actions">
              @if (currentUser()) {
                <a
                  class="who"
                  routerLink="/profile"
                  (click)="closeAll()"
                  aria-label="Profile settings"
                >
                  <span class="avatar" aria-hidden="true">{{ initials() }}</span>
                  <span class="who-name">{{ displayName() }}</span>
                </a>
              }
              <button type="button" class="logout" (click)="onLogout()" [disabled]="loggingOut()">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                  <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" />
                  <path d="M16 17l5-5-5-5" />
                  <path d="M21 12H9" />
                </svg>
                <span>{{ loggingOut() ? 'Signing out…' : 'Logout' }}</span>
              </button>
            </div>

            <button
              type="button"
              class="burger"
              (click)="toggleMobile()"
              [attr.aria-expanded]="mobileOpen()"
              aria-label="Toggle navigation"
            >
              @if (mobileOpen()) {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                  <path d="M18 6 6 18M6 6l12 12" />
                </svg>
              } @else {
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                  <path d="M4 7h16M4 12h16M4 17h16" />
                </svg>
              }
            </button>
          </div>

          @if (activeMenu(); as menu) {
            <div class="mega">
              <div class="mega-grid">
                @for (col of menu.columns; track col.eyebrow) {
                  <div class="mega-col">
                    <span class="eyebrow">{{ col.eyebrow }}</span>
                    <ul>
                      @for (item of col.links; track item.label) {
                        <li>
                          <a
                            class="mega-link"
                            [routerLink]="item.link"
                            [queryParams]="item.queryParams"
                            routerLinkActive="sel"
                            [routerLinkActiveOptions]="{ exact: true }"
                            (click)="closeAll()"
                          >
                            <span class="mega-label">
                              {{ item.label }}
                              @if (item.badge) {
                                <span class="badge">{{ item.badge }}</span>
                              }
                            </span>
                            @if (item.description) {
                              <span class="mega-desc">{{ item.description }}</span>
                            }
                          </a>
                        </li>
                      }
                    </ul>
                  </div>
                }

                <div class="preview">
                  <img [src]="menu.image.src" [alt]="menu.image.alt" loading="lazy" />
                  @if (menu.imageCaption) {
                    <p class="caption">{{ menu.imageCaption }}</p>
                  }
                </div>
              </div>
            </div>
          }

          @if (mobileOpen()) {
            <div class="mobile">
              @for (entry of nav(); track entry.id) {
                @if (entry.menu; as menu) {
                  <button type="button" class="m-link m-btn" (click)="toggle(entry.id)">
                    <span>{{ entry.label }}</span>
                    <svg
                      class="chev"
                      [class.rot]="openMenu() === entry.id"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                      aria-hidden="true"
                    >
                      <path d="m6 9 6 6 6-6" />
                    </svg>
                  </button>
                  @if (openMenu() === entry.id) {
                    <div class="m-sub">
                      @for (col of menu.columns; track col.eyebrow) {
                        <span class="m-eyebrow">{{ col.eyebrow }}</span>
                        @for (item of col.links; track item.label) {
                          <a
                            class="m-sublink"
                            [routerLink]="item.link"
                            [queryParams]="item.queryParams"
                            (click)="closeAll()"
                          >
                            {{ item.label }}
                            @if (item.badge) {
                              <span class="badge">{{ item.badge }}</span>
                            }
                          </a>
                        }
                      }
                    </div>
                  }
                } @else {
                  <a
                    class="m-link"
                    [routerLink]="entry.link"
                    routerLinkActive="active"
                    (click)="closeAll()"
                  >
                    {{ entry.label }}
                  </a>
                }
              }

              <div class="m-account">
                @if (currentUser()) {
                  <a
                    class="m-who"
                    routerLink="/profile"
                    (click)="closeAll()"
                    aria-label="Profile settings"
                  >
                    <span class="avatar" aria-hidden="true">{{ initials() }}</span>
                    <span class="who-name">{{ displayName() }}</span>
                  </a>
                }
                <button type="button" class="logout full" (click)="onLogout()" [disabled]="loggingOut()">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" />
                    <path d="M16 17l5-5-5-5" />
                    <path d="M21 12H9" />
                  </svg>
                  <span>{{ loggingOut() ? 'Signing out…' : 'Logout' }}</span>
                </button>
              </div>
            </div>
          }
        </div>
      </div>
    </header>
  `,
  styleUrls: ['./mega-nav.css'],
})
export class MegaNavComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly currentUser = this.auth.currentUser;
  readonly openMenu = signal<string | null>(null);
  readonly mobileOpen = signal(false);
  readonly loggingOut = signal(false);

  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map((event) => event.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  readonly isAdmin = computed(() => this.currentUser()?.role === 'ADMIN');
  readonly nav = computed<NavEntry[]>(() => (this.isAdmin() ? ADMIN_NAV : USER_NAV));
  readonly homeLink = computed(() => (this.isAdmin() ? '/admin/users' : '/home'));
  readonly activeMenu = computed(
    () => this.nav().find((entry) => entry.id === this.openMenu())?.menu ?? null,
  );

  /** Prefer the profile's full name, fall back to the email local part while loading. */
  readonly displayName = computed(() => {
    const fullName = this.auth.profile()?.fullName?.trim();
    if (fullName) {
      return fullName;
    }
    const localPart = this.currentUser()?.email.split('@')[0]?.trim();
    return localPart || 'Account';
  });

  readonly initials = computed(() => {
    const parts = this.displayName().split(/\s+/).filter(Boolean);
    if (parts.length >= 2) {
      return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
    }
    return this.displayName().slice(0, 2).toUpperCase() || '?';
  });

  ngOnInit(): void {
    if (this.currentUser()) {
      this.auth.loadProfile().subscribe({ error: () => undefined });
    }
  }

  isEntryActive(entry: NavEntry): boolean {
    return entry.activePrefix !== undefined && this.currentUrl().startsWith(entry.activePrefix);
  }

  toggle(id: string): void {
    this.openMenu.update((current) => (current === id ? null : id));
  }

  toggleMobile(): void {
    this.mobileOpen.update((open) => !open);
    if (!this.mobileOpen()) {
      this.openMenu.set(null);
    }
  }

  closeAll(): void {
    this.openMenu.set(null);
    this.mobileOpen.set(false);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.host.nativeElement.contains(event.target as Node)) {
      this.closeAll();
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.closeAll();
  }

  onLogout(): void {
    if (this.loggingOut()) {
      return;
    }
    this.loggingOut.set(true);
    this.auth.logout().subscribe({
      next: () => {
        this.loggingOut.set(false);
        this.closeAll();
        void this.router.navigate(['/login']);
      },
      error: () => {
        this.loggingOut.set(false);
        this.closeAll();
        void this.router.navigate(['/login']);
      },
    });
  }
}
