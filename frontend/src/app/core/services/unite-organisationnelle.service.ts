import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UniteOrganisationnelle } from '../models/unite-organisationnelle.model';

@Injectable({
  providedIn: 'root'
})
export class UniteOrganisationnelleService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/unites-organisationnelles`;

  listerOuFiltrer(entiteCode?: string, typeStructure?: string, actif?: boolean, libelle?: string): Observable<UniteOrganisationnelle[]> {
    let params = new HttpParams();
    if (entiteCode) params = params.set('entiteCode', entiteCode);
    if (typeStructure) params = params.set('typeStructure', typeStructure);
    if (actif !== undefined && actif !== null) params = params.set('actif', actif.toString());
    if (libelle) params = params.set('libelle', libelle);
    return this.http.get<UniteOrganisationnelle[]>(this.baseUrl, { params });
  }

  obtenirParId(id: string): Observable<UniteOrganisationnelle> {
    return this.http.get<UniteOrganisationnelle>(`${this.baseUrl}/${id}`);
  }

  creer(structure: Partial<UniteOrganisationnelle>): Observable<UniteOrganisationnelle> {
    return this.http.post<UniteOrganisationnelle>(this.baseUrl, structure);
  }

  modifier(id: string, structure: Partial<UniteOrganisationnelle>): Observable<UniteOrganisationnelle> {
    return this.http.put<UniteOrganisationnelle>(`${this.baseUrl}/${id}`, structure);
  }

  supprimer(id: string): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.baseUrl}/${id}`);
  }

  exporterCsv(entiteCode?: string, typeStructure?: string, actif?: boolean, libelle?: string): Observable<Blob> {
    let params = new HttpParams();
    if (entiteCode) params = params.set('entiteCode', entiteCode);
    if (typeStructure) params = params.set('typeStructure', typeStructure);
    if (actif !== undefined && actif !== null) params = params.set('actif', actif.toString());
    if (libelle) params = params.set('libelle', libelle);
    return this.http.get(`${this.baseUrl}/export/csv`, { params, responseType: 'blob' });
  }

  exporterPdf(entiteCode?: string, typeStructure?: string, actif?: boolean, libelle?: string): Observable<Blob> {
    let params = new HttpParams();
    if (entiteCode) params = params.set('entiteCode', entiteCode);
    if (typeStructure) params = params.set('typeStructure', typeStructure);
    if (actif !== undefined && actif !== null) params = params.set('actif', actif.toString());
    if (libelle) params = params.set('libelle', libelle);
    return this.http.get(`${this.baseUrl}/export/pdf`, { params, responseType: 'blob' });
  }

  importerCsv(file: File): Observable<{ message: string; elementsImportes: number }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ message: string; elementsImportes: number }>(`${this.baseUrl}/import/csv`, formData);
  }
}
