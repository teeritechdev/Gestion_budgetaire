import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { WorkflowService } from '../../core/services/workflow.service';
import {
  WorkflowDefinition,
  WorkflowDefinitionRequest,
  WorkflowEtape
} from '../../core/models/workflow/workflow.model';
import { PageHeaderComponent } from '../../shared/components/page-header.component';
import { RowActionsComponent } from '../../shared/components/row-actions.component';
import { FormPageComponent } from '../../shared/components/form-page.component';
import { PaginationBarComponent } from '../../shared/components/pagination-bar.component';
import { errorMessage } from '../../core/utils/error-message';

@Component({
  selector: 'app-workflow-definitions-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    PageHeaderComponent,
    RowActionsComponent,
    FormPageComponent,
    PaginationBarComponent,
  ],
  template: `
@if (formOpen()) {
  <app-form-page
    section="Moteur de Workflow & Circuits"
    [title]="editingId() ? 'Modifier le modèle de workflow' : 'Nouveau modèle de workflow'"
    [mode]="editingId() ? 'Modification' : 'Création'"
    subtitle="Paramétrez un circuit d'approbation séquentiel transversal adapté à un type d'entité."
    (back)="formOpen.set(false)">
    <form (ngSubmit)="saveDefinition()" class="space-y-4">
      <section class="space-y-4 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <h3 class="text-sm font-semibold text-slate-900">Identification du circuit</h3>
        <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <label class="block space-y-1 text-xs">
            Code unique du workflow <span class="champ-obligatoire" aria-hidden="true">*</span>
            <input required maxlength="60" [(ngModel)]="draftDef.code" name="code"
              placeholder="Ex. : CHAINE_ENGAGEMENT_P2P"
              [disabled]="!!editingId()"
              class="w-full rounded-lg border border-slate-300 p-2 text-xs uppercase disabled:bg-slate-100">
          </label>
          <label class="block space-y-1 text-xs">
            Type d'entité ciblée <span class="champ-obligatoire" aria-hidden="true">*</span>
            <select required [(ngModel)]="draftDef.typeEntite" name="typeEntite"
              class="w-full rounded-lg border border-slate-300 bg-white p-2 text-xs">
              <option value="DEMANDE_ACHAT">Demande d'Achat (DA)</option>
              <option value="BON_COMMANDE">Bon de Commande (BC)</option>
              <option value="CONTRAT_ABONNEMENT">Contrat d'Abonnement Récurrent</option>
              <option value="DEMANDE_STOCK">Demande d'articles (Stock)</option>
              <option value="REVISION_BUDGETAIRE">Révision Budgétaire</option>
            </select>
          </label>
          <label class="block space-y-1 text-xs sm:col-span-2">
            Libellé / Titre du workflow <span class="champ-obligatoire" aria-hidden="true">*</span>
            <input required maxlength="255" [(ngModel)]="draftDef.libelle" name="libelle"
              placeholder="Ex. : Circuit standard d'engagement des dépenses P2P"
              class="w-full rounded-lg border border-slate-300 p-2 text-xs">
          </label>
          <label class="block space-y-1 text-xs sm:col-span-2">
            Description fonctionnelle
            <textarea rows="3" [(ngModel)]="draftDef.description" name="description"
              placeholder="Description des règles d'escalade et validation…"
              class="w-full rounded-lg border border-slate-300 p-2 text-xs"></textarea>
          </label>
          <label class="flex items-center gap-2 text-xs font-semibold sm:col-span-2">
            <input type="checkbox" [(ngModel)]="draftDef.actif" name="actif" class="rounded border-slate-300 text-green-700">
            Modèle actif pour les nouvelles demandes
          </label>
        </div>
      </section>

      <div class="flex justify-end gap-2 pt-2">
        <button type="button" (click)="formOpen.set(false)" class="rounded-lg border border-slate-200 px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50">Annuler</button>
        <button type="submit" [disabled]="saving()" class="rounded-lg bg-green-700 px-4 py-2 text-xs font-semibold text-white hover:bg-green-800 disabled:opacity-50">
          {{ saving() ? 'Enregistrement…' : 'Enregistrer le modèle' }}
        </button>
      </div>
    </form>
  </app-form-page>
} @else {
  <div class="space-y-5">
    <app-page-header
      icon="workflow"
      module="Moteur de Workflow Transversal"
      title="Modèles de Processus & Circuits"
      description="Référentiel des circuits d'approbation multi-niveaux avec conditions de seuils et rôles autorisés.">
      <div class="flex items-center gap-2">
        <button type="button" (click)="exporterCsv()"
          class="flex items-center gap-1.5 rounded-xl border border-blue-800 bg-blue-700 px-3.5 py-2 text-xs font-medium text-white hover:bg-blue-800">
          <svg aria-hidden="true" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
          Export CSV
        </button>
        @if (canCreate()) {
          <button type="button" (click)="ouvrirCreationDef()"
            class="flex items-center gap-1.5 rounded-xl bg-green-700 px-3.5 py-2 text-xs font-semibold text-white shadow-sm hover:bg-green-800">
            <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-width="2" d="M12 4v16m8-8H4"/></svg>
            Nouveau Modèle
          </button>
        }
      </div>
    </app-page-header>

    <!-- Cartes KPI (Design Djibril stats-grid avec liseré supérieur) -->
    <section class="stats-grid grid gap-3 sm:grid-cols-3">
      <article class="rounded-2xl border border-slate-200 bg-white p-4">
        <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Total modèles déclarés</p>
        <p class="mt-2 text-2xl font-bold font-mono text-slate-900">{{ totalDefinitions() }}</p>
        <p class="mt-1 text-[11px] text-slate-500">Circuits d'approbation paramétrés</p>
      </article>

      <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" (click)="typeEntiteFiltre = ''; rechercher()">
        <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Modèles actifs</p>
        <p class="mt-2 text-2xl font-bold font-mono text-emerald-700">{{ modelesActifs() }}</p>
        <p class="mt-1 text-[11px] text-slate-500">En cours d'application</p>
      </article>

      <article class="rounded-2xl border border-slate-200 bg-white p-4">
        <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Entités couvertes</p>
        <p class="mt-2 text-2xl font-bold font-mono text-purple-700">{{ entitesCouvertes() }}</p>
        <p class="mt-1 text-[11px] text-slate-500">DA, BC, Abonnements, Stock</p>
      </article>
    </section>

    <!-- Filtres -->
    <div class="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 bg-white p-3 shadow-sm">
      <div class="flex flex-1 flex-wrap items-center gap-2">
        <div class="relative min-w-[220px] flex-1">
          <input [(ngModel)]="recherche" (keyup.enter)="rechercher()" placeholder="Rechercher par code, libellé…"
            class="w-full rounded-lg border border-slate-200 bg-slate-50 py-2 pl-8 pr-3 text-xs outline-none focus:border-green-600 focus:bg-white">
          <svg aria-hidden="true" class="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
        </div>
        <select [(ngModel)]="typeEntiteFiltre" (change)="rechercher()" class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-xs outline-none">
          <option value="">Tous les types d'entité</option>
          <option value="DEMANDE_ACHAT">Demande d'Achat</option>
          <option value="BON_COMMANDE">Bon de Commande</option>
          <option value="CONTRAT_ABONNEMENT">Contrat d'Abonnement</option>
          <option value="DEMANDE_STOCK">Demande de Stock</option>
        </select>
      </div>
      <div class="flex items-center gap-2">
        <button (click)="rechercher()" class="rounded-lg bg-slate-100 px-3 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-200">Filtrer</button>
        <button (click)="reinitialiserFiltres()" class="text-xs text-slate-500 hover:underline">Réinitialiser</button>
      </div>
    </div>

    <!-- Table des définitions de workflow -->
    <div class="overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-sm">
      <table class="w-full min-w-[850px] text-left text-xs">
        <thead class="border-b border-slate-200 bg-slate-50 font-semibold text-slate-600 uppercase tracking-wider text-[11px] whitespace-nowrap">
          <tr>
            <th class="p-3 whitespace-nowrap">Code Circuit</th>
            <th class="p-3 whitespace-nowrap">Libellé & Description</th>
            <th class="p-3 whitespace-nowrap">Entité Cible</th>
            <th class="p-3 text-center whitespace-nowrap">Étapes</th>
            <th class="p-3 text-center whitespace-nowrap">Statut</th>
            <th class="p-3 text-right whitespace-nowrap">Actions</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          @if (chargement()) {
            <tr><td colspan="6" class="p-8 text-center text-slate-500">Chargement des modèles de workflow…</td></tr>
          } @else if (definitions().length === 0) {
            <tr><td colspan="6" class="p-8 text-center text-slate-400">Aucun modèle de circuit configuré.</td></tr>
          } @else {
            @for (w of definitions(); track w.id) {
              <tr class="transition hover:bg-slate-50/80">
                <td class="p-3 font-mono font-bold text-slate-900">{{ w.code }}</td>
                <td class="p-3">
                  <div class="font-semibold text-slate-900">{{ w.libelle }}</div>
                  @if (w.description) {
                    <div class="text-[11px] text-slate-500 line-clamp-1">{{ w.description }}</div>
                  }
                </td>
                <td class="p-3">
                  <span class="inline-flex rounded-full px-2 py-0.5 text-[10px] font-semibold bg-blue-50 text-blue-700 border border-blue-200">
                    {{ libelleEntite(w.typeEntite) }}
                  </span>
                </td>
                <td class="p-3 text-center">
                  <button (click)="ouvrirEtapes(w)" class="inline-flex items-center gap-1 rounded-md bg-purple-50 px-2 py-1 text-xs font-semibold text-purple-700 border border-purple-200 hover:bg-purple-100">
                    <span>{{ (w.etapes?.length || 0) }} étape(s)</span>
                    <svg class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-width="2" d="M9 5l7 7-7 7"/></svg>
                  </button>
                </td>
                <td class="p-3 text-center">
                  @if (w.actif) {
                    <span class="inline-flex rounded-full bg-green-100 px-2 py-0.5 text-[10px] font-semibold text-green-800">Actif</span>
                  } @else {
                    <span class="inline-flex rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-semibold text-slate-600">Inactif</span>
                  }
                </td>
                <td class="p-3 text-right">
                  <app-row-actions
                    [canEdit]="canEdit()"
                    [canDelete]="canDelete()"
                    (edit)="editerDef(w)"
                    (delete)="supprimerDef(w)">
                    <button customAction (click)="ouvrirEtapes(w)" title="Configurer les étapes du circuit"
                      class="rounded p-1 text-purple-700 hover:bg-purple-50">
                      <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4"/></svg>
                    </button>
                  </app-row-actions>
                </td>
              </tr>
            }
          }
        </tbody>
      </table>
    </div>

    <app-pagination-bar
      [total]="totalDefinitions()"
      [totalPages]="totalPages()"
      [page]="page()"
      (pageChange)="changerPage($event)" />
  </div>
}

<!-- TIROIR / MODAL DE CONFIGURATION DES ÉTAPES -->
@if (etapesModalOpen()) {
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
    <div class="flex max-h-[90vh] w-full max-w-4xl flex-col rounded-2xl bg-white shadow-2xl">
      <div class="flex items-center justify-between border-b border-slate-200 px-6 py-4">
        <div>
          <h3 class="text-base font-bold text-slate-900">Configuration du Circuit : {{ workflowSelectionne()?.libelle }}</h3>
          <p class="text-xs text-slate-500">Code : <span class="font-mono font-semibold">{{ workflowSelectionne()?.code }}</span> — Ordre des étapes, seuils d'intervention et habilitations.</p>
        </div>
        <button (click)="fermerEtapesModal()" class="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600">✕</button>
      </div>

      <div class="flex-1 overflow-y-auto p-6 space-y-6">
        <!-- Visualisation Pipeline Stepper -->
        <div>
          <h4 class="text-xs font-bold uppercase tracking-wider text-slate-500 mb-3">Pipeline d'approbation séquentiel</h4>
          <div class="flex flex-wrap items-center gap-2 rounded-xl bg-slate-50 p-4 border border-slate-200">
            @for (et of etapes(); track et.id; let idx = $index; let last = $last) {
              <div class="flex items-center gap-2">
                <div class="flex flex-col rounded-lg border border-slate-200 bg-white p-2.5 shadow-xs" [class.border-purple-300]="et.estEtapeFinale">
                  <div class="flex items-center gap-1.5">
                    <span class="flex h-5 w-5 items-center justify-center rounded-full bg-purple-100 text-[10px] font-bold text-purple-700">{{ et.numEtape }}</span>
                    <span class="text-xs font-bold text-slate-800">{{ et.libelle }}</span>
                    @if (et.estEtapeFinale) {
                      <span class="rounded bg-green-100 px-1 text-[9px] font-bold text-green-800">Finale</span>
                    }
                  </div>
                  <div class="mt-1 flex items-center gap-2 text-[10px] text-slate-500">
                    <span>Rôle : <strong class="text-slate-700">{{ et.roleRequis || 'Tout valideur' }}</strong></span>
                    @if (et.montantMinimum && et.montantMinimum > 0) {
                      <span>• Seuil : <strong class="text-amber-700">&gt; {{ et.montantMinimum | number:'1.0-0' }} F</strong></span>
                    }
                  </div>
                </div>
                @if (!last) {
                  <svg class="h-4 w-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"/></svg>
                }
              </div>
            }
          </div>
        </div>

        <!-- Formulaire Ajout / Modification Étape -->
        <div class="rounded-xl border border-purple-200 bg-purple-50/40 p-4 space-y-3">
          <h4 class="text-xs font-bold text-purple-900">{{ editingEtapeId() ? 'Modifier l’étape' : 'Ajouter une étape au circuit' }}</h4>
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <label class="block space-y-1 text-xs">
              Numéro d'ordre <span class="champ-obligatoire">*</span>
              <input type="number" min="1" [(ngModel)]="draftEtape.numEtape" class="w-full rounded-lg border border-slate-300 p-2 text-xs bg-white">
            </label>
            <label class="block space-y-1 text-xs">
              Code Étape <span class="champ-obligatoire">*</span>
              <input maxlength="40" [(ngModel)]="draftEtape.codeEtape" placeholder="ETP_VISA_N1" class="w-full rounded-lg border border-slate-300 p-2 text-xs uppercase bg-white">
            </label>
            <label class="block space-y-1 text-xs">
              Libellé Étape <span class="champ-obligatoire">*</span>
              <input maxlength="100" [(ngModel)]="draftEtape.libelle" placeholder="Visa Responsable N+1" class="w-full rounded-lg border border-slate-300 p-2 text-xs bg-white">
            </label>
            <label class="block space-y-1 text-xs">
              Rôle habilité
              <input maxlength="50" [(ngModel)]="draftEtape.roleRequis" placeholder="Ex. : DAF, DG, RESP_SERVICE" class="w-full rounded-lg border border-slate-300 p-2 text-xs bg-white">
            </label>
            <label class="block space-y-1 text-xs">
              Montant minimum (Seuil FCFA)
              <input type="number" min="0" [(ngModel)]="draftEtape.montantMinimum" placeholder="0" class="w-full rounded-lg border border-slate-300 p-2 text-xs bg-white">
            </label>
            <label class="block space-y-1 text-xs">
              Permission requise
              <input maxlength="60" [(ngModel)]="draftEtape.permissionRequise" placeholder="workflow.valider" class="w-full rounded-lg border border-slate-300 p-2 text-xs bg-white">
            </label>
            <div class="flex items-center gap-4 text-xs font-semibold sm:col-span-3 pt-1">
              <label class="flex items-center gap-1.5">
                <input type="checkbox" [(ngModel)]="draftEtape.estEtapeFinale" class="rounded border-slate-300 text-purple-700">
                Étape finale d'approbation (Dossier validé définitivement)
              </label>
            </div>
          </div>
          <div class="flex justify-end gap-2 pt-2">
            @if (editingEtapeId()) {
              <button (click)="annulerEditionEtape()" class="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs text-slate-700 hover:bg-slate-50">Annuler</button>
            }
            <button (click)="sauvegarderEtape()" [disabled]="savingEtape()" class="rounded-lg bg-purple-700 px-4 py-1.5 text-xs font-semibold text-white hover:bg-purple-800 disabled:opacity-50">
              {{ savingEtape() ? 'Enregistrement…' : (editingEtapeId() ? 'Mettre à jour' : 'Ajouter l’étape') }}
            </button>
          </div>
        </div>

        <!-- Tableau détaillé des étapes -->
        <div class="rounded-xl border border-slate-200 overflow-hidden">
          <table class="w-full text-left text-xs">
            <thead class="bg-slate-50 text-[11px] font-semibold text-slate-600 uppercase border-b border-slate-200">
              <tr>
                <th class="p-2.5 text-center">N°</th>
                <th class="p-2.5">Code</th>
                <th class="p-2.5">Libellé</th>
                <th class="p-2.5">Rôle requis</th>
                <th class="p-2.5 text-right">Seuil min.</th>
                <th class="p-2.5 text-center">Finale</th>
                <th class="p-2.5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              @for (et of etapes(); track et.id) {
                <tr class="hover:bg-slate-50">
                  <td class="p-2.5 text-center font-bold text-purple-700">{{ et.numEtape }}</td>
                  <td class="p-2.5 font-mono text-slate-800">{{ et.codeEtape }}</td>
                  <td class="p-2.5 font-semibold text-slate-900">{{ et.libelle }}</td>
                  <td class="p-2.5 text-slate-600">{{ et.roleRequis || 'Tous' }}</td>
                  <td class="p-2.5 text-right font-medium text-slate-800">{{ (et.montantMinimum || 0) | number:'1.0-0' }} F</td>
                  <td class="p-2.5 text-center">
                    @if (et.estEtapeFinale) {
                      <span class="rounded-full bg-green-100 px-2 py-0.5 text-[10px] font-bold text-green-800">OUI</span>
                    } @else {
                      <span class="text-slate-400 text-[10px]">Non</span>
                    }
                  </td>
                  <td class="p-2.5 text-right space-x-1">
                    <button (click)="editerEtape(et)" class="rounded p-1 text-slate-600 hover:bg-slate-100" title="Modifier">
                      <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"/></svg>
                    </button>
                    <button (click)="supprimerEtape(et)" class="rounded p-1 text-rose-600 hover:bg-rose-50" title="Supprimer">
                      <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
                    </button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </div>

      <div class="flex justify-end border-t border-slate-200 px-6 py-3">
        <button (click)="fermerEtapesModal()" class="rounded-lg bg-slate-800 px-4 py-2 text-xs font-semibold text-white hover:bg-slate-900">Fermer</button>
      </div>
    </div>
  </div>
}
`
})
export class WorkflowDefinitionsListComponent implements OnInit {
  private readonly service = inject(WorkflowService);
  private readonly auth = inject(AuthService);
  private readonly notify = inject(NotificationService);

