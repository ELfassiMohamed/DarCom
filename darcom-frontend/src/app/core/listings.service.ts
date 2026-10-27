import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from './api.config';
import {
  Listing,
  ListingDetails,
  ListingPageResponse,
  ListingQuery,
  CreateListingRequest,
  UpdateListingRequest,
} from './models';

@Injectable({ providedIn: 'root' })
export class ListingsService {
  private readonly http = inject(HttpClient);

  /** Public listing search with optional filters. */
  list(query: ListingQuery): Observable<ListingPageResponse> {
    let params = new HttpParams()
      .set('page', String(query.page ?? 0))
      .set('size', String(query.size ?? 20))
      .set('sort', query.sort || 'createdAt,desc');

    const city = query.city;
    if (city) {
      params = params.set('city', city);
    }
    const minPrice = query.minPrice;
    if (minPrice !== undefined) {
      params = params.set('minPrice', String(minPrice));
    }
    const maxPrice = query.maxPrice;
    if (maxPrice !== undefined) {
      params = params.set('maxPrice', String(maxPrice));
    }
    const mealsIncluded = query.mealsIncluded;
    if (mealsIncluded !== undefined) {
      params = params.set('mealsIncluded', String(mealsIncluded));
    }

    return this.http.get<ListingPageResponse>(`${API_URL}/listings`, { params });
  }

  /** Listings belonging to the authenticated host. */
  mine(): Observable<Listing[]> {
    return this.http.get<Listing[]>(`${API_URL}/listings/mine`);
  }

  /** Full listing details (public). */
  details(id: string): Observable<ListingDetails> {
    return this.http.get<ListingDetails>(`${API_URL}/listings/${id}`);
  }

  /** Create a new listing (Bearer, role HOST, email must be verified). */
  create(body: CreateListingRequest, hostId: string): Observable<Listing> {
    return this.http.post<Listing>(`${API_URL}/listings`, body);
  }

  /** Update a listing (Bearer, role HOST, owner). */
  update(id: string, body: UpdateListingRequest): Observable<Listing> {
    return this.http.put<Listing>(`${API_URL}/listings/${id}`, body);
  }

  /** Soft delete a listing (set status to REMOVED). */
  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${API_URL}/listings/${id}`);
  }

  /** Upload a photo for a listing (multipart/form-data). */
  uploadPhoto(listingId: string, file: File): Observable<ListingPhoto> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<ListingPhoto>(`${API_URL}/listings/${listingId}/photos`, formData);
  }

  /** Delete a photo from a listing. */
  deletePhoto(listingId: string, photoId: string): Observable<void> {
    return this.http.delete<void>(`${API_URL}/listings/${listingId}/photos/${photoId}`);
  }
}

/** Photo returned from the upload endpoint. */
export interface ListingPhoto {
  id: string;
  url: string;
  sortOrder: number;
}