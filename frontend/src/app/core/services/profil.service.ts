import { Injectable, effect, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { JwtResponse } from '../models/auth.models';
import { Profil } from '../models/profil.model';
import { AuthService } from './auth.service';

/** Profil de l'utilisateur connecté : informations, mot de passe et photo (partagée par l'en-tête et le menu). */
@Injectable({ providedIn: 'root' })
export class ProfilService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly baseUrl = `${environment.apiUrl}/profil`;

  /** URL locale (blob:) de la photo de profil, ou null s'il n'y en a pas. */
  readonly photoUrl = signal<string | null>(null);
  private photoChargeePour: string | null = null;

  constructor() {
    // La photo suit la session : chargée à la connexion, libérée à la déconnexion.
    effect(() => {
      const user = this.auth.currentUser();
      const cle = user && user.avecPhoto ? user.id : null;
      if (cle && cle !== this.photoChargeePour) {
        this.photoChargeePour = cle;
        this.chargerPhoto();
      } else if (!cle) {
        this.photoChargeePour = null;
        this.remplacerPhoto(null);
      }
    });
  }

  profil(): Observable<Profil> {
    return this.http.get<Profil>(this.baseUrl);
  }

  /** Change le mot de passe ; la session est remplacée par celle renvoyée (les autres sont fermées). */
  changerMotDePasse(motDePasseActuel: string, nouveauMotDePasse: string): Observable<JwtResponse> {
    return this.http
      .put<JwtResponse>(`${this.baseUrl}/mot-de-passe`, { motDePasseActuel, nouveauMotDePasse })
      .pipe(tap((session) => this.auth.ouvrirSession(session)));
  }

  envoyerPhoto(image: Blob): Observable<Profil> {
    const donnees = new FormData();
    donnees.append('fichier', image, 'photo.jpg');
    return this.http.put<Profil>(`${this.baseUrl}/photo`, donnees).pipe(
      tap(() => {
        this.remplacerPhoto(URL.createObjectURL(image));
        this.photoChargeePour = this.auth.currentUser()?.id ?? null;
        this.auth.mettreAJourSession({ avecPhoto: true });
      }),
    );
  }

  supprimerPhoto(): Observable<Profil> {
    return this.http.delete<Profil>(`${this.baseUrl}/photo`).pipe(
      tap(() => {
        this.remplacerPhoto(null);
        this.auth.mettreAJourSession({ avecPhoto: false });
      }),
    );
  }

  private chargerPhoto(): void {
    this.http.get(`${this.baseUrl}/photo`, { responseType: 'blob' }).subscribe({
      next: (blob) => this.remplacerPhoto(URL.createObjectURL(blob)),
      error: () => this.remplacerPhoto(null),
    });
  }

  private remplacerPhoto(url: string | null): void {
    const ancienne = this.photoUrl();
    if (ancienne) URL.revokeObjectURL(ancienne);
    this.photoUrl.set(url);
  }
}
