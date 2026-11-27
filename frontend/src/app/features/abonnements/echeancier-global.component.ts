import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { AbonnementService } from '../../core/services/abonnement.service';
import { AchatService } from '../../core/services/achat.service';
import {
  EcheanceAbonnement,
  RapprochementFactureRequest
} from '../../core/models/abonnement/abonnement.model';
import { PageHeaderComponent } from '../../shared/components/page-header.component';
import { PaginationBarComponent } from '../../shared/components/pagination-bar.component';
import { errorMessage } from '../../core/utils/error-message';

@Component({
  selector: 'app-echeancier-global',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    PageHeaderComponent,
    PaginationBarComponent,
  ],
  template: `
<div class="mx-auto max-w-7xl space-y-6">
  <app-page-header
    icon="echeancier"
    module="Abonnements & Contrats récurrents"
    title="Échéancier & Facturation"
    description="Planning consolidé des échéances récurrentes, émission des bons de commande et rapprochement des factures.">
    <div class="flex items-center gap-2">
      <button (click)="chargerEcheances()" class="flex items-center gap-2 rounded-xl bg-slate-100 px-3.5 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-200">
        <svg aria-hidden="true" class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/></svg>
        Actualiser
      </button>
    </div>
  </app-page-header>

  <!-- Métriques Synthétiques (Design Djibril stats-grid avec liseré supérieur) -->
  <section class="stats-grid grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
    <article class="rounded-2xl border border-slate-200 bg-white p-4">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Total échéances</p>
      <p class="mt-2 text-2xl font-bold font-mono text-slate-900">{{ totalElements() }}</p>
      <p class="mt-1 text-[11px] text-slate-500">Toutes périodes confondues</p>
    </article>

    <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" (click)="statutFiltre = 'A_VENIR'; rechercher()">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Échéances à venir</p>
      <p class="mt-2 text-2xl font-bold font-mono text-blue-600">{{ statsAVenir() }}</p>
      <p class="mt-1 text-[11px] text-slate-500">En attente d'émission de bon</p>
    </article>

    <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" (click)="statutFiltre = 'BC_GENERE'; rechercher()">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Bons de commande émis</p>
      <p class="mt-2 text-2xl font-bold font-mono text-purple-700">{{ statsBcEmis() }}</p>
      <p class="mt-1 text-[11px] text-slate-500">Prêts pour engagement</p>
    </article>

    <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" (click)="statutFiltre = 'FACTURE_RECUE'; rechercher()">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Factures rapprochées</p>
      <p class="mt-2 text-2xl font-bold font-mono text-emerald-700">{{ statsFacturees() }}</p>
      <p class="mt-1 text-[11px] text-slate-500">Prêtes pour paiement</p>
    </article>
  </section>

  <!-- Barre de recherche et filtres -->
  <div class="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 bg-white p-3 shadow-sm">
    <div class="flex flex-1 flex-wrap items-center gap-2">
      <select [(ngModel)]="statutFiltre" (change)="rechercher()" class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-xs outline-none">
        <option value="">Tous les statuts</option>
        <option value="A_VENIR">À venir (non engagé)</option>
        <option value="BC_GENERE">Bon de commande émis</option>
        <option value="FACTURE_RECUE">Facture rapprochée</option>
      </select>
    </div>
    <div class="flex items-center gap-2">
      <button (click)="rechercher()" class="rounded-lg bg-slate-100 px-3 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-200">Filtrer</button>
      <button (click)="reinitialiserFiltres()" class="text-xs text-slate-500 hover:underline">Réinitialiser</button>
    </div>
  </div>

  <!-- Table des échéances globales -->
  <div class="overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-sm">
    <table class="w-full min-w-[950px] text-left text-xs">
      <thead class="border-b border-slate-200 bg-slate-50 font-semibold text-slate-600 uppercase tracking-wider text-[11px] whitespace-nowrap">
        <tr>
          <th class="px-4 py-3.5 whitespace-nowrap">N° Échéance</th>
          <th class="px-4 py-3.5 whitespace-nowrap">Date Prévue</th>
          <th class="px-4 py-3.5 text-right whitespace-nowrap">Montant Attendu TTC</th>
          <th class="px-4 py-3.5 text-center whitespace-nowrap">Statut</th>
          <th class="px-4 py-3.5 whitespace-nowrap">Bon de Commande</th>
          <th class="px-4 py-3.5 whitespace-nowrap">Facture Fournisseur</th>
          <th class="px-4 py-3.5 text-right whitespace-nowrap">Actions</th>
        </tr>
      </thead>
      <tbody class="divide-y divide-slate-100">
        @if (chargement()) {
          <tr><td colspan="7" class="p-8 text-center text-slate-500">Chargement de l'échéancier global…</td></tr>
        } @else if (echeances().length === 0) {
          <tr><td colspan="7" class="p-8 text-center text-slate-400">Aucune échéance trouvée pour les critères sélectionnés.</td></tr>
        } @else {
          @for (ech of echeances(); track ech.id) {
            <tr class="transition hover:bg-slate-50/80">
              <td class="px-4 py-3.5 align-middle font-bold text-slate-900 font-mono whitespace-nowrap">
                Échéance #{{ ech.numEcheance }}
              </td>
              <td class="px-4 py-3.5 align-middle font-medium text-slate-800 whitespace-nowrap">
                {{ ech.dateEcheance | date:'dd/MM/yyyy' }}
              </td>
              <td class="px-4 py-3.5 align-middle text-right font-mono font-bold text-slate-900 whitespace-nowrap">
                {{ ech.montantAttenduTtc | number:'1.0-0' }} <span class="text-[10px] font-sans font-medium text-slate-500">FCFA</span>
              </td>
              <td class="px-4 py-3.5 align-middle text-center whitespace-nowrap">
                <span [class]="badgeStatutEcheance(ech.statut)" class="inline-flex rounded-full px-2.5 py-1 text-[10px] font-semibold">
                  {{ libelleStatutEcheance(ech.statut) }}
                </span>
              </td>
              <td class="px-4 py-3.5 align-middle whitespace-nowrap">
                @if (ech.bcCode) {
                  <div class="flex items-center gap-1.5">
                    <span class="rounded bg-purple-100 px-1.5 py-0.5 font-mono text-[10px] font-bold text-purple-800 border border-purple-200">
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
              <td class="px-4 py-3.5 align-middle whitespace-nowrap">
                @if (ech.referenceFacture) {
                  <div>
                    <span class="font-mono font-bold text-emerald-800 text-[11px]">{{ ech.referenceFacture }}</span>
                    <div class="text-[10px] text-slate-500 font-mono">{{ ech.montantFactureTtc | number:'1.0-0' }} FCFA</div>
                  </div>
                } @else {
                  <span class="text-[10px] text-slate-400">En attente</span>
                }
              </td>
              <td class="px-4 py-3.5 align-middle text-right whitespace-nowrap">
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
    <app-pagination-bar
      [page]="page()"
      [totalPages]="totalPages()"
      [total]="totalElements()"
      (pageChange)="onPageChange($event)" />
  </div>
</div>

<!-- SOUS-MODAL DE RAPPROCHEMENT FACTURE -->
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
export class EcheancierGlobalComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly notification = inject(NotificationService);
  private readonly abonnementService = inject(AbonnementService);
  private readonly achatService = inject(AchatService);

  readonly echeances = signal<EcheanceAbonnement[]>([]);
  readonly chargement = signal(false);
  readonly actionLoading = signal(false);

  // Pagination & filtres
  readonly page = signal(0);
  readonly pageSize = signal(20);
  readonly totalElements = signal(0);
  readonly totalPages = computed(() => Math.ceil(this.totalElements() / this.pageSize()) || 1);
  statutFiltre = '';

  // Statistiques calculées
  readonly statsAVenir = computed(() => this.echeances().filter(e => e.statut === 'A_VENIR').length);
  readonly statsBcEmis = computed(() => this.echeances().filter(e => !!e.bcCode).length);
  readonly statsFacturees = computed(() => this.echeances().filter(e => !!e.referenceFacture).length);

  // Modal rapprochement facture
  readonly rapprochementModalOpen = signal(false);
  readonly selectedEcheance = signal<EcheanceAbonnement | null>(null);
  factureDraft: RapprochementFactureRequest = {
    referenceFacture: '',
    montantFactureTtc: 0,
    dateFacture: new Date().toISOString().substring(0, 10),
    commentaire: ''
  };

  ngOnInit(): void {
    this.chargerEcheances();
  }

  chargerEcheances(): void {
    this.chargement.set(true);
    const params: Record<string, string | number> = {
      page: this.page(),
      size: this.pageSize()
    };
    if (this.statutFiltre) params['statut'] = this.statutFiltre;

    this.abonnementService.listerToutesEcheances(params).subscribe({
      next: (res) => {
        this.echeances.set(res.content || []);
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
    this.chargerEcheances();
  }

  reinitialiserFiltres(): void {
    this.statutFiltre = '';
    this.page.set(0);
    this.chargerEcheances();
  }

  onPageChange(p: number): void {
    this.page.set(p);
    this.chargerEcheances();
  }

  emettreBc(ech: EcheanceAbonnement): void {
    if (!confirm(`Émettre un bon de commande officiel pour l'échéance n°${ech.numEcheance} (${ech.dateEcheance}) d'un montant de ${ech.montantAttenduTtc} FCFA ?`)) {
      return;
    }
    this.actionLoading.set(true);
    this.abonnementService.genererBcPourEcheance(ech.abonnementId, ech.id).subscribe({
      next: (updatedEch) => {
        this.actionLoading.set(false);
        this.notification.success(`Bon de commande ${updatedEch.bcCode || ''} généré avec succès !`);
        this.chargerEcheances();
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
    const ech = this.selectedEcheance();
    if (!ech) return;

    if (!this.factureDraft.referenceFacture.trim()) {
      this.notification.error('Veuillez renseigner le numéro ou la référence de la facture.');
      return;
    }
    if (!this.factureDraft.montantFactureTtc || this.factureDraft.montantFactureTtc <= 0) {
      this.notification.error('Le montant facturé TTC doit être supérieur à zéro.');
      return;
    }

    this.actionLoading.set(true);
    this.abonnementService.rapprocherFacture(ech.abonnementId, ech.id, this.factureDraft).subscribe({
      next: () => {
        this.actionLoading.set(false);
        this.notification.success('Facture rapprochée avec succès sur l’échéance !');
        this.fermerRapprochement();
        this.chargerEcheances();
      },
      error: (err) => {
        this.actionLoading.set(false);
        this.notification.error(errorMessage(err));
      }
    });
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
