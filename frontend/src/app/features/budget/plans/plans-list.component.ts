import { PageHeaderComponent } from '../../../shared/components/page-header.component';
import { AppIconComponent } from '../../../shared/components/app-icon.component';
import { IMPORT_FORMATS } from '../../../core/models/import-formats';
import { ImportCsvPageComponent } from '../../../shared/components/import-csv-page.component';
import { DataActionsComponent } from '../../../shared/components/data-actions.component';
import { FormPageComponent } from '../../../shared/components/form-page.component';
import { RecordDetailsDialogComponent } from '../../../shared/components/record-details-dialog.component';
import { NativeDatePickerDirective } from '../../../shared/directives/native-date-picker.directive';
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { BudgetService } from '../../../core/services/budget.service';
import { NotificationService } from '../../../core/services/notification.service';
import { AuthService } from '../../../core/services/auth.service';
import {
  PlanStrategique,
  PlanStrategiqueRequestDto,
} from '../../../core/models/budget/plan-strategique.model';

@Component({
  selector: 'app-plans-list',
  standalone: true,
  imports: [PageHeaderComponent, AppIconComponent, DataActionsComponent, ImportCsvPageComponent, FormPageComponent, 
    RecordDetailsDialogComponent,
    NativeDatePickerDirective,
    CommonModule,
    FormsModule,
    DatePipe,
  ],
  template: `
@if (importOpen()) {
<app-import-csv-page section="Plans budgétaires" ressource="plans-strategiques" [format]="importFormats.plans" (back)="importOpen.set(false)" (imported)="refresh()" />
} @else {
@if (modal()) {
<app-form-page section="Plans budgétaires" [title]="editing() ? 'Modifier le plan budgétaire' : 'Créer un plan budgétaire'" [mode]="editing() ? 'Modification' : 'Création'" subtitle="Identification, période de validité et statut du plan." (back)="close()">
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
              >Code du plan<input
                required
                maxlength="50"
                [(ngModel)]="form.codePlan"
                name="codePlan"
                placeholder="PS-2026"
                class="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2.5 text-sm text-white outline-none focus:border-blue-500" /></label
            ><label class="space-y-1.5 text-xs text-slate-600"
              >Libellé<input
                required
                maxlength="150"
                [(ngModel)]="form.libelle"
                name="libelle"
                placeholder="Plan budgétaire 2026–2030"
                class="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2.5 text-sm text-white outline-none focus:border-blue-500" /></label
            ><label class="space-y-1.5 text-xs text-slate-600"
              >Date de début<input
                required
                type="date"
                [(ngModel)]="form.dateDebut"
                name="dateDebut"
                class="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2.5 text-sm text-white outline-none focus:border-blue-500" /></label
            ><label class="space-y-1.5 text-xs text-slate-600"
              >Date de fin<input
                required
                type="date"
                [min]="form.dateDebut"
                [(ngModel)]="form.dateFin"
                name="dateFin"
                class="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2.5 text-sm text-white outline-none focus:border-blue-500"
            /></label>
          </div>
          <label class="flex items-center gap-2 text-xs text-slate-700"
            ><input
              type="checkbox"
              [(ngModel)]="form.actif"
              name="actif"
              class="accent-blue-500"
            />Plan actif</label
          >
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
              {{
                saving()
                  ? 'Enregistrement…'
                  : editing()
                    ? 'Enregistrer les changements'
                    : 'Créer le plan'
              }}
            </button>
          </div>
        </form>
      
</app-form-page>
} @else {
    <div class="mx-auto max-w-7xl space-y-6">
      <app-page-header icon="plans" module="Gestion budgétaire" title="Plans budgétaires" description="Fixez le cap pluriannuel : périodes, priorités et statut de chaque plan.">
<app-data-actions ressource="plans-strategiques" exportPermission="plan.exporter" [ids]="exportIds()" [importPermissions]="['plan.importer']" (importer)="importOpen.set(true)" />
          @if (canCreate()) {
            <button
              (click)="openCreate()"
              class="rounded-xl bg-green-700 px-4 py-2 text-xs font-semibold text-white shadow-lg shadow-blue-900/20 hover:bg-green-600"
            >
              ＋ Nouveau plan
            </button>
          }
</app-page-header>
      <section class="stats-grid grid gap-4 sm:grid-cols-3">
        <div class="rounded-2xl border border-slate-200 bg-white p-4">
          <p class="text-xs text-slate-600">Plans enregistrés</p>
          <p class="mt-2 text-2xl font-bold text-white">{{ plans().length }}</p>
        </div>
        <div class="rounded-2xl border border-slate-200 bg-white p-4">
          <p class="text-xs text-slate-600">Plans actifs</p>
          <p class="mt-2 text-2xl font-bold text-blue-300">{{ plansActifs() }}</p>
        </div>
        <div class="rounded-2xl border border-slate-200 bg-white p-4">
          <p class="text-xs text-slate-600">Période couverte</p>
          <p class="mt-2 text-2xl font-bold text-white">{{ periodeCouverte() }}</p>
        </div>
      </section>
      <section class="overflow-hidden rounded-2xl border border-slate-200 bg-white">
        <div class="flex flex-col gap-3 border-b border-slate-200 p-4 sm:flex-row">
          <input
            [(ngModel)]="search"
            placeholder="Rechercher par code ou libellé…"
            class="min-w-0 flex-1 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-white outline-none placeholder:text-slate-600 focus:border-blue-600"
          /><select
            [(ngModel)]="filter"
            class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-700"
          >
            <option value="tous">Tous les statuts</option>
            <option value="actif">Plans actifs</option>
            <option value="inactif">Plans inactifs</option></select
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
        <div class="overflow-x-auto">
          <table class="w-full min-w-[680px] text-left text-xs">
            <thead class="bg-slate-50/80 text-[10px] uppercase tracking-wider text-slate-500">
              <tr>
                <th class="px-5 py-3">Plan</th>
                <th class="px-4 py-3">Période</th>
                <th class="px-4 py-3">Statut</th>
                <th class="px-5 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-200/80">
              @for (plan of filteredPlans(); track plan.id) {
                <tr class="transition hover:bg-slate-50">
                  <td class="px-5 py-4">
                    <div class="flex items-center gap-3">
                      <span
                        class="flex h-9 w-9 items-center justify-center rounded-xl bg-blue-500/10 font-semibold text-blue-300"
                        ><app-icon name="plans" classe="h-5 w-5" /></span
                      >
                      <div>
                        <p class="font-semibold text-white">{{ plan.libelle }}</p>
                        <p class="mt-1 font-mono text-[10px] text-slate-500">{{ plan.codePlan }}</p>
                      </div>
                    </div>
                  </td>
                  <td class="px-4 py-4 text-slate-700">
                    {{ plan.dateDebut | date: 'dd/MM/yyyy' }}
                    <span class="px-1 text-slate-600">→</span>
                    {{ plan.dateFin | date: 'dd/MM/yyyy' }}
                  </td>
                  <td class="px-4 py-4">
                    <span
                      class="rounded-full px-2.5 py-1 text-[10px]"
                      [class.bg-blue-500/10]="plan.actif"
                      [class.text-blue-300]="plan.actif"
                      [class.bg-slate-100]="!plan.actif"
                      [class.text-slate-600]="!plan.actif"
                      >{{ plan.actif ? 'Actif' : 'Inactif' }}</span
                    >
                  </td>
                  <td class="px-5 py-4">
                    <div class="flex justify-end gap-2">
                      <button
                        type="button"
                        (click)="showDetails('Plan budgétaire', plan)"
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
                          (click)="openEdit(plan)"
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
                          (click)="remove(plan)"
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
                  <td colspan="4" class="px-5 py-14 text-center">
                    <div
                      class="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-100 text-xl text-slate-500"
                    >
                      <app-icon name="plans" classe="h-5 w-5" />
                    </div>
                    <p class="text-sm font-medium text-white">Aucun plan trouvé</p>
                    <p class="mt-1 text-xs text-slate-500">
                      Essayez une autre recherche ou créez un plan budgétaire.
                    </p>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <div class="border-t border-slate-200 px-5 py-3 text-[10px] text-slate-500">
          {{ filteredPlans().length }} plan(s) affiché(s)
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
export class PlansListComponent implements OnInit {
  readonly importOpen = signal(false);
  readonly importFormats = IMPORT_FORMATS;
  /** Lignes affichées (filtres et tri appliqués) : ce sont elles qui sont exportées. */
  exportIds(): (string | undefined)[] {
    return this.filteredPlans().map((item) => item.id);
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
  readonly plans = signal<PlanStrategique[]>([]);
  readonly modal = signal(false);
  readonly editing = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly saveError = signal('');
  search = '';
  filter = 'tous';
  private selectedId = '';
  form: PlanStrategiqueRequestDto = this.emptyForm();
  ngOnInit(): void {
    this.refresh();
  }
  canCreate(): boolean {
    return this.auth.hasPermission('plan.creer');
  }
  canEdit(): boolean {
    return this.auth.hasPermission('plan.modifier');
  }
  canDelete(): boolean {
    return this.auth.hasPermission('plan.supprimer');
  }
  plansActifs(): number {
    return this.plans().filter((p) => p.actif).length;
  }
  periodeCouverte(): string {
    const years = this.plans()
      .map((p) => Number(p.dateDebut?.slice(0, 4)))
      .filter(Boolean);
    return years.length ? `${Math.min(...years)} – ${Math.max(...years)}` : '—';
  }
  filteredPlans(): PlanStrategique[] {
    const q = this.search.trim().toLocaleLowerCase('fr');
    return this.plans().filter(
      (p) =>
        (!q || `${p.codePlan} ${p.libelle}`.toLocaleLowerCase('fr').includes(q)) &&
        (this.filter === 'tous' || (this.filter === 'actif' ? p.actif : !p.actif)),
    );
  }
  refresh(): void {
    this.error.set('');
    this.service
      .listerPlans()
      .subscribe({
        next: (data) => this.plans.set(data),
        error: (error: HttpErrorResponse) =>
          this.error.set(this.describeError('charger les plans', error)),
      });
  }
  openCreate(): void {
    this.selectedId = '';
    this.editing.set(false);
    this.saveError.set('');
    this.form = this.emptyForm();
    this.modal.set(true);
  }
  openEdit(plan: PlanStrategique): void {
    this.selectedId = plan.id || '';
    this.editing.set(true);
    this.saveError.set('');
    this.form = {
      codePlan: plan.codePlan,
      libelle: plan.libelle,
      dateDebut: this.dateInputValue(plan.dateDebut),
      dateFin: this.dateInputValue(plan.dateFin),
      actif: plan.actif,
    };
    this.modal.set(true);
  }
  close(): void {
    if (!this.saving()) this.modal.set(false);
  }
  save(): void {
    if (this.saving()) return;
    this.saveError.set('');
    const codePlan = this.form.codePlan.trim();
    const libelle = this.form.libelle.trim();
    if (!codePlan || !libelle || !this.form.dateDebut || !this.form.dateFin) {
      this.showSaveError('Renseignez le code, le libellé, la date de début et la date de fin.');
      return;
    }
    if (this.form.dateFin < this.form.dateDebut) {
      this.showSaveError('La date de fin doit être postérieure ou égale à la date de début.');
      return;
    }
    this.form = { ...this.form, codePlan, libelle };
    this.saving.set(true);
    const request = this.editing()
      ? this.service.modifierPlan(this.selectedId, this.form)
      : this.service.creerPlan(this.form);
    request.subscribe({
      next: () => {
        this.notifications.success(this.editing() ? 'Plan mis à jour.' : 'Plan créé.');
        this.modal.set(false);
        this.saving.set(false);
        this.refresh();
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        this.showSaveError(
          this.describeError(this.editing() ? 'modifier le plan' : 'créer le plan', error),
        );
      },
    });
  }
  remove(plan: PlanStrategique): void {
    if (!plan.id || !confirm(`Supprimer le plan « ${plan.libelle} » ?`)) return;
    this.service.supprimerPlan(plan.id).subscribe({
      next: () => {
        this.notifications.success('Plan supprimé.');
        this.refresh();
      },
      error: (error: HttpErrorResponse) =>
        this.notifications.error(this.describeError('supprimer le plan', error)),
    });
  }
  private download(blob: Blob, name: string): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = name;
    link.click();
    URL.revokeObjectURL(url);
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
    const detail = this.extractErrorDetail(error.error);
    return `${operation} a échoué (HTTP ${error.status}). ${detail || error.statusText || 'Consultez les journaux du serveur.'}`;
  }
  private extractErrorDetail(body: unknown): string {
    if (body instanceof Blob) return '';
    if (typeof body === 'string') {
      const text = body.trim();
      if (!text) return '';
      try {
        return this.extractErrorDetail(JSON.parse(text));
      } catch {
        return text;
      }
    }
    if (!body || typeof body !== 'object') return '';
    const payload = body as Record<string, unknown>;
    const errors = payload['errors'];
    const fieldErrors =
      errors && typeof errors === 'object'
        ? Object.entries(errors as Record<string, unknown>)
            .map(([field, message]) => `${field} : ${String(message)}`)
            .join('; ')
        : '';
    for (const key of ['detail', 'message', 'title', 'error']) {
      const value = payload[key];
      if (typeof value === 'string' && value.trim())
        return fieldErrors ? `${value.trim()} ${fieldErrors}` : value.trim();
    }
    return fieldErrors;
  }
  private dateInputValue(value: string): string {
    return value.slice(0, 10);
  }
  private emptyForm(): PlanStrategiqueRequestDto {
    return { codePlan: '', libelle: '', dateDebut: '', dateFin: '', actif: true };
  }
}
