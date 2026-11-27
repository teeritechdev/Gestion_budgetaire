import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { BudgetService } from '../../../core/services/budget.service';
import { AuthService } from '../../../core/services/auth.service';
import { DashboardSummary } from '../../../core/models/budget/dashboard-summary.model';
import { AppIconComponent } from '../../../shared/components/app-icon.component';

/** Teinte de la pastille d'icône d'un indicateur. */
type Teinte = 'vert' | 'bleu' | 'ambre' | 'violet' | 'sarcelle' | 'indigo' | 'ardoise';

interface Indicateur {
  libelle: string;
  valeur: string;
  detail: string;
  icone: string;
  teinte: Teinte;
  lien?: { texte: string; route: string };
}

@Component({
  selector: 'app-budget-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, AppIconComponent],
  template: `
    <div class="mx-auto max-w-7xl space-y-8">
      <section class="dashboard-accueil relative overflow-hidden rounded-2xl">
        <div class="relative flex flex-col justify-between gap-5 p-6 md:flex-row md:items-center md:p-8">
          <div>
            <p class="dashboard-accueil-date text-xs font-semibold uppercase tracking-[.14em]">{{ dateDuJour }}</p>
            <h1 class="dashboard-accueil-titre mt-2 text-2xl font-bold tracking-tight md:text-3xl">
              Bonjour{{ nomUtilisateur() ? ', ' + nomUtilisateur() : '' }}
            </h1>
            <p class="dashboard-accueil-texte mt-1.5 text-sm">
              {{ summary()?.structureLibelle || 'Votre espace budgétaire' }}@if (summary()?.entiteNom) { · {{ summary()?.entiteNom }} }
            </p>
          </div>
          <button type="button" (click)="refresh()" [disabled]="loading()"
            class="dashboard-actualiser inline-flex shrink-0 items-center gap-2 self-start rounded-lg px-4 py-2.5 text-sm font-semibold md:self-auto">
            <svg aria-hidden="true" class="h-4 w-4" [class.animate-spin]="loading()" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 12a8 8 0 01-14.3 4.9M4 12A8 8 0 0118.3 7.1M18.5 3v4.2h-4.2M5.5 21v-4.2h4.2" />
            </svg>
            {{ loading() ? 'Actualisation…' : 'Actualiser' }}
          </button>
        </div>
      </section>

      @if (error()) {
        <p role="alert" class="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800">{{ error() }}</p>
      }

      @for (groupe of groupes(); track groupe.titre) {
        <section>
          <div class="dashboard-section-titre mb-4 flex items-center gap-3">
            <span class="dashboard-section-icone" [class]="'dashboard-section-icone teinte-' + groupe.teinte">
              <app-icon [name]="groupe.icone" classe="h-[18px] w-[18px]" />
            </span>
            <div>
              <h2 class="text-base font-semibold text-slate-900">{{ groupe.titre }}</h2>
              <p class="text-xs text-slate-500">{{ groupe.sousTitre }}</p>
            </div>
          </div>
          <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            @for (indicateur of groupe.indicateurs; track indicateur.libelle) {
              <article class="carte-indicateur" [class.carte-indicateur-cliquable]="!!indicateur.lien">
                <div class="flex items-start justify-between gap-3 p-5">
                  <div class="min-w-0">
                    <p class="carte-indicateur-libelle">{{ indicateur.libelle }}</p>
                    <p class="carte-indicateur-valeur mt-3 truncate">{{ indicateur.valeur }}</p>
                    <p class="mt-1 text-xs text-slate-500">{{ indicateur.detail }}</p>
                  </div>
                  <span [class]="'carte-indicateur-icone teinte-' + indicateur.teinte">
                    <app-icon [name]="indicateur.icone" classe="h-5 w-5" />
                  </span>
                </div>
                @if (indicateur.lien) {
                  <a [routerLink]="indicateur.lien.route" class="carte-indicateur-lien">
                    {{ indicateur.lien.texte }}
                    <svg aria-hidden="true" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14m-5-5l5 5-5 5" /></svg>
                  </a>
                }
              </article>
            }
          </div>
        </section>
      }

      <p class="flex items-center gap-2 text-xs text-slate-500">
        <svg aria-hidden="true" class="h-4 w-4 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="9" /><path d="M12 11v5m0-8h.01" /></svg>
        Les indicateurs couvrent l’ensemble des structures et entités auxquelles vous avez accès.
      </p>
    </div>
  `,
})
export class BudgetDashboardComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly budget = inject(BudgetService);
  readonly summary = signal<DashboardSummary | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');

  /** Ex. « jeudi 5 novembre 2026 ». */
  readonly dateDuJour = new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
    .format(new Date());

  readonly groupes = computed(() => {
    const s = this.summary();
    const peut = (permission: string) => this.auth.hasPermission(permission);
    const groupes: { titre: string; sousTitre: string; icone: string; teinte: Teinte; indicateurs: Indicateur[] }[] = [
      {
        titre: 'Indicateurs budgétaires',
        sousTitre: 'Vue globale de toutes les structures et entités',
        icone: 'allocations',
        teinte: 'vert',
        indicateurs: [
          { libelle: 'Budget attribué', valeur: montant(s?.budgetAlloue), detail: 'Toutes les structures', icone: 'allocations', teinte: 'vert' },
          { libelle: 'Crédits engagés', valeur: montant(s?.montantEngage), detail: `Liquidé : ${montant(s?.montantLiquide)}`, icone: 'journal', teinte: 'bleu' },
          {
            libelle: 'Révisions en attente', valeur: nombre(s?.revisionsEnAttente), detail: 'Demandes à traiter', icone: 'revisions', teinte: 'ambre',
            lien: peut('revision.lire') ? { texte: 'Consulter les révisions', route: '/budget/revisions' } : undefined,
          },
          { libelle: 'Structures / entités', valeur: `${nombre(s?.nombreStructures)} / ${nombre(s?.nombreEntites)}`, detail: 'Périmètre couvert', icone: 'structure', teinte: 'violet' },
        ],
      },
      {
        titre: 'Sécurité et organisation',
        sousTitre: 'Comptes, rôles et traçabilité',
        icone: 'roles',
        teinte: 'indigo',
        indicateurs: [
          {
            libelle: 'Collaborateurs', valeur: `${nombre(s?.nombreUtilisateursActifs)} / ${nombre(s?.nombreUtilisateurs)}`, detail: 'Comptes actifs / total', icone: 'utilisateurs', teinte: 'sarcelle',
            lien: peut('utilisateur.lire') ? { texte: 'Gérer les comptes', route: '/security/utilisateurs' } : undefined,
          },
          {
            libelle: 'Rôles', valeur: nombre(s?.nombreRoles), detail: `${nombre(s?.nombrePermissions)} permissions configurées`, icone: 'roles', teinte: 'indigo',
            lien: peut('role.lire') || peut('permission.lire') ? { texte: 'Voir les rôles', route: '/security/roles-permissions' } : undefined,
          },
          {
            libelle: 'Activités journalisées', valeur: nombre(s?.nombreJournauxAudit), detail: 'Événements enregistrés', icone: 'audit', teinte: 'ardoise',
            lien: peut('audit.lire') ? { texte: 'Consulter le journal', route: '/security/audit-logs' } : undefined,
          },
        ],
      },
    ];
    return groupes;
  });

  /** Prénom et nom de l'utilisateur connecté (à défaut, son identifiant). */
  nomUtilisateur(): string {
    const user = this.auth.currentUser();
    const complet = `${user?.prenom ?? ''} ${user?.nom ?? ''}`.trim();
    return complet || user?.username || '';
  }

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set('');
    this.budget.getDashboardSummary().subscribe({
      next: (summary) => {
        this.summary.set(summary);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.summary.set(null);
        this.error.set(error.status === 0
          ? 'Impossible de joindre le serveur (réseau ou CORS). Vérifiez que le backend est démarré.'
          : `Le serveur a refusé le chargement des indicateurs (HTTP ${error.status}).`);
        console.error('Échec du chargement des indicateurs globaux du tableau de bord.', error);
        this.loading.set(false);
      },
    });
  }
}

const FORMAT_NOMBRE = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 });

function nombre(valeur: number | null | undefined): string {
  return valeur === null || valeur === undefined ? '—' : FORMAT_NOMBRE.format(valeur);
}

function montant(valeur: number | null | undefined): string {
  return valeur === null || valeur === undefined ? '—' : `${FORMAT_NOMBRE.format(valeur)} FCFA`;
}
