import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { StockService } from '../../../core/services/stock.service';
import { Article, ArticleRequest, StockKpis } from '../../../core/models/stock/article.model';
import { MouvementStock } from '../../../core/models/stock/mouvement-stock.model';
import { IMPORT_FORMATS } from '../../../core/models/import-formats';
import { PageHeaderComponent } from '../../../shared/components/page-header.component';
import { AppIconComponent } from '../../../shared/components/app-icon.component';
import { RowActionsComponent } from '../../../shared/components/row-actions.component';
import { FormPageComponent } from '../../../shared/components/form-page.component';
import { PaginationBarComponent } from '../../../shared/components/pagination-bar.component';
import { DataActionsComponent } from '../../../shared/components/data-actions.component';
import { ImportCsvPageComponent } from '../../../shared/components/import-csv-page.component';
import { errorMessage } from '../../../core/utils/error-message';

@Component({
  selector: 'app-articles-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    PageHeaderComponent,
    AppIconComponent,
    RowActionsComponent,
    FormPageComponent,
    PaginationBarComponent,
    DataActionsComponent,
    ImportCsvPageComponent,
  ],
  template: `
@if (importOpen()) {
  <app-import-csv-page
    section="Articles"
    ressource="articles"
    [format]="importFormats.articles"
    (back)="importOpen.set(false)"
    (imported)="onImported()" />
} @else if (formOpen()) {
<app-form-page section="Articles" [title]="editingId() ? 'Modifier l’article' : 'Créer un article'" [mode]="editingId() ? 'Modification' : 'Création'" subtitle="Référence du catalogue du magasin. Le stock ne change que par les mouvements de stock." (back)="formOpen.set(false)">
  <form (ngSubmit)="save()">
    <section class="space-y-3 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <h3 class="text-sm font-semibold text-slate-900">Identification</h3>
      <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <label class="block space-y-1 text-xs">Code article <span class="champ-obligatoire" aria-hidden="true">*</span><input required maxlength="50" [(ngModel)]="draft.codeArticle" name="codeArticle" placeholder="Ex. : ART-001" class="w-full rounded-lg border border-slate-300 p-2 uppercase"></label>
        <label class="block space-y-1 text-xs">Unité de mesure <span class="champ-obligatoire" aria-hidden="true">*</span><input required maxlength="30" [(ngModel)]="draft.uniteMesure" name="uniteMesure" placeholder="Ex. : Unité, Ramette, Boîte" class="w-full rounded-lg border border-slate-300 p-2"></label>
        <label class="block space-y-1 text-xs sm:col-span-2">Désignation <span class="champ-obligatoire" aria-hidden="true">*</span><input required maxlength="200" [(ngModel)]="draft.designation" name="designation" placeholder="Ex. : Ramette papier A4 80 g" class="w-full rounded-lg border border-slate-300 p-2"></label>
        <label class="block space-y-1 text-xs sm:col-span-2">Description<textarea rows="3" [(ngModel)]="draft.description" name="description" class="w-full rounded-lg border border-slate-300 p-2"></textarea></label>
      </div>
    </section>
    <section class="mt-4 space-y-3 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <h3 class="text-sm font-semibold text-slate-900">Gestion du stock</h3>
      <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <label class="block space-y-1 text-xs">Seuil de réapprovisionnement<input type="number" min="0" step="1" [(ngModel)]="draft.seuilReapprovisionnement" name="seuil" class="w-full rounded-lg border border-slate-300 p-2"></label>
        <label class="block space-y-1 text-xs">Prix unitaire moyen pondéré (initial)<input type="number" min="0" step="0.01" [(ngModel)]="draft.prixUnitaireMoyenPondere" name="pump" class="w-full rounded-lg border border-slate-300 p-2"></label>
      </div>
      <label class="flex items-center gap-2 text-xs font-medium text-slate-700"><input type="checkbox" [(ngModel)]="draft.actif" name="actif" class="rounded border-slate-300 text-green-700"> Article actif (un article inactif ne peut pas être demandé)</label>
    </section>
    <div class="form-page-actions mt-4 flex items-center justify-end gap-2">
      <button type="button" (click)="formOpen.set(false)" class="rounded-lg border border-slate-300 px-3 py-2 text-xs">Annuler</button>
      <button [disabled]="saving()" class="rounded-lg bg-green-700 px-4 py-2 text-xs font-semibold text-white shadow-sm transition hover:bg-green-800 disabled:opacity-50">{{ saving() ? 'Enregistrement…' : 'Enregistrer' }}</button>
    </div>
  </form>
</app-form-page>
} @else {
<div class="mx-auto max-w-7xl space-y-6">
  <app-page-header icon="articles" module="Gestion des stocks" title="Articles" description="Le catalogue du magasin : références, stock courant, seuils et prix moyen pondéré.">
    <div class="flex flex-wrap items-center gap-2">
      <app-data-actions
        ressource="articles"
        exportPermission="article.lire"
        [ids]="exportIds()"
        [importPermissions]="['article.creer']"
        [importFormat]="importFormats.articles"
        (importer)="importOpen.set(true)" />
      @if (auth.hasPermission('article.creer')) {
        <button type="button" (click)="openForm()" class="whitespace-nowrap rounded-xl bg-green-700 px-4 py-2 text-xs font-semibold text-white shadow-sm transition hover:bg-green-800">＋ Nouvel article</button>
      }
    </div>
  </app-page-header>

  @if (error()) { <div role="alert" class="rounded-xl border border-rose-300 bg-rose-50 p-3 text-sm text-rose-700">{{ error() }}</div> }

  @if (kpis(); as k) {
  <section class="stats-grid grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
    <article class="rounded-2xl border border-slate-200 bg-white p-4">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Articles actifs</p>
      <p class="mt-2 text-2xl font-bold font-mono text-slate-900">{{ k.totalArticles }}</p>
      <p class="mt-1 text-[11px] text-slate-500">Références au catalogue</p>
    </article>

    <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" [class.border-rose-400]="sousSeuil" (click)="filtrerSousSeuil()">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Alertes stock critique</p>
      <p class="mt-2 text-2xl font-bold font-mono" [class.text-rose-600]="k.articlesEnAlerte > 0" [class.text-slate-900]="k.articlesEnAlerte === 0">{{ k.articlesEnAlerte }}</p>
      <p class="mt-1 text-[11px] text-slate-500">{{ sousSeuil ? 'Filtre actif (cliquer pour réinitialiser)' : 'Sous le seuil de réappro.' }}</p>
    </article>

    <article class="rounded-2xl border border-slate-200 bg-white p-4">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Valeur totale stock</p>
      <p class="mt-2 text-2xl font-bold font-mono text-purple-700">{{ (k.valeurTotaleStock | number:'1.0-0') ?? '0' }} <span class="text-xs font-normal text-slate-500">FCFA</span></p>
      <p class="mt-1 text-[11px] text-slate-500">Valorisation au PUMP</p>
    </article>

    <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" routerLink="/stock/demandes-magasin">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Demandes à traiter</p>
      <p class="mt-2 text-2xl font-bold font-mono text-blue-600">{{ k.demandesEnAttenteValidation + k.demandesEnAttenteService }}</p>
      <p class="mt-1 text-[11px] text-slate-500">{{ k.demandesEnAttenteValidation }} à valider · {{ k.demandesEnAttenteService }} à servir</p>
    </article>
  </section>
  }

  <div class="flex flex-wrap items-center gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
    <input type="text" [(ngModel)]="codeFilter" (keyup.enter)="search()" placeholder="Code article…" class="w-40 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs" />
    <input type="text" [(ngModel)]="designationFilter" (keyup.enter)="search()" placeholder="Désignation…" class="min-w-[200px] flex-1 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs" />
    <label class="flex items-center gap-2 text-xs text-slate-700"><input type="checkbox" [(ngModel)]="sousSeuil" (change)="search()"> Stock sous le seuil</label>
    <button type="button" (click)="search()" class="rounded-xl bg-blue-700 px-3 py-2 text-xs font-medium text-white hover:bg-blue-800">Rechercher</button>
    <button type="button" (click)="reset()" class="rounded-xl bg-slate-100 px-3 py-2 text-xs text-slate-600 hover:bg-slate-200">Réinitialiser</button>
  </div>

  <section class="overflow-hidden rounded-2xl border border-slate-200 bg-white">
    <div class="overflow-x-auto">
      <table class="w-full min-w-[850px] text-left text-xs">
        <thead class="bg-slate-50 text-slate-600 whitespace-nowrap">
          <tr><th class="p-3 whitespace-nowrap">Code</th><th class="p-3 whitespace-nowrap">Désignation</th><th class="p-3 whitespace-nowrap">Unité</th><th class="p-3 text-right whitespace-nowrap">Stock</th><th class="p-3 text-right whitespace-nowrap">Seuil</th><th class="p-3 text-right whitespace-nowrap">PUMP</th><th class="p-3 whitespace-nowrap">Statut</th><th class="p-3 text-right whitespace-nowrap">Actions</th></tr>
        </thead>
        <tbody class="divide-y divide-slate-200">
          @for (a of articles(); track a.id) {
            <tr class="hover:bg-slate-50/60 transition">
              <td class="p-3 font-mono font-semibold text-blue-700 cursor-pointer hover:underline" (click)="viewArticle(a)">{{ a.codeArticle }}</td>
              <td class="p-3 font-semibold text-slate-900 cursor-pointer hover:text-blue-700" (click)="viewArticle(a)">{{ a.designation }}</td>
              <td class="p-3">{{ a.uniteMesure }}</td>
              <td class="p-3 text-right font-semibold" [class.text-rose-600]="sousLeSeuil(a)">
                {{ a.quantiteStock }}
                @if (sousLeSeuil(a)) { <span class="ml-1.5 inline-flex items-center rounded bg-rose-50 px-1.5 py-0.5 text-[10px] font-bold text-rose-700 border border-rose-200">Critique</span> }
              </td>
              <td class="p-3 text-right font-mono">{{ a.seuilReapprovisionnement ?? '—' }}</td>
              <td class="p-3 text-right font-mono whitespace-nowrap">{{ a.prixUnitaireMoyenPondere != null ? (a.prixUnitaireMoyenPondere | number:'1.0-2') + ' FCFA' : '—' }}</td>
              <td class="p-3">
                @if (a.actif) {
                  <span class="rounded-full bg-emerald-50 px-2 py-0.5 text-[11px] font-semibold text-emerald-700 border border-emerald-200">Actif</span>
                } @else {
                  <span class="rounded-full bg-slate-100 px-2 py-0.5 text-[11px] font-semibold text-slate-600 border border-slate-200">Inactif</span>
                }
              </td>
              <td class="p-3"><app-row-actions [label]="a.codeArticle" [canEdit]="auth.hasPermission('article.modifier')" (view)="viewArticle(a)" (edit)="edit(a)" /></td>
            </tr>
          } @empty {
            <tr><td colspan="8" class="p-6 text-center text-slate-500">{{ loading() ? 'Chargement des articles…' : 'Aucun article trouvé.' }}</td></tr>
          }
        </tbody>
      </table>
    </div>
    <app-pagination-bar [page]="page()" [totalPages]="totalPages()" [total]="total()" (pageChange)="goTo($event)" />
  </section>
</div>

<!-- Modal Détail Article Enrichi avec Historique Mouvements (Point 4) -->
@if (selectedArticle(); as art) {
  <div class="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-slate-900/60 p-4 backdrop-blur-sm" (click)="selectedArticle.set(null)">
    <div class="relative w-full max-w-4xl max-h-[90vh] flex flex-col rounded-2xl bg-white shadow-2xl overflow-hidden" (click)="$event.stopPropagation()">
      <!-- En-tête -->
      <div class="flex items-center justify-between border-b border-slate-200 px-6 py-4 bg-slate-50/80">
        <div class="flex items-center gap-3">
          <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-green-100 text-green-700">
            <app-icon name="articles" classe="h-5 w-5" />
          </div>
          <div>
            <div class="flex items-center gap-2">
              <h2 class="text-base font-bold text-slate-900 font-mono">{{ art.codeArticle }}</h2>
              <span class="text-slate-400">·</span>
              <span class="text-sm font-semibold text-slate-800">{{ art.designation }}</span>
            </div>
            <p class="text-xs text-slate-500">Fiche détaillée du catalogue & historique des mouvements de stock</p>
          </div>
        </div>
        <div class="flex items-center gap-2">
          @if (art.actif) {
            <span class="rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-semibold text-emerald-700 border border-emerald-200">Actif</span>
          } @else {
            <span class="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-semibold text-slate-600 border border-slate-200">Inactif</span>
          }
          <button type="button" (click)="selectedArticle.set(null)" class="rounded-lg p-1.5 text-slate-400 hover:bg-slate-200 hover:text-slate-700">
            <svg class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" /></svg>
          </button>
        </div>
      </div>

      <!-- Corps (défilable) -->
      <div class="overflow-y-auto p-6 space-y-6">
        <!-- 4 indicateurs clés -->
        <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <div class="rounded-xl border border-slate-200 bg-slate-50/50 p-3.5">
            <p class="text-[11px] font-medium text-slate-500">Stock courant</p>
            <p class="mt-1 text-lg font-bold" [class.text-rose-600]="sousLeSeuil(art)" [class.text-slate-900]="!sousLeSeuil(art)">
              {{ art.quantiteStock }} <span class="text-xs font-normal text-slate-500">{{ art.uniteMesure }}</span>
            </p>
            @if (sousLeSeuil(art)) {
              <span class="mt-1 inline-flex items-center rounded bg-rose-100 px-1.5 py-0.5 text-[10px] font-semibold text-rose-700">Stock critique</span>
            }
          </div>
          <div class="rounded-xl border border-slate-200 bg-slate-50/50 p-3.5">
            <p class="text-[11px] font-medium text-slate-500">Seuil de réappro.</p>
            <p class="mt-1 text-lg font-bold text-slate-900">{{ art.seuilReapprovisionnement ?? '—' }} <span class="text-xs font-normal text-slate-500">{{ art.uniteMesure }}</span></p>
          </div>
          <div class="rounded-xl border border-slate-200 bg-slate-50/50 p-3.5">
            <p class="text-[11px] font-medium text-slate-500">PUMP unitaire</p>
            <p class="mt-1 text-lg font-bold text-slate-900 font-mono">{{ art.prixUnitaireMoyenPondere != null ? (art.prixUnitaireMoyenPondere | number:'1.0-0') : '0' }} <span class="text-xs font-normal text-slate-500">FCFA</span></p>
          </div>
          <div class="rounded-xl border border-slate-200 bg-slate-50/50 p-3.5">
            <p class="text-[11px] font-medium text-slate-500">Valeur totale stock</p>
            <p class="mt-1 text-lg font-bold text-emerald-700 font-mono">{{ (valeurStock(art) | number:'1.0-0') }} <span class="text-xs font-normal text-emerald-600">FCFA</span></p>
          </div>
        </div>

        @if (art.description) {
          <div class="rounded-xl border border-slate-200 bg-slate-50/30 p-3.5">
            <h4 class="text-xs font-semibold text-slate-700">Description</h4>
            <p class="mt-1 text-xs text-slate-600 leading-relaxed">{{ art.description }}</p>
          </div>
        }

        <!-- Section Historique des mouvements -->
        <div>
          <div class="flex items-center justify-between mb-3">
            <div>
              <h3 class="text-sm font-bold text-slate-900">Historique des mouvements</h3>
              <p class="text-xs text-slate-500">Dernières entrées, sorties et ajustements sur cet article</p>
            </div>
            @if (auth.hasPermission('mouvement_stock.creer')) {
              <a routerLink="/stock/mouvements" (click)="selectedArticle.set(null)" class="rounded-lg bg-green-700 px-3 py-1.5 text-xs font-semibold text-white hover:bg-green-800 transition">
                ＋ Saisir un mouvement
              </a>
            }
          </div>

          @if (loadingMouvements()) {
            <div class="rounded-xl border border-slate-200 p-8 text-center text-xs text-slate-500">Chargement de l'historique…</div>
          } @else if (articleMouvements().length === 0) {
            <div class="rounded-xl border border-dashed border-slate-300 p-8 text-center">
              <p class="text-xs text-slate-500">Aucun mouvement enregistré pour cet article.</p>
            </div>
          } @else {
            <div class="overflow-x-auto rounded-xl border border-slate-200">
              <table class="w-full text-left text-xs">
                <thead class="bg-slate-50 text-slate-600">
                  <tr>
                    <th class="p-2.5">Date</th>
                    <th class="p-2.5">Type</th>
                    <th class="p-2.5 text-right">Quantité</th>
                    <th class="p-2.5 text-center">Stock</th>
                    <th class="p-2.5">Réf. document</th>
                    <th class="p-2.5">Auteur</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-200">
                  @for (m of articleMouvements(); track m.id) {
                    <tr class="hover:bg-slate-50/50">
                      <td class="p-2.5 text-slate-700 whitespace-nowrap">{{ m.createdAt | date:'dd/MM/yyyy HH:mm' }}</td>
                      <td class="p-2.5">
                        @if (m.typeMouvement === 'ENTREE') {
                          <span class="inline-flex items-center rounded-full bg-emerald-50 px-2 py-0.5 text-[10px] font-bold text-emerald-700 border border-emerald-200">＋ ENTREE</span>
                        } @else if (m.typeMouvement === 'SORTIE') {
                          <span class="inline-flex items-center rounded-full bg-rose-50 px-2 py-0.5 text-[10px] font-bold text-rose-700 border border-rose-200">− SORTIE</span>
                        } @else {
                          <span class="inline-flex items-center rounded-full bg-purple-50 px-2 py-0.5 text-[10px] font-bold text-purple-700 border border-purple-200">⚙ AJUSTEMENT</span>
                        }
                      </td>
                      <td class="p-2.5 text-right font-mono font-bold" [class.text-emerald-700]="m.typeMouvement === 'ENTREE'" [class.text-rose-600]="m.typeMouvement === 'SORTIE'">
                        {{ m.typeMouvement === 'ENTREE' ? '+' : m.typeMouvement === 'SORTIE' ? '-' : '' }}{{ m.quantite }}
                      </td>
                      <td class="p-2.5 text-center font-mono text-slate-600 whitespace-nowrap">
                        <span class="text-slate-500">{{ m.stockAnterieur }}</span>
                        <span class="mx-1 text-slate-400">→</span>
                        <span class="font-bold text-slate-800">{{ m.nouveauStock }}</span>
                      </td>
                      <td class="p-2.5 font-mono text-slate-700">{{ m.referenceDocument || '—' }}</td>
                      <td class="p-2.5 text-slate-600">{{ m.auteur ? m.auteur.prenom + ' ' + m.auteur.nom : '—' }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </div>
      </div>

      <!-- Pied de page -->
      <div class="flex items-center justify-end gap-2 border-t border-slate-200 bg-slate-50 px-6 py-3">
        @if (sousLeSeuil(art) && auth.hasPermission('demande_achat.creer')) {
          <a [routerLink]="['/achats/demandes-achat']" [queryParams]="{ articleId: art.id }" (click)="selectedArticle.set(null)"
            class="rounded-xl bg-amber-600 px-4 py-2 text-xs font-semibold text-white hover:bg-amber-700 shadow-sm flex items-center gap-1.5 transition">
            <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"/></svg>
            Réapprovisionner (Créer DA)
          </a>
        }
        @if (auth.hasPermission('article.modifier')) {
          <button type="button" (click)="edit(art); selectedArticle.set(null)" class="rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50">
            Modifier l'article
          </button>
        }
        <button type="button" (click)="selectedArticle.set(null)" class="rounded-xl bg-slate-800 px-4 py-2 text-xs font-semibold text-white hover:bg-slate-700">
          Fermer
        </button>
      </div>
    </div>
  </div>
}
}
`,
})
export class ArticlesListComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly stock = inject(StockService);
  private readonly notifications = inject(NotificationService);

  readonly importFormats = IMPORT_FORMATS;

  readonly articles = signal<Article[]>([]);
  readonly kpis = signal<StockKpis | null>(null);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly total = signal(0);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly formOpen = signal(false);
  readonly importOpen = signal(false);
  readonly editingId = signal('');
  readonly selectedArticle = signal<Article | null>(null);
  readonly articleMouvements = signal<MouvementStock[]>([]);
  readonly loadingMouvements = signal(false);

  readonly exportIds = computed(() => this.articles().map((a) => a.id));

  codeFilter = '';
  designationFilter = '';
  sousSeuil = false;
  draft: ArticleRequest = this.emptyDraft();

  ngOnInit(): void {
    this.load();
    this.loadKpis();
  }

  loadKpis(): void {
    this.stock.obtenirKpis().subscribe({
      next: (k) => this.kpis.set(k),
      error: () => {},
    });
  }

  filtrerSousSeuil(): void {
    this.sousSeuil = !this.sousSeuil;
    this.search();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.stock.listerArticles({
      page: this.page(), size: 20, sort: 'codeArticle,asc',
      codeArticle: this.codeFilter.trim(), designation: this.designationFilter.trim(),
      sousSeuil: this.sousSeuil ? true : undefined,
    }).subscribe({
      next: (res) => { this.articles.set(res.content); this.totalPages.set(res.totalPages); this.total.set(res.totalElements); this.loading.set(false); },
      error: (e) => { this.loading.set(false); this.error.set(errorMessage(e)); },
    });
  }

  search(): void { this.page.set(0); this.load(); }
  reset(): void { this.codeFilter = ''; this.designationFilter = ''; this.sousSeuil = false; this.search(); }
  goTo(p: number): void { this.page.set(p); this.load(); }

  sousLeSeuil(a: Article): boolean {
    return a.seuilReapprovisionnement != null && a.quantiteStock <= a.seuilReapprovisionnement;
  }

  valeurStock(a: Article): number {
    return (a.quantiteStock || 0) * (a.prixUnitaireMoyenPondere || 0);
  }

  viewArticle(a: Article): void {
    this.selectedArticle.set(a);
    this.loadingMouvements.set(true);
    this.stock.listerMouvements({ articleId: a.id, size: 50, sort: 'createdAt,desc' }).subscribe({
      next: (res) => {
        this.articleMouvements.set(res.content);
        this.loadingMouvements.set(false);
      },
      error: () => {
        this.articleMouvements.set([]);
        this.loadingMouvements.set(false);
      },
    });
  }

  openForm(): void {
    this.editingId.set('');
    this.draft = this.emptyDraft();
    this.formOpen.set(true);
  }

  edit(a: Article): void {
    this.editingId.set(a.id);
    this.draft = {
      codeArticle: a.codeArticle,
      designation: a.designation,
      description: a.description || '',
      uniteMesure: a.uniteMesure,
      seuilReapprovisionnement: a.seuilReapprovisionnement,
      prixUnitaireMoyenPondere: a.prixUnitaireMoyenPondere || 0,
      actif: a.actif,
    };
    this.formOpen.set(true);
  }

  save(): void {
    this.saving.set(true);
    const op = this.editingId()
      ? this.stock.modifierArticle(this.editingId(), this.draft)
      : this.stock.creerArticle(this.draft);
    op.subscribe({
      next: () => {
        this.saving.set(false);
        this.formOpen.set(false);
        this.notifications.success(this.editingId() ? 'Article mis à jour.' : 'Article créé.');
        this.load();
        this.loadKpis();
      },
      error: (e) => {
        this.saving.set(false);
        this.notifications.error(errorMessage(e));
      },
    });
  }

  onImported(): void {
    this.importOpen.set(false);
    this.notifications.success('Import des articles terminé avec succès.');
    this.load();
    this.loadKpis();
  }

  private emptyDraft(): ArticleRequest {
    return {
      codeArticle: '',
      designation: '',
      description: '',
      uniteMesure: 'Unité',
      seuilReapprovisionnement: 10,
      prixUnitaireMoyenPondere: 0,
      actif: true,
    };
  }
}
