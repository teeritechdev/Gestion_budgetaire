import { PageHeaderComponent } from '../../../shared/components/page-header.component';
import { IMPORT_FORMATS } from '../../../core/models/import-formats';
import { ImportCsvPageComponent } from '../../../shared/components/import-csv-page.component';
import { DataActionsComponent } from '../../../shared/components/data-actions.component';
import { FormPageComponent } from '../../../shared/components/form-page.component';
import { RecordDetailsDialogComponent } from '../../../shared/components/record-details-dialog.component';
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { SearchOptionDirective, SearchSelectComponent } from '../../../shared/components/search-select.component';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { BudgetService } from '../../../core/services/budget.service';
import { StructureService } from '../../../core/services/structure.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Structure } from '../../../core/models/structure.model';
import {
  EntiteStatistiqueCle,
  EntiteStatistiqueCleRequest,
} from '../../../core/models/budget/entite-statistique-cle.model';

@Component({
  selector: 'app-statistiques-cles',
  standalone: true,
  imports: [SearchSelectComponent, SearchOptionDirective, PageHeaderComponent, DataActionsComponent, ImportCsvPageComponent, FormPageComponent, RecordDetailsDialogComponent, CommonModule, FormsModule],
  template: `
@if (importOpen()) {
<app-import-csv-page section="Clés statistiques" ressource="statistiques-cles" [format]="importFormats.statistiques" (back)="importOpen.set(false)" (imported)="refresh()" />
} @else {
@if (modal()) {
<app-form-page section="Clés statistiques" [title]="editing() ? 'Modifier une mesure statistique' : 'Ajouter une mesure statistique'" [mode]="editing() ? 'Modification' : 'Création'" subtitle="Effectif et chiffre d’affaires d’une entité, pour un mois ou pour l’année." (back)="close()">
<form
          novalidate
          (ngSubmit)="save()"
          class="form-page-card space-y-5"
        >
          @if (formError()) {
            <p role="alert" class="rounded-xl border border-rose-300 bg-rose-50 p-3 text-sm font-medium text-rose-900">
              {{ formError() }}
            </p>
          }
          <div class="grid gap-4 sm:grid-cols-2">
            <label class="space-y-1.5 text-xs text-slate-600"
              >Entité <span class="champ-obligatoire" aria-hidden="true">*</span><app-search-select
                required
                [(ngModel)]="form.entiteCode"
                name="entiteCode"
                placeholder="Sélectionner une entité…"
                triggerClass="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2.5 text-xs text-white"
              >
                @for (entity of entities(); track entity.code) {
                  <app-search-option [value]="entity.code" [label]="entity.code + ' — ' + entity.nom" />
                }
              </app-search-select></label
            ><label class="space-y-1.5 text-xs text-slate-600"
              >Année <span class="champ-obligatoire" aria-hidden="true">*</span><input
                required
                type="number"
                min="2000"
                max="2200"
                [(ngModel)]="form.anneeExercice"
                name="anneeExercice"
                class="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2.5 text-xs text-white" /></label
            ><label class="space-y-1.5 text-xs text-slate-600"
              >Période<select
                [(ngModel)]="form.mois"
                name="mois"
                class="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2.5 text-xs text-white"
              >
                <option [ngValue]="null">Année complète</option>
                @for (month of months; track month.value) {
                  <option [ngValue]="month.value">{{ month.label }}</option>
                }
              </select></label
            ><label class="space-y-1.5 text-xs text-slate-600"
              >Effectif agents<input
                type="number"
                min="0"
                step="1"
                [(ngModel)]="form.effectifAgents"
                name="effectifAgents"
                class="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2.5 text-xs text-white" /></label
            ><label class="space-y-1.5 text-xs text-slate-600 sm:col-span-2"
              >Chiffre d’affaires (FCFA)<input
                type="number"
                min="0"
                step="0.01"
                [(ngModel)]="form.chiffreAffaires"
                name="chiffreAffaires"
                class="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2.5 text-xs text-white"
            /></label>
          </div>
          <p class="rounded-lg bg-white p-3 text-[10px] leading-5 text-slate-500">
            Une mesure mensuelle est unique pour le couple entité/année/mois. Une mesure annuelle
            est enregistrée sans mois.
          </p>
          <div class="form-page-actions">
            <button
              type="button"
              (click)="close()"
              class="rounded-lg px-4 py-2 text-xs text-slate-600 hover:bg-slate-100"
            >
              Annuler</button
            ><button
              type="button"
              (click)="save()"
              [disabled]="saving()"
              class="rounded-lg bg-green-700 px-4 py-2 text-xs font-semibold text-white hover:bg-green-600 disabled:opacity-50"
            >
              {{ saving() ? 'Enregistrement…' : 'Enregistrer' }}
            </button>
          </div>
        </form>
      
</app-form-page>
} @else {
    <div class="mx-auto max-w-7xl space-y-6">
      <app-page-header icon="statistiques" module="Répartition & contrôle" title="Clés statistiques" description="Effectifs et chiffres d’affaires de référence qui alimentent les clés de répartition.">
<app-data-actions ressource="statistiques-cles" exportPermission="statistique_cle.exporter" [ids]="exportIds()" [importPermissions]="['statistique_cle.importer']" [importFormat]="importFormats.statistiques" (importer)="importOpen.set(true)" />
          @if (can('statistique_cle.creer')) {
            <button
              (click)="openCreate()"
              class="rounded-xl bg-green-700 px-4 py-2 text-xs font-semibold text-white hover:bg-green-600"
            >
              ＋ Ajouter une période
            </button>
          }
</app-page-header>
      @if (error()) {
        <p class="rounded-xl border border-amber-500/20 bg-amber-500/5 p-3 text-xs text-amber-200">
          {{ error() }}
        </p>
      }
      @if (entityLookupError()) {
        <p class="rounded-xl border border-amber-500/20 bg-amber-500/5 p-3 text-xs text-amber-200">
          {{ entityLookupError() }} Le formulaire de création nécessite l’accès à cette liste.
        </p>
      }
      <section class="stats-grid grid gap-3 sm:grid-cols-3">
        <article class="rounded-2xl border border-slate-200 bg-white p-4">
          <p class="text-[10px] uppercase tracking-wider text-slate-500">Mesures enregistrées</p>
          <p class="mt-2 text-2xl font-bold text-white">{{ entries().length }}</p>
        </article>
        <article class="rounded-2xl border border-slate-200 bg-white p-4">
          <p class="text-[10px] uppercase tracking-wider text-slate-500">Entités suivies</p>
          <p class="mt-2 text-2xl font-bold text-blue-200">{{ entityCount() }}</p>
        </article>
        <article class="rounded-2xl border border-slate-200 bg-white p-4">
          <p class="text-[10px] uppercase tracking-wider text-slate-500">Effectif consolidé</p>
          <p class="mt-2 text-2xl font-bold text-white">{{ totalStaff() | number: '1.0-0' }}</p>
        </article>
      </section>
      <section class="overflow-hidden rounded-2xl border border-slate-200 bg-white">
        <div class="grid gap-3 border-b border-slate-200 p-4 sm:grid-cols-[1fr_200px_220px_auto]">
          <input
            [(ngModel)]="search"
            placeholder="Rechercher une entité…"
            class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-xs text-white outline-none placeholder:text-slate-600 focus:border-blue-500"
          /><input
            type="number"
            [(ngModel)]="yearFilter"
            placeholder="Année"
            class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-xs text-white outline-none focus:border-blue-500"
          /><select
            [(ngModel)]="monthFilter"
            class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-xs text-slate-700"
          >
            <option value="">Tous les mois / annuel</option>
            @for (month of months; track month.value) {
              <option [value]="month.value">{{ month.label }}</option>
            }</select
          ><button
            (click)="refresh()"
            class="rounded-xl border border-slate-300 px-3 py-2 text-xs text-slate-700 hover:bg-slate-100"
          >
            ↻ Actualiser
          </button>
        </div>
        <div class="overflow-x-auto">
          <table class="w-full min-w-[760px] text-left text-xs">
            <thead class="bg-white text-[10px] uppercase tracking-wider text-slate-500">
              <tr>
                <th class="px-5 py-3">Période</th>
                <th class="px-4 py-3">Entité</th>
                <th class="px-4 py-3 text-right">Effectif agents</th>
                <th class="px-4 py-3 text-right">Chiffre d’affaires</th>
                <th class="px-5 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-200/70">
              @for (entry of filtered(); track entry.id) {
                <tr class="hover:bg-slate-50">
                  <td class="px-5 py-3">
                    <p class="font-semibold text-white">{{ entry.anneeExercice }}</p>
                    <p class="mt-1 text-[10px] text-slate-500">{{ monthLabel(entry.mois) }}</p>
                  </td>
                  <td class="px-4 py-3">
                    <p class="font-medium text-slate-800">{{ entityLabel(entry.entiteCode) }}</p>
                    <p class="mt-1 font-mono text-[9px] text-slate-500">{{ entry.entiteCode }}</p>
                  </td>
                  <td class="px-4 py-3 text-right text-slate-800">
                    {{ entry.effectifAgents ?? '—' }}
                  </td>
                  <td class="px-4 py-3 text-right text-slate-800">
                    {{
                      entry.chiffreAffaires == null
                        ? '—'
                        : (entry.chiffreAffaires | number: '1.0-0')
                    }} FCFA
                  </td>
                  <td class="px-5 py-3">
                    <div class="flex justify-end gap-2">
                      <button
                        type="button"
                        (click)="showDetails('Mesure statistique', entry)"
                        class="inline-flex h-10 w-10 items-center justify-center rounded-lg text-blue-700 transition-colors hover:bg-blue-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2"
                        title="Voir les détails"
                        aria-label="Voir les détails"
                      >
                        <svg
                          aria-hidden="true"
                          class="h-5 w-5"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          stroke-width="1.8"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                        >
                          <path
                            d="M2.25 12s3.5-6.5 9.75-6.5 9.75 6.5 9.75 6.5-3.5 6.5-9.75 6.5S2.25 12 2.25 12Z"
                          />
                          <circle cx="12" cy="12" r="3" />
                        </svg>
                      </button>
                      @if (can('statistique_cle.modifier')) {
                        <button
                          (click)="openEdit(entry)"
                          class="inline-flex h-10 w-10 items-center justify-center rounded-lg transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 text-slate-600 hover:bg-green-50 hover:text-green-700 focus-visible:ring-green-600"
                          title="Modifier"
                          aria-label="Modifier"
                        >
                          <svg
                            aria-hidden="true"
                            class="h-5 w-5"
                            fill="none"
                            viewBox="0 0 24 24"
                            stroke="currentColor"
                            stroke-width="1.8"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          >
                            <path
                              stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
                            />
                          </svg>
                        </button>
                      }
                      @if (can('statistique_cle.supprimer')) {
                        <button
                          (click)="remove(entry)"
                          class="inline-flex h-10 w-10 items-center justify-center rounded-lg transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 text-rose-600 hover:bg-rose-50 hover:text-rose-700 focus-visible:ring-rose-600"
                          title="Supprimer"
                          aria-label="Supprimer"
                        >
                          <svg
                            aria-hidden="true"
                            class="h-5 w-5"
                            fill="none"
                            viewBox="0 0 24 24"
                            stroke="currentColor"
                            stroke-width="1.8"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          >
                            <path d="M3 6h18M8 6V4h8v2m2 0-.8 14H6.8L6 6m4 4v6m4-6v6" />
                          </svg>
                        </button>
                      }
                    </div>
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="5" class="p-12 text-center">
                    <p class="text-sm font-medium text-white">Aucune clé statistique</p>
                    <p class="mt-1 text-xs text-slate-500">
                      Ajoutez les données d’une entité pour une année ou un mois.
                    </p>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <div class="border-t border-slate-200 px-5 py-3 text-[10px] text-slate-500">
          {{ filtered().length }} mesure(s) affichée(s)
        </div>
      </section>
    </div>
    
    @if (detailRecord) {
      <app-record-details-dialog
        [title]="detailTitle"
        [record]="detailRecord"
        (close)="detailRecord = null"
      />
    }
}
}

  `,
})
export class StatistiquesClesComponent implements OnInit {
  readonly importOpen = signal(false);
  readonly importFormats = IMPORT_FORMATS;
  /** Lignes affichées (filtres et tri appliqués) : ce sont elles qui sont exportées. */
  exportIds(): (string | undefined)[] {
    return this.filtered().map((item) => item.id);
  }
  detailTitle = '';
  detailRecord: object | null = null;
  showDetails(title: string, record: object): void {
    this.detailTitle = title;
    this.detailRecord = record;
  }

