import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Direction, DirectionRequest } from '../models/direction.model';

@Injectable({ providedIn: 'root' })
export class DirectionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/directions`;

  lister(structureCode?: string, actif?: boolean, recherche?: string): Observable<Direction[]> {
    let params = new HttpParams();
    if (structureCode) params = params.set('structureCode', structureCode);
    if (actif !== undefined) params = params.set('actif', actif);
    if (recherche) params = params.set('recherche', recherche);
    return this.http.get<Direction[]>(this.baseUrl, { params });
  }

  creer(request: DirectionRequest): Observable<Direction> {
    return this.http.post<Direction>(this.baseUrl, request);
  }

  modifier(id: string, request: DirectionRequest): Observable<Direction> {
    return this.http.put<Direction>(`${this.baseUrl}/${id}`, request);
  }

  supprimer(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
