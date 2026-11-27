import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuditLog, AuditLogDetail } from '../models/audit-log.model';

@Injectable({
  providedIn: 'root'
})
export class AuditLogService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/audit-logs`;

  listerOuFiltrer(utilisateurId?: string, typeAction?: string, nomTable?: string, startDate?: string, endDate?: string): Observable<AuditLog[]> {
    let params = new HttpParams();
    if (utilisateurId) params = params.set('utilisateurId', utilisateurId);
    if (typeAction) params = params.set('typeAction', typeAction);
    if (nomTable) params = params.set('nomTable', nomTable);
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get<AuditLog[]>(this.baseUrl, { params });
  }

  obtenirParId(id: string): Observable<AuditLog> {
    return this.http.get<AuditLog>(`${this.baseUrl}/${id}`);
  }

  /** Auteur résolu, navigateur, requête et valeurs champ par champ. */
  detail(id: string): Observable<AuditLogDetail> {
    return this.http.get<AuditLogDetail>(`${this.baseUrl}/${id}/detail`);
  }

  exporterCsv(utilisateurId?: string, typeAction?: string, nomTable?: string, startDate?: string, endDate?: string): Observable<Blob> {
    let params = new HttpParams();
    if (utilisateurId) params = params.set('utilisateurId', utilisateurId);
    if (typeAction) params = params.set('typeAction', typeAction);
    if (nomTable) params = params.set('nomTable', nomTable);
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get(`${this.baseUrl}/export/csv`, { params, responseType: 'blob' });
  }

  exporterPdf(utilisateurId?: string, typeAction?: string, nomTable?: string, startDate?: string, endDate?: string): Observable<Blob> {
    let params = new HttpParams();
    if (utilisateurId) params = params.set('utilisateurId', utilisateurId);
    if (typeAction) params = params.set('typeAction', typeAction);
    if (nomTable) params = params.set('nomTable', nomTable);
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get(`${this.baseUrl}/export/pdf`, { params, responseType: 'blob' });
  }
}
