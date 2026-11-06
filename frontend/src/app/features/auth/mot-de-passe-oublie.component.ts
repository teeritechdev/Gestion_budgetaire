import { Component, OnDestroy, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { AuthShellComponent } from './auth-shell.component';
import { DatePickerComponent } from '../../shared/components/date-picker.component';
import { PasswordInputComponent } from '../../shared/components/password-input.component';
import { PasswordStrengthComponent, motDePasseConforme } from '../../shared/components/password-strength.component';

type Etape = 'identite' | 'nouveau' | 'termine';

/**
 * Mot de passe oublié, sans e-mail ni SMS : vérification de l'identité avec le dossier du personnel
 * (identifiant, matricule, N° CNIB, date de naissance), puis choix d'un nouveau mot de passe.
 */
@Component({
  selector: 'app-mot-de-passe-oublie',
  standalone: true,
  imports: [FormsModule, RouterLink, AuthShellComponent, DatePickerComponent, PasswordInputComponent, PasswordStrengthComponent],
  template: `
    <app-auth-shell>
        <a routerLink="/login" class="mb-6 inline-flex items-center gap-1.5 text-sm font-semibold text-slate-600 hover:text-green-700">
          <svg aria-hidden="true" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 18l-6-6 6-6" /></svg>
          Retour à la connexion
        </a>
        <h1 class="text-2xl font-bold tracking-tight text-slate-900">Mot de passe oublié</h1>
        <p class="mt-1.5 text-sm text-slate-600">Réinitialisez votre mot de passe en vérifiant votre identité.</p>

        <ol class="mb-6 mt-6 flex items-center gap-2 text-xs font-semibold" aria-label="Étapes">
          @for (e of etapes; track e.id; let i = $index) {
            <li class="flex items-center gap-2">
              <span class="auth-etape" [class.auth-etape-active]="etape() === e.id" [class.auth-etape-faite]="rang(e.id) < rang(etape())"
                [attr.aria-current]="etape() === e.id ? 'step' : null">
                @if (rang(e.id) < rang(etape())) { ✓ } @else { {{ i + 1 }} }
              </span>
              <span class="hidden sm:inline" [class.text-slate-900]="etape() === e.id" [class.text-slate-500]="etape() !== e.id">{{ e.libelle }}</span>
              @if (i < etapes.length - 1) { <span aria-hidden="true" class="h-px w-6 bg-slate-300"></span> }
            </li>
          }
        </ol>

        <div class="auth-carte">
          @if (erreur()) {
            <p role="alert" class="mb-5 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2.5 text-sm text-rose-800">{{ erreur() }}</p>
          }

          @switch (etape()) {
            @case ('identite') {
              <h2 class="text-base font-semibold text-slate-900">Vérification de votre identité</h2>
              <p class="mt-1 text-sm text-slate-600">Saisissez les informations de votre dossier du personnel. Elles ne sont utilisées que pour confirmer qu’il s’agit bien de vous.</p>
              <form (ngSubmit)="verifier()" class="auth-formulaire mt-5 space-y-4" novalidate>
                <label class="block text-sm font-semibold text-slate-700">Identifiant de connexion <span class="champ-obligatoire" aria-hidden="true">*</span>
                  <input name="identifiant" required [(ngModel)]="identifiant" autocomplete="username"
                    class="mt-1.5 w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900">
                </label>
                <label class="block text-sm font-semibold text-slate-700">Matricule <span class="champ-obligatoire" aria-hidden="true">*</span>
                  <input name="matricule" required [(ngModel)]="matricule" autocomplete="off"
                    class="mt-1.5 w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 font-mono text-sm uppercase text-slate-900">
                </label>
                <div class="grid gap-4 sm:grid-cols-2">
                  <label class="block text-sm font-semibold text-slate-700">N° CNIB <span class="champ-obligatoire" aria-hidden="true">*</span>
                    <input name="cnib" required [(ngModel)]="numeroCnib" (ngModelChange)="numeroCnib = normaliserCnib($event)"
                      autocomplete="off" maxlength="16"
                      class="mt-1.5 w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 font-mono text-sm uppercase text-slate-900">
                    <span class="mt-1 block text-xs font-normal text-slate-500">B suivi de chiffres, ex. B12345678.</span>
                  </label>
                  <label class="block text-sm font-semibold text-slate-700">Date de naissance <span class="champ-obligatoire" aria-hidden="true">*</span>
                    <app-date-picker name="dateNaissance" required [(ngModel)]="dateNaissance" class="auth-date mt-1.5 block" />
                  </label>
                </div>
                <button type="submit" [disabled]="enCours() || !identiteComplete()"
                  class="w-full rounded-lg bg-green-700 px-4 py-3 text-sm font-semibold text-white hover:bg-green-600 disabled:opacity-50">
                  {{ enCours() ? 'Vérification…' : 'Vérifier mon identité' }}
                </button>
              </form>
            }

            @case ('nouveau') {
              <h2 class="text-base font-semibold text-slate-900">Choisissez un nouveau mot de passe</h2>
              <p class="mt-1 text-sm text-slate-600">Identité confirmée. Ce lien reste valable <strong>{{ minutesRestantes() }} min</strong>.</p>
              <form (ngSubmit)="reinitialiser()" class="auth-formulaire mt-5 space-y-4" novalidate>
                <label class="block text-sm font-semibold text-slate-700">Nouveau mot de passe <span class="champ-obligatoire" aria-hidden="true">*</span>
                  <app-password-input name="nouveau" required [(ngModel)]="nouveau" autocomplete="new-password" class="mt-1.5 block" />
                </label>
                <app-password-strength [motDePasse]="nouveau" [identifiant]="identifiant" />
                <label class="block text-sm font-semibold text-slate-700">Confirmer le mot de passe <span class="champ-obligatoire" aria-hidden="true">*</span>
                  <app-password-input name="confirmation" required [(ngModel)]="confirmation" autocomplete="new-password" class="mt-1.5 block" />
                  @if (confirmation && confirmation !== nouveau) {
                    <span class="mt-1 block text-xs font-normal text-rose-700">Les deux mots de passe ne correspondent pas.</span>
                  }
                </label>
                <button type="submit" [disabled]="enCours() || !nouveauValide()"
                  class="w-full rounded-lg bg-green-700 px-4 py-3 text-sm font-semibold text-white hover:bg-green-600 disabled:opacity-50">
                  {{ enCours() ? 'Enregistrement…' : 'Réinitialiser le mot de passe' }}
                </button>
              </form>
            }

            @case ('termine') {
              <div class="py-2 text-center">
                <div class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-green-100 text-green-700">
                  <svg aria-hidden="true" class="h-7 w-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M5 12.5l4.5 4.5L19 7.5" /></svg>
                </div>
                <h2 class="text-lg font-semibold text-slate-900">Mot de passe réinitialisé</h2>
                <p class="mt-1 text-sm text-slate-600">Vous pouvez maintenant vous connecter avec votre nouveau mot de passe. Par sécurité, toutes vos sessions ouvertes ont été fermées.</p>
                <a routerLink="/login" [queryParams]="{ identifiant: identifiant }"
                  class="mt-6 inline-flex w-full items-center justify-center rounded-lg bg-green-700 px-4 py-3 text-sm font-semibold text-white hover:bg-green-600">Se connecter</a>
              </div>
            }
          }
        </div>

        <p class="mt-8 text-center text-xs text-slate-500">
          Informations incorrectes ou dossier incomplet ? Contactez votre administrateur : il peut vous attribuer un mot de passe temporaire.
        </p>
    </app-auth-shell>
  `,
})
export class MotDePasseOublieComponent implements OnDestroy {
  private readonly auth = inject(AuthService);

  readonly etapes: { id: Etape; libelle: string }[] = [
    { id: 'identite', libelle: 'Identité' },
    { id: 'nouveau', libelle: 'Mot de passe' },
    { id: 'termine', libelle: 'Terminé' },
  ];
  readonly etape = signal<Etape>('identite');
  readonly enCours = signal(false);
  readonly erreur = signal('');
  readonly minutesRestantes = signal(15);

  identifiant = '';
  matricule = '';
  numeroCnib = '';
  dateNaissance = '';
  nouveau = '';
  confirmation = '';
  private jeton = '';
  private expiration = 0;
  private minuteur: ReturnType<typeof setInterval> | null = null;

  normaliserCnib(valeur: string | null): string {
    return (valeur ?? '').replace(/\s/g, '').toUpperCase();
  }

  rang(etape: Etape): number {
    return this.etapes.findIndex((e) => e.id === etape);
  }

  identiteComplete(): boolean {
    return !!(this.identifiant.trim() && this.matricule.trim() && /^B\d{5,15}$/.test(this.numeroCnib) && this.dateNaissance);
  }

  nouveauValide(): boolean {
    return motDePasseConforme(this.nouveau, this.identifiant) && this.nouveau === this.confirmation;
  }

  verifier(): void {
    if (!this.identiteComplete()) return;
    this.enCours.set(true);
    this.erreur.set('');
    this.auth.verifierIdentite({
      identifiant: this.identifiant.trim(),
      matricule: this.matricule.trim(),
      numeroCnib: this.numeroCnib,
      dateNaissance: this.dateNaissance,
    }).subscribe({
      next: (reponse) => {
        this.enCours.set(false);
        this.jeton = reponse.jeton;
        this.expiration = Date.now() + reponse.validiteMinutes * 60_000;
        this.demarrerMinuteur();
        this.etape.set('nouveau');
      },
      error: (erreur) => {
        this.enCours.set(false);
        this.erreur.set(erreur?.error?.message || 'La vérification a échoué. Réessayez plus tard.');
      },
    });
  }

  reinitialiser(): void {
    if (!this.nouveauValide()) return;
    if (Date.now() > this.expiration) {
      this.retourIdentite('Le délai de 15 minutes est dépassé. Recommencez la vérification.');
      return;
    }
    this.enCours.set(true);
    this.erreur.set('');
    this.auth.reinitialiserMotDePasse(this.jeton, this.nouveau).subscribe({
      next: () => {
        this.enCours.set(false);
        this.arreterMinuteur();
        this.jeton = '';
        this.nouveau = this.confirmation = '';
        this.etape.set('termine');
      },
      error: (erreur) => {
        this.enCours.set(false);
        this.erreur.set(erreur?.error?.message || 'La réinitialisation a échoué.');
      },
    });
  }

  ngOnDestroy(): void {
    this.arreterMinuteur();
  }

  private demarrerMinuteur(): void {
    this.arreterMinuteur();
    const maj = () => {
      const restant = Math.ceil((this.expiration - Date.now()) / 60_000);
      if (restant <= 0) this.retourIdentite('Le délai de 15 minutes est dépassé. Recommencez la vérification.');
      else this.minutesRestantes.set(restant);
    };
    maj();
    this.minuteur = setInterval(maj, 15_000);
  }

  private arreterMinuteur(): void {
    if (this.minuteur) clearInterval(this.minuteur);
    this.minuteur = null;
  }

  private retourIdentite(message: string): void {
    this.arreterMinuteur();
    this.jeton = '';
    this.etape.set('identite');
    this.erreur.set(message);
  }
}