  readonly definitions = signal<WorkflowDefinition[]>([]);
  readonly chargement = signal(false);
  readonly formOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly saving = signal(false);

  // Pagination & Filtres
  readonly page = signal(0);
  readonly pageSize = signal(20);
  readonly totalDefinitions = signal(0);
  readonly totalPages = computed(() => Math.ceil(this.totalDefinitions() / this.pageSize()));
  recherche = '';
  typeEntiteFiltre = '';

  // Modal étapes
  readonly etapesModalOpen = signal(false);
  readonly workflowSelectionne = signal<WorkflowDefinition | null>(null);
  readonly etapes = signal<WorkflowEtape[]>([]);
  readonly editingEtapeId = signal<string | null>(null);
  readonly savingEtape = signal(false);

  draftDef: WorkflowDefinitionRequest = {
    code: '',
    libelle: '',
    description: '',
    typeEntite: 'DEMANDE_ACHAT',
    actif: true
  };

  draftEtape: WorkflowEtape = {
    numEtape: 1,
    codeEtape: '',
    libelle: '',
    roleRequis: '',
    permissionRequise: 'workflow.valider',
    montantMinimum: 0,
    estEtapeFinale: false
  };

  readonly modelesActifs = computed(() => this.definitions().filter(d => d.actif).length);
  readonly entitesCouvertes = computed(() => new Set(this.definitions().map(d => d.typeEntite)).size);