  private readonly budget = inject(BudgetService);
  private readonly entityService = inject(StructureService);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  readonly entries = signal<EntiteStatistiqueCle[]>([]);
  readonly entities = signal<Structure[]>([]);
  readonly modal = signal(false);
  readonly editing = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly entityLookupError = signal('');
  readonly formError = signal('');
  readonly months = [
    { value: 1, label: 'Janvier' },
    { value: 2, label: 'Février' },
    { value: 3, label: 'Mars' },
    { value: 4, label: 'Avril' },
    { value: 5, label: 'Mai' },
    { value: 6, label: 'Juin' },
    { value: 7, label: 'Juillet' },
    { value: 8, label: 'Août' },
    { value: 9, label: 'Septembre' },
    { value: 10, label: 'Octobre' },
    { value: 11, label: 'Novembre' },
    { value: 12, label: 'Décembre' },
  ];
  search = '';
  yearFilter: number | null = null;
  monthFilter = '';
  private editId = '';
  form: EntiteStatistiqueCleRequest = this.emptyForm();
  ngOnInit(): void {
    this.refresh();
  }
  can(permission: string): boolean {
    return this.auth.hasPermission(permission);
  }
  entityCount(): number {
    return new Set(this.entries().map((entry) => entry.entiteCode)).size;
  }
  totalStaff(): number {
    return this.entries().reduce((sum, entry) => sum + Number(entry.effectifAgents || 0), 0);
  }
  filtered(): EntiteStatistiqueCle[] {
    const q = this.search.trim().toLocaleLowerCase('fr');
    return [...this.entries()]
      .filter(
        (entry) =>
          (!this.yearFilter || entry.anneeExercice === Number(this.yearFilter)) &&
          (!this.monthFilter || String(entry.mois ?? '') === this.monthFilter) &&
          (!q ||
            `${entry.entiteCode} ${this.entityLabel(entry.entiteCode)}`
              .toLocaleLowerCase('fr')
              .includes(q)),
      )
      .sort(
        (a, b) => b.anneeExercice - a.anneeExercice || Number(b.mois || 0) - Number(a.mois || 0),
      );
  }
  entityLabel(code: string): string {
    const entity = this.entities().find((item) => item.code === code);
    return entity?.nom || code;
  }
  monthLabel(month?: number | null): string {
    return month
      ? this.months.find((item) => item.value === month)?.label || `Mois ${month}`
      : 'Annuel';
  }
  refresh(): void {
    this.error.set('');
    this.entityLookupError.set('');
    forkJoin({
      entries: this.budget.listerStatistiquesCles().pipe(
        catchError((error: HttpErrorResponse) => {
          this.error.set(this.describeApiError('mesures statistiques', error));
          return of([] as EntiteStatistiqueCle[]);
        }),
      ),
      entities: (this.auth.hasPermission('entite.lire')
        ? this.entityService.listerOuFiltrer()
        : of([] as Structure[])).pipe(
        catchError((error: HttpErrorResponse) => {
          this.entityLookupError.set(this.describeApiError('référentiel des entités', error));
          return of([] as Structure[]);
        }),
      ),
    }).subscribe((data) => {
      this.entries.set(data.entries);
      this.entities.set(data.entities);
    });
  }
  private describeApiError(resource: string, error: HttpErrorResponse): string {
    if (error.status === 0)
      return `Échec réseau/CORS lors du chargement des ${resource}. Vérifiez que le backend est démarré et autorise l’origine du front.`;
    if (error.status === 401)
      return `Session absente ou expirée pour charger les ${resource}. Reconnectez-vous.`;
    if (error.status === 403)
      return `Accès refusé au chargement des ${resource} (HTTP 403). Vérifiez les permissions du rôle.`;
    const detail =
      typeof error.error?.message === 'string' ? ` Détail : ${error.error.message}` : '';
    return `Échec du chargement des ${resource} (HTTP ${error.status}).${detail}`;
  }
  openCreate(): void {
    if (!this.entities().length) {
      this.notifications.warning(
        'La liste des entités est indisponible. Vérifiez la permission entite.lire.',
      );
      return;
    }
    this.editing.set(false);
    this.editId = '';
    this.form = this.emptyForm();
    this.formError.set('');
    this.modal.set(true);
  }
  openEdit(entry: EntiteStatistiqueCle): void {
    if (!this.entities().some((entity) => entity.code === entry.entiteCode)) {
      this.notifications.warning(
        'Impossible de modifier cette mesure sans charger son entité depuis le référentiel.',
      );
      return;
    }
    this.editing.set(true);
    this.editId = entry.id || '';
    this.form = {
      anneeExercice: entry.anneeExercice,
      mois: entry.mois ?? null,
      entiteCode: entry.entiteCode,
      effectifAgents: entry.effectifAgents ?? null,
      chiffreAffaires: entry.chiffreAffaires ?? null,
    };
    this.formError.set('');
    this.modal.set(true);
  }
  close(): void {
    if (!this.saving()) {
      this.modal.set(false);
      this.formError.set('');
    }
  }
  save(): void {
    if (this.saving()) return;
    this.formError.set('');
    if (!this.entities().some((entity) => entity.code === this.form.entiteCode)) {
      this.formError.set('Sélectionnez une entité existante dans la liste.');
      return;
    }
    if (
      !Number.isInteger(Number(this.form.anneeExercice)) ||
      this.form.anneeExercice < 2000 ||
      this.form.anneeExercice > 2200
    ) {
      this.formError.set('L’année doit être comprise entre 2000 et 2200.');
      return;
    }
    if (
      this.form.mois != null &&
      (!Number.isInteger(Number(this.form.mois)) || this.form.mois < 1 || this.form.mois > 12)
    ) {
      this.formError.set('Le mois doit être compris entre 1 et 12.');
      return;
    }
    if (
      this.form.effectifAgents != null &&
      (!Number.isInteger(Number(this.form.effectifAgents)) || this.form.effectifAgents < 0)
    ) {
      this.formError.set('L’effectif doit être un entier positif ou nul.');
      return;
    }
    if (
      this.form.chiffreAffaires != null &&
      (!Number.isFinite(Number(this.form.chiffreAffaires)) || this.form.chiffreAffaires < 0)
    ) {
      this.formError.set('Le chiffre d’affaires doit être un montant positif ou nul.');
      return;
    }
    const request = this.editing()
      ? this.budget.modifierStatistiqueCle(this.editId, this.form)
      : this.budget.creerStatistiqueCle(this.form);
    this.saving.set(true);
    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.modal.set(false);
        this.formError.set('');
        this.notifications.success(this.editing() ? 'Mesure mise à jour.' : 'Mesure enregistrée.');
        this.refresh();
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        this.formError.set(this.describeSaveError(error));
      },
    });
  }
  private describeSaveError(error: HttpErrorResponse): string {
    const detail = this.extractErrorDetail(error);
    if (error.status === 409)
      return detail || 'Une mesure existe déjà pour cette entité et cette période.';
    if (error.status === 0)
      return 'Échec réseau/CORS lors de l’enregistrement. Vérifiez que le backend est démarré.';
    if (error.status === 400)
      return detail || 'Les données saisies sont invalides. Vérifiez chaque champ.';
    if (error.status === 401) return 'Session absente ou expirée. Reconnectez-vous.';
    if (error.status === 403)
      return 'Accès refusé pour enregistrer cette mesure. Vérifiez les permissions du rôle.';
    return detail || `Échec de l’enregistrement (HTTP ${error.status}).`;
  }
  private extractErrorDetail(error: HttpErrorResponse): string {
    const body: unknown = error.error;
    if (typeof body === 'string') return body;
    if (!body || typeof body !== 'object') return '';
    const payload = body as Record<string, unknown>;
    const errors = payload['errors'];
    if (errors && typeof errors === 'object') {
      const messages = Object.values(errors as Record<string, unknown>)
        .flatMap((value) => (Array.isArray(value) ? value : [value]))
        .filter((value): value is string => typeof value === 'string');
      if (messages.length) return messages.join(' ');
    }
    for (const key of ['detail', 'message', 'title']) {
      if (typeof payload[key] === 'string' && payload[key]) return payload[key] as string;
    }
    return '';
  }
  remove(entry: EntiteStatistiqueCle): void {
    if (
      !entry.id ||
      !confirm(
        `Supprimer la mesure ${this.monthLabel(entry.mois)} ${entry.anneeExercice} de ${entry.entiteCode} ?`,
      )
    )
      return;
    this.budget.supprimerStatistiqueCle(entry.id).subscribe({
      next: () => {
        this.notifications.success('Mesure supprimée.');
        this.refresh();
      },
      error: () => this.notifications.error('Suppression impossible. Vérifiez vos droits.'),
    });
  }
  private download(blob: Blob, name: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = name;
    a.click();
    URL.revokeObjectURL(url);
  }
  private emptyForm(): EntiteStatistiqueCleRequest {
    return {
      anneeExercice: new Date().getFullYear(),
      mois: new Date().getMonth() + 1,
      entiteCode: '',
      effectifAgents: null,
      chiffreAffaires: null,
    };
  }
}
