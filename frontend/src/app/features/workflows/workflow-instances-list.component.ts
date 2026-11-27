import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { WorkflowService } from '../../core/services/workflow.service';
import {
  WorkflowInstance,
  WorkflowDecisionRequest,
  WorkflowHistorique
} from '../../core/models/workflow/workflow.model';
import { PageHeaderComponent } from '../../shared/components/page-header.component';
import { PaginationBarComponent } from '../../shared/components/pagination-bar.component';
import { errorMessage } from '../../core/utils/error-message';

@Component({
  selector: 'app-workflow-instances-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    PageHeaderComponent,
    PaginationBarComponent,
  ],
  template: `
<div class="space-y-5">
  <app-page-header
    icon="instances"
    module="Moteur de Workflow Transversal"
    title="Suivi des Instances & Circuit d'Approbation"
    description="Supervision transversale des dossiers en cours de visa, escalade hiérarchique et arbitrage.">
    <button type="button" (click)="exporterCsv()"
      class="flex items-center gap-1.5 rounded-xl border border-blue-800 bg-blue-700 px-3.5 py-2 text-xs font-medium text-white hover:bg-blue-800">
      <svg aria-hidden="true" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
      Export CSV
    </button>
  </app-page-header>

  <!-- Onglets de sélection du périmètre -->
  <div class="flex items-center gap-3 border-b border-slate-200 pb-2">
    <button (click)="changerOnglet('TOUT')"
      [class.border-green-700]="ongletActif() === 'TOUT'"
      [class.text-green-800]="ongletActif() === 'TOUT'"
      [class.font-bold]="ongletActif() === 'TOUT'"
      class="flex items-center gap-2 border-b-2 border-transparent px-3 py-2 text-xs text-slate-600 transition hover:text-green-800">
      <span>Tous les circuits d'approbation</span>
      <span class="rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-semibold text-slate-600">{{ totalInstances() }}</span>
    </button>
    <button (click)="changerOnglet('MES_VALIDATIONS')"
      [class.border-amber-600]="ongletActif() === 'MES_VALIDATIONS'"
      [class.text-amber-800]="ongletActif() === 'MES_VALIDATIONS'"
      [class.font-bold]="ongletActif() === 'MES_VALIDATIONS'"
      class="flex items-center gap-2 border-b-2 border-transparent px-3 py-2 text-xs text-slate-600 transition hover:text-amber-800">
      <span>Mes visas en attente</span>
      <span class="rounded-full bg-amber-100 px-2 py-0.5 text-[10px] font-bold text-amber-800" [class.animate-pulse]="mesValidations().length > 0">
        {{ mesValidations().length }}
      </span>
    </button>
  </div>

  @if (ongletActif() === 'MES_VALIDATIONS') {
    <div class="flex items-center justify-between rounded-xl border border-amber-300 bg-amber-50/70 p-4 shadow-xs">
      <div class="flex items-center gap-3">
        <span class="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-200 text-amber-900">
          <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/></svg>
        </span>
        <div>
          <h4 class="text-xs font-bold text-amber-900">Panière de validation hiérarchique & arbitrage</h4>
          <p class="text-[11px] text-amber-800">Ces dossiers sont actuellement positionnés à une étape correspondant à votre rôle ou habilitations dans l'organisation.</p>
        </div>
      </div>
      <button (click)="rafraichirMesValidations()" class="rounded-lg bg-amber-200 px-3 py-1.5 text-xs font-semibold text-amber-900 hover:bg-amber-300">
        Actualiser
      </button>
    </div>
  }

  <!-- Cartes KPI (Design Djibril stats-grid avec liseré supérieur) -->
  <section class="stats-grid grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
    <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" (click)="statutFiltre = 'EN_ATTENTE_VALIDATION'; ongletActif.set('TOUT'); rechercher()">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">En attente de visa</p>
      <p class="mt-2 text-2xl font-bold font-mono" [class.text-amber-500]="statsEnAttente() > 0" [class.text-slate-900]="statsEnAttente() === 0">{{ statsEnAttente() }}</p>
      <p class="mt-1 text-[11px] text-slate-500">Dossiers à instruire</p>
    </article>

    <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" (click)="statutFiltre = 'APPROUVE'; ongletActif.set('TOUT'); rechercher()">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Approuvés définitivement</p>
      <p class="mt-2 text-2xl font-bold font-mono text-emerald-700">{{ statsApprouves() }}</p>
      <p class="mt-1 text-[11px] text-slate-500">Circuits finalisés avec succès</p>
    </article>

    <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" (click)="statutFiltre = 'REJETE'; ongletActif.set('TOUT'); rechercher()">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Rejetés / Bloqués</p>
      <p class="mt-2 text-2xl font-bold font-mono" [class.text-rose-600]="statsRejetes() > 0" [class.text-slate-900]="statsRejetes() === 0">{{ statsRejetes() }}</p>
      <p class="mt-1 text-[11px] text-slate-500">Non conformes ou refusés</p>
    </article>

    <article class="rounded-2xl border border-slate-200 bg-white p-4">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Encours sous circuit</p>
      <p class="mt-2 text-2xl font-bold font-mono text-slate-900">{{ statsVolumeFinancier() | number:'1.0-0' }} <span class="text-xs font-normal text-slate-500">FCFA</span></p>
      <p class="mt-1 text-[11px] text-slate-500">Volume financier total engagé</p>
    </article>
  </section>

  @if (ongletActif() === 'TOUT') {
    <!-- Barre de recherche et filtres pour la vue globale -->
    <div class="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 bg-white p-3 shadow-sm">
      <div class="flex flex-1 flex-wrap items-center gap-2">
        <div class="relative min-w-[220px] flex-1">
          <input [(ngModel)]="recherche" (keyup.enter)="rechercher()" placeholder="Rechercher par référence, objet du dossier…"
            class="w-full rounded-lg border border-slate-200 bg-slate-50 py-2 pl-8 pr-3 text-xs outline-none focus:border-green-600 focus:bg-white">
          <svg aria-hidden="true" class="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
        </div>
        <select [(ngModel)]="statutFiltre" (change)="rechercher()" class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-xs outline-none">
          <option value="">Tous les statuts</option>
          <option value="EN_ATTENTE_VALIDATION">En attente de validation</option>
          <option value="APPROUVE">Approuvé</option>
          <option value="REJETE">Rejeté</option>
        </select>
        <select [(ngModel)]="typeEntiteFiltre" (change)="rechercher()" class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-xs outline-none">
          <option value="">Toutes les entités</option>
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
  }

  <!-- Table des instances de workflow -->
  <div class="overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-sm">
    <table class="w-full min-w-[880px] text-left text-xs">
      <thead class="border-b border-slate-200 bg-slate-50 font-semibold text-slate-600 uppercase tracking-wider text-[11px] whitespace-nowrap">
        <tr>
          <th class="p-3 whitespace-nowrap">Référence Dossier</th>
          <th class="p-3 whitespace-nowrap">Entité & Circuit</th>
          <th class="p-3 whitespace-nowrap">Demandeur</th>
          <th class="p-3 text-right whitespace-nowrap">Montant Total</th>
          <th class="p-3 whitespace-nowrap">Étape Active</th>
          <th class="p-3 text-center whitespace-nowrap">Statut</th>
          <th class="p-3 text-right whitespace-nowrap">Actions</th>
        </tr>
      </thead>
      <tbody class="divide-y divide-slate-100">
        @if (chargement()) {
          <tr><td colspan="7" class="p-8 text-center text-slate-500">Chargement des circuits d'approbation…</td></tr>
        } @else if (listeAffichee().length === 0) {
          <tr><td colspan="7" class="p-8 text-center text-slate-400">
            {{ ongletActif() === 'MES_VALIDATIONS' ? 'Aucun dossier en attente de votre visa actuellement.' : 'Aucun dossier en cours de circuit d’approbation.' }}
          </td></tr>
        } @else {
          @for (inst of listeAffichee(); track inst.id) {
            <tr class="transition hover:bg-slate-50/80">
              <td class="p-3 whitespace-nowrap">
                <div class="font-mono font-bold text-slate-900">{{ inst.referenceDossier }}</div>
                <div class="text-[11px] text-slate-500 line-clamp-1">{{ inst.objet }}</div>
              </td>
              <td class="p-3 whitespace-nowrap">
                <span class="inline-flex rounded-full px-2 py-0.5 text-[10px] font-semibold bg-slate-100 text-slate-700 border border-slate-200">
                  {{ libelleEntite(inst.typeEntite) }}
                </span>
                <div class="text-[10px] text-slate-400 mt-0.5">{{ inst.workflowLibelle }}</div>
              </td>
              <td class="p-3 whitespace-nowrap">
                @if (inst.demandeur) {
                  <span class="font-medium text-slate-800">{{ inst.demandeur.prenom }} {{ inst.demandeur.nom }}</span>
                } @else {
                  <span class="text-slate-400">—</span>
                }
              </td>
              <td class="p-3 text-right font-bold text-slate-900 whitespace-nowrap font-mono">
                {{ inst.montantTotal | number:'1.0-0' }} <span class="text-[10px] font-normal text-slate-500 font-sans">FCFA</span>
              </td>
              <td class="p-3 whitespace-nowrap">
                @if (inst.etapeCourante) {
                  <div class="inline-flex items-center gap-1.5 rounded-lg bg-amber-50 px-2 py-1 text-xs font-semibold text-amber-800 border border-amber-200">
                    <span class="flex h-4 w-4 items-center justify-center rounded-full bg-amber-200 text-[9px] font-bold">{{ inst.etapeCourante.numEtape }}</span>
                    <span>{{ inst.etapeCourante.libelle }}</span>
                  </div>
                  @if (inst.etapeCourante.roleRequis) {
                    <div class="text-[10px] text-amber-600 mt-0.5">Rôle : {{ inst.etapeCourante.roleRequis }}</div>
                  }
                } @else {
                  <span class="text-slate-400 text-xs italic whitespace-nowrap">Circuit terminé</span>
                }
              </td>
              <td class="p-3 text-center whitespace-nowrap">
                @if (inst.statutActuel === 'EN_ATTENTE_VALIDATION') {
                  <span class="inline-flex rounded-full bg-amber-100 px-2.5 py-1 text-[11px] font-bold text-amber-800 animate-pulse">En attente</span>
                } @else if (inst.statutActuel === 'APPROUVE') {
                  <span class="inline-flex rounded-full bg-green-100 px-2.5 py-1 text-[11px] font-bold text-green-800">Approuvé</span>
                } @else if (inst.statutActuel === 'REJETE') {
                  <span class="inline-flex rounded-full bg-rose-100 px-2.5 py-1 text-[11px] font-bold text-rose-800">Rejeté</span>
                } @else {
                  <span class="inline-flex rounded-full bg-slate-100 px-2.5 py-1 text-[11px] font-bold text-slate-700">{{ inst.statutActuel }}</span>
                }
              </td>
              <td class="p-3 text-right space-x-1 whitespace-nowrap">
                <button (click)="consulterHistorique(inst)" title="Historique d'audit des visas"
                  class="rounded-lg bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-700 hover:bg-slate-200">
                  Historique
                </button>
                @if (inst.statutActuel === 'EN_ATTENTE_VALIDATION' && canValidate()) {
                  <button (click)="ouvrirDecisionModal(inst)" title="Prendre une décision d'approbation ou de rejet"
                    class="rounded-lg bg-purple-700 px-2.5 py-1 text-xs font-semibold text-white hover:bg-purple-800 shadow-xs">
                    Décider
                  </button>
                }
              </td>
            </tr>
          }
        }
      </tbody>
    </table>
  </div>

  @if (ongletActif() === 'TOUT') {
    <app-pagination-bar
      [total]="totalInstances()"
      [totalPages]="totalPages()"
      [page]="page()"
      (pageChange)="changerPage($event)" />
  }
</div>

<!-- MODAL DÉCISION WORKFLOW (APPROBATION / REJET) -->
@if (decisionModalOpen()) {
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
    <div class="w-full max-w-lg rounded-2xl bg-white shadow-2xl overflow-hidden">
      <div class="flex items-center justify-between border-b border-slate-200 px-6 py-4 bg-slate-50">
        <div>
          <h3 class="text-base font-bold text-slate-900">Arbitrage Circuit d'Approbation</h3>
          <p class="text-xs text-slate-500">Dossier : <strong class="text-slate-800">{{ instanceSelectionnee()?.referenceDossier }}</strong></p>
        </div>
        <button (click)="decisionModalOpen.set(false)" class="rounded-lg p-1.5 text-slate-400 hover:bg-slate-200">✕</button>
      </div>

      <div class="p-6 space-y-4 text-xs">
        <div class="rounded-xl border border-slate-200 bg-slate-50 p-3 space-y-1">
          <p class="text-slate-600"><strong>Objet :</strong> {{ instanceSelectionnee()?.objet }}</p>
          <p class="text-slate-600"><strong>Montant total engagé :</strong> <span class="font-bold text-slate-900">{{ instanceSelectionnee()?.montantTotal | number:'1.0-0' }} FCFA</span></p>
          <p class="text-slate-600"><strong>Étape active :</strong> <span class="font-bold text-amber-700">{{ instanceSelectionnee()?.etapeCourante?.libelle }}</span></p>
        </div>

        <div class="space-y-2">
          <label class="block font-semibold text-slate-800">Votre décision <span class="champ-obligatoire">*</span></label>
          <div class="grid grid-cols-2 gap-3">
            <button type="button" (click)="choixDecision = 'VALIDER'"
              [class.ring-2]="choixDecision === 'VALIDER'"
              [class.ring-green-600]="choixDecision === 'VALIDER'"
              [class.bg-green-50]="choixDecision === 'VALIDER'"
              class="flex flex-col items-center justify-center rounded-xl border border-green-200 p-3 text-center transition hover:bg-green-50">
              <svg class="h-6 w-6 text-green-700" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-width="2" d="M5 13l4 4L19 7"/></svg>
              <span class="mt-1 font-bold text-green-800">Valider / Approuver</span>
              <span class="text-[10px] text-green-600">Passer à l'étape suivante</span>
            </button>
            <button type="button" (click)="choixDecision = 'REJETER'"
              [class.ring-2]="choixDecision === 'REJETER'"
              [class.ring-rose-600]="choixDecision === 'REJETER'"
              [class.bg-rose-50]="choixDecision === 'REJETER'"
              class="flex flex-col items-center justify-center rounded-xl border border-rose-200 p-3 text-center transition hover:bg-rose-50">
              <svg class="h-6 w-6 text-rose-700" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
              <span class="mt-1 font-bold text-rose-800">Rejeter le dossier</span>
              <span class="text-[10px] text-rose-600">Motif obligatoire</span>
            </button>
          </div>
        </div>

        <label class="block space-y-1">
          <span class="font-semibold text-slate-800">Commentaire / Justification de l'arbitrage</span>
          <textarea rows="3" [(ngModel)]="commentaireDecision" placeholder="Précisez les observations éventuelles ou le motif de rejet…"
            class="w-full rounded-lg border border-slate-300 p-2 text-xs outline-none focus:border-green-600"></textarea>
        </label>
      </div>

      <div class="flex justify-end gap-2 border-t border-slate-200 bg-slate-50 px-6 py-3">
        <button (click)="decisionModalOpen.set(false)" class="rounded-lg border border-slate-200 px-4 py-2 text-xs font-medium text-slate-700 hover:bg-white">Annuler</button>
        <button (click)="soumettreDecision()" [disabled]="soumettantDecision()"
          class="rounded-lg bg-slate-900 px-4 py-2 text-xs font-semibold text-white hover:bg-black disabled:opacity-50">
          {{ soumettantDecision() ? 'Enregistrement…' : 'Confirmer la décision' }}
        </button>
      </div>
    </div>
  </div>
}

<!-- MODAL HISTORIQUE D'AUDIT -->
@if (historiqueModalOpen()) {
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
    <div class="flex max-h-[85vh] w-full max-w-2xl flex-col rounded-2xl bg-white shadow-2xl overflow-hidden">
      <div class="flex items-center justify-between border-b border-slate-200 px-6 py-4 bg-slate-50">
        <div>
          <h3 class="text-base font-bold text-slate-900">Piste d'Audit & Historique des Visas</h3>
          <p class="text-xs text-slate-500">Dossier : <span class="font-mono font-bold text-slate-800">{{ instanceSelectionnee()?.referenceDossier }}</span></p>
        </div>
        <button (click)="historiqueModalOpen.set(false)" class="rounded-lg p-1.5 text-slate-400 hover:bg-slate-200">✕</button>
      </div>

      <div class="flex-1 overflow-y-auto p-6">
        @if (chargementHistorique()) {
          <div class="py-8 text-center text-xs text-slate-500">Chargement de l'audit…</div>
        } @else if (historique().length === 0) {
          <div class="py-8 text-center text-xs text-slate-400">Aucune décision enregistrée pour l'instant dans ce circuit.</div>
        } @else {
          <div class="relative pl-6 space-y-6 before:absolute before:bottom-0 before:left-2.5 before:top-2 before:w-0.5 before:bg-slate-200">
            @for (h of historique(); track h.id) {
              <div class="relative">
                <span class="absolute -left-6 top-1 flex h-5 w-5 items-center justify-center rounded-full"
                  [class.bg-green-100]="h.decision === 'APPROUVE' || h.decision === 'VALIDER'"
                  [class.bg-rose-100]="h.decision === 'REJETE' || h.decision === 'REJET'">
                  @if (h.decision === 'APPROUVE' || h.decision === 'VALIDER') {
                    <svg class="h-3 w-3 text-green-700" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-width="2" d="M5 13l4 4L19 7"/></svg>
                  } @else {
                    <svg class="h-3 w-3 text-rose-700" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
                  }
                </span>
                <div class="rounded-xl border border-slate-200 bg-slate-50 p-3 text-xs space-y-1">
                  <div class="flex items-center justify-between">
                    <span class="font-bold text-slate-900">{{ h.valideur?.prenom }} {{ h.valideur?.nom }}</span>
                    <span class="text-[10px] text-slate-400">{{ h.dateDecision | date:'dd/MM/yyyy HH:mm' }}</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <span class="font-mono text-[10px] font-semibold text-slate-600 bg-white px-1.5 py-0.5 rounded border border-slate-200">{{ h.etape }}</span>
                    <span class="font-bold"
                      [class.text-green-700]="h.decision === 'APPROUVE' || h.decision === 'VALIDER'"
                      [class.text-rose-700]="h.decision === 'REJETE' || h.decision === 'REJET'">
                      {{ h.decision }}
                    </span>
                  </div>
                  @if (h.commentaire) {
                    <p class="text-slate-600 italic bg-white p-2 rounded border border-slate-100 mt-1">« {{ h.commentaire }} »</p>
                  }
                </div>
              </div>
            }
          </div>
        }
      </div>

      <div class="flex justify-end border-t border-slate-200 bg-slate-50 px-6 py-3">
        <button (click)="historiqueModalOpen.set(false)" class="rounded-lg bg-slate-800 px-4 py-2 text-xs font-semibold text-white hover:bg-slate-900">Fermer</button>
      </div>
    </div>
  </div>
}
`
})
export class WorkflowInstancesListComponent implements OnInit {
  private readonly service = inject(WorkflowService);
  private readonly auth = inject(AuthService);
  private readonly notify = inject(NotificationService);