  canCreate(): boolean {
    return this.auth.role() === 'ADMIN_SYSTEME' || this.auth.hasPermission('workflow.creer');
  }

  canEdit(): boolean {
    return this.auth.role() === 'ADMIN_SYSTEME' || this.auth.hasPermission('workflow.modifier');
  }

  canDelete(): boolean {
    return this.auth.role() === 'ADMIN_SYSTEME' || this.auth.hasPermission('workflow.supprimer');
  }

  ngOnInit(): void {
    this.chargerDefinitions();
  }

  chargerDefinitions(): void {
    this.chargement.set(true);
    this.service.listerDefinitions({
      typeEntite: this.typeEntiteFiltre || null,
      recherche: this.recherche.trim() || null,
      page: this.page(),
      size: this.pageSize()
    }).subscribe({
      next: res => {
        this.definitions.set(res.content);
        this.totalDefinitions.set(res.totalElements);
        this.chargement.set(false);
      },
      error: err => {
        this.notify.error('Erreur chargement workflows: ' + errorMessage(err));
        this.chargement.set(false);
      }
    });
  }

  rechercher(): void {
    this.page.set(0);
    this.chargerDefinitions();
  }

  reinitialiserFiltres(): void {
    this.recherche = '';
    this.typeEntiteFiltre = '';
    this.page.set(0);
    this.chargerDefinitions();
  }

