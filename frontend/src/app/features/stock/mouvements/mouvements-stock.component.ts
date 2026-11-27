import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { StockService } from '../../../core/services/stock.service';
import { Article } from '../../../core/models/stock/article.model';
import { MouvementStock, MouvementStockRequest, TypeMouvement } from '../../../core/models/stock/mouvement-stock.model';
import { PageHeaderComponent } from '../../../shared/components/page-header.component';
import { FormPageComponent } from '../../../shared/components/form-page.component';
import { PaginationBarComponent } from '../../../shared/components/pagination-bar.component';
import { DatePickerComponent } from '../../../shared/components/date-picker.component';
import { SearchOptionDirective, SearchSelectComponent } from '../../../shared/components/search-select.component';
import { errorMessage } from '../../../core/utils/error-message';

@Component({
  selector: 'app-mouvements-stock',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHeaderComponent, FormPageComponent, PaginationBarComponent, DatePickerComponent, SearchSelectComponent, SearchOptionDirective],
  template: `
@if (formOpen()) {
<app-form-page section="Mouvements de stock" title="Enregistrer un mouvement" mode="Création" subtitle="Entrée, sortie ou ajustement d’inventaire. Une sortie ne peut pas dépasser le stock disponible." (back)="formOpen.set(false)">
  <form (ngSubmit)="save()">
    <section class="space-y-3 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <label class="block space-y-1 text-xs">Article <span class="champ-obligatoire" aria-hidden="true">*</span>
          <app-search-select required [(ngModel)]="draft.articleId" name="articleId" triggerClass="w-full rounded-lg border border-slate-300 bg-white p-2 text-xs" [isDisabled]="!articles().length">
            @for (a of articles(); track a.id) { <app-search-option [value]="a.id" [label]="a.codeArticle + ' — ' + a.designation + ' (stock: ' + a.quantiteStock + ')'" /> }
          </app-search-select>
        </label>
        <label class="block space-y-1 text-xs">Type de mouvement <span class="champ-obligatoire" aria-hidden="true">*</span>
          <select required [(ngModel)]="draft.typeMouvement" name="type" class="w-full rounded-lg border border-slate-300 bg-white p-2 text-xs">
            <option value="ENTREE">Entrée</option><option value="SORTIE">Sortie</option><option value="AJUSTEMENT">Ajustement (inventaire)</option>
          </select>
        </label>
        <label class="block space-y-1 text-xs">{{ draft.typeMouvement === 'AJUSTEMENT' ? 'Stock physique compté' : 'Quantité' }} <span class="champ-obligatoire" aria-hidden="true">*</span>
          <input type="number" required [min]="draft.typeMouvement === 'AJUSTEMENT' ? 0 : 1" step="1" [(ngModel)]="draft.quantite" name="quantite" class="w-full rounded-lg border border-slate-300 bg-white p-2 text-xs">
        </label>
        <label class="block space-y-1 text-xs">Référence du document <span class="champ-obligatoire" aria-hidden="true">*</span><input required maxlength="100" [(ngModel)]="draft.referenceDocument" name="ref" placeholder="Ex. : BL-2026-014" class="w-full rounded-lg border border-slate-300 bg-white p-2 text-xs"></label>
        @if (draft.typeMouvement === 'ENTREE') {
          <label class="block space-y-1 text-xs">Prix unitaire (recalcule le PUMP)<input type="number" min="0" step="0.01" [(ngModel)]="draft.prixUnitaire" name="prix" class="w-full rounded-lg border border-slate-300 bg-white p-2 text-xs"></label>
        }
      </div>
      @if (draft.typeMouvement === 'AJUSTEMENT') {
        <p class="text-xs text-slate-500">Pour un ajustement, saisissez le stock réellement compté : le mouvement enregistre l’écart avec le stock actuel.</p>
      }
    </section>
    <div class="form-page-actions mt-4 flex items-center justify-end gap-2">
      <button type="button" (click)="formOpen.set(false)" class="rounded-lg border border-slate-300 px-3 py-2 text-xs">Annuler</button>
      <button [disabled]="saving()" class="rounded-lg bg-green-700 px-4 py-2 text-xs font-semibold text-white shadow-sm transition hover:bg-green-800 disabled:opacity-50">{{ saving() ? 'Enregistrement…' : 'Enregistrer' }}</button>
    </div>
  </form>
</app-form-page>
} @else {
<div class="mx-auto max-w-7xl space-y-6">
  <app-page-header icon="mouvements" module="Gestion des stocks" title="Mouvements de stock" description="L’historique des entrées, sorties et ajustements, avec le stock avant et après.">
    @if (auth.hasPermission('mouvement_stock.creer')) {
      <button type="button" (click)="openForm()" class="whitespace-nowrap rounded-xl bg-green-700 px-4 py-2 text-xs font-semibold text-white hover:bg-green-600">＋ Nouveau mouvement</button>
    }
  </app-page-header>

  @if (error()) { <div role="alert" class="rounded-xl border border-rose-300 bg-rose-50 p-3 text-sm text-rose-700">{{ error() }}</div> }

  <!-- Cartes KPI (Design Djibril stats-grid avec liseré supérieur) -->
  <section class="stats-grid grid gap-3 sm:grid-cols-3">
    <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" (click)="typeFilter = ''; search()">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Total mouvements</p>
      <p class="mt-2 text-2xl font-bold font-mono text-slate-900">{{ total() }}</p>
      <p class="mt-1 text-[11px] text-slate-500">Flux d'entrées, sorties et ajustements</p>
    </article>

    <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" (click)="typeFilter = 'ENTREE'; search()">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Entrées en stock</p>
      <p class="mt-2 text-2xl font-bold font-mono text-emerald-700">{{ statsEntrees() }}</p>
      <p class="mt-1 text-[11px] text-slate-500">Réceptions fournisseurs & retours</p>
    </article>

    <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" (click)="typeFilter = 'SORTIE'; search()">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Sorties magasin</p>
      <p class="mt-2 text-2xl font-bold font-mono text-rose-600">{{ statsSorties() }}</p>
      <p class="mt-1 text-[11px] text-slate-500">Besoins servis aux services</p>
    </article>
  </section>

  <div class="flex flex-wrap items-center gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
    <div class="min-w-[240px]">
      <app-search-select [(ngModel)]="articleFilter" (ngModelChange)="search()" triggerClass="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs">
        <app-search-option value="" label="Tous les articles" />
        @for (a of articles(); track a.id) { <app-search-option [value]="a.id" [label]="a.codeArticle + ' — ' + a.designation" /> }
      </app-search-select>
    </div>
    <select [(ngModel)]="typeFilter" (change)="search()" class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs">
      <option value="">Tous types</option><option value="ENTREE">Entrée</option><option value="SORTIE">Sortie</option><option value="AJUSTEMENT">Ajustement</option>
    </select>
    <div class="flex items-center gap-1.5 text-xs text-slate-600">
      <span>Du</span>
      <div class="w-36"><app-date-picker [(ngModel)]="dateDebut" (ngModelChange)="search()" /></div>
    </div>
    <div class="flex items-center gap-1.5 text-xs text-slate-600">
      <span>Au</span>
      <div class="w-36"><app-date-picker [(ngModel)]="dateFin" (ngModelChange)="search()" /></div>
    </div>
    <button type="button" (click)="reset()" class="rounded-xl bg-slate-100 px-3 py-2 text-xs text-slate-600 hover:bg-slate-200">Réinitialiser</button>
  </div>

  <section class="overflow-hidden rounded-2xl border border-slate-200 bg-white">
    <div class="overflow-x-auto">
      <table class="w-full min-w-[850px] text-left text-xs">
        <thead class="bg-slate-50 text-slate-600 whitespace-nowrap">
          <tr><th class="p-3 whitespace-nowrap">Date</th><th class="p-3 whitespace-nowrap">Article</th><th class="p-3 whitespace-nowrap">Type</th><th class="p-3 text-right whitespace-nowrap">Quantité</th><th class="p-3 text-right whitespace-nowrap">Avant</th><th class="p-3 text-right whitespace-nowrap">Après</th><th class="p-3 whitespace-nowrap">Référence</th><th class="p-3 whitespace-nowrap">Auteur</th></tr>
        </thead>
        <tbody class="divide-y divide-slate-200">
          @for (m of mouvements(); track m.id) {
            <tr>
              <td class="p-3 whitespace-nowrap">{{ m.createdAt | date:'dd/MM/yyyy HH:mm' }}</td>
              <td class="p-3">{{ articleLabel(m.articleId) }}</td>
              <td class="p-3"><span class="rounded-full px-2 py-0.5 font-semibold" [ngClass]="badge(m.typeMouvement)">{{ m.typeMouvement }}</span></td>
              <td class="p-3 text-right font-semibold">{{ m.quantite }}</td>
              <td class="p-3 text-right">{{ m.stockAnterieur }}</td>
              <td class="p-3 text-right font-semibold">{{ m.nouveauStock }}</td>
              <td class="p-3 font-mono">{{ m.referenceDocument }}</td>
              <td class="p-3">{{ m.auteur ? m.auteur.prenom + ' ' + m.auteur.nom : '—' }}</td>
            </tr>
          } @empty {
            <tr><td colspan="8" class="p-6 text-center text-slate-500">{{ loading() ? 'Chargement des mouvements…' : 'Aucun mouvement trouvé.' }}</td></tr>
          }
        </tbody>
      </table>
    </div>
    <app-pagination-bar [page]="page()" [totalPages]="totalPages()" [total]="total()" (pageChange)="goTo($event)" />
  </section>
</div>
}
`,
})
export class MouvementsStockComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly stock = inject(StockService);
  private readonly notifications = inject(NotificationService);

  readonly mouvements = signal<MouvementStock[]>([]);
  readonly articles = signal<Article[]>([]);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly total = signal(0);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly formOpen = signal(false);

  readonly statsEntrees = computed(() => this.mouvements().filter(m => m.typeMouvement === 'ENTREE').length);
  readonly statsSorties = computed(() => this.mouvements().filter(m => m.typeMouvement === 'SORTIE').length);
  readonly statsAjustements = computed(() => this.mouvements().filter(m => m.typeMouvement === 'AJUSTEMENT').length);

  articleFilter = '';
  typeFilter = '';
  dateDebut = '';
  dateFin = '';
  draft: MouvementStockRequest = this.emptyDraft();

  ngOnInit(): void {
    if (this.auth.hasPermission('article.lire')) this.loadArticles();
    this.load();
  }

  private loadArticles(): void {
    this.stock.listerArticles({ page: 0, size: 200, sort: 'codeArticle,asc' }).subscribe({ next: (r) => this.articles.set(r.content), error: () => {} });
  }
  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.stock.listerMouvements({
      page: this.page(), size: 20, sort: 'createdAt,desc',
      articleId: this.articleFilter, typeMouvement: this.typeFilter, dateDebut: this.dateDebut, dateFin: this.dateFin,
    }).subscribe({
      next: (res) => { this.mouvements.set(res.content); this.totalPages.set(res.totalPages); this.total.set(res.totalElements); this.loading.set(false); },
      error: (e) => { this.loading.set(false); this.error.set(errorMessage(e)); },
    });
  }
  search(): void { this.page.set(0); this.load(); }
  reset(): void { this.articleFilter = ''; this.typeFilter = ''; this.dateDebut = ''; this.dateFin = ''; this.search(); }
  goTo(p: number): void { this.page.set(p); this.load(); }

  articleLabel(id: string): string {
    const a = this.articles().find((x) => x.id === id);
    return a ? `${a.codeArticle} — ${a.designation}` : id.substring(0, 8);
  }
  badge(t: TypeMouvement): string {
    return t === 'ENTREE' ? 'bg-green-100 text-green-800' : t === 'SORTIE' ? 'bg-rose-100 text-rose-800' : 'bg-amber-100 text-amber-800';
  }
  openForm(): void { this.draft = this.emptyDraft(); this.formOpen.set(true); }
  save(): void {
    this.saving.set(true);
    const body: MouvementStockRequest = { ...this.draft, prixUnitaire: this.draft.typeMouvement === 'ENTREE' ? this.draft.prixUnitaire : null };
    this.stock.creerMouvement(body).subscribe({
      next: () => { this.saving.set(false); this.formOpen.set(false); this.notifications.success('Mouvement enregistré.'); this.loadArticles(); this.load(); },
      error: (e) => { this.saving.set(false); this.notifications.error(errorMessage(e)); },
    });
  }
  private emptyDraft(): MouvementStockRequest {
    return { articleId: '', typeMouvement: 'ENTREE', quantite: 1, referenceDocument: '', prixUnitaire: null };
  }
}
