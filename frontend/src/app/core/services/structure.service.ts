import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Structure } from '../models/structure.model';

@Injectable({
  providedIn: 'root'
})
export class StructureService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/structures`;

  listerOuFiltrer(nom?: string): Observable<Structure[]> {
    let params = new HttpParams();
    if (nom) params = params.set('nom', nom);
    return this.http.get<Structure[]>(this.baseUrl, { params });
  }

  obtenirParCode(code: string): Observable<Structure> {
    return this.http.get<Structure>(`${this.baseUrl}/${code}`);
  }

  creer(structure: Partial<Structure>): Observable<Structure> {
    return this.http.post<Structure>(this.baseUrl, structure);
  }

  modifier(code: string, structure: Partial<Structure>): Observable<Structure> {
    return this.http.put<Structure>(`${this.baseUrl}/${code}`, structure);
  }

  supprimer(code: string): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.baseUrl}/${code}`);
  }

  exporterCsv(nom?: string): Observable<Blob> {
    let params = new HttpParams();
    if (nom) params = params.set('nom', nom);
    return this.http.get(`${this.baseUrl}/export/csv`, { params, responseType: 'blob' });
  }

  exporterPdf(nom?: string): Observable<Blob> {
    let params = new HttpParams();
    if (nom) params = params.set('nom', nom);
    return this.http.get(`${this.baseUrl}/export/pdf`, { params, responseType: 'blob' });
  }

  importerCsv(file: File): Observable<{ message: string; elementsImportes: number }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ message: string; elementsImportes: number }>(`${this.baseUrl}/import/csv`, formData);
  }
}
