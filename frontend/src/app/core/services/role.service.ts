import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Role, RoleRequestDto } from '../models/role.model';

@Injectable({
  providedIn: 'root'
})
export class RoleService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/roles`;

  listerOuFiltrer(code?: string, libelle?: string): Observable<Role[]> {
    let params = new HttpParams();
    if (code) params = params.set('code', code);
    if (libelle) params = params.set('libelle', libelle);
    return this.http.get<Role[]>(this.baseUrl, { params });
  }

  obtenirParId(id: string): Observable<Role> {
    return this.http.get<Role>(`${this.baseUrl}/${id}`);
  }

  creer(request: RoleRequestDto): Observable<Role> {
    return this.http.post<Role>(this.baseUrl, request);
  }

  modifier(id: string, request: RoleRequestDto): Observable<Role> {
    return this.http.put<Role>(`${this.baseUrl}/${id}`, request);
  }

  supprimer(id: string): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.baseUrl}/${id}`);
  }

  exporterCsv(code?: string, libelle?: string): Observable<Blob> {
    let params = new HttpParams();
    if (code) params = params.set('code', code);
    if (libelle) params = params.set('libelle', libelle);
    return this.http.get(`${this.baseUrl}/export/csv`, { params, responseType: 'blob' });
  }

  exporterPdf(code?: string, libelle?: string): Observable<Blob> {
    let params = new HttpParams();
    if (code) params = params.set('code', code);
    if (libelle) params = params.set('libelle', libelle);
    return this.http.get(`${this.baseUrl}/export/pdf`, { params, responseType: 'blob' });
  }

  importerCsv(file: File): Observable<{ message: string; elementsImportes: number }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ message: string; elementsImportes: number }>(`${this.baseUrl}/import/csv`, formData);
  }
}