  changerPage(p: number): void {
    this.page.set(p);
    this.chargerDefinitions();
  }

  libelleEntite(type: string): string {
    const map: Record<string, string> = {
      DEMANDE_ACHAT: 'Demande d’achat',
      BON_COMMANDE: 'Bon de commande',
      CONTRAT_ABONNEMENT: 'Contrat d’abonnement',
      DEMANDE_STOCK: 'Demande de stock',
      REVISION_BUDGETAIRE: 'Révision budgétaire'
    };
    return map[type] || type;
  }

  ouvrirCreationDef(): void {
    this.editingId.set(null);
    this.draftDef = {
      code: '',
      libelle: '',
      description: '',
      typeEntite: 'DEMANDE_ACHAT',
      actif: true
    };
    this.formOpen.set(true);
  }

  editerDef(w: WorkflowDefinition): void {
    this.editingId.set(w.id);
    this.draftDef = {
      code: w.code,
      libelle: w.libelle,
      description: w.description || '',
      typeEntite: w.typeEntite,
      actif: w.actif
    };
    this.formOpen.set(true);
  }

  supprimerDef(w: WorkflowDefinition): void {
    if (confirm(`Confirmez-vous la suppression du modèle de workflow « ${w.libelle} » ?`)) {
      this.service.supprimerDefinition(w.id).subscribe({
        next: () => {
          this.notify.success('Modèle de workflow supprimé.');
          this.chargerDefinitions();
        },
        error: err => this.notify.error('Erreur suppression: ' + errorMessage(err))
      });
    }
  }