  readonly instances = signal<WorkflowInstance[]>([]);
  readonly mesValidations = signal<WorkflowInstance[]>([]);
  readonly chargement = signal(false);

  // Onglet sélectionné : 'TOUT' ou 'MES_VALIDATIONS'
  readonly ongletActif = signal<'TOUT' | 'MES_VALIDATIONS'>('TOUT');

  // Pagination & Filtres
  readonly page = signal(0);
  readonly pageSize = signal(20);
  readonly totalInstances = signal(0);
  readonly totalPages = computed(() => Math.ceil(this.totalInstances() / this.pageSize()));
  recherche = '';
  statutFiltre = '';
  typeEntiteFiltre = '';

  // Decision Modal
  readonly decisionModalOpen = signal(false);
  readonly instanceSelectionnee = signal<WorkflowInstance | null>(null);
  choixDecision: 'VALIDER' | 'REJETER' = 'VALIDER';
  commentaireDecision = '';
  readonly soumettantDecision = signal(false);

  // Historique Modal
  readonly historiqueModalOpen = signal(false);
  readonly historique = signal<WorkflowHistorique[]>([]);
  readonly chargementHistorique = signal(false);

  readonly listeAffichee = computed(() => {
    return this.ongletActif() === 'MES_VALIDATIONS' ? this.mesValidations() : this.instances();
  });

