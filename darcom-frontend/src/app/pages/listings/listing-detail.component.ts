import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { Listing, ListingService } from '../../core/listings.service';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-listing-detail',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="py-8 max-w-3xl mx-auto px-4">
      @if (loading) {
        <div class="rounded-2xl border border-zinc-200 bg-white p-8 shadow-sm">
          <p class="text-sm text-zinc-500">Loading listing…</p>
        </div>
      } @else if (error) {
        <div class="rounded-2xl border border-red-200 bg-red-50 p-8">
          <p class="text-sm font-medium text-red-800" role="alert">{{ error }}</p>
        </div>
      } @else if (listing) {
        <div class="bg-white rounded-lg p-6 shadow">
          <div class="mb-4">
            @if (listing.photos && listing.photos.length > 0) {
              <img
                [src]="listing.photos[0].url"
                alt="{{ listing.title }}"
                class="w-full h-64 object-cover rounded-md mb-4"
              />
            } @else {
              <div
                class="w-full h-64 bg-zinc-200 rounded-md flex items-center justify-center text-zinc-400 mb-4"
              >
                No photo
              </div>
            }
          </div>

          <h1 class="text-3xl font-semibold text-zinc-900 mb-2">{{ listing.title }}</h1>
          <p class="text-zinc-500 mb-3">{{ listing.city }}, {{ listing.address }}</p>

          <div class="grid grid-cols-2 gap-4 mb-6">
            <div>
              <p class="text-sm text-zinc-500">Price</p>
              <p class="text-3xl font-bold text-brand">${{
                listing.pricePerNight.toFixed(0)
              }}</p>
            </div>
            <div>
              <p class="text-sm text-zinc-500">Meals</p>
              <p class="text-lg font-medium">{{ listing.mealsIncluded ? 'Included' : 'Not included' }}</p>
            </div>
          </div>

          <p class="text-zinc-700 mb-4">{{ listing.description }}</p>

          @if (listing.activities && listing.activities.length > 0) {
            <div class="mb-6">
              <p class="text-sm text-zinc-500 mb-2">Activities</p>
              <div class="flex flex-wrap gap-2">
                @for (activity of listing.activities; track activity) {
                  <span
                    class="inline-flex items-center rounded-full border border-zinc-300 px-2.5 py-0.5 text-sm font-semibold text-zinc-800"
                  >
                    {{ activity }}
                  </span>
                }
              </div>
            </div>
          }

          <p class="text-zinc-500 text-sm mt-6">Hosted by {{ listing.host.fullName }}</p>

          @if (isHost()) {
            <div class="mt-6 p-4 rounded-lg bg-indigo-50">
              <button
                class="inline-flex items-center gap-2 rounded bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700"
              >
                Edit
                <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
                  <path d="M18.5 2.5a2.121 2.121 0 013 3.5L12 15l-3 3" />
                </button>
                <button
                  class="ml-3 inline-flex items-center gap-2 rounded bg-red-100 px-4 py-2 text-sm font-medium text-red-800 hover:bg-red-200"
                >
                  Delete
                </button>
              </button>
            </div>
          }
        </div>
      }
    </section>
  `,
  styles: [],
})
export class ListingDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly listingsService = inject(ListingService);
  private readonly auth = inject(AuthService);

  listing: Listing | null = null;
  loading = true;
  error: string | null = null;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error = 'Listing ID required';
      this.loading = false;
      return;
    }

    this.listingsService.details(id).subscribe({
      next: (data) => {
        this.listing = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load listing';
        this.loading = false;
      },
    });
  }

  isHost(): boolean {
    return this.auth.currentUser()?.role === 'HOST';
  }
}