  saveDefinition(): void {
    if (!this.draftDef.code || !this.draftDef.libelle) {
      this.notify.error('Le code et le libellé sont obligatoires.');
      return;
    }
    this.saving.set(true);
    const id = this.editingId();
    const obs = id 
      ? this.service.modifierDefinition(id, this.draftDef)
      : this.service.creerDefinition(this.draftDef);

    obs.subscribe({
      next: () => {
        this.notify.success(id ? 'Modèle mis à jour.' : 'Modèle de workflow créé.');
        this.saving.set(false);
        this.formOpen.set(false);
        this.chargerDefinitions();
      },
      error: err => {
        this.notify.error('Erreur enregistrement: ' + errorMessage(err));
        this.saving.set(false);
      }
    });
  }

  // --- Gestion des étapes ---
  ouvrirEtapes(w: WorkflowDefinition): void {
    this.workflowSelectionne.set(w);
    this.chargerEtapes(w.id);
    this.annulerEditionEtape();
    this.etapesModalOpen.set(true);
  }

  fermerEtapesModal(): void {
    this.etapesModalOpen.set(false);
    this.workflowSelectionne.set(null);
    this.chargerDefinitions();
  }

  chargerEtapes(workflowId: string): void {
    this.service.listerEtapes(workflowId).subscribe({
      next: res => this.etapes.set(res),
      error: err => this.notify.error('Erreur chargement étapes: ' + errorMessage(err))
    });
  }

