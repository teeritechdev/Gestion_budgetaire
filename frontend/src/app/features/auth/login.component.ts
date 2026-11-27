import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { PasswordInputComponent } from '../../shared/components/password-input.component';
import { AuthShellComponent } from './auth-shell.component';

const CLE_IDENTIFIANT = 'uab_budget_identifiant';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink, PasswordInputComponent, AuthShellComponent],
  template: `
    <app-auth-shell>
      <div class="text-center">
        <h1 class="text-2xl font-bold tracking-tight text-slate-900">Connexion</h1>
        <p class="mt-1.5 text-sm text-slate-600">Connectez-vous pour accéder à votre espace.</p>
      </div>

      @if (erreur()) {
        <div role="alert" class="mt-6 flex items-start gap-2.5 rounded-lg border border-rose-200 bg-rose-50 px-3.5 py-3 text-sm text-rose-800">
          <svg aria-hidden="true" class="mt-0.5 h-4 w-4 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="9" /><path d="M12 8v4.5m0 3.5h.01" /></svg>
          <span>{{ erreur() }}</span>
        </div>
      }

      <form (ngSubmit)="seConnecter()" class="auth-formulaire mt-6 space-y-5" novalidate>
        <label class="block text-sm font-semibold text-slate-700">Identifiant
          <input name="username" type="text" required autocomplete="username" autocapitalize="none" spellcheck="false"
            [(ngModel)]="identifiant" autofocus
            class="mt-1.5 w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900">
        </label>

        <div>
          <label for="mot-de-passe" class="text-sm font-semibold text-slate-700">Mot de passe</label>
          <app-password-input inputId="mot-de-passe" name="password" required [(ngModel)]="motDePasse" autocomplete="current-password"
            (keydown)="detecterMajuscules($event)" (keyup)="detecterMajuscules($event)" class="mt-1.5 block" />
          @if (majusculesActives()) {
            <p class="mt-1.5 flex items-center gap-1.5 text-xs font-medium text-amber-700">
              <svg aria-hidden="true" class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 4l7 8h-4v4H9v-4H5zM9 20h6" /></svg>
              Verrouillage des majuscules activé
            </p>
          }
        </div>

        <label class="flex cursor-pointer items-center gap-2.5 text-sm text-slate-700">
          <input type="checkbox" name="memoriser" [(ngModel)]="memoriser" class="h-4 w-4 accent-green-700">
          Mémoriser mon identifiant
        </label>

        <button type="submit" [disabled]="enCours()"
          class="flex w-full items-center justify-center gap-2 rounded-lg bg-green-700 px-4 py-3 text-sm font-semibold text-white hover:bg-green-600 disabled:cursor-wait disabled:opacity-70">
          @if (enCours()) {
            <svg aria-hidden="true" class="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="3" class="opacity-25" /><path d="M21 12a9 9 0 00-9-9" stroke="currentColor" stroke-width="3" stroke-linecap="round" /></svg>
            Connexion…
          } @else {
            Se connecter
          }
        </button>
      </form>

      <div class="mt-5 text-center">
        <a routerLink="/mot-de-passe-oublie" class="text-sm font-semibold text-green-700 hover:underline">Mot de passe oublié ?</a>
      </div>

      <p class="mt-8 text-center text-xs text-slate-500 sm:whitespace-nowrap">Un problème de connexion ? Contactez votre administrateur.</p>
    </app-auth-shell>
  `,
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly notify = inject(NotificationService);

  readonly enCours = signal(false);
  readonly erreur = signal<string | null>(null);
  readonly majusculesActives = signal(false);

  identifiant = '';
  motDePasse = '';
  memoriser = false;

  constructor() {
    const memorise = lireIdentifiantMemorise();
    // Retour de « mot de passe oublié » : l'identifiant est prérempli.
    const transmis = this.route.snapshot.queryParamMap.get('identifiant');
    this.identifiant = transmis || memorise || '';
    this.memoriser = !!memorise;
  }

  detecterMajuscules(event: KeyboardEvent): void {
    if (typeof event.getModifierState === 'function') {
      this.majusculesActives.set(event.getModifierState('CapsLock'));
    }
  }

  seConnecter(): void {
    const identifiant = this.identifiant.trim();
    if (!identifiant || !this.motDePasse) {
      this.erreur.set('Renseignez votre identifiant et votre mot de passe.');
      return;
    }
    this.enCours.set(true);
    this.erreur.set(null);

    this.authService.login({ username: identifiant, password: this.motDePasse }).subscribe({
      next: (reponse) => {
        this.enCours.set(false);
        memoriserIdentifiant(this.memoriser ? identifiant : null);
        this.motDePasse = '';
        if (reponse.doitChangerMotDePasse) {
          this.notify.warning('Votre mot de passe est temporaire : choisissez-en un nouveau pour continuer.', 'Mot de passe à changer');
          this.router.navigate(['/profil'], { queryParams: { onglet: 'securite' } });
          return;
        }
        this.notify.success(`Bienvenue, ${reponse.prenom} ${reponse.nom}.`);
        this.router.navigateByUrl(this.pageDemandee());
      },
      error: (erreur) => {
        this.enCours.set(false);
        if (erreur.status === 401 || erreur.status === 403) {
          this.erreur.set('Identifiant ou mot de passe incorrect.');
        } else if (erreur.status === 0) {
          this.erreur.set('Le serveur est injoignable. Vérifiez votre connexion puis réessayez.');
        } else {
          this.erreur.set(erreur.error?.message || 'La connexion a échoué. Réessayez dans un instant.');
        }
      },
    });
  }

  /** Page demandée avant la redirection vers la connexion (adresse interne uniquement). */
  private pageDemandee(): string {
    const retour = this.route.snapshot.queryParamMap.get('returnUrl');
    return retour && retour.startsWith('/') && !retour.startsWith('//') && !retour.startsWith('/login') ? retour : '/dashboard';
  }
}

function lireIdentifiantMemorise(): string | null {
  try {
    return localStorage.getItem(CLE_IDENTIFIANT);
  } catch {
    return null;
  }
}

function memoriserIdentifiant(identifiant: string | null): void {
  try {
    if (identifiant) localStorage.setItem(CLE_IDENTIFIANT, identifiant);
    else localStorage.removeItem(CLE_IDENTIFIANT);
  } catch {
    // Stockage indisponible (navigation privée) : l'identifiant n'est simplement pas mémorisé.
  }
}
