import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Affectation, AffectationRequest } from '../models/affectation.model';

@Injectable({ providedIn: 'root' })
export class AffectationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/affectations`;

  lister(salarieId?: string): Observable<Affectation[]> {
    let params = new HttpParams();
    if (salarieId) params = params.set('salarieId', salarieId);
    return this.http.get<Affectation[]>(this.baseUrl, { params });
  }

  creer(request: AffectationRequest): Observable<Affectation> {
    return this.http.post<Affectation>(this.baseUrl, request);
  }

  modifier(id: string, request: AffectationRequest): Observable<Affectation> {
    return this.http.put<Affectation>(`${this.baseUrl}/${id}`, request);
  }

  supprimer(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
