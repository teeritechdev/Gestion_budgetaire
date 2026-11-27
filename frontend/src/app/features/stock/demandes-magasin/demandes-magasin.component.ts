import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ProfilService } from '../../../core/services/profil.service';
import { ServiceOrganisationnelService } from '../../../core/services/service-organisationnel.service';
import { StockService } from '../../../core/services/stock.service';
import { Article } from '../../../core/models/stock/article.model';
import { DemandeMagasin, StatutDemandeMagasin } from '../../../core/models/stock/demande-magasin.model';
import { ServiceOrganisationnel } from '../../../core/models/service-organisationnel.model';
import { PageHeaderComponent } from '../../../shared/components/page-header.component';
import { FormPageComponent } from '../../../shared/components/form-page.component';
import { PaginationBarComponent } from '../../../shared/components/pagination-bar.component';
import { SearchOptionDirective, SearchSelectComponent } from '../../../shared/components/search-select.component';
import { errorMessage } from '../../../core/utils/error-message';

interface LigneDraft { articleId: string; quantiteDemande: number; }
interface ServirDraft { ligneId: string; designation: string; reste: number; quantiteServie: number; }

const STATUTS: Record<StatutDemandeMagasin, { label: string; classe: string }> = {
  SOUMIS: { label: 'Soumise', classe: 'bg-blue-50 text-blue-700 border border-blue-200' },
  VALIDE_N1: { label: 'Validée N+1', classe: 'bg-emerald-50 text-emerald-700 border border-emerald-200' },
  PARTIELLEMENT_SERVIE: { label: 'Partiellement servie', classe: 'bg-amber-50 text-amber-700 border border-amber-200' },
  SERVIE: { label: 'Servie', classe: 'bg-green-50 text-green-700 border border-green-200' },
  REJETEE: { label: 'Rejetée', classe: 'bg-rose-50 text-rose-700 border border-rose-200' },
};

