import { HttpResponse, http } from 'msw';
import { db } from '../db';
import { apiPath } from './paths';

interface ListingPhoto {
  id: string;
  url: string;
  sortOrder: number;
}

interface ListingHost {
  id: string;
  fullName: string;
}

interface Listing {
  id: string;
  host: ListingHost;
  title: string;
  description: string;
  city: string;
  address: string;
  pricePerNight: number;
  currency: string;
  mealsIncluded: boolean;
  activities: string[];
  status: 'ACTIVE' | 'HIDDEN' | 'REMOVED';
  photos: ListingPhoto[];
  createdAt: string;
  updatedAt: string | null;
}

const mockHost: ListingHost = {
  id: '00000000-0000-4000-8000-000000000002',
  fullName: 'Hana Host',
};

const mockPhotos: ListingPhoto[] = [
  { id: 'photo-uuid-1', url: 'photo123.jpg', sortOrder: 0 },
];

export const listingHandlers = [
  http.post(apiPath('/listings'), async ({ request }) => {
    const auth = requireAuthWithRole(request, ['HOST']);
    if (!auth.ok) {
      return auth.response;
    }

    const body = await readJsonBody(request);
    if (!body) {
      return apiError(400, 'VALIDATION_ERROR', 'Request body is required', request);
    }

    // Check email verified (backend rule)
    if (!auth.user.verified) {
      return apiError(403, 'EMAIL_NOT_VERIFIED', 'Your email is not verified', request);
    }

    const listing: Listing = {
      id: crypto.randomUUID(),
      host: mockHost,
      title: body.title,
      description: body.description,
      city: body.city,
      address: body.address,
      pricePerNight: body.pricePerNight,
      currency: body.currency,
      mealsIncluded: body.mealsIncluded,
      activities: body.activities || [],
      status: 'ACTIVE',
      photos: [],
      createdAt: new Date().toISOString(),
      updatedAt: null,
    };

    db.addListing(listing);
    return HttpResponse.json(listing, { status: 201 });
  }),

  http.get(apiPath('/listings'), ({ request }) => {
    const url = new URL(request.url);
    const city = url.searchParams.get('city') || undefined;
    const minPrice = url.searchParams.get('minPrice') ? Number(url.searchParams.get('minPrice')) : undefined;
    const maxPrice = url.searchParams.get('maxPrice') ? Number(url.searchParams.get('maxPrice')) : undefined;
    const mealsIncluded = url.searchParams.get('mealsIncluded') ? url.searchParams.get('mealsIncluded') === 'true' : undefined;
    const page = url.searchParams.get('page') ? Number(url.searchParams.get('page')) : 0;
    const size = url.searchParams.get('size') ? Math.min(Number(url.searchParams.get('size')), 100) : 20;
    const sort = url.searchParams.get('sort') || 'createdAt,desc';

    let results = db.listings || [];

    // Filter by city (partial match)
    if (city) {
      results = results.filter((l) => l.city && l.city.toLowerCase().includes(city.toLowerCase()));
    }

    // Filter by minPrice
    if (minPrice !== undefined) {
      results = results.filter((l) => l.pricePerNight >= minPrice);
    }

    // Filter by maxPrice
    if (maxPrice !== undefined) {
      results = results.filter((l) => l.pricePerNight <= maxPrice);
    }

    // Filter by mealsIncluded
    if (mealsIncluded !== undefined) {
      results = results.filter((l) => l.mealsIncluded === mealsIncluded);
    }

    // Sort
    const [sortField, sortDir] = sort.split(',');
    results.sort((a, b) => {
      const aVal = a[sortField as keyof Listing];
      const bVal = b[sortField as keyof Listing];
      const comparison = String(aVal) < String(bVal) ? -1 : String(aVal) > String(bVal) ? 1 : 0;
      return sortDir === 'asc' ? comparison : -comparison;
    });

    // Paginate
    const totalItems = results.length;
    const start = page * size;
    const items = results.slice(start, start + size);

    return HttpResponse.json({
      items,
      page,
      size,
      totalItems,
      totalPages: Math.ceil(totalItems / size),
    });
  }),

  http.get(apiPath('/listings/mine'), ({ request }) => {
    const auth = requireUser(request);
    if (!auth.ok) {
      return auth.response;
    }

    const userId = auth.user.id;
    const allListings = db.listings || [];
    const mine = allListings.filter((l) => l.host.id === userId);

    return HttpResponse.json({
      items: mine,
      page: 0,
      size: mine.length,
      totalItems: mine.length,
      totalPages: 1,
    });
  }),

  http.get(apiPath('/listings/:id'), ({ params }) => {
    const listing = db.listings?.find((l) => l.id === String(params['id']));
    if (!listing) {
      return apiError(404, 'LISTING_NOT_FOUND', 'Listing not found', request);
    }

    return HttpResponse.json({
      ...listing,
      host: {
        id: listing.host.id,
        fullName: 'Youssef El Amrani',
      },
      photos: listing.photos.map((p) => ({
        id: p.id,
        url: p.url,
        sortOrder: p.sortOrder,
      })),
    });
  }),

  http.put(apiPath('/listings/:id'), async ({ request, params }) => {
    const auth = requireAuthWithRole(request, ['HOST']);
    if (!auth.ok) {
      return auth.response;
    }

    // Check ownership - simplified: just check if the user is a HOST
    const listing = db.listings?.find((l) => l.id === String(params['id']));
    if (!listing) {
      return apiError(404, 'LISTING_NOT_FOUND', 'Listing not found', request);
    }

    // In a real app, check owner. For mock, allow host.
    if (auth.user.role !== 'HOST') {
      return apiError(403, 'NOT_OWNER', 'You are not the owner', request);
    }

    const body = await readJsonBody(request);
    if (!body) {
      return apiError(400, 'VALIDATION_ERROR', 'Request body is required', request);
    }

    const updated: Listing = {
      ...listing,
      title: body.title,
      description: body.description,
      city: body.city,
      address: body.address,
      pricePerNight: body.pricePerNight,
      currency: body.currency,
      mealsIncluded: body.mealsIncluded,
      activities: body.activities || listing.activities,
      status: body.status ?? listing.status,
      updatedAt: new Date().toISOString(),
    };

    return HttpResponse.json(updated);
  }),

  http.delete(apiPath('/listings/:id'), ({ request }) => {
    const auth = requireAuthWithRole(request, ['HOST']);
    if (!auth.ok) {
      return auth.response;
    }

    const listing = db.listings?.find((l) => l.id === String(params['id']));
    if (!listing) {
      return apiError(404, 'LISTING_NOT_FOUND', 'Listing not found', request);
    }

    // Soft delete: set status to REMOVED
    const idx = db.listings?.findIndex((l) => l.id === String(params['id']));
    if (idx !== undefined) {
      db.listings[idx].status = 'REMOVED';
    }

    return new HttpResponse(null, { status: 204 });
  }),

  http.post(apiPath('/listings/:id/photos'), async ({ request, params }) => {
    const auth = requireAuthWithRole(request, ['HOST']);
    if (!auth.ok) {
      return auth.response;
    }

    // Check ownership - simplified
    const listing = db.listings?.find((l) => l.id === String(params['id']));
    if (!listing) {
      return apiError(404, 'LISTING_NOT_FOUND', 'Listing not found', request);
    }

    const formData = await request.formData();
    const file = formData.get('file');

    if (!file) {
      return apiError(400, 'VALIDATION_ERROR', 'File is required', request);
    }

    const fileName = file.name;
    const allowedTypes = ['image/jpeg', 'image/png', 'image/webp'];
    if (!allowedTypes.includes(file.type)) {
      return apiError(400, 'INVALID_FILE_TYPE', 'Invalid file type. Only JPEG, PNG, WEBP allowed', request);
    }

    if (file.size > 5 * 1024 * 1024) {
      return apiError(413, 'FILE_TOO_LARGE', 'File exceeds 5MB limit', request);
    }

    // Create a mock photo entry
    const photo: ListingPhoto = {
      id: `photo-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`,
      url: fileName,
      sortOrder: listing.photos.length,
    };

    // Update listing photos
    const photos = [...(listing.photos || []), photo];

    const idx = db.listings?.findIndex((l) => l.id === String(params['id']));
    if (idx !== undefined) {
      db.listings[idx].photos = photos;
    }

    return HttpResponse.json(photo, { status: 201 });
  }),

  http.delete(apiPath('/listings/:id/photos/:photoId'), ({ params, request }) => {
    const auth = requireAuthWithRole(request, ['HOST']);
    if (!auth.ok) {
      return auth.response;
    }

    const listing = db.listings?.find((l) => l.id === String(params['id']));
    if (!listing) {
      return apiError(404, 'LISTING_NOT_FOUND', 'Listing not found', request);
    }

    const photoId = String(params['photoId']);
    const photos = listing.photos?.filter((p) => p.id !== photoId) || [];

    const idx = db.listings?.findIndex((l) => l.id === String(params['id']));
    if (idx !== undefined) {
      db.listings[idx].photos = photos;
    }

    return new HttpResponse(null, { status: 204 });
  }),
];

function requireAuthWithRole(request: Request, allowedRoles: string[]) {
  const auth = requireUser(request);
  if (!auth.ok) {
    return auth.response;
  }
  if (!allowedRoles.includes(auth.user.role)) {
    return apiError(403, 'FORBIDDEN', 'Insufficient role', request);
  }
  return { ok: true };
}