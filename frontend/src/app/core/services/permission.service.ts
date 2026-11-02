import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Permission } from '../models/permission.model';

@Injectable({
  providedIn: 'root'
})
export class PermissionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/permissions`;

  listerOuFiltrer(code?: string, libelle?: string, module?: string): Observable<Permission[]> {
    let params = new HttpParams();
    if (code) params = params.set('code', code);
    if (libelle) params = params.set('libelle', libelle);
    if (module) params = params.set('module', module);
    return this.http.get<Permission[]>(this.baseUrl, { params });
  }

  obtenirParId(id: string): Observable<Permission> {
    return this.http.get<Permission>(`${this.baseUrl}/${id}`);
  }

  creer(permission: Partial<Permission>): Observable<Permission> {
    return this.http.post<Permission>(this.baseUrl, permission);
  }

  modifier(id: string, permission: Partial<Permission>): Observable<Permission> {
    return this.http.put<Permission>(`${this.baseUrl}/${id}`, permission);
  }

  supprimer(id: string): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.baseUrl}/${id}`);
  }

  exporterCsv(code?: string, libelle?: string, module?: string): Observable<Blob> {
    let params = new HttpParams();
    if (code) params = params.set('code', code);
    if (libelle) params = params.set('libelle', libelle);
    if (module) params = params.set('module', module);
    return this.http.get(`${this.baseUrl}/export/csv`, { params, responseType: 'blob' });
  }

  exporterPdf(code?: string, libelle?: string, module?: string): Observable<Blob> {
    let params = new HttpParams();
    if (code) params = params.set('code', code);
    if (libelle) params = params.set('libelle', libelle);
    if (module) params = params.set('module', module);
    return this.http.get(`${this.baseUrl}/export/pdf`, { params, responseType: 'blob' });
  }

  importerCsv(file: File): Observable<{ message: string; elementsImportes: number }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ message: string; elementsImportes: number }>(`${this.baseUrl}/import/csv`, formData);
  }
}
