import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Listing, ListingService, ListingPhoto } from '../../core/listings.service';
import { AuthService } from '../../core/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-listing-list',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="py-8">
      <div class="max-w-6xl mx-auto px-4">
        <header class="mb-6">
          <h1 class="text-3xl font-semibold text-zinc-900">Listings</h1>
          <p class="text-zinc-500 mt-1">Find your stay or share your space</p>
        </header>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          @for (listing of listings; track listing.id) {
            <article class="group border rounded-lg overflow-hidden hover:shadow-lg transition-shadow">
              @if (listing.photos && listing.photos.length > 0) {
                <img
                  [src]="listing.photos[0].url"
                  alt="{{ listing.title }}"
                  class="w-full h-48 object-cover"
                />
              } @else {
                <div
                  class="w-full h-48 bg-zinc-200 flex items-center justify-center text-zinc-400"
                >
                  No photo
                </div>
              }

              <div class="p-4">
                <h2 class="text-lg font-medium text-zinc-900 line-clamp-2">
                  {{ listing.title }}
                </h2>
                <p class="text-sm text-zinc-500 line-clamp-2 mt-2">
                  {{ listing.city }}
                </p>
                <div class="mt-3 flex items-center justify-between">
                  <span class="text-lg font-bold text-brand">${{
                    listing.pricePerNight
                      .toFixed(0)
                      .toString()
                  }}</span>
                  <span class="text-sm text-zinc-500">{{ listing.mealsIncluded ? 'Meals included' : '' }}</span>
                </div>
              </div>
            </article>
          }
        </div>
      </div>
    </section>
  `,
  styles: [],
})
export class ListingListComponent implements OnInit {
  private readonly listingsService = inject(ListingService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  listings: Listing[] = [];
  loading = true;

  ngOnInit(): void {
    this.listingsService.list({ page: 0, size: 20 }).subscribe({
      next: (response) => {
        this.listings = response.items;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  isHost(): boolean {
    return this.auth.currentUser()?.role === 'HOST';
  }

  goToDetail(id: string): void {
    this.router.navigate(['/listings', id]);
  }
}