import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/page-response.model';
import { Fournisseur, FournisseurRequest } from '../models/achat/fournisseur.model';
import {
  DemandeAchat,
  DemandeAchatRequest,
  HistoriqueValidation,
  DecisionValidationRequest,
  VentilationImpact,
  VentilationImpactRequest
} from '../models/achat/demande-achat.model';
import {
  BonCommande,
  CreerBcDepuisDaRequest
} from '../models/achat/bon-commande.model';

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
export class AchatService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  // Fournisseurs
  listerFournisseurs(params: ListParams): Observable<PageResponse<Fournisseur>> {
    return this.http.get<PageResponse<Fournisseur>>(`${this.api}/fournisseurs`, { params: toHttpParams(params) });
  }

  obtenirFournisseur(id: string): Observable<Fournisseur> {
    return this.http.get<Fournisseur>(`${this.api}/fournisseurs/${id}`);
  }

  creerFournisseur(dto: FournisseurRequest): Observable<Fournisseur> {
    return this.http.post<Fournisseur>(`${this.api}/fournisseurs`, dto);
  }

  modifierFournisseur(id: string, dto: FournisseurRequest): Observable<Fournisseur> {
    return this.http.put<Fournisseur>(`${this.api}/fournisseurs/${id}`, dto);
  }

  // Demandes d'Achat
  listerDemandesAchat(params: ListParams): Observable<PageResponse<DemandeAchat>> {
    return this.http.get<PageResponse<DemandeAchat>>(`${this.api}/demandes-achat`, { params: toHttpParams(params) });
  }

  obtenirDemandeAchat(id: string): Observable<DemandeAchat> {
    return this.http.get<DemandeAchat>(`${this.api}/demandes-achat/${id}`);
  }

  creerDemandeAchat(dto: DemandeAchatRequest): Observable<DemandeAchat> {
    return this.http.post<DemandeAchat>(`${this.api}/demandes-achat`, dto);
  }

  modifierDemandeAchat(id: string, dto: DemandeAchatRequest): Observable<DemandeAchat> {
    return this.http.put<DemandeAchat>(`${this.api}/demandes-achat/${id}`, dto);
  }

  validerDemandeAchat(id: string, dto: DecisionValidationRequest): Observable<DemandeAchat> {
    return this.http.put<DemandeAchat>(`${this.api}/demandes-achat/${id}/valider`, dto);
  }

  obtenirHistoriqueValidations(daId: string): Observable<HistoriqueValidation[]> {
    return this.http.get<HistoriqueValidation[]>(`${this.api}/demandes-achat/${daId}/historique-validations`);
  }

  obtenirVentilations(daId: string): Observable<VentilationImpact[]> {
    return this.http.get<VentilationImpact[]>(`${this.api}/demandes-achat/${daId}/ventilations`);
  }

  enregistrerVentilations(daId: string, dtos: VentilationImpactRequest[]): Observable<VentilationImpact[]> {
    return this.http.put<VentilationImpact[]>(`${this.api}/demandes-achat/${daId}/ventilations`, dtos);
  }

  // Bons de Commande
  listerBonsCommande(params: ListParams): Observable<PageResponse<BonCommande>> {
    return this.http.get<PageResponse<BonCommande>>(`${this.api}/bons-commande`, { params: toHttpParams(params) });
  }

  obtenirBonCommande(id: string): Observable<BonCommande> {
    return this.http.get<BonCommande>(`${this.api}/bons-commande/${id}`);
  }

  creerBcDepuisDa(daId: string, dto: CreerBcDepuisDaRequest): Observable<BonCommande> {
    return this.http.post<BonCommande>(`${this.api}/bons-commande/depuis-da/${daId}`, dto);
  }

  telechargerBonCommandePdf(id: string): Observable<Blob> {
    return this.http.get(`${this.api}/bons-commande/${id}/pdf`, { responseType: 'blob' });
  }
}

