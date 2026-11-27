import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/page-response.model';
import {
  Abonnement,
  AbonnementRequest,
  EcheanceAbonnement,
  RapprochementFactureRequest
} from '../models/abonnement/abonnement.model';

export type ListParams = Record<string, string | number | boolean | null | undefined>;

function toHttpParams(params: ListParams): HttpParams {
  let httpParams = new HttpParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== null && value !== undefined && value !== '') {
      httpParams = httpParams.set(key, String(value));
    }
  }
  return httpParams;
}

@Injectable({ providedIn: 'root' })
export class AbonnementService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  listerAbonnements(params: ListParams): Observable<PageResponse<Abonnement>> {
    return this.http.get<PageResponse<Abonnement>>(`${this.api}/abonnements`, {
      params: toHttpParams(params)
    });
  }

  obtenirAbonnement(id: string): Observable<Abonnement> {
    return this.http.get<Abonnement>(`${this.api}/abonnements/${id}`);
  }

  creerAbonnement(dto: AbonnementRequest): Observable<Abonnement> {
    return this.http.post<Abonnement>(`${this.api}/abonnements`, dto);
  }

  modifierAbonnement(id: string, dto: AbonnementRequest): Observable<Abonnement> {
    return this.http.put<Abonnement>(`${this.api}/abonnements/${id}`, dto);
  }

  supprimerAbonnement(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/abonnements/${id}`);
  }

  obtenirEcheances(id: string): Observable<EcheanceAbonnement[]> {
    return this.http.get<EcheanceAbonnement[]>(`${this.api}/abonnements/${id}/echeances`);
  }

  listerToutesEcheances(params: ListParams): Observable<PageResponse<EcheanceAbonnement>> {
    return this.http.get<PageResponse<EcheanceAbonnement>>(`${this.api}/abonnements/echeances`, {
      params: toHttpParams(params)
    });
  }

  genererEcheances(id: string): Observable<EcheanceAbonnement[]> {
    return this.http.post<EcheanceAbonnement[]>(`${this.api}/abonnements/${id}/generer-echeances`, {});
  }

  genererBcPourEcheance(abonnementId: string, echeanceId: string): Observable<EcheanceAbonnement> {
    return this.http.post<EcheanceAbonnement>(
      `${this.api}/abonnements/${abonnementId}/echeances/${echeanceId}/generer-bc`,
      {}
    );
  }

  rapprocherFacture(
    abonnementId: string,
    echeanceId: string,
    dto: RapprochementFactureRequest
  ): Observable<EcheanceAbonnement> {
    return this.http.post<EcheanceAbonnement>(
      `${this.api}/abonnements/${abonnementId}/echeances/${echeanceId}/rapprocher-facture`,
      dto
    );
  }
}
