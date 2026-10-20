import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { AdminService } from '../../core/admin.service';
import { toUserMessage } from '../../core/error-messages';
import { AdminSortField, AdminUserSummary, Role } from '../../core/models';
import { MegaNavComponent } from '../../shared/mega-nav/mega-nav.component';

const PAGE_SIZE = 10;

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [ReactiveFormsModule, MegaNavComponent],
  template: `
    <app-mega-nav />
    <div class="admin-page">
      <div class="wrap">
        <div class="card">
          <div class="toolbar">
            <h1>Users</h1>
            <span class="count">{{ totalItems() }} total</span>
            @if (selectedCount() > 0) {
              <span class="count">{{ selectedCount() }} selected</span>
            }
            <span class="spacer"></span>
            <input
              class="search"
              type="search"
              placeholder="Search name or email"
              [formControl]="searchControl"
              aria-label="Search users"
            />
            <select
              class="filter"
              [value]="roleFilter()"
              (change)="onRoleChange($event)"
              aria-label="Filter by role"
            >
              <option value="">All roles</option>
              <option value="VISITOR">VISITOR</option>
              <option value="HOST">HOST</option>
              <option value="ADMIN">ADMIN</option>
            </select>
            <select
              class="filter"
              [value]="verifiedFilter()"
              (change)="onFlagChange($event, 'verified')"
              aria-label="Filter by verified"
            >
              <option value="">Verified: all</option>
              <option value="true">Verified</option>
              <option value="false">Not verified</option>
            </select>
            <select
              class="filter"
              [value]="blockedFilter()"
              (change)="onFlagChange($event, 'blocked')"
              aria-label="Filter by blocked"
            >
              <option value="">Status: all</option>
              <option value="false">Active</option>
              <option value="true">Blocked</option>
            </select>
          </div>

          @if (errorMsg()) {
            <p class="list-error" role="alert">{{ errorMsg() }}</p>
          }

          @if (loading()) {
            <p class="state">Loading users...</p>
          } @else if (users().length === 0) {
            <p class="state">No users found.</p>
          } @else {
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>
                      <input
                        type="checkbox"
                        [checked]="allSelected()"
                        (change)="onToggleAll($event)"
                        aria-label="Select all users on this page"
                      />
                    </th>
                    <th class="sortable" (click)="toggleSort('fullName')">
                      User {{ sortArrow('fullName') }}
                    </th>
                    <th class="sortable" (click)="toggleSort('role')">
                      Role {{ sortArrow('role') }}
                    </th>
                    <th>Verified</th>
                    <th>Status</th>
                    <th class="sortable" (click)="toggleSort('createdAt')">
                      Joined {{ sortArrow('createdAt') }}
                    </th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  @for (u of users(); track u.id) {
                    <tr>
                      <td>
                        <input
                          type="checkbox"
                          [checked]="isSelected(u.id)"
                          (change)="onRowCheck($event, u.id)"
                          [attr.aria-label]="'Select ' + u.fullName"
                        />
                      </td>
                      <td>
                        <div class="user-cell">
                          <span class="avatar" aria-hidden="true">{{ initials(u.fullName) }}</span>
                          <div>
                            <div class="user-name">{{ u.fullName }}</div>
                            <div class="user-email">{{ u.email }}</div>
                          </div>
                        </div>
                      </td>
                      <td><span class="pill">{{ u.role }}</span></td>
                      <td>
                        @if (u.verified) {
                          <span class="pill ok">Verified</span>
                        } @else {
                          <span class="pill idle">Unverified</span>
                        }
                      </td>
                      <td>
                        @if (u.blocked) {
                          <span class="pill bad">Blocked</span>
                        } @else {
                          <span class="pill ok">Active</span>
                        }
                      </td>
                      <td class="muted">{{ formatDate(u.createdAt) }}</td>
                      <td>
                        <details class="menu">
                          <summary>Actions &#9662;</summary>
                          <div class="menu-list">
                            @if (u.blocked) {
                              <button
                                type="button"
                                (click)="unblockUser(u, $event)"
                                [disabled]="busyId() === u.id"
                              >
                                Unblock
                              </button>
                            } @else {
                              <button
                                type="button"
                                class="danger"
                                (click)="startBlock(u, $event)"
                                [disabled]="u.role === 'ADMIN'"
                                [title]="u.role === 'ADMIN' ? 'Admin accounts cannot be blocked' : ''"
                              >
                                Block
                              </button>
                            }
                          </div>
                        </details>
                        @if (blockingId() === u.id) {
                          <div class="block-form">
                            <input
                              type="text"
                              placeholder="Reason (required)"
                              maxlength="500"
                              [formControl]="reasonControl"
                              aria-label="Block reason"
                            />
                            <button
                              type="button"
                              class="mini-btn primary"
                              (click)="confirmBlock(u)"
                              [disabled]="reasonControl.invalid || busyId() === u.id"
                            >
                              Confirm
                            </button>
                            <button type="button" class="mini-btn" (click)="cancelBlock()">
                              Cancel
                            </button>
                          </div>
                        }
                        @if (rowError()?.id === u.id) {
                          <p class="row-error" role="alert">{{ rowError()?.message }}</p>
                        }
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>

            <div class="footer">
              <span>{{ rangeText() }}</span>
              @if (selectedCount() > 0) {
                <button type="button" class="mini-btn" (click)="clearSelection()">Clear selection</button>
              }
              <span class="spacer"></span>
              <div class="pager">
                <button type="button" (click)="goTo(page() - 1)" [disabled]="page() === 0">
                  Prev
                </button>
                @for (p of pageNumbers(); track p) {
                  <button
                    type="button"
                    (click)="goTo(p)"
                    [class.current]="p === page()"
                    [attr.aria-label]="'Go to page ' + (p + 1)"
                  >
                    {{ p + 1 }}
                  </button>
                }
                <button
                  type="button"
                  (click)="goTo(page() + 1)"
                  [disabled]="page() >= totalPages() - 1"
                >
                  Next
                </button>
              </div>
            </div>
          }
        </div>
      </div>
    </div>
  `,
  styleUrls: ['./admin-users.css'],
})
export class AdminUsersComponent implements OnInit {
  private readonly admin = inject(AdminService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly users = signal<AdminUserSummary[]>([]);
  readonly totalItems = signal(0);
  readonly totalPages = signal(0);
  readonly page = signal(0);
  readonly loading = signal(true);
  readonly errorMsg = signal<string | null>(null);

  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly reasonControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(500)],
  });

  readonly roleFilter = signal<'' | Role>('');
  readonly verifiedFilter = signal<'' | 'true' | 'false'>('');
  readonly blockedFilter = signal<'' | 'true' | 'false'>('');
  readonly sortField = signal<AdminSortField>('createdAt');
  readonly sortDir = signal<'asc' | 'desc'>('desc');

  readonly selected = signal<string[]>([]);
  readonly selectedCount = computed(() => this.selected().length);
  readonly allSelected = computed(() => {
    const ids = this.users().map((u) => u.id);
    return ids.length > 0 && ids.every((id) => this.selected().includes(id));
  });

  readonly blockingId = signal<string | null>(null);
  readonly busyId = signal<string | null>(null);
  readonly rowError = signal<{ id: string; message: string } | null>(null);

  ngOnInit(): void {
    this.searchControl.valueChanges
      .pipe(debounceTime(400), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.page.set(0);
        this.load();
      });
    // Nav submenu presets (e.g. ?blocked=true) share this page: re-apply on change.
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => this.applyQueryParams(params));
  }

  private applyQueryParams(params: ParamMap): void {
    const role = params.get('role');
    this.roleFilter.set(role === 'VISITOR' || role === 'HOST' || role === 'ADMIN' ? role : '');
    const verified = params.get('verified');
    this.verifiedFilter.set(verified === 'true' || verified === 'false' ? verified : '');
    const blocked = params.get('blocked');
    this.blockedFilter.set(blocked === 'true' || blocked === 'false' ? blocked : '');
    this.searchControl.setValue(params.get('search') ?? '', { emitEvent: false });
    this.page.set(0);
    this.selected.set([]);
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorMsg.set(null);
    const role = this.roleFilter();
    const verified = this.verifiedFilter();
    const blocked = this.blockedFilter();
    this.admin
      .list({
        page: this.page(),
        size: PAGE_SIZE,
        sortField: this.sortField(),
        sortDir: this.sortDir(),
        search: this.searchControl.value,
        role: role === '' ? undefined : role,
        verified: verified === '' ? undefined : verified === 'true',
        blocked: blocked === '' ? undefined : blocked === 'true',
      })
      .subscribe({
        next: (res) => {
          this.users.set(res.items);
          this.totalItems.set(res.totalItems);
          this.totalPages.set(res.totalPages);
          this.page.set(res.page);
          this.loading.set(false);
        },
        error: (err: unknown) => {
          this.loading.set(false);
          this.errorMsg.set(toUserMessage(err));
        },
      });
  }

  onRoleChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value as '' | Role;
    this.roleFilter.set(value);
    this.page.set(0);
    this.load();
  }

  onFlagChange(event: Event, which: 'verified' | 'blocked'): void {
    const value = (event.target as HTMLSelectElement).value as '' | 'true' | 'false';
    if (which === 'verified') {
      this.verifiedFilter.set(value);
    } else {
      this.blockedFilter.set(value);
    }
    this.page.set(0);
    this.load();
  }

  toggleSort(field: AdminSortField): void {
    if (this.sortField() === field) {
      this.sortDir.set(this.sortDir() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortField.set(field);
      this.sortDir.set(field === 'createdAt' ? 'desc' : 'asc');
    }
    this.page.set(0);
    this.load();
  }

  sortArrow(field: AdminSortField): string {
    if (this.sortField() !== field) {
      return '';
    }
    return this.sortDir() === 'asc' ? ' \u25B2' : ' \u25BC';
  }

  isSelected(id: string): boolean {
    return this.selected().includes(id);
  }

  onRowCheck(event: Event, id: string): void {
    const checked = (event.target as HTMLInputElement).checked;
    this.selected.set(
      checked ? [...this.selected(), id] : this.selected().filter((s) => s !== id),
    );
  }

  onToggleAll(event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    this.selected.set(checked ? this.users().map((u) => u.id) : []);
  }

  clearSelection(): void {
    this.selected.set([]);
  }

  goTo(p: number): void {
    const max = Math.max(this.totalPages() - 1, 0);
    const next = Math.min(Math.max(p, 0), max);
    if (next === this.page()) {
      return;
    }
    this.page.set(next);
    this.load();
  }

  pageNumbers(): number[] {
    const total = this.totalPages();
    const current = this.page();
    if (total <= 0) {
      return [];
    }
    const start = Math.max(0, Math.min(current - 2, total - 5));
    const end = Math.min(total, start + 5);
    const out: number[] = [];
    for (let i = Math.max(0, end - 5); i < end; i++) {
      out.push(i);
    }
    return out;
  }

  rangeText(): string {
    const total = this.totalItems();
    if (total === 0) {
      return 'No users';
    }
    const from = this.page() * PAGE_SIZE + 1;
    const to = Math.min((this.page() + 1) * PAGE_SIZE, total);
    return `Showing ${from}\u2013${to} of ${total}`;
  }

  startBlock(u: AdminUserSummary, event: Event): void {
    this.closeMenu(event);
    this.reasonControl.reset();
    this.rowError.set(null);
    this.blockingId.set(u.id);
  }

  cancelBlock(): void {
    this.blockingId.set(null);
    this.reasonControl.reset();
  }

  confirmBlock(u: AdminUserSummary): void {
    if (this.reasonControl.invalid || this.busyId() !== null) {
      this.reasonControl.markAsTouched();
      return;
    }
    this.busyId.set(u.id);
    this.rowError.set(null);
    this.admin.block(u.id, this.reasonControl.value.trim()).subscribe({
      next: () => {
        this.busyId.set(null);
        this.blockingId.set(null);
        this.load();
      },
      error: (err: unknown) => {
        this.busyId.set(null);
        this.rowError.set({ id: u.id, message: toUserMessage(err) });
      },
    });
  }

  unblockUser(u: AdminUserSummary, event: Event): void {
    this.closeMenu(event);
    if (this.busyId() !== null) {
      return;
    }
    this.busyId.set(u.id);
    this.rowError.set(null);
    this.admin.unblock(u.id).subscribe({
      next: () => {
        this.busyId.set(null);
        this.load();
      },
      error: (err: unknown) => {
        this.busyId.set(null);
        this.rowError.set({ id: u.id, message: toUserMessage(err) });
      },
    });
  }

  initials(name: string): string {
    const parts = name.split(' ').filter((p) => p.length > 0);
    if (parts.length === 0) {
      return '?';
    }
    return parts
      .map((p) => p[0])
      .join('')
      .toUpperCase();
  }

  formatDate(iso: string): string {
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) {
      return iso;
    }
    return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
  }

  private closeMenu(event: Event): void {
    const details = (event.target as HTMLElement).closest('details');
    if (details) {
      details.removeAttribute('open');
    }
  }
}
