import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Salarie, SalarieRequest } from '../models/salarie.model';

@Injectable({ providedIn: 'root' })
export class SalarieService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/salaries`;

  lister(serviceId?: string, actif?: boolean, recherche?: string): Observable<Salarie[]> {
    let params = new HttpParams();
    if (serviceId) params = params.set('serviceId', serviceId);
    if (actif !== undefined) params = params.set('actif', actif);
    if (recherche) params = params.set('recherche', recherche);
    return this.http.get<Salarie[]>(this.baseUrl, { params });
  }

  obtenir(id: string): Observable<Salarie> {
    return this.http.get<Salarie>(`${this.baseUrl}/${id}`);
  }

  creer(request: SalarieRequest): Observable<Salarie> {
    return this.http.post<Salarie>(this.baseUrl, request);
  }

  modifier(id: string, request: SalarieRequest): Observable<Salarie> {
    return this.http.put<Salarie>(`${this.baseUrl}/${id}`, request);
  }

  supprimer(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