  annulerEditionEtape(): void {
    this.editingEtapeId.set(null);
    const count = this.etapes().length;
    this.draftEtape = {
      numEtape: count + 1,
      codeEtape: '',
      libelle: '',
      roleRequis: '',
      permissionRequise: 'workflow.valider',
      montantMinimum: 0,
      estEtapeFinale: false
    };
  }

  editerEtape(et: WorkflowEtape): void {
    this.editingEtapeId.set(et.id || null);
    this.draftEtape = { ...et };
  }

  sauvegarderEtape(): void {
    const wf = this.workflowSelectionne();
    if (!wf) return;
    if (!this.draftEtape.codeEtape || !this.draftEtape.libelle) {
      this.notify.error('Le code et le libellé de l’étape sont obligatoires.');
      return;
    }
    this.savingEtape.set(true);
    this.service.enregistrerEtape(wf.id, this.draftEtape).subscribe({
      next: () => {
        this.notify.success('Étape enregistrée.');
        this.savingEtape.set(false);
        this.annulerEditionEtape();
        this.chargerEtapes(wf.id);
      },
      error: err => {
        this.notify.error('Erreur sauvegarde étape: ' + errorMessage(err));
        this.savingEtape.set(false);
      }
    });
  }

  supprimerEtape(et: WorkflowEtape): void {
    const wf = this.workflowSelectionne();
    if (!wf || !et.id) return;
    if (confirm(`Supprimer l'étape « ${et.libelle} » ?`)) {
      this.service.supprimerEtape(wf.id, et.id).subscribe({
        next: () => {
          this.notify.success('Étape supprimée.');
          this.chargerEtapes(wf.id);
        },
        error: err => this.notify.error('Erreur suppression étape: ' + errorMessage(err))
      });
    }
  }

  exporterCsv(): void {
    const rows = [
      ['Code', 'Libelle', 'Entite Cible', 'Actif', 'Etapes'],
      ...this.definitions().map(d => [
        d.code,
        `"${d.libelle.replace(/"/g, '""')}"`,
        d.typeEntite,
        d.actif ? 'OUI' : 'NON',
        String(d.etapes?.length || 0)
      ])
    ];
    const csvContent = 'data:text/csv;charset=utf-8,\uFEFF' + rows.map(e => e.join(';')).join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `modeles_workflows_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }
}
