import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Utilisateur, UtilisateurRequest } from '../models/utilisateur.model';

@Injectable({
  providedIn: 'root'
})
export class UtilisateurService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/utilisateurs`;

  listerOuFiltrer(serviceId?: string, roleId?: string, actif?: boolean, recherche?: string): Observable<Utilisateur[]> {
    let params = new HttpParams();
    if (serviceId) params = params.set('serviceId', serviceId);
    if (roleId) params = params.set('roleId', roleId);
    if (actif !== undefined && actif !== null) params = params.set('actif', actif.toString());
    if (recherche) params = params.set('recherche', recherche);
    return this.http.get<Utilisateur[]>(this.baseUrl, { params });
  }

  obtenirParId(id: string): Observable<Utilisateur> {
    return this.http.get<Utilisateur>(`${this.baseUrl}/${id}`);
  }

  creer(utilisateur: UtilisateurRequest, motDePasse: string): Observable<Utilisateur> {
    return this.http.post<Utilisateur>(this.baseUrl, { utilisateur, motDePasse });
  }

  modifier(id: string, utilisateur: UtilisateurRequest, nouveauMotDePasse?: string): Observable<Utilisateur> {
    return this.http.put<Utilisateur>(`${this.baseUrl}/${id}`, { utilisateur, motDePasse: nouveauMotDePasse || null });
  }

  supprimer(id: string): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.baseUrl}/${id}`);
  }

  exporterCsv(serviceId?: string, roleId?: string, actif?: boolean, recherche?: string): Observable<Blob> {
    let params = new HttpParams();
    if (serviceId) params = params.set('serviceId', serviceId);
    if (roleId) params = params.set('roleId', roleId);
    if (actif !== undefined && actif !== null) params = params.set('actif', actif.toString());
    if (recherche) params = params.set('recherche', recherche);
    return this.http.get(`${this.baseUrl}/export/csv`, { params, responseType: 'blob' });
  }

  exporterPdf(serviceId?: string, roleId?: string, actif?: boolean, recherche?: string): Observable<Blob> {
    let params = new HttpParams();
    if (serviceId) params = params.set('serviceId', serviceId);
    if (roleId) params = params.set('roleId', roleId);
    if (actif !== undefined && actif !== null) params = params.set('actif', actif.toString());
    if (recherche) params = params.set('recherche', recherche);
    return this.http.get(`${this.baseUrl}/export/pdf`, { params, responseType: 'blob' });
  }

  importerCsv(file: File): Observable<{ message: string; elementsImportes: number }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ message: string; elementsImportes: number }>(`${this.baseUrl}/import/csv`, formData);
  }
}
