import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { AchatService } from '../../../core/services/achat.service';
import { BonCommande, StatutBonCommande } from '../../../core/models/achat/bon-commande.model';
import { Fournisseur } from '../../../core/models/achat/fournisseur.model';
import { PageHeaderComponent } from '../../../shared/components/page-header.component';
import { PaginationBarComponent } from '../../../shared/components/pagination-bar.component';
import { errorMessage } from '../../../core/utils/error-message';

const STATUTS_BC: Record<StatutBonCommande, { label: string; classe: string }> = {
  EMIS: { label: 'Émis', classe: 'bg-purple-50 text-purple-700 border border-purple-200' },
  EN_COURS: { label: 'En cours', classe: 'bg-blue-50 text-blue-700 border border-blue-200' },
  SOLDE: { label: 'Soldé', classe: 'bg-emerald-50 text-emerald-700 border border-emerald-200' },
  ANNULE: { label: 'Annulé', classe: 'bg-rose-50 text-rose-700 border border-rose-200' },
};

@Component({
  selector: 'app-bons-commande-list',
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
    icon="bons-commande"
    module="Gestion des achats"
    title="Bons de Commande (BC)"
    description="Engagements financiers et contractuels auprès des fournisseurs, suivi des montants HT/TTC et édition des bons officiels.">
  </app-page-header>

  @if (error()) {
    <div role="alert" class="rounded-xl border border-rose-300 bg-rose-50 p-3 text-sm text-rose-700">{{ error() }}</div>
  }

  <!-- Cartes KPI (Design Djibril stats-grid avec liseré supérieur) -->
  <section class="stats-grid grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
    <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" (click)="statutFilter = ''; search()">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Bons de Commande</p>
      <p class="mt-2 text-2xl font-bold font-mono text-slate-900">{{ total() }}</p>
      <p class="mt-1 text-[11px] text-slate-500">Total bons émis & actifs</p>
    </article>

    <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" (click)="statutFilter = 'EMIS'; search()">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">En cours / Émis</p>
      <p class="mt-2 text-2xl font-bold font-mono text-purple-700">{{ statsEmis() }}</p>
      <p class="mt-1 text-[11px] text-slate-500">En cours d'exécution tiers</p>
    </article>

    <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" (click)="statutFilter = 'SOLDE'; search()">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Livrés / Soldés</p>
      <p class="mt-2 text-2xl font-bold font-mono text-emerald-700">{{ statsSoldes() }}</p>
      <p class="mt-1 text-[11px] text-slate-500">Réceptionnés et clôturés</p>
    </article>

    <article class="rounded-2xl border border-slate-200 bg-white p-4">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Montant total engagé</p>
      <p class="mt-2 text-2xl font-bold font-mono text-purple-900">{{ statsMontantTtc() | number:'1.0-0' }} <span class="text-xs font-normal text-slate-500">FCFA</span></p>
      <p class="mt-1 text-[11px] text-slate-500">Volume financier total TTC</p>
    </article>
  </section>

  <div class="flex flex-wrap items-center gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
    <input
      type="text"
      [(ngModel)]="recherche"
      (keyup.enter)="search()"
      placeholder="Recherche par code BC, DA, fournisseur…"
      class="min-w-[240px] flex-1 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs" />

    <select [(ngModel)]="statutFilter" (change)="search()" class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs">
      <option value="">Tous les statuts</option>
      @for (s of statutKeys; track s) {
        <option [value]="s">{{ statuts[s].label }}</option>
      }
    </select>

    <select [(ngModel)]="fournisseurFilter" (change)="search()" class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs max-w-xs">
      <option value="">Tous les fournisseurs</option>
      @for (f of fournisseurs(); track f.id) {
        <option [value]="f.id">{{ f.raisonSociale }}</option>
      }
    </select>

    <button type="button" (click)="search()" class="rounded-xl bg-blue-700 px-3 py-2 text-xs font-medium text-white hover:bg-blue-800">
      Rechercher
    </button>
    <button type="button" (click)="reset()" class="rounded-xl bg-slate-100 px-3 py-2 text-xs text-slate-600 hover:bg-slate-200">
      Réinitialiser
    </button>
  </div>

  <section class="overflow-hidden rounded-2xl border border-slate-200 bg-white">
    <div class="overflow-x-auto">
      <table class="w-full min-w-[900px] text-left text-xs">
        <thead class="bg-slate-50 text-slate-600 whitespace-nowrap">
          <tr>
            <th class="p-3 whitespace-nowrap">N° Bon Commande</th>
            <th class="p-3 whitespace-nowrap">Dossier DA</th>
            <th class="p-3 whitespace-nowrap">Fournisseur</th>
            <th class="p-3 whitespace-nowrap">Type</th>
            <th class="p-3 text-right whitespace-nowrap">Montant HT</th>
            <th class="p-3 text-right whitespace-nowrap">Montant TTC</th>
            <th class="p-3 whitespace-nowrap">Statut</th>
            <th class="p-3 whitespace-nowrap">Date émission</th>
            <th class="p-3 text-right whitespace-nowrap">Actions</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-200">
          @for (bc of bonsCommande(); track bc.id) {
            <tr class="hover:bg-slate-50/60 transition">
              <td class="p-3 font-mono font-bold text-purple-700 cursor-pointer hover:underline" (click)="detailRecord = bc">
                {{ bc.codeBc }}
              </td>
              <td class="p-3 font-mono text-slate-600">{{ bc.daCodeDossier || '—' }}</td>
              <td class="p-3">
                <span class="font-semibold text-slate-900 block">{{ bc.fournisseur.raisonSociale }}</span>
                <span class="text-[11px] text-slate-400 font-mono">IFU: {{ bc.fournisseur.ifu }}</span>
              </td>
              <td class="p-3 text-[11px] font-medium text-slate-600">{{ bc.typeBc }}</td>
              <td class="p-3 text-right font-mono font-medium text-slate-700 whitespace-nowrap">{{ (bc.montantTotalHt | number:'1.0-0') }} FCFA</td>
              <td class="p-3 text-right font-mono font-bold text-purple-900 whitespace-nowrap">{{ (bc.montantTotalTtc | number:'1.0-0') }} FCFA</td>
              <td class="p-3">
                <span class="rounded-full px-2 py-0.5 text-[11px] font-semibold {{ statuts[bc.statut]?.classe }}">
                  {{ statuts[bc.statut]?.label || bc.statut }}
                </span>
              </td>
              <td class="p-3 text-slate-500 whitespace-nowrap">{{ bc.dateEmission | date:'dd/MM/yyyy' }}</td>
              <td class="p-3 text-right space-x-1.5 whitespace-nowrap">
                <button
                  type="button"
                  (click)="detailRecord = bc"
                  class="rounded-lg border border-slate-300 bg-white px-2.5 py-1 text-[11px] font-medium text-slate-700 hover:bg-slate-50">
                  Détails
                </button>
                <button
                  type="button"
                  (click)="telechargerPdf(bc)"
                  [disabled]="downloadingId() === bc.id"
                  class="rounded-lg bg-blue-700 px-2.5 py-1 text-[11px] font-semibold text-white shadow-xs hover:bg-blue-800 disabled:opacity-50">
                  {{ downloadingId() === bc.id ? 'Génération…' : 'PDF' }}
                </button>
              </td>
            </tr>
          } @empty {
            <tr>
              <td colspan="9" class="p-6 text-center text-slate-500">
                {{ loading() ? 'Chargement des bons de commande…' : 'Aucun bon de commande trouvé.' }}
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
    <app-pagination-bar [page]="page()" [totalPages]="totalPages()" [total]="total()" (pageChange)="goTo($event)" />
  </section>
</div>

<!-- Modal Détail du Bon de Commande -->
@if (detailRecord; as bc) {
  <div class="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-slate-900/60 p-4 backdrop-blur-sm" (click)="detailRecord = null">
    <div class="relative w-full max-w-4xl max-h-[92vh] flex flex-col rounded-2xl bg-white shadow-2xl overflow-hidden" (click)="$event.stopPropagation()">
      <div class="flex items-center justify-between border-b border-slate-200 px-6 py-4 bg-purple-50">
        <div>
          <div class="flex items-center gap-2">
            <h2 class="text-base font-bold text-purple-950 font-mono">{{ bc.codeBc }}</h2>
            <span class="rounded-full px-2.5 py-0.5 text-[11px] font-semibold {{ statuts[bc.statut]?.classe }}">
              {{ statuts[bc.statut]?.label || bc.statut }}
            </span>
          </div>
          <p class="text-xs text-purple-700 mt-0.5">Dossier source DA : <strong class="font-mono">{{ bc.daCodeDossier }}</strong> — {{ bc.daObjet }}</p>
        </div>
        <button type="button" (click)="detailRecord = null" class="rounded-lg p-1.5 text-slate-400 hover:bg-slate-200">✕</button>
      </div>

      <div class="overflow-y-auto p-6 space-y-5">
        <!-- Blocs Fournisseur & Engagement -->
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div class="rounded-xl border border-slate-200 bg-slate-50/50 p-4 text-xs space-y-1.5">
            <h4 class="font-bold text-slate-900 text-sm border-b border-slate-200 pb-1">Fournisseur attributaire</h4>
            <div><span class="text-slate-500">Raison sociale :</span> <strong class="text-slate-900">{{ bc.fournisseur.raisonSociale }}</strong></div>
            <div><span class="text-slate-500">Code Fournisseur :</span> <strong class="font-mono text-slate-900">{{ bc.fournisseur.codeFournisseur }}</strong></div>
            <div><span class="text-slate-500">N° IFU :</span> <strong class="font-mono text-slate-900">{{ bc.fournisseur.ifu }}</strong></div>
            <div><span class="text-slate-500">Téléphone :</span> <span class="text-slate-700">{{ bc.fournisseur.telephone || '—' }}</span></div>
            <div><span class="text-slate-500">E-mail :</span> <span class="text-slate-700">{{ bc.fournisseur.email || '—' }}</span></div>
            <div><span class="text-slate-500">Adresse :</span> <span class="text-slate-700">{{ bc.fournisseur.adresse || '—' }}</span></div>
          </div>

          <div class="rounded-xl border border-slate-200 bg-slate-50/50 p-4 text-xs space-y-1.5">
            <h4 class="font-bold text-slate-900 text-sm border-b border-slate-200 pb-1">Détails d'émission</h4>
            <div><span class="text-slate-500">Date d'émission :</span> <strong class="text-slate-900">{{ bc.dateEmission | date:'dd/MM/yyyy' }}</strong></div>
            <div><span class="text-slate-500">Type de bon :</span> <strong class="text-slate-900">{{ bc.typeBc }}</strong></div>
            <div><span class="text-slate-500">Statut actuel :</span> <strong class="text-slate-900">{{ bc.statut }}</strong></div>
            <div class="pt-2 border-t border-slate-200">
              <span class="text-slate-500">Montant facturé :</span> <strong class="font-mono text-slate-900">{{ (bc.montantFactureTtc | number:'1.0-0') }} FCFA</strong>
            </div>
            <div>
              <span class="text-slate-500">Solde restant TTC :</span> <strong class="font-mono text-purple-900">{{ (bc.soldeRestantTtc | number:'1.0-0') }} FCFA</strong>
            </div>
          </div>
        </div>

        <!-- Lignes du Bon de commande -->
        <div>
          <h4 class="text-xs font-bold text-slate-900 mb-2">Prestations & Marchandises commandées</h4>
          <div class="overflow-x-auto rounded-xl border border-slate-200">
            <table class="w-full text-left text-xs">
              <thead class="bg-slate-50 text-slate-600">
                <tr>
                  <th class="p-2.5">N°</th>
                  <th class="p-2.5">Désignation</th>
                  <th class="p-2.5 text-right">Quantité</th>
                  <th class="p-2.5 text-right">P.U. HT</th>
                  <th class="p-2.5 text-right">Total HT</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-200">
                @for (l of bc.lignes; track l.id; let idx = $index) {
                  <tr>
                    <td class="p-2.5 font-mono text-slate-400">{{ idx + 1 }}</td>
                    <td class="p-2.5 font-medium text-slate-900">{{ l.description }}</td>
                    <td class="p-2.5 text-right font-mono">{{ l.quantiteCommandee }}</td>
                    <td class="p-2.5 text-right font-mono">{{ (l.prixUnitaireHt | number:'1.0-0') }} FCFA</td>
                    <td class="p-2.5 text-right font-mono font-bold text-slate-900">{{ (l.montantTotalHt | number:'1.0-0') }} FCFA</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <!-- Récapitulatif financier -->
          <div class="mt-4 flex justify-end">
            <div class="w-72 rounded-xl border border-slate-200 bg-slate-50 p-3.5 space-y-1.5 text-xs">
              <div class="flex justify-between text-slate-600">
                <span>Total Général HT :</span>
                <strong class="font-mono text-slate-900">{{ (bc.montantTotalHt | number:'1.0-0') }} FCFA</strong>
              </div>
              <div class="flex justify-between text-slate-600">
                <span>TVA (18%) :</span>
                <strong class="font-mono text-slate-900">{{ ((bc.montantTotalTtc - bc.montantTotalHt) | number:'1.0-0') }} FCFA</strong>
              </div>
              <div class="flex justify-between border-t border-slate-200 pt-1.5 font-bold text-purple-950 text-sm">
                <span>TOTAL GÉNÉRAL TTC :</span>
                <span class="font-mono">{{ (bc.montantTotalTtc | number:'1.0-0') }} FCFA</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="flex items-center justify-between border-t border-slate-200 bg-slate-50 px-6 py-3">
        <button
          type="button"
          (click)="telechargerPdf(bc)"
          [disabled]="downloadingId() === bc.id"
          class="rounded-xl bg-blue-700 px-4 py-2 text-xs font-semibold text-white shadow-sm hover:bg-blue-800 disabled:opacity-50">
          {{ downloadingId() === bc.id ? 'Génération du PDF…' : '📄 Télécharger le Bon de Commande officiel (PDF)' }}
        </button>
        <button type="button" (click)="detailRecord = null" class="rounded-xl bg-slate-800 px-4 py-2 text-xs font-semibold text-white hover:bg-slate-700">Fermer</button>
      </div>
    </div>
  </div>
}
`,
})
export class BonsCommandeListComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly achatService = inject(AchatService);
  private readonly notifications = inject(NotificationService);

  readonly statuts = STATUTS_BC;
  readonly statutKeys = Object.keys(STATUTS_BC) as StatutBonCommande[];

  readonly bonsCommande = signal<BonCommande[]>([]);
  readonly fournisseurs = signal<Fournisseur[]>([]);

  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly total = signal(0);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly downloadingId = signal<string | null>(null);

  readonly statsEmis = computed(() => this.bonsCommande().filter(b => b.statut === 'EMIS' || b.statut === 'EN_COURS').length);
  readonly statsSoldes = computed(() => this.bonsCommande().filter(b => b.statut === 'SOLDE').length);
  readonly statsMontantTtc = computed(() => this.bonsCommande().reduce((acc, b) => acc + (b.montantTotalTtc || 0), 0));

  recherche = '';
  statutFilter = '';
  fournisseurFilter = '';
  detailRecord: BonCommande | null = null;

  ngOnInit(): void {
    this.load();
    this.loadFournisseurs();
  }

  loadFournisseurs(): void {
    this.achatService.listerFournisseurs({ page: 0, size: 200 }).subscribe({
      next: (res) => this.fournisseurs.set(res.content),
      error: () => {},
    });
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.achatService.listerBonsCommande({
      page: this.page(),
      size: 20,
      recherche: this.recherche.trim(),
      statut: this.statutFilter || undefined,
      fournisseurId: this.fournisseurFilter || undefined,
      sort: 'createdAt,desc',
    }).subscribe({
      next: (res) => {
        this.bonsCommande.set(res.content);
        this.totalPages.set(res.totalPages);
        this.total.set(res.totalElements);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }

  search(): void { this.page.set(0); this.load(); }
  reset(): void { this.recherche = ''; this.statutFilter = ''; this.fournisseurFilter = ''; this.search(); }
  goTo(p: number): void { this.page.set(p); this.load(); }

  telechargerPdf(bc: BonCommande): void {
    this.downloadingId.set(bc.id);
    this.achatService.telechargerBonCommandePdf(bc.id).subscribe({
      next: (blob) => {
        this.downloadingId.set(null);
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Bon_de_Commande_${bc.codeBc}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        this.notifications.success(`Bon de commande ${bc.codeBc} téléchargé.`);
      },
      error: (e) => {
        this.downloadingId.set(null);
        this.notifications.error(errorMessage(e));
      },
    });
  }
}
