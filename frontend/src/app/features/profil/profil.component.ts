import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ProfilService } from '../../core/services/profil.service';
import { NotificationService } from '../../core/services/notification.service';
import { Profil } from '../../core/models/profil.model';
import { AvatarComponent } from '../../shared/components/avatar.component';
import { PasswordInputComponent } from '../../shared/components/password-input.component';
import { PasswordStrengthComponent, motDePasseConforme } from '../../shared/components/password-strength.component';
import { PhotoDialogComponent } from './photo-dialog.component';

type Onglet = 'informations' | 'securite';

@Component({
  selector: 'app-profil',
  standalone: true,
  imports: [CommonModule, FormsModule, AvatarComponent, PasswordInputComponent, PasswordStrengthComponent, PhotoDialogComponent],
  template: `
    <div class="mx-auto max-w-5xl space-y-6">
      <!-- En-tête du profil -->
      <section class="profil-entete overflow-hidden rounded-2xl border border-slate-200 bg-white">
        <div class="profil-bandeau h-24 sm:h-28"></div>
        <div class="flex flex-col gap-4 px-5 pb-5 sm:flex-row sm:items-end sm:justify-between sm:px-8">
          <div class="-mt-12 flex flex-col items-center gap-4 text-center sm:flex-row sm:items-end sm:text-left">
            <div class="relative">
              <app-avatar class="profil-avatar" [photoUrl]="profilService.photoUrl()" taille="xl"
                [libelle]="'Photo de profil de ' + nomComplet()" />
              <button type="button" (click)="photoOuverte.set(true)" aria-label="Modifier la photo de profil"
                class="absolute bottom-1 right-1 flex h-9 w-9 items-center justify-center rounded-full border-2 border-white bg-green-700 text-white shadow hover:bg-green-600">
                <svg aria-hidden="true" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M4 8.5A2.5 2.5 0 016.5 6h1.3l1.4-2h5.6l1.4 2h1.3A2.5 2.5 0 0120 8.5v8a2.5 2.5 0 01-2.5 2.5h-11A2.5 2.5 0 014 16.5z" /><circle cx="12" cy="12.5" r="3.5" />
                </svg>
              </button>
            </div>
            <div class="pb-1">
              <h1 class="text-xl font-bold text-slate-900 sm:text-2xl">{{ nomComplet() }}</h1>
              <p class="text-sm text-slate-600">{{ profil()?.poste || 'Poste non renseigné' }}@if (profil()?.serviceLibelle) { · {{ profil()?.serviceLibelle }} }</p>
              <div class="mt-2 flex flex-wrap justify-center gap-2 sm:justify-start">
                @if (profil()?.roleLibelle) {
                  <span class="rounded-full border border-slate-200 bg-slate-100 px-2.5 py-0.5 text-xs font-semibold text-slate-700">{{ profil()?.roleLibelle }}</span>
                }
                @if (profil()?.matricule) {
                  <span class="rounded-full border border-slate-200 bg-white px-2.5 py-0.5 font-mono text-xs text-slate-600">{{ profil()?.matricule }}</span>
                }
              </div>
            </div>
          </div>
          <button type="button" (click)="photoOuverte.set(true)"
            class="self-center rounded-lg border border-slate-300 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-100 sm:self-end">
            {{ profilService.photoUrl() ? 'Changer la photo' : 'Ajouter une photo' }}
          </button>
        </div>

        <nav class="flex gap-1 border-t border-slate-200 px-3 sm:px-6" role="tablist" aria-label="Sections du profil">
          @for (o of onglets; track o.id) {
            <button type="button" role="tab" [attr.aria-selected]="onglet() === o.id" (click)="choisirOnglet(o.id)"
              class="profil-onglet" [class.profil-onglet-actif]="onglet() === o.id">{{ o.libelle }}</button>
          }
        </nav>
      </section>

      @if (obligatoire()) {
        <div role="alert" class="flex items-start gap-3 rounded-xl border border-amber-300 bg-amber-50 p-4 text-sm text-amber-900">
          <svg aria-hidden="true" class="mt-0.5 h-5 w-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 9v4m0 4h.01M10.3 3.9L2.6 17.5A2 2 0 004.3 20.5h15.4a2 2 0 001.7-3L13.7 3.9a2 2 0 00-3.4 0z" /></svg>
          <div><p class="font-semibold">Changement de mot de passe requis</p>
            <p class="mt-0.5 text-amber-800">Votre mot de passe a été défini par un administrateur. Choisissez votre propre mot de passe pour accéder à l’application.</p></div>
        </div>
      }

      @if (onglet() === 'informations') {
        @if (profil(); as p) {
          <div class="grid gap-5 lg:grid-cols-2">
            <section class="profil-carte">
              <h2>Identité</h2>
              <dl>
                <div><dt>Nom</dt><dd>{{ p.nom || '—' }}</dd></div>
                <div><dt>Prénom</dt><dd>{{ p.prenom || '—' }}</dd></div>
                <div><dt>Sexe</dt><dd>{{ libelleSexe(p.sexe) }}</dd></div>
                <div><dt>Matricule</dt><dd class="font-mono">{{ p.matricule || '—' }}</dd></div>
              </dl>
            </section>
            <section class="profil-carte">
              <h2>Coordonnées</h2>
              <dl>
                <div><dt>E-mail professionnel</dt><dd class="break-all">{{ p.email || '—' }}</dd></div>
                <div><dt>Téléphone</dt><dd>{{ p.telephone || '—' }}</dd></div>
              </dl>
            </section>
            <section class="profil-carte">
              <h2>Affectation</h2>
              <dl>
                <div><dt>Poste</dt><dd>{{ p.poste || '—' }}</dd></div>
                <div><dt>Service</dt><dd>{{ p.serviceLibelle || '—' }}</dd></div>
                <div><dt>Direction</dt><dd>{{ p.directionLibelle || '—' }}</dd></div>
                <div><dt>Structure</dt><dd>{{ p.structureCode || '—' }}</dd></div>
                <div><dt>Date d’embauche</dt><dd>{{ p.dateEmbauche ? (p.dateEmbauche | date:'dd/MM/yyyy') : '—' }}</dd></div>
              </dl>
            </section>
            <section class="profil-carte">
              <h2>Compte</h2>
              <dl>
                <div><dt>Identifiant</dt><dd class="break-all">{{ p.username }}</dd></div>
                <div><dt>Rôle</dt><dd>{{ p.roleLibelle || '—' }}</dd></div>
                <div><dt>Compte créé le</dt><dd>{{ p.compteCreeLe ? (p.compteCreeLe | date:'dd/MM/yyyy') : '—' }}</dd></div>
                <div><dt>Mot de passe</dt><dd>{{ p.motDePasseModifieLe ? 'Modifié le ' + (p.motDePasseModifieLe | date:'dd/MM/yyyy à HH:mm') : 'Jamais modifié' }}</dd></div>
              </dl>
            </section>
          </div>
          <p class="flex items-start gap-2 text-xs text-slate-500">
            <svg aria-hidden="true" class="mt-0.5 h-4 w-4 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="9" /><path d="M12 11v5m0-8h.01" /></svg>
            Ces informations proviennent de votre dossier du personnel. Pour toute correction, adressez-vous à votre service des ressources humaines ou à votre administrateur.
          </p>
        } @else {
          <p class="text-sm text-slate-500">Chargement du profil…</p>
        }
      }

      @if (onglet() === 'securite') {
        <div class="grid gap-5 lg:grid-cols-[1.4fr_1fr]">
          <section class="profil-carte">
            <h2>Changer le mot de passe</h2>
            <form #formulaire="ngForm" (ngSubmit)="changerMotDePasse(formulaire)" class="profil-formulaire space-y-4" novalidate>
              <label class="block text-sm font-semibold text-slate-700">Mot de passe actuel <span class="champ-obligatoire" aria-hidden="true">*</span>
                <app-password-input name="actuel" required [(ngModel)]="actuel" autocomplete="current-password" />
              </label>
              <label class="block text-sm font-semibold text-slate-700">Nouveau mot de passe <span class="champ-obligatoire" aria-hidden="true">*</span>
                <app-password-input name="nouveau" required [(ngModel)]="nouveau" autocomplete="new-password" />
              </label>
              <app-password-strength [motDePasse]="nouveau" [identifiant]="auth.currentUser()?.username || ''" />
              <label class="block text-sm font-semibold text-slate-700">Confirmer le nouveau mot de passe <span class="champ-obligatoire" aria-hidden="true">*</span>
                <app-password-input name="confirmation" required [(ngModel)]="confirmation" autocomplete="new-password" />
                @if (confirmation && confirmation !== nouveau) {
                  <span class="mt-1 block text-xs font-normal text-rose-700">Les deux mots de passe ne correspondent pas.</span>
                }
              </label>
              @if (erreur()) {
                <p role="alert" class="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-800">{{ erreur() }}</p>
              }
              <div class="flex justify-end pt-1">
                <button type="submit" [disabled]="enCours() || !peutValider()"
                  class="rounded-lg bg-green-700 px-5 py-2.5 text-sm font-semibold text-white hover:bg-green-600 disabled:opacity-50">
                  {{ enCours() ? 'Mise à jour…' : 'Mettre à jour le mot de passe' }}
                </button>
              </div>
            </form>
          </section>

          <div class="space-y-5">
            <section class="profil-carte">
              <h2>Sécurité du compte</h2>
              <ul class="space-y-3 text-sm text-slate-700">
                <li class="flex gap-2.5">
                  <svg aria-hidden="true" class="mt-0.5 h-4 w-4 shrink-0 text-green-700" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 3l7 3v5c0 4.5-3 8.5-7 10-4-1.5-7-5.5-7-10V6z" /></svg>
                  <span>Dernier changement : <strong>{{ profil()?.motDePasseModifieLe ? (profil()!.motDePasseModifieLe | date:'dd/MM/yyyy à HH:mm') : 'jamais' }}</strong></span>
                </li>
                <li class="flex gap-2.5">
                  <svg aria-hidden="true" class="mt-0.5 h-4 w-4 shrink-0 text-green-700" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="12" rx="2" /><path d="M8 20h8M12 16v4" /></svg>
                  <span>Après un changement, vos autres sessions ouvertes (autres navigateurs ou postes) sont automatiquement déconnectées.</span>
                </li>
              </ul>
            </section>
            <section class="profil-carte">
              <h2>Conseils</h2>
              <ul class="list-disc space-y-1.5 pl-5 text-sm text-slate-600">
                <li>Préférez une phrase facile à retenir, par exemple « Budget-Ouaga-2026! ».</li>
                <li>N’utilisez pas ce mot de passe sur d’autres sites.</li>
                <li>Ne le communiquez jamais, même à un administrateur.</li>
              </ul>
            </section>
          </div>
        </div>
      }
    </div>

    @if (photoOuverte()) {
      <app-photo-dialog [photoActuelle]="profilService.photoUrl()" (fermeture)="photoOuverte.set(false); recharger()" />
    }
  `,
})
export class ProfilComponent implements OnInit {
  readonly auth = inject(AuthService);
  readonly profilService = inject(ProfilService);
  private readonly notify = inject(NotificationService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly onglets: { id: Onglet; libelle: string }[] = [
    { id: 'informations', libelle: 'Informations personnelles' },
    { id: 'securite', libelle: 'Sécurité et mot de passe' },
  ];
  readonly onglet = signal<Onglet>('informations');
  readonly profil = signal<Profil | null>(null);
  readonly photoOuverte = signal(false);
  readonly enCours = signal(false);
  readonly erreur = signal('');
  readonly obligatoire = computed(() => !!this.auth.currentUser()?.doitChangerMotDePasse);

  actuel = '';
  nouveau = '';
  confirmation = '';

  readonly nomComplet = computed(() => {
    const p = this.profil();
    const u = this.auth.currentUser();
    return `${p?.prenom ?? u?.prenom ?? ''} ${p?.nom ?? u?.nom ?? ''}`.trim() || (u?.username ?? '');
  });

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const demande = params.get('onglet');
      this.onglet.set(demande === 'securite' || this.obligatoire() ? 'securite' : 'informations');
    });
    this.recharger();
  }

  recharger(): void {
    this.profilService.profil().subscribe({
      next: (profil) => this.profil.set(profil),
      error: () => this.notify.error('Impossible de charger votre profil.'),
    });
  }

  choisirOnglet(onglet: Onglet): void {
    this.router.navigate([], { relativeTo: this.route, queryParams: { onglet }, replaceUrl: true });
  }

  peutValider(): boolean {
    return !!this.actuel && motDePasseConforme(this.nouveau, this.auth.currentUser()?.username) && this.nouveau === this.confirmation;
  }

  changerMotDePasse(formulaire: NgForm): void {
    if (!this.peutValider()) return;
    this.enCours.set(true);
    this.erreur.set('');
    const etaitObligatoire = this.obligatoire();
    this.profilService.changerMotDePasse(this.actuel, this.nouveau).subscribe({
      next: () => {
        this.enCours.set(false);
        this.actuel = this.nouveau = this.confirmation = '';
        formulaire.resetForm();
        this.notify.success('Votre mot de passe a été modifié. Vos autres sessions ont été déconnectées.');
        this.recharger();
        if (etaitObligatoire) this.router.navigate(['/dashboard']);
      },
      error: (erreur) => {
        this.enCours.set(false);
        this.erreur.set(erreur?.error?.message || 'Le mot de passe n’a pas pu être modifié.');
      },
    });
  }

  libelleSexe(sexe: string | null): string {
    return sexe === 'HOMME' ? 'Masculin' : sexe === 'FEMME' ? 'Féminin' : '—';
  }
}