@Component({
  selector: 'app-demandes-magasin',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHeaderComponent, FormPageComponent, PaginationBarComponent, SearchSelectComponent, SearchOptionDirective],
  template: `
@if (formOpen()) {
<app-form-page section="Demandes d'articles" title="Créer une demande d'articles" mode="Création" subtitle="Demande d’articles du magasin pour un service. Elle sera soumise à la validation du responsable (N+1)." (back)="formOpen.set(false)">
  <form (ngSubmit)="save()">
    <section class="space-y-3 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <h3 class="text-sm font-semibold text-slate-900">Demande</h3>
      <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <label class="block space-y-1 text-xs">Service demandeur <span class="champ-obligatoire" aria-hidden="true">*</span>
          <app-search-select required [(ngModel)]="serviceId" name="serviceId" triggerClass="w-full rounded-lg border border-slate-300 p-2 text-xs" [isDisabled]="!services().length">
            @for (s of services(); track s.id) { <app-search-option [value]="s.id" [label]="s.libelle + ' (' + s.code + ')'" /> }
          </app-search-select>
          @if (serviceDetecte()) {
            <p class="mt-1 flex items-center gap-1 text-[11px] font-medium text-emerald-700">
              <svg class="h-3.5 w-3.5 shrink-0" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" /></svg>
              <span>Service pré-rempli d’après votre affectation : <strong>{{ serviceDetecte() }}</strong></span>
            </p>
          }
        </label>
        <label class="block space-y-1 text-xs">Objet <span class="champ-obligatoire" aria-hidden="true">*</span><input required maxlength="200" [(ngModel)]="objet" name="objet" placeholder="Ex. : Fournitures de bureau du mois" class="w-full rounded-lg border border-slate-300 p-2 text-xs"></label>
      </div>
      @if (!services().length) { <p class="text-xs text-amber-700">Aucun service disponible : vérifiez la permission service.lire.</p> }
    </section>
    <section class="mt-4 space-y-3 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <div class="flex items-center justify-between"><h3 class="text-sm font-semibold text-slate-900">Articles demandés</h3>
        <button type="button" (click)="addLigne()" class="rounded-lg border border-slate-300 px-3 py-1.5 text-xs font-semibold hover:bg-slate-50">＋ Ajouter une ligne</button></div>
      @for (l of lignes; track $index) {
        <div class="grid grid-cols-[1fr_8rem_auto] items-end gap-3 rounded-lg border border-slate-100 bg-slate-50/50 p-2">
          <label class="block space-y-1 text-xs">Article <span class="champ-obligatoire" aria-hidden="true">*</span>
            <app-search-select required [(ngModel)]="l.articleId" [name]="'article' + $index" triggerClass="w-full rounded-lg border border-slate-300 bg-white p-2 text-xs" [isDisabled]="!articles().length">
              @for (a of articles(); track a.id) { <app-search-option [value]="a.id" [label]="a.codeArticle + ' — ' + a.designation + (a.quantiteStock <= 0 ? ' [RUPTURE DE STOCK : 0 ' + a.uniteMesure + ']' : ' (stock: ' + a.quantiteStock + ' ' + a.uniteMesure + ')')" /> }
            </app-search-select>
          </label>
          <label class="block space-y-1 text-xs">Quantité <span class="champ-obligatoire" aria-hidden="true">*</span><input type="number" required min="1" step="1" [(ngModel)]="l.quantiteDemande" [name]="'qte' + $index" class="w-full rounded-lg border border-slate-300 bg-white p-2 text-xs"></label>
          <button type="button" (click)="removeLigne($index)" [disabled]="lignes.length === 1" class="rounded-lg px-3 py-2 text-xs font-medium text-rose-600 hover:bg-rose-50 disabled:opacity-30">Retirer</button>
          @if (getArticle(l.articleId); as art) {
            @if (art.quantiteStock <= 0) {
              <p class="col-span-3 text-[11px] font-semibold text-rose-600">
                ⚠️ L'article « {{ art.designation }} » ({{ art.codeArticle }}) est en rupture de stock (0 {{ art.uniteMesure }} disponible). Vous ne pouvez pas demander cet article.
              </p>
            } @else if (l.quantiteDemande > art.quantiteStock) {
              <p class="col-span-3 text-[11px] font-semibold text-amber-600">
                ⚠️ La quantité demandée ({{ l.quantiteDemande }}) dépasse le stock disponible ({{ art.quantiteStock }} {{ art.uniteMesure }}).
              </p>
            }
          }
        </div>
      }
    </section>
    <div class="form-page-actions mt-4 flex items-center justify-end gap-2">
      <button type="button" (click)="formOpen.set(false)" class="rounded-lg border border-slate-300 px-3 py-2 text-xs">Annuler</button>
      <button [disabled]="saving()" class="rounded-lg bg-green-700 px-4 py-2 text-xs font-semibold text-white shadow-sm transition hover:bg-green-800 disabled:opacity-50">{{ saving() ? 'Envoi…' : 'Soumettre la demande' }}</button>
    </div>
  </form>
</app-form-page>
} @else {
<div class="mx-auto max-w-7xl space-y-6">
  <app-page-header icon="demandes" module="Gestion des stocks" title="Demandes d'articles" description="Les demandes d’articles : validation par le responsable puis service par le magasinier.">
    @if (auth.hasPermission('demande_magasin.creer')) {
      <button type="button" (click)="openForm()" class="whitespace-nowrap rounded-xl bg-green-700 px-4 py-2 text-xs font-semibold text-white hover:bg-green-600">＋ Nouvelle demande</button>
    }
  </app-page-header>

  @if (error()) { <div role="alert" class="rounded-xl border border-rose-300 bg-rose-50 p-3 text-sm text-rose-700">{{ error() }}</div> }

  <!-- Cartes KPI (Design Djibril stats-grid avec liseré supérieur) -->
  <section class="stats-grid grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
    <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" (click)="statutFilter = ''; search()">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Demandes d'articles</p>
      <p class="mt-2 text-2xl font-bold font-mono text-slate-900">{{ total() }}</p>
      <p class="mt-1 text-[11px] text-slate-500">Total des demandes internes</p>
    </article>

    <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" (click)="statutFilter = 'SOUMIS'; search()">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">En attente de visa</p>
      <p class="mt-2 text-2xl font-bold font-mono" [class.text-amber-500]="statsSoumises() > 0" [class.text-slate-900]="statsSoumises() === 0">{{ statsSoumises() }}</p>
      <p class="mt-1 text-[11px] text-slate-500">Validation hiérarchique N+1</p>
    </article>

    <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" (click)="statutFilter = 'VALIDE_N1'; search()">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">À servir en magasin</p>
      <p class="mt-2 text-2xl font-bold font-mono text-teal-700">{{ statsValidees() }}</p>
      <p class="mt-1 text-[11px] text-slate-500">Prêtes pour remise magasinier</p>
    </article>

    <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" (click)="statutFilter = 'SERVIE'; search()">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Servies / Clôturées</p>
      <p class="mt-2 text-2xl font-bold font-mono text-emerald-700">{{ statsServies() }}</p>
      <p class="mt-1 text-[11px] text-slate-500">Articles totalement délivrés</p>
    </article>
  </section>

  <div class="flex flex-wrap items-center gap-3 rounded-2xl border border-slate-200 bg-white p-4">
    <select [(ngModel)]="statutFilter" (change)="search()" class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs">
      <option value="">Tous statuts</option>
      @for (s of statutKeys; track s) { <option [value]="s">{{ statuts[s].label }}</option> }
    </select>
    <label class="flex items-center gap-2 text-xs text-slate-700"><input type="checkbox" [(ngModel)]="mesDemandes" (change)="search()"> Mes demandes uniquement</label>
    <button type="button" (click)="reset()" class="rounded-xl bg-slate-100 px-3 py-2 text-xs text-slate-600 hover:bg-slate-200">Réinitialiser</button>
  </div>

  <section class="overflow-hidden rounded-2xl border border-slate-200 bg-white">
    <div class="overflow-x-auto">
      <table class="w-full min-w-[850px] text-left text-xs">
        <thead class="bg-slate-50 text-slate-600 whitespace-nowrap">
          <tr><th class="p-3 whitespace-nowrap">N°</th><th class="p-3 whitespace-nowrap">Objet</th><th class="p-3 whitespace-nowrap">Demandeur</th><th class="p-3 whitespace-nowrap">Structure</th><th class="p-3 text-right whitespace-nowrap">Lignes</th><th class="p-3 whitespace-nowrap">Statut</th><th class="p-3 whitespace-nowrap">Date</th><th class="p-3 text-right whitespace-nowrap">Actions</th></tr>
        </thead>
        <tbody class="divide-y divide-slate-200">
          @for (d of demandes(); track d.id) {
            <tr>
              <td class="p-3 font-mono whitespace-nowrap">{{ d.numeroDemande }}</td>
              <td class="p-3 font-semibold text-slate-900">{{ d.objet }}</td>
              <td class="p-3 whitespace-nowrap">{{ d.demandeur.prenom }} {{ d.demandeur.nom }}</td>
              <td class="p-3 whitespace-nowrap">{{ d.structureCode }}</td>
              <td class="p-3 text-right whitespace-nowrap">{{ d.lignes.length }}</td>
              <td class="p-3 whitespace-nowrap"><span class="rounded-full px-2 py-0.5 font-semibold" [ngClass]="statuts[d.statut].classe">{{ statuts[d.statut].label }}</span></td>
              <td class="p-3 whitespace-nowrap">{{ d.createdAt | date:'dd/MM/yyyy' }}</td>
              <td class="p-3 whitespace-nowrap">
                <div class="flex justify-end gap-1.5">
                  <button type="button" (click)="detail.set(d)" class="rounded-lg px-2 py-1 text-slate-700 hover:bg-slate-100 font-medium">Détails</button>
                  <button type="button" (click)="imprimerPdf(d)" [disabled]="telechargeantPdf() === d.id" class="rounded-lg border border-slate-300 px-2 py-1 text-slate-700 hover:bg-slate-50 font-medium flex items-center gap-1" title="Télécharger le bon de sortie PDF">
                    <svg aria-hidden="true" class="h-3.5 w-3.5 text-rose-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
                    PDF
                  </button>
                  @if (peutValider(d)) { <button type="button" (click)="openValider(d)" class="rounded-lg bg-green-700 px-2 py-1 font-semibold text-white hover:bg-green-800">Valider</button> }
                  @if (peutServir(d)) { <button type="button" (click)="openServir(d)" class="rounded-lg bg-blue-700 px-2 py-1 font-semibold text-white hover:bg-blue-800">Servir</button> }
                </div>
              </td>
            </tr>
          } @empty {
            <tr><td colspan="8" class="p-6 text-center text-slate-500">{{ loading() ? 'Chargement des demandes…' : 'Aucune demande trouvée.' }}</td></tr>
          }
        </tbody>
      </table>
    </div>
    <app-pagination-bar [page]="page()" [totalPages]="totalPages()" [total]="total()" (pageChange)="goTo($event)" />
  </section>
</div>

@if (detail(); as d) {
  <div class="fixed inset-0 z-[70] flex items-center justify-center bg-slate-950/50 p-4" (click)="$event.target === $event.currentTarget && detail.set(null)">
    <section role="dialog" aria-modal="true" class="max-h-[85vh] w-full max-w-2xl overflow-y-auto rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl">
      <p class="text-xs font-semibold uppercase tracking-wider text-green-700">{{ d.numeroDemande }}</p>
      <h2 class="mt-1 text-xl font-semibold text-slate-900">{{ d.objet }}</h2>
      <p class="mt-1 text-xs text-slate-600">Demandeur : {{ d.demandeur.prenom }} {{ d.demandeur.nom }} · Statut : {{ statuts[d.statut].label }}</p>
      @if (d.valideurN1) { <p class="text-xs text-slate-600">Validée par {{ d.valideurN1.prenom }} {{ d.valideurN1.nom }} le {{ d.dateValidation | date:'dd/MM/yyyy HH:mm' }}</p> }
      @if (d.magasinier) { <p class="text-xs text-slate-600">Servie par {{ d.magasinier.prenom }} {{ d.magasinier.nom }} le {{ d.dateServie | date:'dd/MM/yyyy HH:mm' }}</p> }
      <table class="mt-4 w-full text-left text-xs">
        <thead class="bg-slate-50 text-slate-600"><tr><th class="p-2">Article</th><th class="p-2 text-right">Demandé</th><th class="p-2 text-right">Servi</th></tr></thead>
        <tbody class="divide-y divide-slate-200">
          @for (l of d.lignes; track l.id) { <tr><td class="p-2">{{ l.article.codeArticle }} — {{ l.article.designation }}</td><td class="p-2 text-right">{{ l.quantiteDemande }}</td><td class="p-2 text-right">{{ l.quantiteServie }}</td></tr> }
        </tbody>
      </table>
      <div class="mt-5 flex items-center justify-between border-t border-slate-200 pt-4">
        <button type="button" (click)="imprimerPdf(d)" [disabled]="telechargeantPdf() === d.id" class="flex items-center gap-1.5 rounded-lg border border-slate-300 px-3 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50">
          <svg aria-hidden="true" class="h-4 w-4 text-rose-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
          {{ telechargeantPdf() === d.id ? 'Génération...' : 'Imprimer le Bon de Sortie (PDF)' }}
        </button>
        <button type="button" (click)="detail.set(null)" class="rounded-lg bg-slate-800 px-4 py-2 text-xs font-semibold text-white hover:bg-slate-700">Fermer</button>
      </div>
    </section>
  </div>
}

@if (valider(); as d) {
  <div class="fixed inset-0 z-[70] flex items-center justify-center bg-slate-950/50 p-4">
    <section role="dialog" aria-modal="true" class="w-full max-w-lg rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl">
      <h2 class="text-lg font-semibold text-slate-900">Valider la demande {{ d.numeroDemande }}</h2>
      <p class="mt-1 text-xs text-slate-600">{{ d.objet }} — {{ d.lignes.length }} ligne(s)</p>
      <label class="mt-4 block space-y-1 text-xs">Commentaire<textarea rows="3" [(ngModel)]="commentaire" name="commentaire" class="w-full rounded-lg border border-slate-300 p-2"></textarea></label>
      <div class="mt-5 flex justify-end gap-2">
        <button type="button" (click)="valider.set(null)" class="rounded-lg border px-3 py-2 text-xs">Annuler</button>
        <button type="button" [disabled]="saving()" (click)="decider('REJETE')" class="rounded-lg bg-rose-600 px-4 py-2 text-xs font-semibold text-white disabled:opacity-50">Rejeter</button>
        <button type="button" [disabled]="saving()" (click)="decider('APPROUVE')" class="rounded-lg bg-green-700 px-4 py-2 text-xs font-semibold text-white disabled:opacity-50">Approuver</button>
      </div>
    </section>
  </div>
}

@if (servir(); as d) {
  <div class="fixed inset-0 z-[70] flex items-center justify-center bg-slate-950/50 p-4">
    <section role="dialog" aria-modal="true" class="max-h-[85vh] w-full max-w-2xl overflow-y-auto rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl">
      <h2 class="text-lg font-semibold text-slate-900">Servir la demande {{ d.numeroDemande }}</h2>
      <p class="mt-1 text-xs text-slate-600">Saisissez les quantités remises. Elles s’ajoutent aux quantités déjà servies et chaque service crée une sortie de stock.</p>
      <table class="mt-4 w-full text-left text-xs">
        <thead class="bg-slate-50 text-slate-600"><tr><th class="p-2">Article</th><th class="p-2 text-right">Reste à servir</th><th class="p-2 text-right">Quantité à servir</th></tr></thead>
        <tbody class="divide-y divide-slate-200">
          @for (l of servirLignes; track l.ligneId) {
            <tr><td class="p-2">{{ l.designation }}</td><td class="p-2 text-right">{{ l.reste }}</td>
              <td class="p-2 text-right"><input type="number" min="0" [max]="l.reste" step="1" [(ngModel)]="l.quantiteServie" [name]="'s' + l.ligneId" class="w-24 rounded-lg border border-slate-300 p-1.5 text-right"></td></tr>
          }
        </tbody>
      </table>
      <div class="mt-5 flex justify-end gap-2">
        <button type="button" (click)="servir.set(null)" class="rounded-lg border px-3 py-2 text-xs">Annuler</button>
        <button type="button" [disabled]="saving()" (click)="confirmerServir()" class="rounded-lg bg-blue-700 px-4 py-2 text-xs font-semibold text-white disabled:opacity-50">Confirmer le service</button>
      </div>
    </section>
  </div>
}
}
`,
})
export class DemandesMagasinComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly stock = inject(StockService);
  private readonly serviceApi = inject(ServiceOrganisationnelService);
  private readonly profilApi = inject(ProfilService);
  private readonly notifications = inject(NotificationService);

  readonly statuts = STATUTS;
  readonly statutKeys = Object.keys(STATUTS) as StatutDemandeMagasin[];

  readonly demandes = signal<DemandeMagasin[]>([]);
  readonly articles = signal<Article[]>([]);
  readonly services = signal<ServiceOrganisationnel[]>([]);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly total = signal(0);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly formOpen = signal(false);
  readonly detail = signal<DemandeMagasin | null>(null);
  readonly valider = signal<DemandeMagasin | null>(null);
  readonly servir = signal<DemandeMagasin | null>(null);
  readonly telechargeantPdf = signal<string | null>(null);
  readonly serviceDetecte = signal<string | null>(null);

  readonly statsSoumises = computed(() => this.demandes().filter(d => d.statut === 'SOUMIS').length);
  readonly statsValidees = computed(() => this.demandes().filter(d => d.statut === 'VALIDE_N1' || d.statut === 'PARTIELLEMENT_SERVIE').length);
  readonly statsServies = computed(() => this.demandes().filter(d => d.statut === 'SERVIE').length);

  statutFilter = '';
  mesDemandes = false;
  serviceId = '';
  objet = '';
  lignes: LigneDraft[] = [{ articleId: '', quantiteDemande: 1 }];
  commentaire = '';
  servirLignes: ServirDraft[] = [];

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.stock.listerDemandes({
      page: this.page(), size: 20, sort: 'createdAt,desc',
      statut: this.statutFilter, demandeurId: this.mesDemandes ? this.auth.currentUser()?.id : undefined,
    }).subscribe({
      next: (res) => { this.demandes.set(res.content); this.totalPages.set(res.totalPages); this.total.set(res.totalElements); this.loading.set(false); },
      error: (e) => { this.loading.set(false); this.error.set(errorMessage(e)); },
    });
  }
  search(): void { this.page.set(0); this.load(); }
  reset(): void { this.statutFilter = ''; this.mesDemandes = false; this.search(); }
  goTo(p: number): void { this.page.set(p); this.load(); }

  // Le serveur applique les règles (responsable N+1, jamais sa propre demande) : ici on masque seulement ce qui est évident.
  private estDemandeur(d: DemandeMagasin): boolean { return d.demandeur.id === this.auth.currentUser()?.id; }
  peutValider(d: DemandeMagasin): boolean { return d.statut === 'SOUMIS' && this.auth.hasPermission('demande_magasin.valider') && !this.estDemandeur(d); }
  peutServir(d: DemandeMagasin): boolean {
    return (d.statut === 'VALIDE_N1' || d.statut === 'PARTIELLEMENT_SERVIE') && this.auth.hasPermission('demande_magasin.servir') && !this.estDemandeur(d);
  }

  openForm(): void {
    this.serviceId = ''; this.objet = ''; this.lignes = [{ articleId: '', quantiteDemande: 1 }];
    this.serviceDetecte.set(null);
    if (this.auth.hasPermission('service.lire')) {
      this.serviceApi.lister(undefined, true).subscribe({
        next: (services) => {
          this.services.set(services);
          // Auto-remplissage basé sur l'affectation active de l'utilisateur
          this.profilApi.profil().subscribe({
            next: (p) => {
              if (p.serviceLibelle) {
                const s = services.find((srv) => srv.libelle.trim().toLowerCase() === p.serviceLibelle!.trim().toLowerCase());
                if (s) {
                  this.serviceId = s.id;
                  this.serviceDetecte.set(s.libelle + (s.code ? ` (${s.code})` : ''));
                }
              }
            },
            error: () => {},
          });
        },
        error: () => {},
      });
    }
    if (this.auth.hasPermission('article.lire')) {
      this.stock.listerArticles({ page: 0, size: 200, sort: 'codeArticle,asc' }).subscribe({
        next: (r) => this.articles.set(r.content.filter((a) => a.actif)),
        error: () => {},
      });
    }
    this.formOpen.set(true);
  }
  addLigne(): void { this.lignes = [...this.lignes, { articleId: '', quantiteDemande: 1 }]; }
  removeLigne(i: number): void { this.lignes = this.lignes.filter((_, idx) => idx !== i); }

  getArticle(id: string): Article | undefined {
    return this.articles().find((a) => a.id === id);
  }

  save(): void {
    if (!this.serviceId) {
      this.notifications.warning('Choisissez un service.');
      return;
    }
    const service = this.services().find((s) => s.id === this.serviceId);
    if (!service) {
      this.notifications.warning('Choisissez un service valide.');
      return;
    }
    if (!this.objet.trim()) {
      this.notifications.warning('Renseignez l’objet de la demande.');
      return;
    }
    if (!this.lignes.length) {
      this.notifications.warning('Ajoutez au moins une ligne d’article.');
      return;
    }

    for (const l of this.lignes) {
      if (!l.articleId) {
        this.notifications.warning('Veuillez sélectionner un article pour chaque ligne.');
        return;
      }
      const art = this.getArticle(l.articleId);
      if (!art) continue;
      const stockDispo = art.quantiteStock ?? 0;
      if (stockDispo <= 0) {
        this.notifications.error(`L'article « ${art.designation} » (${art.codeArticle}) est en rupture de stock (0 ${art.uniteMesure} disponible). Impossible de soumettre la demande.`);
        return;
      }
      if (!l.quantiteDemande || l.quantiteDemande <= 0) {
        this.notifications.warning(`La quantité demandée pour « ${art.designation} » doit être supérieure à zéro.`);
        return;
      }
      if (l.quantiteDemande > stockDispo) {
        this.notifications.error(`La quantité demandée (${l.quantiteDemande}) pour « ${art.designation} » dépasse le stock disponible (${stockDispo} ${art.uniteMesure}).`);
        return;
      }
    }

    this.saving.set(true);
    this.stock.creerDemande({ serviceId: service.id, structureCode: service.structureCode, objet: this.objet.trim(), lignes: this.lignes }).subscribe({
      next: () => { this.saving.set(false); this.formOpen.set(false); this.notifications.success('Demande soumise avec succès.'); this.load(); },
      error: (e) => { this.saving.set(false); this.notifications.error(errorMessage(e)); },
    });
  }

  openValider(d: DemandeMagasin): void { this.commentaire = ''; this.valider.set(d); }
  decider(decision: 'APPROUVE' | 'REJETE'): void {
    const d = this.valider();
    if (!d) return;
    this.saving.set(true);
    this.stock.validerDemande(d.id, { decision, commentaire: this.commentaire.trim() || undefined }).subscribe({
      next: () => { this.saving.set(false); this.valider.set(null); this.notifications.success(decision === 'APPROUVE' ? 'Demande approuvée.' : 'Demande rejetée.'); this.load(); },
      error: (e) => { this.saving.set(false); this.notifications.error(errorMessage(e)); },
    });
  }

  openServir(d: DemandeMagasin): void {
    this.servirLignes = d.lignes.map((l) => ({
      ligneId: l.id, designation: `${l.article.codeArticle} — ${l.article.designation}`,
      reste: l.quantiteDemande - l.quantiteServie, quantiteServie: l.quantiteDemande - l.quantiteServie,
    }));
    this.servir.set(d);
  }
  confirmerServir(): void {
    const d = this.servir();
    if (!d) return;
    this.saving.set(true);
    this.stock.servirDemande(d.id, { lignes: this.servirLignes.map((l) => ({ ligneId: l.ligneId, quantiteServie: l.quantiteServie ?? 0 })) }).subscribe({
      next: () => { this.saving.set(false); this.servir.set(null); this.notifications.success('Service enregistré.'); this.load(); },
      error: (e) => { this.saving.set(false); this.notifications.error(errorMessage(e)); },
    });
  }

  imprimerPdf(d: DemandeMagasin): void {
    this.telechargeantPdf.set(d.id);
    this.stock.telechargerBonDeSortiePdf(d.id).subscribe({
      next: (blob) => {
        this.telechargeantPdf.set(null);
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `bon_de_sortie_${d.numeroDemande}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (e) => {
        this.telechargeantPdf.set(null);
        this.notifications.error(errorMessage(e));
      },
    });
  }
}
