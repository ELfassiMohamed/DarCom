import {
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  inject,
  viewChild,
} from '@angular/core';

interface Dot {
  x: number;
  y: number;
  radius: number;
  opacity: number;
}

interface RoutePoint {
  x: number;
  y: number;
  delay: number;
}

interface Route {
  start: RoutePoint;
  end: RoutePoint;
  color: string;
}

/** Travel routes animated on top of the dotted map (coordinates from the original component). */
const ROUTES: readonly Route[] = [
  { start: { x: 100, y: 150, delay: 0 }, end: { x: 200, y: 80, delay: 2 }, color: '#2563eb' },
  { start: { x: 200, y: 80, delay: 2 }, end: { x: 260, y: 120, delay: 4 }, color: '#2563eb' },
  { start: { x: 50, y: 50, delay: 1 }, end: { x: 150, y: 180, delay: 3 }, color: '#2563eb' },
  { start: { x: 280, y: 60, delay: 0.5 }, end: { x: 180, y: 180, delay: 2.5 }, color: '#2563eb' },
];

const ROUTE_DURATION_S = 3;
const RESTART_AFTER_S = 15;
const DOT_GAP = 12;
const DOT_RADIUS = 1;

/** Dot grid shaped like a world map silhouette (simple continents), as in the original component. */
const generateDots = (width: number, height: number): Dot[] => {
  const dots: Dot[] = [];
  for (let x = 0; x < width; x += DOT_GAP) {
    for (let y = 0; y < height; y += DOT_GAP) {
      const isInMapShape =
        // North America
        (x < width * 0.25 && x > width * 0.05 && y < height * 0.4 && y > height * 0.1) ||
        // South America
        (x < width * 0.25 && x > width * 0.15 && y < height * 0.8 && y > height * 0.4) ||
        // Europe
        (x < width * 0.45 && x > width * 0.3 && y < height * 0.35 && y > height * 0.15) ||
        // Africa
        (x < width * 0.5 && x > width * 0.35 && y < height * 0.65 && y > height * 0.35) ||
        // Asia
        (x < width * 0.7 && x > width * 0.45 && y < height * 0.5 && y > height * 0.1) ||
        // Australia
        (x < width * 0.8 && x > width * 0.65 && y < height * 0.8 && y > height * 0.6);

      if (isInMapShape && Math.random() > 0.3) {
        dots.push({ x, y, radius: DOT_RADIUS, opacity: Math.random() * 0.5 + 0.2 });
      }
    }
  }
  return dots;
};

@Component({
  selector: 'app-dot-map',
  standalone: true,
  template: `
    <div class="relative w-full h-full overflow-hidden">
      <canvas #canvas class="absolute inset-0 w-full h-full"></canvas>
    </div>
  `,
  styles: `
    :host {
      display: block;
      width: 100%;
      height: 100%;
    }
  `,
})
export class DotMapComponent {
  private readonly canvasRef = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private readonly destroyRef = inject(DestroyRef);

  private frameId: number | undefined;
  private resizeObserver: ResizeObserver | undefined;
  private startTime = 0;

  constructor() {
    afterNextRender(() => this.observeParent());
    this.destroyRef.onDestroy(() => this.stop());
  }

  private observeParent(): void {
    const canvas = this.canvasRef().nativeElement;
    const host = canvas.parentElement;
    if (!host) {
      return;
    }
    if (typeof ResizeObserver === 'function') {
      this.resizeObserver = new ResizeObserver((entries) => {
        const rect = entries[0]?.contentRect;
        if (rect) {
          this.setSize(canvas, rect.width, rect.height);
        }
      });
      this.resizeObserver.observe(host);
    } else {
      this.setSize(canvas, host.clientWidth, host.clientHeight);
    }
  }

  private setSize(canvas: HTMLCanvasElement, width: number, height: number): void {
    if (width === 0 || height === 0) {
      // Hidden panel (mobile): nothing to draw until it becomes visible.
      return;
    }
    canvas.width = width;
    canvas.height = height;
    this.render(canvas, width, height);
  }

  private render(canvas: HTMLCanvasElement, width: number, height: number): void {
    if (this.frameId !== undefined) {
      cancelAnimationFrame(this.frameId);
      this.frameId = undefined;
    }
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      return;
    }
    const dots = generateDots(width, height);

    const reducedMotion =
      typeof window.matchMedia === 'function' &&
      window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reducedMotion) {
      // Static map only, no animated routes.
      this.drawDots(ctx, dots, width, height);
      return;
    }

    this.startTime = performance.now();
    const animate = (): void => {
      const elapsedS = (performance.now() - this.startTime) / 1000;
      this.drawDots(ctx, dots, width, height);
      this.drawRoutes(ctx, elapsedS);
      if (elapsedS > RESTART_AFTER_S) {
        this.startTime = performance.now();
      }
      this.frameId = requestAnimationFrame(animate);
    };
    animate();
  }

  private drawDots(
    ctx: CanvasRenderingContext2D,
    dots: readonly Dot[],
    width: number,
    height: number,
  ): void {
    ctx.clearRect(0, 0, width, height);
    for (const dot of dots) {
      ctx.beginPath();
      ctx.arc(dot.x, dot.y, dot.radius, 0, Math.PI * 2);
      ctx.fillStyle = `rgba(37, 99, 235, ${dot.opacity})`;
      ctx.fill();
    }
  }

  private drawRoutes(ctx: CanvasRenderingContext2D, elapsedS: number): void {
    for (const route of ROUTES) {
      const elapsed = elapsedS - route.start.delay;
      if (elapsed <= 0) {
        continue;
      }
      const progress = Math.min(elapsed / ROUTE_DURATION_S, 1);
      const x = route.start.x + (route.end.x - route.start.x) * progress;
      const y = route.start.y + (route.end.y - route.start.y) * progress;

      ctx.beginPath();
      ctx.moveTo(route.start.x, route.start.y);
      ctx.lineTo(x, y);
      ctx.strokeStyle = route.color;
      ctx.lineWidth = 1.5;
      ctx.stroke();

      ctx.beginPath();
      ctx.arc(route.start.x, route.start.y, 3, 0, Math.PI * 2);
      ctx.fillStyle = route.color;
      ctx.fill();

      ctx.beginPath();
      ctx.arc(x, y, 3, 0, Math.PI * 2);
      ctx.fillStyle = '#3b82f6';
      ctx.fill();

      // Glow around the moving point.
      ctx.beginPath();
      ctx.arc(x, y, 6, 0, Math.PI * 2);
      ctx.fillStyle = 'rgba(59, 130, 246, 0.4)';
      ctx.fill();

      if (progress === 1) {
        ctx.beginPath();
        ctx.arc(route.end.x, route.end.y, 3, 0, Math.PI * 2);
        ctx.fillStyle = route.color;
        ctx.fill();
      }
    }
  }

  private stop(): void {
    if (this.frameId !== undefined) {
      cancelAnimationFrame(this.frameId);
      this.frameId = undefined;
    }
    this.resizeObserver?.disconnect();
    this.resizeObserver = undefined;
  }
}
