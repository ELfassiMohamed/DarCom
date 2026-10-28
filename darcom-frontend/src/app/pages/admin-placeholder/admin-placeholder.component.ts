import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MegaNavComponent } from '../../shared/mega-nav/mega-nav.component';

/**
 * Temporary home for admin destinations that do not have a backend yet.
 * The title and description come from the route `data` block.
 */
@Component({
  selector: 'app-admin-placeholder',
  standalone: true,
  imports: [RouterLink, MegaNavComponent],
  template: `
    <app-mega-nav />
    <main class="mx-auto flex min-h-[calc(100vh-84px)] w-full max-w-5xl items-start px-5 py-10">
      <section
        class="w-full rounded-3xl border border-zinc-200 bg-white p-8 text-center shadow-[0_1px_2px_rgba(0,0,0,0.04),0_12px_32px_-8px_rgba(0,0,0,0.10)] md:p-12"
      >
        <span
          class="inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-600"
          aria-hidden="true"
        >
          <svg
            class="h-7 w-7"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M4 5h16v14H4z" />
            <path d="M8 9h8M8 13h8M8 17h5" />
          </svg>
        </span>

        <span
          class="mt-5 inline-block rounded-full bg-zinc-900 px-3 py-1 text-[11px] font-semibold uppercase tracking-wide text-white"
        >
          Coming soon
        </span>
        <h1 class="mt-4 text-2xl font-semibold tracking-tight text-zinc-900 md:text-3xl">
          {{ title }}
        </h1>
        <p class="mx-auto mt-3 max-w-xl text-sm leading-6 text-zinc-500 md:text-base">
          {{ description }}
        </p>

        <div class="mt-8 flex flex-col items-center justify-center gap-3 sm:flex-row">
          <a
            routerLink="/admin/users"
            class="inline-flex items-center justify-center rounded-full bg-zinc-900 px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-zinc-800"
          >
            Back to user dashboard
          </a>
          <a
            routerLink="/home"
            class="inline-flex items-center justify-center rounded-full border border-zinc-200 bg-zinc-50 px-5 py-2.5 text-sm font-semibold text-zinc-900 transition-colors hover:bg-zinc-100"
          >
            Go to home
          </a>
        </div>
      </section>
    </main>
  `,
})
export class AdminPlaceholderComponent {
  private readonly route = inject(ActivatedRoute);

  readonly title = (this.route.snapshot.data['title'] as string | undefined) ?? 'Admin';
  readonly description =
    (this.route.snapshot.data['description'] as string | undefined) ??
    'This section is not available yet.';
}