  readonly statsEnAttente = computed(() => this.instances().filter(i => i.statutActuel === 'EN_ATTENTE_VALIDATION').length);
  readonly statsApprouves = computed(() => this.instances().filter(i => i.statutActuel === 'APPROUVE').length);
  readonly statsRejetes = computed(() => this.instances().filter(i => i.statutActuel === 'REJETE').length);
  readonly statsVolumeFinancier = computed(() => this.instances().reduce((acc, i) => acc + (Number(i.montantTotal) || 0), 0));

  canValidate(): boolean {
    return this.auth.role() === 'ADMIN_SYSTEME' || this.auth.hasPermission('workflow.valider');
  }

  ngOnInit(): void {
    this.chargerInstances();
    this.rafraichirMesValidations();
  }

  changerOnglet(onglet: 'TOUT' | 'MES_VALIDATIONS'): void {
    this.ongletActif.set(onglet);
    if (onglet === 'MES_VALIDATIONS') {
      this.rafraichirMesValidations();
    } else {
      this.chargerInstances();
    }
  }

  rafraichirMesValidations(): void {
    this.service.mesValidations().subscribe({
      next: res => this.mesValidations.set(res),
      error: () => {}
    });
  }

  chargerInstances(): void {
    this.chargement.set(true);
    this.service.listerInstances({
      statut: this.statutFiltre || null,
      typeEntite: this.typeEntiteFiltre || null,
      recherche: this.recherche.trim() || null,
      page: this.page(),
      size: this.pageSize()
    }).subscribe({
      next: res => {
        this.instances.set(res.content);
        this.totalInstances.set(res.totalElements);
        this.chargement.set(false);
      },
      error: err => {
        this.notify.error('Erreur chargement instances: ' + errorMessage(err));
        this.chargement.set(false);
      }
    });
  }

