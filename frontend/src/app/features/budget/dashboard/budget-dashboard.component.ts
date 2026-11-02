import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { BudgetService } from '../../../core/services/budget.service';
import { AuthService } from '../../../core/services/auth.service';
import { DashboardSummary } from '../../../core/models/budget/dashboard-summary.model';

@Component({
  selector: 'app-budget-dashboard',
  standalone: true,
  imports: [CommonModule, DatePipe, RouterLink],
  template: `
    <div class="mx-auto max-w-7xl space-y-8">
      <section class="dashboard-welcome relative overflow-hidden rounded-3xl border border-green-950 bg-green-900">
        <div class="relative flex flex-col justify-between gap-5 p-6 md:flex-row md:items-end md:p-8">
          <div>
            <div class="mb-3 inline-flex items-center gap-2 rounded-full border border-green-200/30 bg-white/10 px-3 py-1 text-[10px] font-semibold uppercase tracking-[.16em] text-green-100">
              Tableau de bord global
            </div>
            <h1 class="text-2xl font-bold tracking-tight text-white md:text-3xl">
              Bienvenue{{ nomUtilisateur() ? ', ' + nomUtilisateur() : '' }}
            </h1>
            <p class="mt-2 text-sm leading-6 text-green-100">
              {{ summary()?.structureLibelle || 'Votre espace budgétaire' }}
              @if (summary()?.entiteNom) { · {{ summary()?.entiteNom }} }
            </p>
          </div>
          <div class="flex shrink-0 items-center gap-3">
            <div class="hidden text-right sm:block">
              <p class="text-xs font-medium text-green-50">{{ today | date:'EEEE d MMMM y' }}</p>
              <p class="mt-1 text-[10px] text-green-100">Données actualisées à la demande</p>
            </div>
            <button (click)="refresh()" [disabled]="loading()" class="rounded-xl border border-slate-300 bg-slate-100 px-4 py-2.5 text-xs font-medium text-slate-800 transition hover:border-green-300 hover:bg-white disabled:opacity-50">
              {{ loading() ? 'Actualisation…' : '↻ Actualiser' }}
            </button>
          </div>
        </div>
      </section>

      @if (error()) {
        <p role="alert" class="rounded-xl border border-rose-500/20 bg-rose-500/5 p-4 text-xs text-rose-200">{{ error() }}</p>
      }

      <section>
        <div class="mb-4">
          <h2 class="text-sm font-semibold text-white">Indicateurs budgétaires</h2>
          <p class="mt-1 text-[11px] text-slate-500">Vue globale de toutes les structures et entités</p>
        </div>
        <div class="stats-grid grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <article class="rounded-2xl border border-slate-200 bg-white p-5">
            <p class="text-[11px] font-semibold uppercase tracking-wider text-slate-600">Budget attribué</p>
            <p class="mt-4 truncate text-2xl font-bold text-white">{{ summary()?.budgetAlloue | number:'1.0-0' }}&nbsp;FCFA</p>
            <p class="mt-1 text-[10px] text-slate-500">Toutes les structures</p>
          </article>
          <article class="rounded-2xl border border-slate-200 bg-white p-5">
            <p class="text-[11px] font-semibold uppercase tracking-wider text-slate-600">Crédits engagés</p>
            <p class="mt-4 truncate text-2xl font-bold text-white">{{ summary()?.montantEngage | number:'1.0-0' }}&nbsp;FCFA</p>
            <p class="mt-1 text-[10px] text-slate-500">Liquidé : {{ summary()?.montantLiquide | number:'1.0-0' }}&nbsp;FCFA</p>
          </article>
          <article class="rounded-2xl border border-slate-200 bg-white p-5">
            <p class="text-[11px] font-semibold uppercase tracking-wider text-slate-600">Révisions en attente</p>
            <p class="mt-4 text-3xl font-bold text-white">{{ summary()?.revisionsEnAttente ?? 'Indisponible' }}</p>
            <p class="mt-1 text-[10px] text-slate-500">Toutes les demandes en attente</p>
            @if (auth.hasPermission('revision.lire')) {
              <a routerLink="/budget/revisions" class="mt-3 inline-block text-[10px] text-blue-300 hover:text-blue-200">Consulter les révisions →</a>
            }
          </article>
          <article class="rounded-2xl border border-slate-200 bg-white p-5">
            <p class="text-[11px] font-semibold uppercase tracking-wider text-slate-600">Structures / entités</p>
            <p class="mt-4 text-3xl font-bold text-white">{{ summary()?.nombreStructures ?? 'Indisponible' }} / {{ summary()?.nombreEntites ?? 'Indisponible' }}</p>
            <p class="mt-1 text-[10px] text-slate-500">Totaux toutes entités</p>
          </article>
        </div>
      </section>

      <section>
        <div class="mb-4">
          <h2 class="text-sm font-semibold text-white">Indicateurs sécurité & organisation</h2>
          <p class="mt-1 text-[11px] text-slate-500">Totaux toutes entités et tous utilisateurs</p>
        </div>
        <div class="stats-grid grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <article class="rounded-2xl border border-slate-200 bg-white p-5">
            <p class="text-[11px] font-semibold uppercase tracking-wider text-slate-600">Collaborateurs</p>
            <p class="mt-4 text-3xl font-bold text-white">{{ summary()?.nombreUtilisateursActifs ?? 'Indisponible' }} / {{ summary()?.nombreUtilisateurs ?? 'Indisponible' }}</p>
            <p class="mt-1 text-[10px] text-slate-500">Comptes actifs / total</p>
            @if (auth.hasPermission('utilisateur.lire')) {
              <a routerLink="/security/utilisateurs" class="mt-3 inline-block text-[10px] text-blue-300 hover:text-blue-200">Gérer les comptes →</a>
            }
          </article>
          <article class="rounded-2xl border border-slate-200 bg-white p-5">
            <p class="text-[11px] font-semibold uppercase tracking-wider text-slate-600">Rôles RBAC</p>
            <p class="mt-4 text-3xl font-bold text-white">{{ summary()?.nombreRoles ?? 'Indisponible' }}</p>
            <p class="mt-1 text-[10px] text-slate-500">{{ summary()?.nombrePermissions ?? 'Indisponible' }} permissions configurées</p>
            @if (auth.hasPermission('role.lire') || auth.hasPermission('permission.lire')) {
              <a routerLink="/security/roles-permissions" class="mt-3 inline-block text-[10px] text-blue-300 hover:text-blue-200">Voir les rôles →</a>
            }
          </article>
          <article class="rounded-2xl border border-slate-200 bg-white p-5">
            <p class="text-[11px] font-semibold uppercase tracking-wider text-slate-600">Activités journalisées</p>
            <p class="mt-4 text-3xl font-bold text-white">{{ summary()?.nombreJournauxAudit ?? 'Indisponible' }}</p>
            <p class="mt-1 text-[10px] text-slate-500">Événements toutes entités</p>
            @if (auth.hasPermission('audit.lire')) {
              <a routerLink="/security/audit-logs" class="mt-3 inline-block text-[10px] text-blue-300 hover:text-blue-200">Consulter l’audit →</a>
            }
          </article>
        </div>
      </section>

      <section class="rounded-2xl border border-slate-200 bg-white p-5">
        <h2 class="text-sm font-semibold text-white">Votre espace</h2>
        <p class="mt-2 text-xs text-slate-600">{{ auth.currentUser()?.email }}</p>
        <p class="mt-1 text-[10px] text-slate-500">Les statistiques affichées couvrent l’ensemble des structures et entités.</p>
      </section>
    </div>
  `,
})
export class BudgetDashboardComponent implements OnInit {
  /** Prénom et nom de l'utilisateur connecté (à défaut, son identifiant). */
  nomUtilisateur(): string {
    const user = this.auth.currentUser();
    const complet = `${user?.prenom ?? ''} ${user?.nom ?? ''}`.trim();
    return complet || user?.username || '';
  }
  readonly auth = inject(AuthService);
  private readonly budget = inject(BudgetService);
  readonly today = new Date();
  readonly summary = signal<DashboardSummary | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');

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
