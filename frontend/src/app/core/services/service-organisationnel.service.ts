import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ServiceOrganisationnel,
  ServiceOrganisationnelRequest,
} from '../models/service-organisationnel.model';

@Injectable({ providedIn: 'root' })
export class ServiceOrganisationnelService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/services`;

  lister(directionId?: string, actif?: boolean, recherche?: string): Observable<ServiceOrganisationnel[]> {
    let params = new HttpParams();
    if (directionId) params = params.set('directionId', directionId);
    if (actif !== undefined) params = params.set('actif', actif);
    if (recherche) params = params.set('recherche', recherche);
    return this.http.get<ServiceOrganisationnel[]>(this.baseUrl, { params });
  }

  creer(request: ServiceOrganisationnelRequest): Observable<ServiceOrganisationnel> {
    return this.http.post<ServiceOrganisationnel>(this.baseUrl, request);
  }

  modifier(id: string, request: ServiceOrganisationnelRequest): Observable<ServiceOrganisationnel> {
    return this.http.put<ServiceOrganisationnel>(`${this.baseUrl}/${id}`, request);
  }

  supprimer(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
