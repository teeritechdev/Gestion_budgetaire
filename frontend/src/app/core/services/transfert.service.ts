import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export type FormatExport = 'csv' | 'pdf';

export interface ResultatImport {
  message: string;
  elementsImportes: number;
}

/**
 * Exports et imports communs à tous les onglets.
 * L'export reçoit les identifiants des lignes affichées (après filtres et tri) : le fichier
 * produit par le backend contient exactement ces lignes, dans le même ordre.
 */
@Injectable({ providedIn: 'root' })
export class TransfertService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  exporter(ressource: string, format: FormatExport, ids: readonly (string | undefined | null)[]): Observable<void> {
    const selection = ids.filter((id): id is string => !!id);
    return this.http
      .post(`${this.api}/exports/${ressource}/${format}`, { ids: selection }, { observe: 'response', responseType: 'blob' })
      .pipe(
        tap((response) => this.telecharger(response, `${ressource}.${format}`)),
        map(() => undefined),
      );
  }

  importer(ressource: string, fichier: File): Observable<ResultatImport> {
    const formData = new FormData();
    formData.append('file', fichier);
    return this.http.post<ResultatImport>(`${this.api}/imports/${ressource}`, formData);
  }

  /** Génère et télécharge un modèle CSV (en-tête + lignes d'exemple), séparateur « ; ». */
  telechargerModele(nomFichier: string, colonnes: readonly string[], exemples: readonly (readonly string[])[]): void {
    const cellule = (valeur: string) => (/[;"\n]/.test(valeur) ? `"${valeur.replace(/"/g, '""')}"` : valeur);
    const lignes = [colonnes, ...exemples].map((ligne) => ligne.map(cellule).join(';'));
    const blob = new Blob(['﻿' + lignes.join('\n') + '\n'], { type: 'text/csv;charset=utf-8' });
    this.enregistrer(blob, nomFichier);
  }

  private telecharger(response: HttpResponse<Blob>, nomParDefaut: string): void {
    const disposition = response.headers.get('Content-Disposition') || '';
    const correspondance = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(disposition);
    const nom = correspondance ? decodeURIComponent(correspondance[1]) : nomParDefaut;
    if (response.body) this.enregistrer(response.body, nom);
  }

  private enregistrer(blob: Blob, nom: string): void {
    const url = URL.createObjectURL(blob);
    const lien = document.createElement('a');
    lien.href = url;
    lien.download = nom;
    document.body.appendChild(lien);
    lien.click();
    document.body.removeChild(lien);
    URL.revokeObjectURL(url);
  }
}
