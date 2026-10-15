import { Component, HostListener, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/auth.service';

interface NavSubItem {
  label: string;
  description: string;
  icon: 'users' | 'shield' | 'mail';
  link: string;
  params?: Record<string, string>;
}

interface NavGroup {
  title: string;
  items: NavSubItem[];
}

interface NavEntry {
  id: string;
  label: string;
  link?: string;
  groups?: NavGroup[];
}

/**
 * Top dropdown navigation for admin pages only.
 * Plain-CSS Angular port of `Dorpdown Navigation.txt` (no framer-motion/lucide):
 * hover pill on top-level buttons, rotating chevron, hover/click mega-menu
 * card with grouped links, plus a Logout button. All items point at real routes.
 */
@Component({
  selector: 'app-admin-nav',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="adminbar">
      <div class="bar-inner">
        <a class="brand" routerLink="/admin/users" (click)="close()">
          DarCom <span>Admin</span>
        </a>
        <button
          class="burger"
          type="button"
          (click)="toggleMobile()"
          aria-label="Toggle navigation"
          [attr.aria-expanded]="mobileOpen()"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path d="M4 7h16M4 12h16M4 17h16" />
          </svg>
        </button>
        <nav class="nav" [class.open]="mobileOpen()" aria-label="Admin">
          <ul class="menu">
            @for (entry of nav; track entry.id) {
              <li
                class="item"
                (mouseenter)="onHover(entry.groups ? entry.id : null)"
                (mouseleave)="onHover(null)"
              >
                @if (entry.groups) {
                  <button
                    type="button"
                    class="topbtn"
                    [class.active]="openMenu() === entry.id"
                    (click)="toggle(entry.id, $event)"
                    [attr.aria-expanded]="openMenu() === entry.id"
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
                      <path d="M6 9l6 6 6-6" />
                    </svg>
                  </button>
                  @if (openMenu() === entry.id) {
                    <div class="drop">
                      <div class="drop-card" (click)="close()">
                        <div class="groups">
                          @for (g of entry.groups; track g.title) {
                            <div class="group">
                              <h3>{{ g.title }}</h3>
                              <ul>
                                @for (sub of g.items; track sub.label) {
                                  <li>
                                    <a
                                      class="sub"
                                      [routerLink]="sub.link"
                                      [queryParams]="sub.params"
                                      routerLinkActive="sel"
                                      [routerLinkActiveOptions]="{ exact: true }"
                                    >
                                      <span class="tile" aria-hidden="true">
                                        @switch (sub.icon) {
                                          @case ('users') {
                                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                              <circle cx="9" cy="8" r="3.5" />
                                              <path d="M3 20c0-3.3 2.7-6 6-6s6 2.7 6 6" />
                                              <circle cx="17" cy="9" r="2.5" />
                                              <path d="M16 14.5c2.8.4 5 2.6 5 5.5" />
                                            </svg>
                                          }
                                          @case ('shield') {
                                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                              <path d="M12 3l7 3v5c0 4.5-3 8.5-7 10-4-1.5-7-5.5-7-10V6z" />
                                            </svg>
                                          }
                                          @case ('mail') {
                                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                              <rect x="3" y="5" width="18" height="14" rx="2" />
                                              <path d="M3 7l9 6 9-6" />
                                            </svg>
                                          }
                                        }
                                      </span>
                                      <span class="txt">
                                        <span class="t">{{ sub.label }}</span>
                                        <span class="d">{{ sub.description }}</span>
                                      </span>
                                    </a>
                                  </li>
                                }
                              </ul>
                            </div>
                          }
                        </div>
                      </div>
                    </div>
                  }
                } @else {
                  <a
                    class="topbtn link"
                    [routerLink]="entry.link"
                    routerLinkActive="sel"
                    (click)="close()"
                  >
                    <span>{{ entry.label }}</span>
                  </a>
                }
              </li>
            }
          </ul>
          <div class="side">
            @if (currentUser()?.email) {
              <span class="who">{{ currentUser()?.email }}</span>
            }
            <button type="button" class="logout" (click)="onLogout()" [disabled]="loggingOut()">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" />
                <path d="M16 17l5-5-5-5" />
                <path d="M21 12H9" />
              </svg>
              <span>Logout</span>
            </button>
          </div>
        </nav>
      </div>
    </header>
  `,
  styleUrls: ['./admin-nav.css'],
})
export class AdminNavComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly currentUser = this.auth.currentUser;
  readonly openMenu = signal<string | null>(null);
  readonly mobileOpen = signal(false);
  readonly loggingOut = signal(false);

  readonly nav: NavEntry[] = [
    {
      id: 'users',
      label: 'Users',
      groups: [
        {
          title: 'Manage',
          items: [
            {
              label: 'All users',
              description: 'Search, filter and sort accounts',
              icon: 'users',
              link: '/admin/users',
            },
          ],
        },
        {
          title: 'Moderation',
          items: [
            {
              label: 'Blocked',
              description: 'Review blocked accounts',
              icon: 'shield',
              link: '/admin/users',
              params: { blocked: 'true' },
            },
            {
              label: 'Unverified',
              description: 'Pending email verification',
              icon: 'mail',
              link: '/admin/users',
              params: { verified: 'false' },
            },
          ],
        },
      ],
    },
    { id: 'home', label: 'Home', link: '/home' },
  ];

  onHover(id: string | null): void {
    this.openMenu.set(id);
  }

  toggle(id: string, event: Event): void {
    event.stopPropagation();
    this.openMenu.update((cur) => (cur === id ? null : id));
  }

  toggleMobile(): void {
    this.mobileOpen.update((v) => !v);
  }

  close(): void {
    this.openMenu.set(null);
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.close();
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.close();
  }

  onLogout(): void {
    if (this.loggingOut()) {
      return;
    }
    this.loggingOut.set(true);
    this.auth.logout().subscribe({
      next: () => {
        this.loggingOut.set(false);
        void this.router.navigate(['/login']);
      },
      error: () => {
        this.loggingOut.set(false);
        void this.router.navigate(['/login']);
      },
    });
  }
}
