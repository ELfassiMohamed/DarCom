import {
  Component,
  DestroyRef,
  ElementRef,
  Injector,
  afterNextRender,
  computed,
  inject,
  signal,
  viewChildren,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

interface Square {
  id: number;
  src: string;
}

/** 16 stock photos (Unsplash) of stays and hosting, used by the shuffling hero grid. */
const SQUARE_IMAGES: readonly string[] = [
  'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1493809842364-78817add7ffb?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1560185007-cde436f6a4d0?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1600566753086-00f18fb6b3ea?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1600573472592-401b489a3cdc?auto=format&fit=crop&w=600&q=80',
  // Hosting side: people sharing their space.
  'https://images.unsplash.com/photo-1521737604893-d14cc237f11d?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1600880292203-757bb62b4baf?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1554774853-aae0a22c8aa4?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1560518883-ce09059eeffa?auto=format&fit=crop&w=600&q=80',
];

const shuffle = <T>(items: T[]): T[] => {
  let currentIndex = items.length;
  while (currentIndex !== 0) {
    const randomIndex = Math.floor(Math.random() * currentIndex);
    currentIndex--;
    [items[currentIndex], items[randomIndex]] = [items[randomIndex], items[currentIndex]];
  }
  return items;
};

const orderedSquares = (): Square[] => SQUARE_IMAGES.map((src, id) => ({ id, src }));

const generateSquares = (): Square[] => shuffle(orderedSquares());

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section
      class="w-full px-8 py-12 grid grid-cols-1 md:grid-cols-2 items-center gap-8 max-w-6xl mx-auto"
    >
      <div>
        <span class="block mb-4 text-xs md:text-sm text-brand font-medium"
          >Co-hosting, simplified</span
        >
        <h1 class="text-4xl md:text-6xl font-semibold text-slate-900">
          Find a stay, or share your space
        </h1>
        <p class="text-base md:text-lg text-slate-700 my-4 md:my-6">
          DarCom connects visitors looking for a place to stay with hosts who have space to share.
          Browse listings, message hosts, and request bookings in one place.
        </p>
        @if (loggedIn()) {
          <a
            routerLink="/home"
            class="inline-block bg-brand text-white font-medium py-2 px-4 rounded transition-all hover:bg-brand-dark active:scale-95"
          >
            Go to your dashboard
          </a>
        } @else {
          <a
            routerLink="/register"
            class="inline-block bg-brand text-white font-medium py-2 px-4 rounded transition-all hover:bg-brand-dark active:scale-95"
          >
            Get started
          </a>
          <a
            routerLink="/login"
            class="inline-block ml-2 text-brand font-medium py-2 px-4 rounded transition-all hover:bg-brand/10"
          >
            Log in
          </a>
        }
      </div>
      <div class="grid grid-cols-4 grid-rows-4 h-[450px] gap-1" aria-hidden="true">
        @for (sq of squares(); track sq.id) {
          <div
            #tile
            [style.background-image]="'url(' + sq.src + ')'"
            class="tile w-full h-full bg-cover bg-center rounded-sm"
          ></div>
        }
      </div>
    </section>
  `,
  styles: `
    .tile {
      transition: transform 900ms cubic-bezier(0.34, 1.56, 0.64, 1);
    }
    .tile.flip-pin {
      transition: none;
    }
  `,
})
export class LandingComponent {
  private readonly auth = inject(AuthService);
  private readonly injector = inject(Injector);
  private readonly destroyRef = inject(DestroyRef);

  readonly squares = signal<Square[]>(orderedSquares());
  readonly loggedIn = computed(() => this.auth.currentUser() !== null);

  private readonly tiles = viewChildren<ElementRef<HTMLElement>>('tile');
  private timeoutRef: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    // The React original shuffles once right after mount; same here, browser-only.
    afterNextRender(() => this.shuffleSquares());
    this.destroyRef.onDestroy(() => {
      if (this.timeoutRef !== undefined) {
        clearTimeout(this.timeoutRef);
      }
    });
  }

  private shuffleSquares(): void {
    const previous = this.captureRects();
    this.squares.set(generateSquares());
    afterNextRender(() => this.animateFlip(previous), { injector: this.injector });
    this.timeoutRef = setTimeout(() => this.shuffleSquares(), 3000);
  }

  /** Position of every tile right now, keyed by image id. */
  private captureRects(): Map<number, DOMRect> {
    const rects = new Map<number, DOMRect>();
    const tiles = this.tiles();
    this.squares().forEach((sq, index) => {
      const tile = tiles[index];
      if (tile) {
        rects.set(sq.id, tile.nativeElement.getBoundingClientRect());
      }
    });
    return rects;
  }

  /**
   * FLIP animation replacing framer-motion's `layout` prop (not available in Angular):
   * each moved tile is pinned to its old position, then transitioned to its new grid slot.
   */
  private animateFlip(previous: Map<number, DOMRect>): void {
    const prefersReducedMotion =
      typeof window.matchMedia === 'function' &&
      window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (prefersReducedMotion) {
      return;
    }

    const moved: { el: HTMLElement; dx: number; dy: number }[] = [];
    const tiles = this.tiles();
    this.squares().forEach((sq, index) => {
      const tile = tiles[index];
      const before = previous.get(sq.id);
      if (!tile || !before) {
        return;
      }
      const el = tile.nativeElement;
      const after = el.getBoundingClientRect();
      const dx = before.left - after.left;
      const dy = before.top - after.top;
      if (dx !== 0 || dy !== 0) {
        moved.push({ el, dx, dy });
      }
    });

    if (moved.length === 0) {
      return;
    }

    for (const { el, dx, dy } of moved) {
      el.classList.add('flip-pin');
      el.style.transform = `translate(${dx}px, ${dy}px)`;
    }
    const scheduleFrame: (cb: () => void) => void =
      typeof requestAnimationFrame === 'function'
        ? requestAnimationFrame
        : (cb) => void setTimeout(cb, 16);
    scheduleFrame(() => {
      for (const { el } of moved) {
        el.classList.remove('flip-pin');
        el.style.transform = '';
      }
    });
  }
}
