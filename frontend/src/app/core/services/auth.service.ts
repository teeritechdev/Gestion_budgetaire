import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CurrentUser, JwtResponse, LoginRequest } from '../models/auth.models';

const STORAGE_KEY = 'uab_budget_user';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly baseUrl = `${environment.apiUrl}/auth`;

  private readonly _currentUser = signal<CurrentUser | null>(this.loadUserFromStorage());
  public readonly currentUser = this._currentUser.asReadonly();

  // Méthode (et non computed) : l'expiration dépend de l'heure courante, pas seulement du signal.
  public isAuthenticated(): boolean {
    const token = this._currentUser()?.token;
    return !!token && !isTokenExpired(token);
  }
  public readonly token = computed(() => this._currentUser()?.token ?? null);
  public readonly role = computed(() => this._currentUser()?.role ?? '');
  public readonly permissions = computed(() => this._currentUser()?.permissions ?? []);

  login(credentials: LoginRequest): Observable<JwtResponse> {
    return this.http.post<JwtResponse>(`${this.baseUrl}/login`, credentials).pipe(
      tap((res) => this.ouvrirSession(res))
    );
  }

  /** Enregistre une session (connexion, ou nouveau jeton après un changement de mot de passe). */
  ouvrirSession(res: JwtResponse): void {
    this.saveUser({
      id: res.id,
      username: res.username,
      email: res.email,
      nom: res.nom,
      prenom: res.prenom,
      role: res.role,
      permissions: res.permissions || [],
      token: res.token,
      doitChangerMotDePasse: !!res.doitChangerMotDePasse,
      avecPhoto: !!res.avecPhoto,
    });
  }

  /** Met à jour un indicateur de la session courante sans changer de jeton. */
  mettreAJourSession(modifications: Partial<Pick<CurrentUser, 'avecPhoto' | 'doitChangerMotDePasse'>>): void {
    const user = this._currentUser();
    if (user) this.saveUser({ ...user, ...modifications });
  }

  /** Mot de passe oublié, étape 1 : renvoie un jeton de réinitialisation (15 minutes). */
  verifierIdentite(demande: { identifiant: string; matricule: string; numeroCnib: string; dateNaissance: string }) {
    return this.http.post<{ jeton: string; validiteMinutes: number }>(`${this.baseUrl}/mot-de-passe-oublie/verification`, demande);
  }

  /** Mot de passe oublié, étape 2. */
  reinitialiserMotDePasse(jeton: string, nouveauMotDePasse: string) {
    return this.http.post<{ message: string }>(`${this.baseUrl}/mot-de-passe-oublie/reinitialisation`, { jeton, nouveauMotDePasse });
  }

  logout(): void {
    // Le backend révoque le token présenté : il ne pourra plus être réutilisé.
    if (this._currentUser()?.token) {
      this.http.post(`${this.baseUrl}/logout`, {}).subscribe({
        next: () => {},
        error: () => {}
      });
    }

    this.clearSession();
    this.router.navigate(['/login']);
  }

  /** Supprime la session locale (token expiré ou déconnexion). */
  clearSession(): void {
    localStorage.removeItem(STORAGE_KEY);
    this._currentUser.set(null);
  }

  hasPermission(permissionCode: string): boolean {
    const user = this._currentUser();
    if (!user) return false;
    // Si ADMIN_SYSTEME a tout pouvoir ou si le code est dans la liste
    if (user.role === 'ADMIN_SYSTEME') return true;
    return user.permissions.includes(permissionCode);
  }

  hasAnyPermission(permissions: string[]): boolean {
    return permissions.some(p => this.hasPermission(p));
  }

  private saveUser(user: CurrentUser): void {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
    } catch (e) {
      console.error('Erreur de stockage de session', e);
    }
    this._currentUser.set(user);
  }

  private loadUserFromStorage(): CurrentUser | null {
    try {
      const data = localStorage.getItem(STORAGE_KEY);
      if (data) {
        const user = JSON.parse(data) as CurrentUser;
        if (!user?.token || isTokenExpired(user.token)) {
          localStorage.removeItem(STORAGE_KEY);
          return null;
        }
        return user;
      }
    } catch (e) {
      console.error('Erreur lecture session', e);
    }
    return null;
  }
}

/** Lit la date d'expiration (claim `exp`) du JWT ; un token illisible est considéré comme expiré. */
export function isTokenExpired(token: string): boolean {
  try {
    const payload = token.split('.')[1];
    if (!payload) return true;
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
    const { exp } = JSON.parse(atob(padded)) as { exp?: number };
    return typeof exp !== 'number' || exp * 1000 <= Date.now();
  } catch {
    return true;
  }
}