  rechercher(): void {
    this.page.set(0);
    this.chargerInstances();
  }

  reinitialiserFiltres(): void {
    this.recherche = '';
    this.statutFiltre = '';
    this.typeEntiteFiltre = '';
    this.page.set(0);
    this.chargerInstances();
  }

  changerPage(p: number): void {
    this.page.set(p);
    this.chargerInstances();
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

  // --- Décision ---
  ouvrirDecisionModal(inst: WorkflowInstance): void {
    this.instanceSelectionnee.set(inst);
    this.choixDecision = 'VALIDER';
    this.commentaireDecision = '';
    this.decisionModalOpen.set(true);
  }

  soumettreDecision(): void {
    const inst = this.instanceSelectionnee();
    if (!inst) return;
    if (this.choixDecision === 'REJETER' && !this.commentaireDecision.trim()) {
      this.notify.error('Un commentaire / motif est obligatoire en cas de rejet.');
      return;
    }
    this.soumettantDecision.set(true);
    const dto: WorkflowDecisionRequest = {
      decision: this.choixDecision,
      commentaire: this.commentaireDecision.trim() || undefined
    };
    this.service.traiterDecision(inst.id, dto).subscribe({
      next: updated => {
        this.notify.success(this.choixDecision === 'VALIDER' ? 'Dossier approuvé avec succès.' : 'Dossier rejeté.');
        this.soumettantDecision.set(false);
        this.decisionModalOpen.set(false);
        this.chargerInstances();
        this.rafraichirMesValidations();
      },
      error: err => {
        this.notify.error('Erreur lors de l’arbitrage: ' + errorMessage(err));
        this.soumettantDecision.set(false);
      }
    });
  }

  // --- Historique ---
  consulterHistorique(inst: WorkflowInstance): void {
    this.instanceSelectionnee.set(inst);
    this.chargementHistorique.set(true);
    this.historiqueModalOpen.set(true);
    this.service.obtenirHistorique(inst.id).subscribe({
      next: res => {
        this.historique.set(res);
        this.chargementHistorique.set(false);
      },
      error: err => {
        this.notify.error('Erreur chargement historique: ' + errorMessage(err));
        this.chargementHistorique.set(false);
      }
    });
  }

  imprimer(): void {
    window.print();
  }

  exporterCsv(): void {
    const rows = [
      ['Reference', 'Objet', 'Entite', 'Demandeur', 'Montant', 'Etape Active', 'Statut'],
      ...this.listeAffichee().map(i => [
        i.referenceDossier,
        `"${i.objet.replace(/"/g, '""')}"`,
        i.typeEntite,
        i.demandeur ? `"${i.demandeur.prenom} ${i.demandeur.nom}"` : '',
        String(i.montantTotal),
        i.etapeCourante ? `"${i.etapeCourante.libelle}"` : 'Terminé',
        i.statutActuel
      ])
    ];
    const csvContent = 'data:text/csv;charset=utf-8,\uFEFF' + rows.map(e => e.join(';')).join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `suivi_instances_workflows_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }
}
