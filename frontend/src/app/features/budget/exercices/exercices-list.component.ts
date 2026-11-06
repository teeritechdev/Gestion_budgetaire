import { PageHeaderComponent } from '../../../shared/components/page-header.component';
import { IMPORT_FORMATS } from '../../../core/models/import-formats';
import { ImportCsvPageComponent } from '../../../shared/components/import-csv-page.component';
import { DataActionsComponent } from '../../../shared/components/data-actions.component';
import { FormPageComponent } from '../../../shared/components/form-page.component';
import { RecordDetailsDialogComponent } from '../../../shared/components/record-details-dialog.component';
import { DatePickerComponent } from '../../../shared/components/date-picker.component';
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { SearchOptionDirective, SearchSelectComponent } from '../../../shared/components/search-select.component';
import { BudgetService } from '../../../core/services/budget.service';
import { NotificationService } from '../../../core/services/notification.service';
import { AuthService } from '../../../core/services/auth.service';
import { PlanStrategique } from '../../../core/models/budget/plan-strategique.model';
import {
  ExerciceBudgetaire,
  ExerciceBudgetaireRequestDto,
} from '../../../core/models/budget/exercice-budgetaire.model';

@Component({
  selector: 'app-exercices-list',
  standalone: true,
  imports: [SearchSelectComponent, SearchOptionDirective, PageHeaderComponent, DataActionsComponent, ImportCsvPageComponent, FormPageComponent, 
    RecordDetailsDialogComponent,
    DatePickerComponent,
    CommonModule,
    FormsModule,
  ],
  template: `
@if (importOpen()) {
<app-import-csv-page section="Exercices budgétaires" ressource="exercices-budgetaires" [format]="importFormats.exercices" (back)="importOpen.set(false)" (imported)="refresh()" />
} @else {
@if (modal()) {
<app-form-page section="Exercices budgétaires" [title]="editing() ? 'Modifier l’exercice budgétaire' : 'Créer un exercice budgétaire'" [mode]="editing() ? 'Modification' : 'Création'" subtitle="Année, plan budgétaire de rattachement, enveloppe votée et période d’ouverture." (back)="close()">
<form
          (ngSubmit)="save()"
          novalidate
          class="form-page-card space-y-5"
        >
          @if (saveError()) {
            <p
              role="alert"
              aria-live="assertive"
              class="rounded-lg border border-rose-300 bg-rose-50 p-3 text-sm text-rose-800"
            >
              {{ saveError() }}
            </p>
          }
          <div class="grid gap-4 sm:grid-cols-2">
            <label class="space-y-1.5 text-xs text-slate-600"
              >Année <span class="champ-obligatoire" aria-hidden="true">*</span><input
                required
                type="number"
                min="2000"
                max="2200"
                [(ngModel)]="form.annee"
                name="annee"
                class="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2.5 text-sm text-white outline-none focus:border-blue-500" /></label
            ><label class="space-y-1.5 text-xs text-slate-600"
              >Statut<select
                [(ngModel)]="form.statut"
                name="statut"
                class="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2.5 text-sm text-white outline-none focus:border-blue-500"
              >
                <option value="PREPARATION">Préparation</option>
                <option value="OUVERT">Ouvert</option>
                <option value="CLOTURE">Clôturé</option>
              </select></label
            ><label class="space-y-1.5 text-xs text-slate-600 sm:col-span-2"
              >Plan budgétaire <span class="champ-obligatoire" aria-hidden="true">*</span><app-search-select
                required
                [(ngModel)]="form.planId"
                name="planId"
                placeholder="Sélectionner un plan"
                triggerClass="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2.5 text-sm text-white outline-none focus:border-blue-500"
              >
                @for (plan of plans(); track plan.id) {
                  <app-search-option [value]="plan.id" [label]="plan.codePlan + ' — ' + plan.libelle" />
                }
              </app-search-select>
              @if (!plans().length) {
                <span class="mt-1 block text-[10px] text-amber-300"
                  >Créez d’abord un plan budgétaire.</span
                >
              }</label
            ><label class="space-y-1.5 text-xs text-slate-600 sm:col-span-2"
              >Montant total voté (FCFA) <span class="champ-obligatoire" aria-hidden="true">*</span><input
                required
                type="number"
                min="0"
                step="1"
                [(ngModel)]="form.montantTotalVote"
                name="montantTotalVote"
                class="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2.5 text-sm text-white outline-none focus:border-blue-500" /></label
            ><label class="space-y-1.5 text-xs text-slate-600"
              >Date d’ouverture<app-date-picker
                [(ngModel)]="form.dateOuverture"
                name="dateOuverture" /></label
            ><label class="space-y-1.5 text-xs text-slate-600"
              >Date de fermeture<app-date-picker
                [min]="form.dateOuverture || undefined"
                [(ngModel)]="form.dateFermeture"
                name="dateFermeture"
            /></label>
          </div>
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
              {{ saving() ? 'Enregistrement…' : editing() ? 'Enregistrer' : 'Créer l’exercice' }}
            </button>
          </div>
        </form>
      
</app-form-page>
} @else {
    <div class="mx-auto max-w-7xl space-y-6">
      <app-page-header icon="exercices" module="Gestion budgétaire" title="Exercices budgétaires" description="Ouvrez, suivez et clôturez chaque année budgétaire avec son enveloppe votée.">
<app-data-actions ressource="exercices-budgetaires" exportPermission="exercice.exporter" [ids]="exportIds()" [importPermissions]="['exercice.importer']" [importFormat]="importFormats.exercices" (importer)="importOpen.set(true)" />
          @if (canCreate()) {
            <button
              (click)="openCreate()"
              class="rounded-xl bg-green-700 px-4 py-2 text-xs font-semibold text-white hover:bg-green-600"
            >
              ＋ Nouvel exercice
            </button>
          }
</app-page-header>
      <div class="grid gap-4 sm:grid-cols-3">
        <div class="rounded-2xl border border-slate-200 bg-white p-4">
          <p class="text-xs text-slate-600">Exercices suivis</p>
          <p class="mt-2 text-2xl font-bold text-white">{{ exercices().length }}</p>
        </div>
        <div class="rounded-2xl border border-slate-200 bg-white p-4">
          <p class="text-xs text-slate-600">Budgets ouverts</p>
          <p class="mt-2 text-2xl font-bold text-blue-300">{{ ouverts() }}</p>
        </div>
        <div class="rounded-2xl border border-slate-200 bg-white p-4">
          <p class="text-xs text-slate-600">Total voté</p>
          <p class="mt-2 truncate text-xl font-bold text-white">
            {{ totalVote() | number: '1.0-0' }} FCFA
          </p>
        </div>
      </div>
      <section class="overflow-hidden rounded-2xl border border-slate-200 bg-white">
        <div class="flex flex-col gap-3 border-b border-slate-200 p-4 sm:flex-row">
          <input
            type="number"
            [(ngModel)]="yearFilter"
            placeholder="Filtrer par année"
            class="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-white outline-none focus:border-blue-600 sm:w-52"
          /><select
            [(ngModel)]="statusFilter"
            class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-700"
          >
            <option value="tous">Tous les statuts</option>
            <option value="PREPARATION">Préparation</option>
            <option value="OUVERT">Ouvert</option>
            <option value="CLOTURE">Clôturé</option></select
          ><button
            (click)="refresh()"
            class="rounded-xl border border-slate-300 px-3 py-2 text-xs text-slate-700 hover:bg-slate-100"
          >
            ↻ Actualiser
          </button>
        </div>
        @if (error()) {
          <p class="m-4 rounded-lg bg-rose-500/10 p-3 text-xs text-rose-300">{{ error() }}</p>
        }
        @if (plansError()) {
          <p role="alert" class="m-4 rounded-lg bg-rose-500/10 p-3 text-xs text-rose-300">
            {{ plansError() }}
          </p>
        }
        <div class="overflow-x-auto">
          <table class="w-full min-w-[760px] text-left text-xs">
            <thead class="bg-slate-50/80 text-[10px] uppercase tracking-wider text-slate-500">
              <tr>
                <th class="px-5 py-3">Exercice</th>
                <th class="px-4 py-3">Plan budgétaire</th>
                <th class="px-4 py-3">Budget voté</th>
                <th class="px-4 py-3">Statut</th>
                <th class="px-5 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-200/80">
              @for (exercice of filtered(); track exercice.id) {
                <tr class="hover:bg-slate-50">
                  <td class="px-5 py-4">
                    <p class="font-semibold text-white">{{ exercice.annee }}</p>
                    <p class="mt-1 text-[10px] text-slate-500">
                      {{ exercice.dateOuverture || 'Date non définie' }}
                    </p>
                  </td>
                  <td class="px-4 py-4">
                    <p class="text-slate-800">{{ exercice.planStrategique?.libelle || '—' }}</p>
                    <p class="mt-1 font-mono text-[10px] text-slate-500">
                      {{ exercice.planStrategique?.codePlan || '' }}
                    </p>
                  </td>
                  <td class="px-4 py-4 font-medium text-white">
                    {{ exercice.montantTotalVote | number: '1.0-0' }} FCFA
                  </td>
                  <td class="px-4 py-4">
                    <span
                      class="rounded-full px-2.5 py-1 text-[10px]"
                      [class.bg-blue-500/10]="exercice.statut === 'OUVERT'"
                      [class.text-blue-300]="exercice.statut === 'OUVERT'"
                      [class.bg-amber-500/10]="exercice.statut === 'PREPARATION'"
                      [class.text-amber-300]="exercice.statut === 'PREPARATION'"
                      [class.bg-slate-100]="exercice.statut === 'CLOTURE'"
                      [class.text-slate-600]="exercice.statut === 'CLOTURE'"
                      >{{ statusLabel(exercice.statut) }}</span
                    >
                  </td>
                  <td class="px-5 py-4">
                    <div class="flex justify-end gap-2">
                      <button
                        type="button"
                        (click)="showDetails('Exercice budgétaire', exercice)"
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
                      @if (canEdit()) {
                        <button
                          (click)="openEdit(exercice)"
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
                      @if (canDelete()) {
                        <button
                          (click)="remove(exercice)"
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
                  <td colspan="5" class="px-5 py-14 text-center">
                    <p class="text-sm font-medium text-white">Aucun exercice trouvé</p>
                    <p class="mt-1 text-xs text-slate-500">
                      Créez un exercice puis rattachez-le à un plan budgétaire.
                    </p>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <div class="border-t border-slate-200 px-5 py-3 text-[10px] text-slate-500">
          {{ filtered().length }} exercice(s) affiché(s)
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
export class ExercicesListComponent implements OnInit {
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

  private readonly service = inject(BudgetService);
  private readonly notifications = inject(NotificationService);
  private readonly auth = inject(AuthService);
  readonly exercices = signal<ExerciceBudgetaire[]>([]);
  readonly plans = signal<PlanStrategique[]>([]);
  readonly modal = signal(false);
  readonly editing = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly saveError = signal('');
  readonly plansError = signal('');
  yearFilter: number | null = null;
  statusFilter = 'tous';
  private selectedId = '';
  form: ExerciceBudgetaireRequestDto = this.emptyForm();
  ngOnInit(): void {
    this.refresh();
    this.plansError.set('');
    if (!this.auth.hasPermission('plan.lire')) {
      this.plans.set([]);
      return;
    }
    this.service
      .listerPlans()
      .subscribe({
        next: (p) => this.plans.set(p),
        error: (error: HttpErrorResponse) => {
          const message = this.describeError('charger les plans budgétaires', error);
          this.plansError.set(message);
          this.notifications.error(message);
        },
      });
  }
  canCreate(): boolean {
    return this.auth.hasPermission('exercice.creer') && this.auth.hasPermission('plan.lire');
  }
  canEdit(): boolean {
    return this.auth.hasPermission('exercice.modifier') && this.auth.hasPermission('plan.lire');
  }
  canDelete(): boolean {
    return this.auth.hasPermission('exercice.supprimer');
  }
  ouverts(): number {
    return this.exercices().filter((e) => e.statut === 'OUVERT').length;
  }
  totalVote(): number {
    return this.exercices().reduce((s, e) => s + Number(e.montantTotalVote || 0), 0);
  }
  filtered(): ExerciceBudgetaire[] {
    return this.exercices().filter(
      (e) =>
        (!this.yearFilter || e.annee === Number(this.yearFilter)) &&
        (this.statusFilter === 'tous' || e.statut === this.statusFilter),
    );
  }
  refresh(): void {
    this.error.set('');
    this.service
      .listerExercices()
      .subscribe({
        next: (data) => this.exercices.set([...data].sort((a, b) => b.annee - a.annee)),
        error: (error: HttpErrorResponse) =>
          this.error.set(this.describeError('charger les exercices budgétaires', error)),
      });
  }
  openCreate(): void {
    this.selectedId = '';
    this.editing.set(false);
    this.saveError.set('');
    this.form = this.emptyForm();
    this.modal.set(true);
  }
  openEdit(item: ExerciceBudgetaire): void {
    this.selectedId = item.id || '';
    this.editing.set(true);
    this.saveError.set('');
    this.form = {
      annee: item.annee,
      planId: item.planStrategique?.id || '',
      statut: item.statut,
      montantTotalVote: item.montantTotalVote,
      dateOuverture: item.dateOuverture ? this.dateInputValue(item.dateOuverture) : '',
      dateFermeture: item.dateFermeture ? this.dateInputValue(item.dateFermeture) : '',
    };
    this.modal.set(true);
  }
  close(): void {
    if (!this.saving()) this.modal.set(false);
  }
  save(): void {
    if (this.saving()) return;
    this.saveError.set('');
    if (
      !Number.isInteger(this.form.annee) ||
      this.form.annee < 2000 ||
      this.form.annee > 2200
    ) {
      this.showSaveError('Saisissez une année comprise entre 2000 et 2200.');
      return;
    }
    if (!this.form.planId || !this.plans().some((plan) => plan.id === this.form.planId)) {
      this.showSaveError('Sélectionnez un plan budgétaire disponible.');
      return;
    }
    if (
      !Number.isFinite(Number(this.form.montantTotalVote)) ||
      this.form.montantTotalVote < 0
    ) {
      this.showSaveError('Le montant total voté doit être un nombre positif ou nul.');
      return;
    }
    if (
      this.form.dateOuverture &&
      this.form.dateFermeture &&
      this.form.dateFermeture < this.form.dateOuverture
    ) {
      this.showSaveError('La date de fermeture doit suivre la date d’ouverture.');
      return;
    }
    this.saving.set(true);
    const req = this.editing()
      ? this.service.modifierExercice(this.selectedId, this.form)
      : this.service.creerExercice(this.form);
    req.subscribe({
      next: () => {
        this.notifications.success(this.editing() ? 'Exercice mis à jour.' : 'Exercice créé.');
        this.modal.set(false);
        this.saving.set(false);
        this.refresh();
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        this.showSaveError(
          this.describeError(this.editing() ? 'modifier l’exercice' : 'créer l’exercice', error),
        );
      },
    });
  }
  remove(item: ExerciceBudgetaire): void {
    if (!item.id || !confirm(`Supprimer l’exercice ${item.annee} ?`)) return;
    this.service.supprimerExercice(item.id).subscribe({
      next: () => {
        this.notifications.success('Exercice supprimé.');
        this.refresh();
      },
      error: (error: HttpErrorResponse) =>
        this.notifications.error(this.describeError('supprimer l’exercice budgétaire', error)),
    });
  }
  statusLabel(status: string): string {
    return (
      (
        { PREPARATION: 'Préparation', OUVERT: 'Ouvert', CLOTURE: 'Clôturé' } as Record<
          string,
          string
        >
      )[status] || status
    );
  }
  private dateInputValue(value: string): string {
    return value.slice(0, 10);
  }
  private showSaveError(message: string): void {
    this.saveError.set(message);
    this.notifications.error(message);
  }
  private describeError(operation: string, error: HttpErrorResponse): string {
    if (error.status === 0)
      return `Le serveur est injoignable pendant l’action « ${operation} ». Vérifiez la connexion puis réessayez.`;
    if (error.status === 401) return `Votre session a expiré. Reconnectez-vous pour ${operation}.`;
    if (error.status === 403)
      return `Accès refusé : vous n’avez pas les droits nécessaires pour ${operation}.`;
    const body = error.error;
    const detail =
      typeof body?.message === 'string'
        ? body.message
        : typeof body?.detail === 'string'
          ? body.detail
          : typeof body === 'string'
            ? body
            : '';
    return `${operation} a échoué (HTTP ${error.status}). ${detail || error.statusText || 'Consultez les journaux du serveur.'}`;
  }
  private emptyForm(): ExerciceBudgetaireRequestDto {
    return {
      annee: new Date().getFullYear(),
      planId: '',
      statut: 'PREPARATION',
      montantTotalVote: 0,
      dateOuverture: '',
      dateFermeture: '',
    };
  }
}
