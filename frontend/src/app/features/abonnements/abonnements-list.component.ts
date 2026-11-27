import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { AbonnementService } from '../../core/services/abonnement.service';
import { AchatService } from '../../core/services/achat.service';
import { NomenclatureService } from '../../core/services/nomenclature.service';
import {
  Abonnement,
  AbonnementRequest,
  EcheanceAbonnement,
  PeriodiciteAbonnement,
  RapprochementFactureRequest,
  StatutAbonnement
} from '../../core/models/abonnement/abonnement.model';
import { Fournisseur } from '../../core/models/achat/fournisseur.model';
import { ActiviteCatalogue } from '../../core/models/budget/activite-catalogue.model';
import { PageHeaderComponent } from '../../shared/components/page-header.component';
import { RowActionsComponent } from '../../shared/components/row-actions.component';
import { FormPageComponent } from '../../shared/components/form-page.component';
import { PaginationBarComponent } from '../../shared/components/pagination-bar.component';
import { DataActionsComponent } from '../../shared/components/data-actions.component';
import { SearchOptionDirective, SearchSelectComponent } from '../../shared/components/search-select.component';
import { errorMessage } from '../../core/utils/error-message';

@Component({
  selector: 'app-abonnements-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    PageHeaderComponent,
    RowActionsComponent,
    FormPageComponent,
    PaginationBarComponent,
    DataActionsComponent,
    SearchSelectComponent,
    SearchOptionDirective,
  ],
  template: `
@if (formOpen()) {
  <app-form-page
    section="Achats récurrents & contrats"
    [title]="editingId() ? 'Modifier le contrat d’abonnement' : 'Nouveau contrat d’abonnement'"
    [mode]="editingId() ? 'Modification' : 'Création'"
    subtitle="Contrat récurrent (loyer, fibre, assurance, maintenance, énergie) et échéancier automatique."
    (back)="formOpen.set(false)">
    <form (ngSubmit)="save()" class="space-y-4">
      <section class="space-y-4 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <h3 class="text-sm font-semibold text-slate-900">Identification du contrat</h3>
        <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <label class="block space-y-1 text-xs sm:col-span-2">
            Objet / Libellé de l'abonnement <span class="champ-obligatoire" aria-hidden="true">*</span>
            <input required maxlength="255" [(ngModel)]="draft.libelle" name="libelle"
              placeholder="Ex. : Abonnement Internet Fibre Optique Dédiée Siège"
              class="w-full rounded-lg border border-slate-300 p-2 text-xs">
          </label>
          <label class="block space-y-1 text-xs">
            Fournisseur / Prestataire <span class="champ-obligatoire" aria-hidden="true">*</span>
            <app-search-select required [(ngModel)]="draft.fournisseurId" name="fournisseurId"
              placeholder="Sélectionner le prestataire…"
              triggerClass="w-full rounded-lg border border-slate-300 bg-white p-2 text-xs">
              @for (f of fournisseurs(); track f.id) {
                <app-search-option [value]="f.id" [label]="f.codeFournisseur + ' — ' + f.raisonSociale" />
              }
            </app-search-select>
          </label>
          <label class="block space-y-1 text-xs">
            Activité budgétaire de rattachement
            <app-search-select [(ngModel)]="draft.activiteId" name="activiteId"
              placeholder="Sélectionner l'activité budgétaire…"
              triggerClass="w-full rounded-lg border border-slate-300 bg-white p-2 text-xs">
              @for (act of activites(); track act.id) {
                <app-search-option [value]="act.id" [label]="act.codeActivite + ' — ' + act.libelle" />
              }
            </app-search-select>
          </label>
          <label class="block space-y-1 text-xs">
            Statut du contrat
            <select [(ngModel)]="draft.statut" name="statut" class="w-full rounded-lg border border-slate-300 p-2 text-xs">
              <option value="ACTIF">ACTIF — En vigueur</option>
              <option value="SUSPENDU">SUSPENDU — En attente / pause</option>
              <option value="RESILIE">RESILIÉ — Fin anticipée</option>
              <option value="EXPIRE">EXPIRÉ — Échu</option>
            </select>
          </label>
        </div>
      </section>

      <section class="space-y-4 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <h3 class="text-sm font-semibold text-slate-900">Modalités financières & récurrence</h3>
        <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
          <label class="block space-y-1 text-xs">
            Périodicité de facturation <span class="champ-obligatoire" aria-hidden="true">*</span>
            <select required [(ngModel)]="draft.periodicite" name="periodicite" class="w-full rounded-lg border border-slate-300 p-2 text-xs font-semibold">
              <option value="MENSUEL">MENSUEL (Tous les mois)</option>
              <option value="TRIMESTRIEL">TRIMESTRIEL (Tous les 3 mois)</option>
              <option value="SEMESTRIEL">SEMESTRIEL (Tous les 6 mois)</option>
              <option value="ANNUEL">ANNUEL (Une fois par an)</option>
            </select>
          </label>
          <label class="block space-y-1 text-xs">
            Montant de l'échéance HT (FCFA) <span class="champ-obligatoire" aria-hidden="true">*</span>
            <input required type="number" min="1" step="1" [(ngModel)]="draft.montantEcheanceHt"
              (ngModelChange)="onMontantHtChange($event)" name="montantEcheanceHt"
              placeholder="Ex. : 250000" class="w-full rounded-lg border border-slate-300 p-2 text-xs">
          </label>
          <label class="block space-y-1 text-xs">
            Montant de l'échéance TTC (FCFA) <span class="champ-obligatoire" aria-hidden="true">*</span>
            <input required type="number" min="1" step="1" [(ngModel)]="draft.montantEcheanceTtc"
              name="montantEcheanceTtc"
              placeholder="Ex. : 295000" class="w-full rounded-lg border border-slate-300 p-2 text-xs">
          </label>
        </div>
        <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
          <label class="block space-y-1 text-xs">
            Jour mensuel de facturation (1 - 31)
            <input type="number" min="1" max="31" [(ngModel)]="draft.jourFacturation" name="jourFacturation"
              class="w-full rounded-lg border border-slate-300 p-2 text-xs">
            <span class="text-[10px] text-slate-500">Par défaut le 1er de la période.</span>
          </label>
          <label class="block space-y-1 text-xs">
            Date de début de contrat <span class="champ-obligatoire" aria-hidden="true">*</span>
            <input required type="date" [(ngModel)]="draft.dateDebut" name="dateDebut"
              class="w-full rounded-lg border border-slate-300 p-2 text-xs">
          </label>
          <label class="block space-y-1 text-xs">
            Date d'échéance / fin prévisionnelle
            <input type="date" [(ngModel)]="draft.dateFin" name="dateFin"
              class="w-full rounded-lg border border-slate-300 p-2 text-xs">
            <span class="text-[10px] text-slate-500">Laisser vide si contrat sans fin ferme.</span>
          </label>
        </div>
      </section>

      <section class="space-y-4 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <h3 class="text-sm font-semibold text-slate-900">Clauses & Tacite reconduction</h3>
        <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <label class="block space-y-1 text-xs">
            Préavis de dénonciation / résiliation (jours)
            <input type="number" min="0" max="365" [(ngModel)]="draft.preavisJours" name="preavisJours"
              class="w-full rounded-lg border border-slate-300 p-2 text-xs">
          </label>
          <div class="flex items-center pt-5">
            <label class="flex items-center gap-2 text-xs font-semibold text-slate-700 cursor-pointer">
              <input type="checkbox" [(ngModel)]="draft.taciteReconduction" name="taciteReconduction"
                class="h-4 w-4 rounded border-slate-300 text-green-700">
              Contrat à tacite reconduction automatique
            </label>
          </div>
        </div>
      </section>

      <div class="form-page-actions mt-4 flex items-center justify-end gap-2">
        <button type="button" (click)="formOpen.set(false)" class="rounded-lg border border-slate-300 px-3 py-2 text-xs">Annuler</button>
        <button [disabled]="saving()" class="rounded-lg bg-green-700 px-4 py-2 text-xs font-semibold text-white shadow-sm transition hover:bg-green-800 disabled:opacity-50">
          {{ saving() ? 'Enregistrement…' : 'Enregistrer le contrat' }}
        </button>
      </div>
    </form>
  </app-form-page>
} @else {
  <div class="mx-auto max-w-7xl space-y-6">
    <app-page-header
      icon="abonnements"
      module="Abonnements & Contrats récurrents"
      title="Contrats d’abonnements récurrents"
      description="Suivi des abonnements, contrats-cadres récurrents, émission de bons de commande et rapprochement des factures.">
      <div class="flex flex-wrap items-center gap-2">
        <app-data-actions
          ressource="abonnements"
          exportPermission="abonnement.lire"
          (exporter)="exporterCsv()"
          (importer)="openImport()" />
        @if (auth.hasPermission('abonnement.creer')) {
          <button (click)="openCreate()" class="flex items-center gap-2 rounded-xl bg-green-700 px-4 py-2.5 text-xs font-semibold text-white shadow hover:bg-green-800">
            <svg aria-hidden="true" class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-width="2" d="M12 4v16m8-8H4"/></svg>
            Nouveau contrat
          </button>
        }
      </div>
    </app-page-header>

    <!-- Métriques Synthétiques (Design Djibril stats-grid avec liseré supérieur) -->
    <section class="stats-grid grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
      <article class="rounded-2xl border border-slate-200 bg-white p-4">
        <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Total contrats</p>
        <p class="mt-2 text-2xl font-bold font-mono text-slate-900">{{ totalElements() }}</p>
        <p class="mt-1 text-[11px] text-slate-500">Référentiel global</p>
      </article>

      <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" (click)="statutFiltre = 'ACTIF'; rechercher()">
        <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Contrats actifs</p>
        <p class="mt-2 text-2xl font-bold font-mono text-emerald-700">{{ statsActifs() }}</p>
        <p class="mt-1 text-[11px] text-slate-500">En cours d'exécution</p>
      </article>

      <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" routerLink="/abonnements/echeancier">
        <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Échéances gérées</p>
        <p class="mt-2 text-2xl font-bold font-mono text-blue-600">{{ statsTotalEcheances() }}</p>
        <p class="mt-1 text-[11px] text-slate-500">Planifiées sur la période</p>
      </article>

      <article class="rounded-2xl border border-slate-200 bg-white p-4">
        <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Volume récurrent TTC</p>
        <p class="mt-2 text-2xl font-bold font-mono text-purple-700">{{ statsVolumeTtc() | number:'1.0-0' }} <span class="text-xs font-normal text-slate-500">FCFA</span></p>
        <p class="mt-1 text-[11px] text-slate-500">Total des échéances</p>
      </article>
    </section>

    <!-- Barre de recherche et filtres -->
    <div class="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 bg-white p-3 shadow-sm">
      <div class="flex flex-1 flex-wrap items-center gap-2">
        <div class="relative min-w-[220px] flex-1">
          <input [(ngModel)]="recherche" (keyup.enter)="rechercher()" placeholder="Rechercher par code, libellé, prestataire…"
            class="w-full rounded-lg border border-slate-200 bg-slate-50 py-2 pl-8 pr-3 text-xs outline-none focus:border-green-600 focus:bg-white">
          <svg aria-hidden="true" class="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
        </div>
        <select [(ngModel)]="statutFiltre" (change)="rechercher()" class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-xs outline-none">
          <option value="">Tous les statuts</option>
          <option value="ACTIF">Actif</option>
          <option value="SUSPENDU">Suspendu</option>
          <option value="RESILIE">Résilié</option>
          <option value="EXPIRE">Expiré</option>
        </select>
        <select [(ngModel)]="periodiciteFiltre" (change)="rechercher()" class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-xs outline-none">
          <option value="">Toutes périodicités</option>
          <option value="MENSUEL">Mensuel</option>
          <option value="TRIMESTRIEL">Trimestriel</option>
          <option value="SEMESTRIEL">Semestriel</option>
          <option value="ANNUEL">Annuel</option>
        </select>
      </div>
      <div class="flex items-center gap-2">
        <button (click)="rechercher()" class="rounded-lg bg-slate-100 px-3 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-200">Filtrer</button>
        <button (click)="reinitialiserFiltres()" class="text-xs text-slate-500 hover:underline">Réinitialiser</button>
      </div>
    </div>

    <!-- Table des abonnements -->
    <div class="overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-sm">
      <table class="w-full min-w-[1050px] text-left text-xs">
        <thead class="border-b border-slate-200 bg-slate-50 font-semibold text-slate-600 uppercase tracking-wider text-[11px] whitespace-nowrap">
          <tr>
            <th class="px-4 py-3.5 whitespace-nowrap">Code Contrat</th>
            <th class="px-4 py-3.5 whitespace-nowrap min-w-[280px]">Objet & Prestataire</th>
            <th class="px-4 py-3.5 whitespace-nowrap">Périodicité</th>
            <th class="px-4 py-3.5 text-right whitespace-nowrap">Montant Échéance</th>
            <th class="px-4 py-3.5 whitespace-nowrap min-w-[190px]">Validité</th>
            <th class="px-4 py-3.5 text-center whitespace-nowrap">Échéances</th>
            <th class="px-4 py-3.5 text-center whitespace-nowrap">Statut</th>
            <th class="px-4 py-3.5 text-right whitespace-nowrap">Actions</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          @if (chargement()) {
            <tr><td colspan="8" class="p-8 text-center text-slate-500">Chargement des abonnements…</td></tr>
          } @else if (abonnements().length === 0) {
            <tr><td colspan="8" class="p-8 text-center text-slate-400">Aucun contrat d'abonnement trouvé. Cliquez sur « Nouveau contrat » pour en créer un.</td></tr>
          } @else {
            @for (a of abonnements(); track a.id) {
              <tr class="transition hover:bg-slate-50/80">
                <td class="px-4 py-3.5 align-middle font-mono font-bold text-slate-900 whitespace-nowrap">
                  <button (click)="ouvrirDetail(a)" class="text-green-800 hover:underline" title="Voir l'échéancier et les bons de commande">
                    {{ a.codeAbonnement }}
                  </button>
                </td>
                <td class="px-4 py-3.5 align-middle">
                  <div class="font-semibold text-slate-900 leading-snug">{{ a.libelle }}</div>
                  <div class="mt-1 flex flex-wrap items-center gap-1.5 text-[11px] text-slate-500">
                    <span class="font-medium text-slate-700 whitespace-nowrap">{{ a.fournisseur.raisonSociale }}</span>
                    @if (a.fournisseur.codeFournisseur) {
                      <span class="rounded bg-slate-100 px-1.5 py-0.5 font-mono text-[9px] text-slate-600 whitespace-nowrap border border-slate-200">{{ a.fournisseur.codeFournisseur }}</span>
                    }
                    @if (a.activiteCode) {
                      <span class="whitespace-nowrap text-slate-400">• Act. <strong class="text-slate-600 font-mono">{{ a.activiteCode }}</strong></span>
                    }
                  </div>
                </td>
                <td class="px-4 py-3.5 align-middle whitespace-nowrap">
                  <span [class]="badgePeriodicite(a.periodicite)" class="inline-flex items-center rounded-full px-2.5 py-1 text-[10px] font-semibold">
                    {{ a.periodicite }}
                  </span>
                  <span class="block text-[10px] text-slate-400 mt-1 whitespace-nowrap">Jour {{ a.jourFacturation || 1 }}</span>
                </td>
                <td class="px-4 py-3.5 align-middle text-right font-mono whitespace-nowrap">
                  <div class="font-bold text-slate-900 whitespace-nowrap text-sm">{{ a.montantEcheanceTtc | number:'1.0-0' }} <span class="text-[10px] font-sans font-medium text-slate-500">TTC</span></div>
                  <div class="text-[10px] text-slate-400 whitespace-nowrap font-sans">{{ a.montantEcheanceHt | number:'1.0-0' }} HT</div>
                </td>
                <td class="px-4 py-3.5 align-middle whitespace-nowrap text-xs">
                  <div class="font-medium text-slate-700 whitespace-nowrap">
                    {{ a.dateDebut | date:'dd/MM/yyyy' }} <span class="text-slate-400 mx-1">→</span> {{ a.dateFin ? (a.dateFin | date:'dd/MM/yyyy') : (a.taciteReconduction ? 'Tacite reconduction' : 'Indéterminée') }}
                  </div>
                  @if (a.preavisJours) {
                    <div class="text-[10px] text-amber-700 mt-0.5 whitespace-nowrap font-medium">Préavis : {{ a.preavisJours }} jours</div>
                  }
                </td>
                <td class="px-4 py-3.5 align-middle text-center whitespace-nowrap">
                  <button (click)="ouvrirDetail(a)" class="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-slate-50 px-2 py-1 text-xs font-semibold text-slate-700 transition hover:bg-green-50 hover:border-green-300 hover:text-green-800">
                    <svg aria-hidden="true" class="h-3.5 w-3.5 text-green-700" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>
                    {{ a.nombreEcheances }}
                  </button>
                </td>
                <td class="px-4 py-3.5 align-middle text-center whitespace-nowrap">
                  <span [class]="badgeStatut(a.statut)" class="inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold">
                    {{ a.statut }}
                  </span>
                </td>
                <td class="px-4 py-3.5 align-middle text-right whitespace-nowrap">
                  <app-row-actions
                    [label]="a.libelle"
                    [canEdit]="auth.hasPermission('abonnement.modifier')"
                    [canDelete]="auth.hasPermission('abonnement.supprimer')"
                    (view)="ouvrirDetail(a)"
                    (edit)="editer(a)"
                    (remove)="supprimer(a)" />
                </td>
              </tr>
            }
          }
        </tbody>
      </table>
      <app-pagination-bar
        [page]="page()"
        [totalPages]="totalPages()"
        [total]="totalElements()"
        (pageChange)="onPageChange($event)" />
    </div>
  </div>
}

<!-- MODAL DE DÉTAIL, ÉMISSION DE BC & RAPPROCHEMENT FACTURE (ÉTAPES 2, 3 & 4) -->
@if (detailModalOpen() && selectedAbonnement()) {
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
    <div class="max-h-[94vh] w-full max-w-5xl overflow-hidden rounded-2xl bg-white shadow-2xl flex flex-col">
      <!-- Modal Header -->
      <div class="flex items-center justify-between border-b border-slate-200 bg-slate-50 px-6 py-4">
        <div>
          <div class="flex items-center gap-2">
            <span class="rounded bg-green-100 px-2 py-0.5 font-mono text-xs font-bold text-green-800">{{ selectedAbonnement()!.codeAbonnement }}</span>
            <span [class]="badgeStatut(selectedAbonnement()!.statut)" class="rounded-full px-2 py-0.5 text-[10px] font-semibold">{{ selectedAbonnement()!.statut }}</span>
            <span [class]="badgePeriodicite(selectedAbonnement()!.periodicite)" class="rounded-full px-2 py-0.5 text-[10px] font-semibold">{{ selectedAbonnement()!.periodicite }}</span>
          </div>
          <h2 class="mt-1 text-base font-bold text-slate-900">{{ selectedAbonnement()!.libelle }}</h2>
          <p class="text-xs text-slate-500">Prestataire : <strong>{{ selectedAbonnement()!.fournisseur.raisonSociale }}</strong> • Activité : <strong>{{ selectedAbonnement()!.activiteLibelle || 'Non spécifiée' }}</strong></p>
        </div>
        <button (click)="fermerDetail()" class="rounded-lg p-1.5 text-slate-400 hover:bg-slate-200 hover:text-slate-700">✕</button>
      </div>

      <!-- Modal Body -->
      <div class="flex-1 overflow-y-auto p-6 space-y-6">
        <!-- Synthèse du contrat -->
        <div class="grid grid-cols-2 gap-3 sm:grid-cols-4 rounded-xl border border-slate-200 bg-slate-50/50 p-3">
          <div>
            <p class="text-[10px] font-semibold text-slate-500 uppercase">Montant / Échéance TTC</p>
            <p class="text-sm font-bold text-slate-900">{{ selectedAbonnement()!.montantEcheanceTtc | number:'1.0-0' }} FCFA</p>
            <span class="text-[10px] text-slate-400">{{ selectedAbonnement()!.montantEcheanceHt | number:'1.0-0' }} HT</span>
          </div>
          <div>
            <p class="text-[10px] font-semibold text-slate-500 uppercase">Période contractuelle</p>
            <p class="text-xs font-medium text-slate-800">Du {{ selectedAbonnement()!.dateDebut }}</p>
            <p class="text-[10px] text-slate-500">{{ selectedAbonnement()!.dateFin ? 'Au ' + selectedAbonnement()!.dateFin : 'Sans terme fixe' }}</p>
          </div>
          <div>
            <p class="text-[10px] font-semibold text-slate-500 uppercase">Facturation & Préavis</p>
            <p class="text-xs font-medium text-slate-800">Le {{ selectedAbonnement()!.jourFacturation || 1 }} du mois</p>
            <span class="text-[10px] text-slate-400">Préavis : {{ selectedAbonnement()!.preavisJours || 0 }} jours</span>
          </div>
          <div>
            <p class="text-[10px] font-semibold text-slate-500 uppercase">Tacite reconduction</p>
            <p class="text-xs font-semibold" [class.text-green-700]="selectedAbonnement()!.taciteReconduction" [class.text-slate-500]="!selectedAbonnement()!.taciteReconduction">
              {{ selectedAbonnement()!.taciteReconduction ? 'Oui (Automatique)' : 'Non' }}
            </p>
          </div>
        </div>

        <!-- Indicateurs de pilotage de l'échéancier (Étape 4) -->
        <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <div class="rounded-lg border border-slate-200 bg-white p-2.5">
            <p class="text-[10px] uppercase text-slate-500 font-semibold">Total Échéances</p>
            <p class="text-lg font-bold text-slate-800">{{ echeances().length }}</p>
          </div>
          <div class="rounded-lg border border-purple-200 bg-purple-50/40 p-2.5">
            <p class="text-[10px] uppercase text-purple-700 font-semibold">Bons de Commande Émis</p>
            <p class="text-lg font-bold text-purple-900">{{ countBcEmis() }}</p>
          </div>
          <div class="rounded-lg border border-emerald-200 bg-emerald-50/40 p-2.5">
            <p class="text-[10px] uppercase text-emerald-700 font-semibold">Factures Rapprochées</p>
            <p class="text-lg font-bold text-emerald-900">{{ countFacturees() }}</p>
          </div>
          <div class="rounded-lg border border-blue-200 bg-blue-50/40 p-2.5">
            <p class="text-[10px] uppercase text-blue-700 font-semibold">Montant Facturé / Exigible</p>
            <p class="text-sm font-bold text-blue-900">{{ totalMontantFacture() | number:'1.0-0' }} / {{ totalMontantAttendu() | number:'1.0-0' }} <span class="text-[10px]">FCFA</span></p>
          </div>
        </div>

        <!-- Tableau des Échéances & Actions (Étapes 2, 3 & 4) -->
        <div>
          <div class="flex items-center justify-between mb-3">
            <div>
              <h3 class="text-sm font-bold text-slate-900 flex items-center gap-2">
                <svg aria-hidden="true" class="h-4 w-4 text-green-700" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>
                Planning prévisionnel des échéances & exécution
              </h3>
              <p class="text-xs text-slate-500">Émission de bon de commande par échéance, rapprochement de facture et suivi du règlement.</p>
            </div>
            @if (auth.hasPermission('abonnement.modifier')) {
              <button (click)="genererEcheancier()" [disabled]="actionLoading()"
                class="flex items-center gap-1.5 rounded-xl border border-green-700 bg-green-50 px-3 py-1.5 text-xs font-semibold text-green-800 hover:bg-green-100 disabled:opacity-50 transition">
                <svg aria-hidden="true" class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/></svg>
                {{ actionLoading() ? 'Calcul…' : '⚡ Régénérer planning' }}
              </button>
            }
          </div>

          <div class="overflow-x-auto rounded-xl border border-slate-200 bg-white">
            <table class="w-full text-left text-xs">
              <thead class="border-b border-slate-200 bg-slate-50 font-semibold text-slate-600 uppercase tracking-wider text-[10px]">
                <tr>
                  <th class="p-2.5">N°</th>
                  <th class="p-2.5">Date Prévue</th>
                  <th class="p-2.5 text-right">Montant Attendu</th>
                  <th class="p-2.5 text-center">Statut</th>
                  <th class="p-2.5">Bon de Commande (Étape 3)</th>
                  <th class="p-2.5">Facture Fournisseur (Étape 4)</th>
                  <th class="p-2.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                @if (echeances().length === 0) {
                  <tr>
                    <td colspan="7" class="p-6 text-center text-slate-400">
                      Aucune échéance disponible. Cliquez sur « ⚡ Régénérer planning » pour créer l'échéancier.
                    </td>
                  </tr>
                } @else {
                  @for (ech of echeances(); track ech.id) {
                    <tr class="hover:bg-slate-50/80 transition">
                      <td class="p-2.5 font-bold text-slate-700">#{{ ech.numEcheance }}</td>
                      <td class="p-2.5 font-medium text-slate-900">{{ ech.dateEcheance }}</td>
                      <td class="p-2.5 text-right font-mono font-bold text-slate-900">{{ ech.montantAttenduTtc | number:'1.0-0' }} FCFA</td>
                      <td class="p-2.5 text-center">
                        <span [class]="badgeStatutEcheance(ech.statut)" class="inline-flex rounded-full px-2 py-0.5 text-[9px] font-semibold">
                          {{ libelleStatutEcheance(ech.statut) }}
                        </span>
                      </td>
                      <td class="p-2.5">
                        @if (ech.bcCode) {
                          <div class="flex items-center gap-1.5">
                            <span class="rounded bg-purple-100 px-1.5 py-0.5 font-mono text-[10px] font-bold text-purple-800">
                              {{ ech.bcCode }}
                            </span>
                            @if (ech.bcId) {
                              <button (click)="telechargerBcPdf(ech.bcId, ech.bcCode)" title="Télécharger le bon de commande PDF"
                                class="rounded p-1 text-purple-700 hover:bg-purple-100">
                                <svg aria-hidden="true" class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
                              </button>
                            }
                          </div>
                        } @else {
                          <span class="text-[10px] text-slate-400">Non émis</span>
                        }
                      </td>
                      <td class="p-2.5">
                        @if (ech.referenceFacture) {
                          <div>
                            <span class="font-mono font-bold text-emerald-800 text-[11px]">{{ ech.referenceFacture }}</span>
                            <div class="text-[10px] text-slate-500 font-mono">{{ ech.montantFactureTtc | number:'1.0-0' }} FCFA</div>
                          </div>
                        } @else {
                          <span class="text-[10px] text-slate-400">En attente</span>
                        }
                      </td>
                      <td class="p-2.5 text-right">
                        <div class="flex items-center justify-end gap-1.5">
                          @if (!ech.bcCode && auth.hasPermission('abonnement.modifier')) {
                            <button (click)="emettreBc(ech)" [disabled]="actionLoading()"
                              title="Générer un bon de commande officiel pour cette échéance"
                              class="inline-flex items-center gap-1 rounded bg-purple-700 px-2 py-1 text-[10px] font-semibold text-white shadow-sm hover:bg-purple-800 disabled:opacity-50">
                              <svg aria-hidden="true" class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"/></svg>
                              Émettre BC
                            </button>
                          }
                          @if (!ech.referenceFacture && auth.hasPermission('abonnement.modifier')) {
                            <button (click)="ouvrirRapprochement(ech)"
                              title="Rapprocher la facture fournisseur reçue"
                              class="inline-flex items-center gap-1 rounded border border-emerald-600 bg-emerald-50 px-2 py-1 text-[10px] font-semibold text-emerald-800 hover:bg-emerald-100">
                              <svg aria-hidden="true" class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
                              Rapprocher facture
                            </button>
                          }
                        </div>
                      </td>
                    </tr>
                  }
                }
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Modal Footer -->
      <div class="flex items-center justify-between border-t border-slate-200 bg-slate-50 px-6 py-3">
        <span class="text-xs text-slate-500">Module 6 • Contrats récurrents & Abonnements (Étapes 1 à 4)</span>
        <button (click)="fermerDetail()" class="rounded-lg border border-slate-300 bg-white px-4 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50">
          Fermer
        </button>
      </div>
    </div>
  </div>
}

<!-- SOUS-MODAL DE RAPPROCHEMENT FACTURE (ÉTAPE 4) -->
@if (rapprochementModalOpen() && selectedEcheance()) {
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
    <div class="w-full max-w-md rounded-2xl bg-white shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-150">
      <div class="border-b border-slate-200 bg-emerald-50/70 px-5 py-4 flex items-center justify-between">
        <div>
          <h3 class="text-sm font-bold text-slate-900">Rapprochement de Facture Fournisseur</h3>
          <p class="text-xs text-slate-500">Échéance n°{{ selectedEcheance()!.numEcheance }} • Date exigible : {{ selectedEcheance()!.dateEcheance }}</p>
        </div>
        <button (click)="fermerRapprochement()" class="text-slate-400 hover:text-slate-700">✕</button>
      </div>

      <form (ngSubmit)="validerRapprochement()" class="p-5 space-y-4 text-xs">
        <div class="rounded-lg border border-slate-200 bg-slate-50 p-2.5">
          <span class="text-[10px] uppercase font-semibold text-slate-500">Montant contractuel attendu</span>
          <p class="text-sm font-bold text-slate-900 font-mono">{{ selectedEcheance()!.montantAttenduTtc | number:'1.0-0' }} FCFA</p>
          @if (selectedEcheance()!.bcCode) {
            <span class="text-[10px] text-purple-700 font-medium">Bon de commande associé : {{ selectedEcheance()!.bcCode }}</span>
          }
        </div>

        <label class="block space-y-1">
          Numéro / Référence de la facture fournisseur <span class="champ-obligatoire" aria-hidden="true">*</span>
          <input required maxlength="100" [(ngModel)]="factureDraft.referenceFacture" name="referenceFacture"
            placeholder="Ex. : FACT-2026/01/0087"
            class="w-full rounded-lg border border-slate-300 p-2 font-mono text-xs uppercase">
        </label>

        <label class="block space-y-1">
          Montant réel facturé TTC (FCFA) <span class="champ-obligatoire" aria-hidden="true">*</span>
          <input required type="number" min="1" step="1" [(ngModel)]="factureDraft.montantFactureTtc" name="montantFactureTtc"
            class="w-full rounded-lg border border-slate-300 p-2 font-mono text-xs font-bold text-slate-900">
        </label>

        <label class="block space-y-1">
          Date de réception / émission facture
          <input type="date" [(ngModel)]="factureDraft.dateFacture" name="dateFacture"
            class="w-full rounded-lg border border-slate-300 p-2 text-xs">
        </label>

        <label class="block space-y-1">
          Observations / Remarques
          <textarea rows="2" [(ngModel)]="factureDraft.commentaire" name="commentaire"
            placeholder="Conformité des index, pénalités éventuelles, solde restant..."
            class="w-full rounded-lg border border-slate-300 p-2 text-xs"></textarea>
        </label>

        <div class="flex items-center justify-end gap-2 pt-2 border-t border-slate-100">
          <button type="button" (click)="fermerRapprochement()" class="rounded-lg border border-slate-300 px-3 py-1.5 text-xs text-slate-600">Annuler</button>
          <button [disabled]="actionLoading()" class="rounded-lg bg-emerald-700 px-4 py-1.5 text-xs font-semibold text-white shadow hover:bg-emerald-800 disabled:opacity-50">
            {{ actionLoading() ? 'Enregistrement…' : 'Valider le rapprochement' }}
          </button>
        </div>
      </form>
    </div>
  </div>
}
`
})
export class AbonnementsListComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly notification = inject(NotificationService);
  private readonly abonnementService = inject(AbonnementService);
  private readonly achatService = inject(AchatService);
  private readonly nomenclatureService = inject(NomenclatureService);

  readonly abonnements = signal<Abonnement[]>([]);
  readonly chargement = signal(false);
  readonly saving = signal(false);
  readonly actionLoading = signal(false);
  readonly formOpen = signal(false);
  readonly editingId = signal<string | null>(null);

  // Pagination & filtres
  readonly page = signal(0);
  readonly pageSize = signal(20);
  readonly totalElements = signal(0);
  readonly totalPages = computed(() => Math.ceil(this.totalElements() / this.pageSize()) || 1);
  recherche = '';
  statutFiltre = '';
  periodiciteFiltre = '';

  // Données référentielles
  readonly fournisseurs = signal<Fournisseur[]>([]);
  readonly activites = signal<ActiviteCatalogue[]>([]);

  // Modal détail & échéancier
  readonly detailModalOpen = signal(false);
  readonly selectedAbonnement = signal<Abonnement | null>(null);
  readonly echeances = signal<EcheanceAbonnement[]>([]);

  // Modal sous-jacent de rapprochement facture (Étape 4)
  readonly rapprochementModalOpen = signal(false);
  readonly selectedEcheance = signal<EcheanceAbonnement | null>(null);
  factureDraft: RapprochementFactureRequest = {
    referenceFacture: '',
    montantFactureTtc: 0,
    dateFacture: new Date().toISOString().substring(0, 10),
    commentaire: ''
  };

  // Formulaire draft contrat
  draft: AbonnementRequest = this.nouveauDraft();

  // Statistiques calculées globales
  readonly statsActifs = computed(() => this.abonnements().filter(a => a.statut === 'ACTIF').length);
  readonly statsTotalEcheances = computed(() => this.abonnements().reduce((acc, a) => acc + (a.nombreEcheances || 0), 0));
  readonly statsVolumeTtc = computed(() =>
    this.abonnements().reduce((acc, a) => acc + (Number(a.montantEcheanceTtc || 0) * (a.nombreEcheances || 1)), 0)
  );

  // Statistiques de l'abonnement sélectionné
  readonly countBcEmis = computed(() => this.echeances().filter(e => !!e.bcCode).length);
  readonly countFacturees = computed(() => this.echeances().filter(e => !!e.referenceFacture).length);
  readonly totalMontantAttendu = computed(() => this.echeances().reduce((acc, e) => acc + Number(e.montantAttenduTtc || 0), 0));
  readonly totalMontantFacture = computed(() => this.echeances().reduce((acc, e) => acc + Number(e.montantFactureTtc || 0), 0));

  ngOnInit(): void {
    this.chargerReferentiels();
    this.chargerAbonnements();
  }

  chargerReferentiels(): void {
    this.achatService.listerFournisseurs({ size: 1000 }).subscribe({
      next: (res) => this.fournisseurs.set(res.content || []),
      error: () => {}
    });
    this.nomenclatureService.listerActivites().subscribe({
      next: (res) => this.activites.set(res || []),
      error: () => {}
    });
  }

  chargerAbonnements(): void {
    this.chargement.set(true);
    const params: Record<string, string | number> = {
      page: this.page(),
      size: this.pageSize()
    };
    if (this.recherche.trim()) params['recherche'] = this.recherche.trim();
    if (this.statutFiltre) params['statut'] = this.statutFiltre;
    if (this.periodiciteFiltre) params['periodicite'] = this.periodiciteFiltre;

    this.abonnementService.listerAbonnements(params).subscribe({
      next: (res) => {
        this.abonnements.set(res.content || []);
        this.totalElements.set(res.totalElements || 0);
        this.chargement.set(false);
      },
      error: (err) => {
        this.chargement.set(false);
        this.notification.error(errorMessage(err));
      }
    });
  }

  rechercher(): void {
    this.page.set(0);
    this.chargerAbonnements();
  }

  reinitialiserFiltres(): void {
    this.recherche = '';
    this.statutFiltre = '';
    this.periodiciteFiltre = '';
    this.page.set(0);
    this.chargerAbonnements();
  }

  onPageChange(p: number): void {
    this.page.set(p);
    this.chargerAbonnements();
  }

  nouveauDraft(): AbonnementRequest {
    const today = new Date().toISOString().substring(0, 10);
    return {
      libelle: '',
      fournisseurId: '',
      activiteId: null,
      periodicite: 'MENSUEL',
      montantEcheanceHt: 0,
      montantEcheanceTtc: 0,
      dateDebut: today,
      dateFin: null,
      jourFacturation: 1,
      preavisJours: 30,
      taciteReconduction: true,
      statut: 'ACTIF'
    };
  }

  openCreate(): void {
    this.draft = this.nouveauDraft();
    if (this.fournisseurs().length > 0) {
      this.draft.fournisseurId = this.fournisseurs()[0].id;
    }
    if (this.activites().length > 0) {
      this.draft.activiteId = this.activites()[0].id;
    }
    this.editingId.set(null);
    this.formOpen.set(true);
  }

  editer(a: Abonnement): void {
    this.draft = {
      codeAbonnement: a.codeAbonnement,
      libelle: a.libelle,
      fournisseurId: a.fournisseur.id,
      activiteId: a.activiteId || null,
      periodicite: a.periodicite,
      montantEcheanceHt: a.montantEcheanceHt,
      montantEcheanceTtc: a.montantEcheanceTtc,
      dateDebut: a.dateDebut,
      dateFin: a.dateFin || null,
      jourFacturation: a.jourFacturation || 1,
      preavisJours: a.preavisJours || 0,
      taciteReconduction: a.taciteReconduction,
      statut: a.statut
    };
    this.editingId.set(a.id);
    this.formOpen.set(true);
  }

  onMontantHtChange(ht: number): void {
    if (ht && (!this.draft.montantEcheanceTtc || this.draft.montantEcheanceTtc === 0)) {
      this.draft.montantEcheanceTtc = Math.round(Number(ht) * 1.18);
    }
  }

  save(): void {
    if (!this.draft.libelle.trim()) {
      this.notification.error('Veuillez renseigner le libellé de l’abonnement.');
      return;
    }
    if (!this.draft.fournisseurId) {
      this.notification.error('Veuillez sélectionner un fournisseur.');
      return;
    }
    if (!this.draft.dateDebut) {
      this.notification.error('La date de début est obligatoire.');
      return;
    }
    if (!this.draft.montantEcheanceHt || this.draft.montantEcheanceHt <= 0) {
      this.notification.error('Le montant HT de l’échéance doit être supérieur à zéro.');
      return;
    }

    this.saving.set(true);
    const id = this.editingId();
    const req$ = id
      ? this.abonnementService.modifierAbonnement(id, this.draft)
      : this.abonnementService.creerAbonnement(this.draft);

    req$.subscribe({
      next: () => {
        this.saving.set(false);
        this.formOpen.set(false);
        this.notification.success(
          id ? 'Contrat d’abonnement mis à jour avec succès.' : 'Contrat créé et échéancier généré avec succès.'
        );
        this.chargerAbonnements();
      },
      error: (err) => {
        this.saving.set(false);
        this.notification.error(errorMessage(err));
      }
    });
  }

  supprimer(a: Abonnement): void {
    if (!confirm(`Êtes-vous sûr de vouloir supprimer le contrat ${a.codeAbonnement} — ${a.libelle} ?`)) {
      return;
    }
    this.abonnementService.supprimerAbonnement(a.id).subscribe({
      next: () => {
        this.notification.success('Contrat supprimé avec succès.');
        this.chargerAbonnements();
      },
      error: (err) => {
        this.notification.error(errorMessage(err));
      }
    });
  }

  ouvrirDetail(a: Abonnement): void {
    this.selectedAbonnement.set(a);
    this.echeances.set([]);
    this.detailModalOpen.set(true);
    this.chargerEcheances(a.id);
  }

  fermerDetail(): void {
    this.detailModalOpen.set(false);
    this.selectedAbonnement.set(null);
    this.echeances.set([]);
  }

  chargerEcheances(abonnementId: string): void {
    this.abonnementService.obtenirEcheances(abonnementId).subscribe({
      next: (list) => this.echeances.set(list || []),
      error: (err) => this.notification.error(errorMessage(err))
    });
  }

  genererEcheancier(): void {
    const a = this.selectedAbonnement();
    if (!a) return;
    this.actionLoading.set(true);
    this.abonnementService.genererEcheances(a.id).subscribe({
      next: (nouvellesEcheances) => {
        this.actionLoading.set(false);
        this.echeances.set(nouvellesEcheances || []);
        this.notification.success('Échéancier généré et recalculé avec succès.');
        this.chargerAbonnements();
      },
      error: (err) => {
        this.actionLoading.set(false);
        this.notification.error(errorMessage(err));
      }
    });
  }

  emettreBc(ech: EcheanceAbonnement): void {
    const a = this.selectedAbonnement();
    if (!a) return;
    if (!confirm(`Émettre un bon de commande officiel pour l'échéance n°${ech.numEcheance} (${ech.dateEcheance}) d'un montant de ${ech.montantAttenduTtc} FCFA ?`)) {
      return;
    }
    this.actionLoading.set(true);
    this.abonnementService.genererBcPourEcheance(a.id, ech.id).subscribe({
      next: (updatedEch) => {
        this.actionLoading.set(false);
        this.notification.success(`Bon de commande ${updatedEch.bcCode || ''} généré avec succès !`);
        this.chargerEcheances(a.id);
      },
      error: (err) => {
        this.actionLoading.set(false);
        this.notification.error(errorMessage(err));
      }
    });
  }

  telechargerBcPdf(bcId: string, bcCode?: string | null): void {
    this.achatService.telechargerBonCommandePdf(bcId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `bon_commande_${bcCode || bcId}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => this.notification.error(errorMessage(err))
    });
  }

  ouvrirRapprochement(ech: EcheanceAbonnement): void {
    this.selectedEcheance.set(ech);
    this.factureDraft = {
      referenceFacture: '',
      montantFactureTtc: ech.montantAttenduTtc,
      dateFacture: new Date().toISOString().substring(0, 10),
      commentaire: ''
    };
    this.rapprochementModalOpen.set(true);
  }

  fermerRapprochement(): void {
    this.rapprochementModalOpen.set(false);
    this.selectedEcheance.set(null);
  }

  validerRapprochement(): void {
    const a = this.selectedAbonnement();
    const ech = this.selectedEcheance();
    if (!a || !ech) return;

    if (!this.factureDraft.referenceFacture.trim()) {
      this.notification.error('Veuillez renseigner le numéro ou la référence de la facture.');
      return;
    }
    if (!this.factureDraft.montantFactureTtc || this.factureDraft.montantFactureTtc <= 0) {
      this.notification.error('Le montant facturé TTC doit être supérieur à zéro.');
      return;
    }

    this.actionLoading.set(true);
    this.abonnementService.rapprocherFacture(a.id, ech.id, this.factureDraft).subscribe({
      next: () => {
        this.actionLoading.set(false);
        this.notification.success('Facture rapprochée avec succès sur l’échéance !');
        this.fermerRapprochement();
        this.chargerEcheances(a.id);
      },
      error: (err) => {
        this.actionLoading.set(false);
        this.notification.error(errorMessage(err));
      }
    });
  }

  openImport(): void {
    this.notification.info('Fonctionnalité d’import disponible prochainement.');
  }

  exporterCsv(): void {
    const list = this.abonnements();
    if (!list.length) {
      this.notification.info('Aucune donnée à exporter.');
      return;
    }
    const headers = ['Code', 'Libelle', 'Fournisseur', 'Periodicite', 'Montant TTC', 'Date Debut', 'Date Fin', 'Statut'];
    const rows = list.map(a => [
      a.codeAbonnement,
      `"${a.libelle.replace(/"/g, '""')}"`,
      `"${(a.fournisseur?.raisonSociale || '').replace(/"/g, '""')}"`,
      a.periodicite,
      a.montantEcheanceTtc,
      a.dateDebut,
      a.dateFin || '',
      a.statut
    ]);
    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(';'), ...rows.map(e => e.join(';'))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `abonnements_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  badgePeriodicite(p: PeriodiciteAbonnement): string {
    switch (p) {
      case 'MENSUEL': return 'bg-blue-100 text-blue-800';
      case 'TRIMESTRIEL': return 'bg-purple-100 text-purple-800';
      case 'SEMESTRIEL': return 'bg-amber-100 text-amber-800';
      case 'ANNUEL': return 'bg-emerald-100 text-emerald-800';
      default: return 'bg-slate-100 text-slate-800';
    }
  }

  badgeStatut(s: StatutAbonnement): string {
    switch (s) {
      case 'ACTIF': return 'bg-green-100 text-green-800 border border-green-200';
      case 'SUSPENDU': return 'bg-amber-100 text-amber-800 border border-amber-200';
      case 'RESILIE': return 'bg-red-100 text-red-800 border border-red-200';
      case 'EXPIRE': return 'bg-slate-100 text-slate-700 border border-slate-200';
      default: return 'bg-slate-100 text-slate-800';
    }
  }

  badgeStatutEcheance(s: string): string {
    switch (s) {
      case 'A_VENIR': return 'bg-blue-100 text-blue-800';
      case 'BC_GENERE':
      case 'BON_COMMANDE_GENERE': return 'bg-purple-100 text-purple-800 border border-purple-200';
      case 'FACTURE_RECUE':
      case 'FACTURE': return 'bg-emerald-100 text-emerald-800 border border-emerald-200';
      case 'ANNULE': return 'bg-red-100 text-red-700 line-through';
      default: return 'bg-slate-100 text-slate-700';
    }
  }

  libelleStatutEcheance(s: string): string {
    switch (s) {
      case 'A_VENIR': return 'À venir';
      case 'BC_GENERE':
      case 'BON_COMMANDE_GENERE': return 'BC Généré';
      case 'FACTURE_RECUE':
      case 'FACTURE': return 'Facture Rapprochée';
      case 'ANNULE': return 'Annulé';
      default: return s;
    }
  }
}
