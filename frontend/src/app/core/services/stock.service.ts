import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/page-response.model';
import { Article, ArticleRequest, StockKpis } from '../models/stock/article.model';
import {
  DemandeMagasin,
  DemandeMagasinRequest,
  ServirDemande,
  ValidationDecision,
} from '../models/stock/demande-magasin.model';
import { MouvementStock, MouvementStockRequest } from '../models/stock/mouvement-stock.model';

/** Paramètres de liste : pagination serveur + filtres (les valeurs vides sont ignorées). */
export type ListParams = Record<string, string | number | boolean | null | undefined>;

function toHttpParams(params: ListParams): HttpParams {
  let httpParams = new HttpParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== null && value !== undefined && value !== '') httpParams = httpParams.set(key, String(value));
  }
  return httpParams;
}

/** Module 3 : articles, demandes magasin, mouvements de stock. */
@Injectable({ providedIn: 'root' })
export class StockService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  // Articles
  obtenirKpis(): Observable<StockKpis> {
    return this.http.get<StockKpis>(`${this.api}/articles/kpis`);
  }
  listerArticles(params: ListParams): Observable<PageResponse<Article>> {
    return this.http.get<PageResponse<Article>>(`${this.api}/articles`, { params: toHttpParams(params) });
  }
  creerArticle(dto: ArticleRequest): Observable<Article> {
    return this.http.post<Article>(`${this.api}/articles`, dto);
  }
  modifierArticle(id: string, dto: ArticleRequest): Observable<Article> {
    return this.http.put<Article>(`${this.api}/articles/${id}`, dto);
  }

  // Demandes magasin
  listerDemandes(params: ListParams): Observable<PageResponse<DemandeMagasin>> {
    return this.http.get<PageResponse<DemandeMagasin>>(`${this.api}/demandes-magasin`, { params: toHttpParams(params) });
  }
  creerDemande(dto: DemandeMagasinRequest): Observable<DemandeMagasin> {
    return this.http.post<DemandeMagasin>(`${this.api}/demandes-magasin`, dto);
  }
  validerDemande(id: string, dto: ValidationDecision): Observable<DemandeMagasin> {
    return this.http.put<DemandeMagasin>(`${this.api}/demandes-magasin/${id}/valider-n1`, dto);
  }
  servirDemande(id: string, dto: ServirDemande): Observable<DemandeMagasin> {
    return this.http.put<DemandeMagasin>(`${this.api}/demandes-magasin/${id}/servir`, dto);
  }
  telechargerBonDeSortiePdf(id: string): Observable<Blob> {
    return this.http.get(`${this.api}/demandes-magasin/${id}/pdf`, { responseType: 'blob' });
  }

  // Mouvements
  listerMouvements(params: ListParams): Observable<PageResponse<MouvementStock>> {
    return this.http.get<PageResponse<MouvementStock>>(`${this.api}/mouvements-stock`, { params: toHttpParams(params) });
  }
  creerMouvement(dto: MouvementStockRequest): Observable<MouvementStock> {
    return this.http.post<MouvementStock>(`${this.api}/mouvements-stock`, dto);
  }
}
