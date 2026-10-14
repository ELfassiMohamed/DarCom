import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from './api.config';
import { AdminUserQuery, AdminUserSummary, PageResponse, User } from './models';

@Injectable({
  providedIn: 'root',
})
export class AdminService {
  private readonly http = inject(HttpClient);

  list(query: AdminUserQuery): Observable<PageResponse<AdminUserSummary>> {
    let params = new HttpParams()
      .set('page', String(query.page))
      .set('size', String(query.size))
      .set('sort', `${query.sortField},${query.sortDir}`);
    const search = query.search?.trim();
    if (search) {
      params = params.set('search', search);
    }
    if (query.role) {
      params = params.set('role', query.role);
    }
    if (query.verified !== undefined) {
      params = params.set('verified', String(query.verified));
    }
    if (query.blocked !== undefined) {
      params = params.set('blocked', String(query.blocked));
    }
    return this.http.get<PageResponse<AdminUserSummary>>(`${API_URL}/admin/users`, { params });
  }

  block(id: string, reason: string): Observable<User> {
    return this.http.patch<User>(`${API_URL}/admin/users/${id}/block`, { reason });
  }

  unblock(id: string): Observable<User> {
    return this.http.patch<User>(`${API_URL}/admin/users/${id}/unblock`, {});
  }
}
