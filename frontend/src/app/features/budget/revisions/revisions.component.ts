import { PageHeaderComponent } from '../../../shared/components/page-header.component';
import { AppIconComponent } from '../../../shared/components/app-icon.component';
import { IMPORT_FORMATS } from '../../../core/models/import-formats';
import { ImportCsvPageComponent } from '../../../shared/components/import-csv-page.component';
import { DataActionsComponent } from '../../../shared/components/data-actions.component';
import { FormPageComponent } from '../../../shared/components/form-page.component';
import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { catchError, forkJoin, of } from 'rxjs';
import { BudgetService } from '../../../core/services/budget.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import {
  DemandeRevisionBudgetaire,
  DemandeRevisionRequestDto,
  LigneRevisionBudgetaire,
  ValidationRevisionRequestDto,
} from '../../../core/models/budget/revision-budgetaire.model';
import { BudgetAllocationStructure } from '../../../core/models/budget/budget-allocation.model';
import { ExerciceBudgetaire } from '../../../core/models/budget/exercice-budgetaire.model';

interface RevisionLineForm {
  typeMouvement: 'VIREMENT_INTERNE' | 'EXTENSION';
  montant: number;
  allocationSourceId: string;
  allocationDestinationId: string;
}

@Component({
  selector: 'app-revisions',
  standalone: true,
  imports: [PageHeaderComponent, AppIconComponent, DataActionsComponent, ImportCsvPageComponent, FormPageComponent, CommonModule, DatePipe, FormsModule],
  template: `
@if (importOpen()) {
<app-import-csv-page section="Révisions budgétaires" ressource="revisions-budgetaires" [format]="importFormats.revisions" (back)="importOpen.set(false)" (imported)="refresh()" />
} @else {
@if (modal()) {
<app-form-page section="Révisions budgétaires" [title]="editingDemandId() ? 'Modifier le brouillon de révision' : 'Nouvelle demande de révision'" [mode]="editingDemandId() ? 'Modification' : 'Création'" subtitle="La demande est enregistrée au statut brouillon ; sa validation applique les virements et extensions." (back)="close()">
<form
          (ngSubmit)="create()"
          novalidate
          class="form-page-card space-y-5"
        >
          <div class="grid gap-4 sm:grid-cols-2">
            <label class="space-y-1.5 text-xs text-slate-700"
              >Numéro de demande<input
                required
                maxlength="50"
                [(ngModel)]="draft.numeroDemande"
                name="numeroDemande"
                class="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2.5 font-mono text-xs text-slate-900" /></label
            ><label class="space-y-1.5 text-xs text-slate-700"
              >Exercice<select
                required
                [(ngModel)]="draft.anneeExercice"
                name="anneeExercice"
                class="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2.5 text-xs text-slate-900"
              >
                <option [ngValue]="null">Choisir une année…</option>
                @for (exercise of exercises(); track exercise.id) {
                  <option [ngValue]="exercise.annee">
                    {{ exercise.annee }} — {{ exercise.planStrategique?.libelle }}
                  </option>
                }
              </select></label
            ><label class="space-y-1.5 text-xs text-slate-700 sm:col-span-2"
              >Motif<textarea
                required
                rows="2"
                maxlength="2000"
                [(ngModel)]="draft.motif"
                name="motif"
                placeholder="Justification du transfert ou de l’extension…"
                class="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2.5 text-xs text-slate-900"
              ></textarea>
            </label>
          </div>
          <div class="flex items-center justify-between">
            <div>
              <h3 class="text-xs font-semibold text-slate-900">Mouvements demandés</h3>
              <p class="mt-1 text-[10px] text-slate-600">
                Virement interne : source et destination. Extension : destination seulement.
              </p>
            </div>
            <button
              type="button"
              (click)="addLine()"
              class="rounded-lg border border-slate-300 px-3 py-2 text-[10px] text-slate-700 hover:bg-slate-100"
            >
              ＋ Ajouter une ligne
            </button>
          </div>
          <div class="max-h-[44vh] space-y-3 overflow-y-auto pr-1">
            @for (line of draft.lignes; track $index; let i = $index) {
              <div class="rounded-xl border border-slate-200 bg-slate-50/50 p-4">
                <div class="mb-3 flex items-center justify-between">
                  <span class="text-[10px] font-semibold uppercase tracking-wider text-slate-600"
                    >Mouvement {{ i + 1 }}</span
                  >
                  @if (draft.lignes.length > 1) {
                    <button
                      type="button"
                      (click)="removeLine(i)"
                      class="text-[10px] text-rose-700 hover:text-rose-900"
                    >
                      Retirer
                    </button>
                  }
                </div>
                <div class="grid gap-3 md:grid-cols-2">
                  <label class="space-y-1 text-[10px] text-slate-700"
                    >Type<select
                      [(ngModel)]="line.typeMouvement"
                      [name]="'type-' + i"
                      (ngModelChange)="onLineTypeChange(line)"
                      class="w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-xs text-slate-900"
                    >
                      <option value="VIREMENT_INTERNE">Virement interne</option>
                      <option value="EXTENSION">Extension budgétaire</option>
                    </select></label
                  ><label class="space-y-1 text-[10px] text-slate-700"
                    >Montant<input
                      required
                      type="number"
                      min="0.01"
                      step="0.01"
                      [(ngModel)]="line.montant"
                      [name]="'montant-' + i"
                      class="w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-xs text-slate-900"
                    /><span class="block text-[9px] text-slate-600">
                      @if (line.typeMouvement === 'VIREMENT_INTERNE' && line.allocationSourceId) {
                        Disponible à la source :
                        {{
                          sourceAvailable(line.allocationSourceId) | number: '1.0-0'
                        }} FCFA
                      }
                    </span></label
                  >
                  @if (line.typeMouvement === 'VIREMENT_INTERNE') {
                    <label class="space-y-1 text-[10px] text-slate-700"
                      >Allocation source<select
                        required
                        [(ngModel)]="line.allocationSourceId"
                        [name]="'source-' + i"
                        class="w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-xs text-slate-900"
                      >
                        <option value="">Choisir la structure source…</option>
                        @for (allocation of sourceOptions(i); track allocation.id) {
                          <option [value]="allocation.id">
                            {{ allocationLabel(allocation) }} · disponible
                            {{
                              allocation.montantDisponible | number: '1.0-0'
                            }} FCFA
                          </option>
                        }
                      </select></label
                    >
                  }
                  <label class="space-y-1 text-[10px] text-slate-700"
                    >Allocation destination<select
                      required
                      [(ngModel)]="line.allocationDestinationId"
                      [name]="'destination-' + i"
                      class="w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-xs text-slate-900"
                    >
                      <option value="">Choisir la structure bénéficiaire…</option>
                      @for (allocation of destinationOptions(); track allocation.id) {
                        <option [value]="allocation.id">{{ allocationLabel(allocation) }}</option>
                      }
                    </select></label
                  >
                </div>
              </div>
            }
          </div>
          @if (!draft.lignes.length || !canCreateValid()) {
            <p
              class="rounded-lg border border-amber-200 bg-amber-50 p-3 text-[10px] text-amber-900"
            >
              Renseignez un montant positif et une destination pour chaque ligne. Pour un virement,
              sélectionnez aussi une source différente avec un disponible suffisant.
            </p>
          }
          @if (createError()) {
            <p role="alert" class="rounded-lg border border-rose-500/20 bg-rose-50 p-3 text-xs text-rose-700">
              {{ createError() }}
            </p>
          }
          <div class="form-page-actions">
            <button
              type="button"
              (click)="close()"
              class="rounded-lg px-4 py-2 text-xs text-slate-600 hover:bg-slate-100"
            >
              Annuler</button
            ><button
              type="button"
              (click)="create()"
              [disabled]="saving()"
              class="rounded-lg bg-green-700 px-4 py-2 text-xs font-semibold text-white hover:bg-green-600 disabled:opacity-40"
            >
              {{
                saving()
                  ? 'Enregistrement…'
                  : editingDemandId()
                    ? 'Enregistrer les modifications'
                    : 'Enregistrer le brouillon'
              }}
            </button>
          </div>
        </form>
      
</app-form-page>
} @else {
    <div class="mx-auto max-w-7xl space-y-6">
      <app-page-header icon="revisions" module="Répartition & contrôle" title="Révisions budgétaires" description="Demandez, validez et tracez les virements et extensions de crédits.">
<app-data-actions ressource="revisions-budgetaires" exportPermission="revision.exporter" [ids]="exportIds()" [importPermissions]="['revision.importer']" (importer)="importOpen.set(true)" />
          @if (can('revision.creer')) {
            <button
              (click)="openCreate()"
              class="rounded-xl bg-green-700 px-4 py-2 text-xs font-semibold text-white hover:bg-green-600"
            >
              ＋ Nouvelle demande
            </button>
          }
</app-page-header>
      @if (partial()) {
        <p class="rounded-xl border border-amber-500/20 bg-amber-500/5 p-3 text-xs text-amber-200">
          Les demandes sont visibles, mais les allocations ou exercices requis à la création ne sont
          pas tous accessibles.
        </p>
      }
      @if (error()) {
        <p class="rounded-xl border border-amber-500/20 bg-amber-500/5 p-3 text-xs text-amber-200">
          {{ error() }}
        </p>
      }
      <section class="stats-grid grid gap-3 sm:grid-cols-3">
        <article class="rounded-2xl border border-slate-200 bg-white p-4">
          <p class="text-[10px] uppercase tracking-wider text-slate-500">Demandes enregistrées</p>
          <p class="mt-2 text-2xl font-bold text-white">{{ demands().length }}</p>
        </article>
        <article class="rounded-2xl border border-amber-500/15 bg-white p-4">
          <p class="text-[10px] uppercase tracking-wider text-slate-500">À valider</p>
          <p class="mt-2 text-2xl font-bold text-amber-200">{{ pendingCount() }}</p>
        </article>
        <article class="rounded-2xl border border-blue-500/15 bg-white p-4">
          <p class="text-[10px] uppercase tracking-wider text-slate-500">Validées</p>
          <p class="mt-2 text-2xl font-bold text-blue-200">{{ validatedCount() }}</p>
        </article>
      </section>
      <section class="grid items-start gap-5 xl:grid-cols-[.9fr_1.1fr]">
        <div class="overflow-hidden rounded-2xl border border-slate-200 bg-white">
          <div class="grid gap-2 border-b border-slate-200 p-4 sm:grid-cols-2">
            <input
              type="number"
              [(ngModel)]="yearFilter"
              placeholder="Toutes les années"
              class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-white"
            /><select
              [(ngModel)]="statusFilter"
              class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-700"
            >
              <option value="">Tous les statuts</option>
              <option value="BROUILLON">Brouillon</option>
              <option value="EN_ATTENTE">En attente</option>
              <option value="VALIDE">Validée</option>
              <option value="REJETE">Rejetée</option>
            </select>
          </div>
          <div class="max-h-[680px] divide-y divide-slate-200/70 overflow-y-auto">
            @for (demand of filtered(); track demand.id) {
              <button
                (click)="select(demand)"
                class="w-full p-4 text-left transition hover:bg-slate-50"
                [class.bg-rose-500/5]="selected()?.id === demand.id"
              >
                <span class="flex items-start justify-between gap-3"
                  ><span
                    ><span class="block text-xs font-semibold text-white">{{
                      demand.numeroDemande
                    }}</span
                    ><span class="mt-1 block text-[10px] text-slate-500"
                      >Exercice {{ demand.anneeExercice }} ·
                      {{ demand.createdAt | date: 'dd/MM/yyyy' }}</span
                    ></span
                  ><span
                    class="rounded-full px-2.5 py-1 text-[9px]"
                    [class.bg-amber-500/10]="
                      demand.statut === 'BROUILLON' || demand.statut === 'EN_ATTENTE'
                    "
                    [class.text-amber-200]="
                      demand.statut === 'BROUILLON' || demand.statut === 'EN_ATTENTE'
                    "
                    [class.bg-blue-500/10]="demand.statut === 'VALIDE'"
                    [class.text-blue-200]="demand.statut === 'VALIDE'"
                    [class.bg-rose-500/10]="demand.statut === 'REJETE'"
                    [class.text-rose-200]="demand.statut === 'REJETE'"
                    >{{ statusLabel(demand.statut) }}</span
                  ></span
                ><span class="mt-2 line-clamp-2 block text-[10px] leading-4 text-slate-600">{{
                  demand.motif || 'Aucun motif renseigné'
                }}</span>
              </button>
            } @empty {
              <div class="p-10 text-center">
                <p class="text-xs font-medium text-white">Aucune demande de révision</p>
                <p class="mt-1 text-[10px] text-slate-500">Les demandes créées apparaîtront ici.</p>
              </div>
            }
          </div>
        </div>
        <div class="overflow-hidden rounded-2xl border border-slate-200 bg-white">
          @if (selected()) {
            <div class="border-b border-slate-200 p-5">
              <div class="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p class="font-mono text-[10px] text-rose-300">{{ selected()?.numeroDemande }}</p>
                  <h2 class="mt-1 text-base font-semibold text-white">Détail de la demande</h2>
                  <p class="mt-1 text-[10px] text-slate-500">
                    Exercice {{ selected()?.anneeExercice }} · créée le
                    {{ selected()?.createdAt | date: 'dd/MM/yyyy à HH:mm' }}
                  </p>
                </div>
                <span class="rounded-full bg-slate-100 px-3 py-1 text-[10px] text-slate-700">{{
                  statusLabel(selected()?.statut || '')
                }}</span>
              </div>
              <p class="mt-4 rounded-xl bg-slate-50/60 p-3 text-xs leading-5 text-slate-700">
                {{ selected()?.motif || 'Aucun motif renseigné.' }}
              </p>
              @if (selected()?.statut === 'BROUILLON') {
                <div class="mt-4 flex flex-wrap gap-2">
                  @if (can('revision.modifier')) {
                    <button
                      type="button"
                      (click)="openEdit()"
                      class="rounded-lg border border-slate-300 px-3 py-2 text-[10px] font-semibold text-slate-700 hover:bg-slate-100"
                    >
                      Modifier
                    </button>
                  }
                  @if (can('revision.supprimer')) {
                    <button
                      type="button"
                      (click)="deleteDraft()"
                      [disabled]="saving()"
                      class="rounded-lg border border-rose-200 px-3 py-2 text-[10px] font-semibold text-rose-700 hover:bg-rose-50 disabled:opacity-50"
                    >
                      Supprimer
                    </button>
                  }
                  @if (can('revision.soumettre')) {
                    <button
                      type="button"
                      (click)="submitDraft()"
                      [disabled]="saving() || !lines().length"
                      class="rounded-lg bg-blue-700 px-3 py-2 text-[10px] font-semibold text-white hover:bg-blue-600 disabled:opacity-50"
                    >
                      Soumettre
                    </button>
                  }
                  @if (!can('revision.modifier') || !can('revision.supprimer') || !can('revision.soumettre')) {
                    <p class="basis-full text-[10px] text-slate-500">
                      Certaines actions sont indisponibles : vérifiez les droits
                      <code>revision.modifier</code>, <code>revision.supprimer</code> et
                      <code>revision.soumettre</code> de votre rôle.
                    </p>
                  }
                </div>
              }
            </div>
            <div class="p-5">
              <div class="mb-3 flex items-center justify-between">
                <div>
                  <h3 class="text-xs font-semibold text-white">Lignes de mouvement</h3>
                  <p class="mt-1 text-[10px] text-slate-500">
                    {{ lines().length }} opération(s) enregistrée(s)
                  </p>
                </div>
              </div>
              <div class="space-y-2">
                @for (line of lines(); track line.id) {
                  <div class="rounded-xl border border-slate-200 bg-slate-50/50 p-3">
                    <div class="flex items-center justify-between gap-2">
                      <span
                        class="rounded-md px-2 py-1 text-[9px]"
                        [class.bg-blue-500/10]="line.typeMouvement === 'VIREMENT_INTERNE'"
                        [class.text-blue-200]="line.typeMouvement === 'VIREMENT_INTERNE'"
                        [class.bg-blue-500/10]="line.typeMouvement === 'EXTENSION'"
                        [class.text-blue-200]="line.typeMouvement === 'EXTENSION'"
                        >{{ movementLabel(line.typeMouvement) }}</span
                      ><span class="text-sm font-semibold text-white">{{
                        line.montant | number: '1.0-0'
                      }} FCFA</span>
                    </div>
                    <div class="mt-3 grid gap-2 sm:grid-cols-2">
                      <div class="rounded-lg bg-white p-2.5">
                        <p class="text-[9px] uppercase text-slate-500">Source</p>
                        <p class="mt-1 text-[10px] text-slate-800">
                          {{ allocationLabel(line.allocationSource) }}
                        </p>
                      </div>
                      <div class="rounded-lg bg-white p-2.5">
                        <p class="text-[9px] uppercase text-slate-500">Destination</p>
                        <p class="mt-1 text-[10px] text-slate-800">
                          {{ allocationLabel(line.allocationDestination) }}
                        </p>
                      </div>
                    </div>
                  </div>
                } @empty {
                  <p
                    class="rounded-lg border border-dashed border-slate-200 p-5 text-center text-xs text-slate-500"
                  >
                    Aucune ligne trouvée pour cette demande.
                  </p>
                }
              </div>
              @if (isPendingDecision()) {
                <div class="mt-5 space-y-3 rounded-xl border border-amber-500/20 bg-amber-500/5 p-4">
                  <div>
                    <h3 class="text-xs font-semibold text-white">Décision sur la demande</h3>
                    <p class="mt-1 text-[10px] leading-4 text-slate-500">
                      Une validation applique les mouvements aux allocations ; un rejet conserve les
                      lignes budgétaires inchangées.
                    </p>
                  </div>
                  @if (isOwnDemand()) {
                    <p role="alert" class="rounded-lg border border-amber-500/20 bg-amber-500/5 p-3 text-[10px] leading-4 text-amber-200">
                      Vous êtes l’auteur de cette demande : sa décision revient à un autre valideur.
                    </p>
                  } @else if (!can('revision.valider')) {
                    <p role="alert" class="rounded-lg border border-rose-500/20 bg-rose-500/5 p-3 text-[10px] leading-4 text-rose-200">
                      Votre rôle n’a pas le droit <code>revision.valider</code>. Demandez à un
                      administrateur de l’ajouter à votre rôle, puis reconnectez-vous.
                    </p>
                  }
                  <label class="block space-y-1.5 text-[10px] text-slate-600">
                    Statut à attribuer
                    <select
                      [(ngModel)]="validationStatus"
                      name="validationStatus"
                      [disabled]="!canDecide() || saving()"
                      class="w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-xs text-slate-800"
                    >
                      <option value="VALIDE">Validée — appliquer les imputations</option>
                      <option value="REJETE">Rejetée — ne pas modifier les budgets</option>
                    </select>
                  </label>
                  <label class="block space-y-1.5 text-[10px] text-slate-600">
                    Commentaire de décision (facultatif)
                    <textarea
                      [(ngModel)]="validationComment"
                      name="validationComment"
                      rows="2"
                      [disabled]="!canDecide() || saving()"
                      class="w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-xs text-slate-800"
                    ></textarea>
                  </label>
                  <button
                    type="button"
                    (click)="validate()"
                    [disabled]="saving() || !canDecide()"
                    class="w-full rounded-xl px-4 py-3 text-xs font-semibold text-white disabled:cursor-wait disabled:opacity-50"
                    [class.bg-green-700]="validationStatus === 'VALIDE'"
                    [class.hover:bg-green-600]="validationStatus === 'VALIDE'"
                    [class.bg-rose-700]="validationStatus === 'REJETE'"
                    [class.hover:bg-rose-600]="validationStatus === 'REJETE'"
                  >
                    {{
                      saving()
                        ? 'Enregistrement de la décision…'
                        : validationStatus === 'VALIDE'
                          ? '✓ Valider et appliquer les mouvements'
                          : '✕ Rejeter sans modifier le budget'
                    }}
                  </button>
                </div>
              }
              @if (selected()?.dateValidation) {
                <div class="mt-4 rounded-xl border border-blue-500/15 bg-blue-500/5 p-3">
                  <p class="text-[10px] text-blue-200">
                    {{ selected()?.statut === 'REJETE' ? 'Rejetée' : 'Validée' }} le
                    {{ selected()?.dateValidation | date: 'dd/MM/yyyy à HH:mm' }}
                  </p>
                  <p class="mt-1 text-[10px] text-slate-600">
                    {{ selected()?.commentaireValidation || 'Aucun commentaire de décision.' }}
                  </p>
                </div>
              }
            </div>
          } @else {
            <div class="flex min-h-[380px] flex-col items-center justify-center p-8 text-center">
              <span
                class="mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-rose-500/10 text-xl text-rose-300"
                ><app-icon name="revisions" classe="h-5 w-5" /></span
              >
              <h2 class="text-sm font-semibold text-white">Sélectionnez une demande</h2>
              <p class="mt-1 text-xs text-slate-500">
                Choisissez une ligne à gauche pour examiner ses mouvements.
              </p>
            </div>
          }
        </div>
      </section>
    </div>

    
}
}

  `,
})
export class RevisionsComponent implements OnInit {
  readonly importOpen = signal(false);
  readonly importFormats = IMPORT_FORMATS;
  /** Lignes affichées (filtres et tri appliqués) : ce sont elles qui sont exportées. */
  exportIds(): (string | undefined)[] {
    return this.filtered().map((item) => item.id);
  }
  private readonly budget = inject(BudgetService);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  readonly demands = signal<DemandeRevisionBudgetaire[]>([]);
  readonly lines = signal<LigneRevisionBudgetaire[]>([]);
  readonly exercises = signal<ExerciceBudgetaire[]>([]);
  readonly allocations = signal<BudgetAllocationStructure[]>([]);
  readonly selected = signal<DemandeRevisionBudgetaire | null>(null);
  readonly modal = signal(false);
  readonly saving = signal(false);
  readonly partial = signal(false);
  readonly error = signal('');
  readonly createError = signal('');
  readonly editingDemandId = signal<string | null>(null);
  yearFilter: number | null = null;
  statusFilter = '';
  validationComment = '';
  validationStatus: 'VALIDE' | 'REJETE' = 'VALIDE';
  draft: {
    numeroDemande: string;
    anneeExercice: number | null;
    demandeurId: string;
    motif: string;
    lignes: RevisionLineForm[];
  } = this.emptyDraft();
  ngOnInit(): void {
    this.refresh();
  }
  can(permission: string): boolean {
    return this.auth.hasPermission(permission);
  }
  isPendingDecision(): boolean {
    return this.selected()?.statut === 'EN_ATTENTE';
  }
  isOwnDemand(): boolean {
    const demand = this.selected();
    return !!demand && demand.demandeurId === this.auth.currentUser()?.id;
  }
  canDecide(): boolean {
    return this.can('revision.valider') && !this.isOwnDemand();
  }
  pendingCount(): number {
    return this.demands().filter(
      (item) => item.statut === 'BROUILLON' || item.statut === 'EN_ATTENTE',
    ).length;
  }
  validatedCount(): number {
    return this.demands().filter((item) => item.statut === 'VALIDE').length;
  }
  filtered(): DemandeRevisionBudgetaire[] {
    return [...this.demands()]
      .filter(
        (item) =>
          (!this.yearFilter || item.anneeExercice === Number(this.yearFilter)) &&
          (!this.statusFilter || item.statut === this.statusFilter),
      )
      .sort((a, b) => new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime());
  }
  statusLabel(status: string): string {
    return (
      (
        {
          BROUILLON: 'Brouillon',
          EN_ATTENTE: 'Soumise',
          VALIDE: 'Validée',
          REJETE: 'Rejetée',
        } as Record<string, string>
      )[status] || status
    );
  }
  movementLabel(type: string): string {
    return type === 'VIREMENT_INTERNE'
      ? 'Virement interne'
      : type === 'EXTENSION'
        ? 'Extension'
        : type;
  }
  allocationLabel(allocation?: BudgetAllocationStructure): string {
    if (!allocation) return 'Non applicable';
    const structure = allocation.structure?.nom || allocation.structure?.code || 'Structure';
    const activity = allocation.budgetAllocationActivite?.activiteCatalogue?.libelle || 'Activité';
    return `${structure} · ${activity}`;
  }
  refresh(): void {
    this.error.set('');
    this.partial.set(false);
    forkJoin({
      demands: this.budget.listerRevisions().pipe(
        catchError(() => {
          this.error.set('Demandes de révision indisponibles. Vérifiez revision.lire.');
          return of([] as DemandeRevisionBudgetaire[]);
        }),
      ),
      exercises: (this.auth.hasPermission('exercice.lire')
        ? this.budget.listerExercices()
        : of([] as ExerciceBudgetaire[])).pipe(
        catchError(() => {
          this.partial.set(true);
          return of([] as ExerciceBudgetaire[]);
        }),
      ),
      allocations: (this.auth.hasPermission('execution.lire')
        ? this.budget.listerAllocations()
        : of([] as BudgetAllocationStructure[])).pipe(
        catchError(() => {
          this.partial.set(true);
          return of([] as BudgetAllocationStructure[]);
        }),
      ),
    }).subscribe((data) => {
      this.demands.set(data.demands);
      this.exercises.set(data.exercises);
      this.allocations.set(data.allocations);
      if (this.selected()?.id) {
        const current = data.demands.find((item) => item.id === this.selected()?.id);
        this.selected.set(current || null);
      }
    });
  }
  select(demand: DemandeRevisionBudgetaire): void {
    this.selected.set(demand);
    this.lines.set([]);
    this.validationComment = '';
    this.validationStatus = 'VALIDE';
    if (!demand.id) return;
    this.budget
      .listerLignesRevision(demand.id)
      .subscribe({
        next: (value) => this.lines.set(value),
        error: () => this.notifications.error('Impossible de charger les lignes de cette demande.'),
      });
  }
  openCreate(): void {
    this.editingDemandId.set(null);
    this.draft = this.emptyDraft();
    this.createError.set('');
    this.draft.numeroDemande = `REV-${new Date().getFullYear()}-${Date.now().toString().slice(-6)}`;
    this.draft.demandeurId = this.auth.currentUser()?.id || '';
    this.draft.anneeExercice =
      this.exercises().find((item) => item.annee === new Date().getFullYear())?.annee ||
      this.exercises()[0]?.annee ||
      null;
    this.draft.lignes = [this.newLine()];
    this.modal.set(true);
  }
  openEdit(): void {
    const demand = this.selected();
    if (!demand?.id || demand.statut !== 'BROUILLON' || !this.can('revision.modifier')) return;
    this.editingDemandId.set(demand.id);
    this.createError.set('');
    this.draft = {
      numeroDemande: demand.numeroDemande,
      anneeExercice: demand.anneeExercice,
      demandeurId: demand.demandeurId,
      motif: demand.motif || '',
      lignes: this.lines().map((line) => ({
        typeMouvement: line.typeMouvement === 'EXTENSION' ? 'EXTENSION' : 'VIREMENT_INTERNE',
        montant: Number(line.montant),
        allocationSourceId: line.allocationSource?.id || '',
        allocationDestinationId: line.allocationDestination?.id || '',
      })),
    };
    this.modal.set(true);
  }
  close(): void {
    if (!this.saving()) this.modal.set(false);
  }
  addLine(): void {
    this.draft.lignes.push(this.newLine());
  }
  removeLine(index: number): void {
    this.draft.lignes.splice(index, 1);
  }
  onLineTypeChange(line: RevisionLineForm): void {
    if (line.typeMouvement === 'EXTENSION') line.allocationSourceId = '';
  }
  sourceOptions(index: number): BudgetAllocationStructure[] {
    const year = this.draft.anneeExercice;
    const line = this.draft.lignes[index];
    return this.allocations().filter(
      (item) =>
        this.yearOf(item) === year &&
        Number(item.montantDisponible || 0) > 0 &&
        item.id !== line.allocationDestinationId,
    );
  }
  destinationOptions(): BudgetAllocationStructure[] {
    return this.allocations().filter((item) => this.yearOf(item) === this.draft.anneeExercice);
  }
  sourceAvailable(id: string): number {
    const allocation = this.allocations().find((item) => item.id === id);
    const totalOther = this.draft.lignes
      .filter((line) => line.allocationSourceId === id)
      .reduce((sum, line) => sum + Number(line.montant || 0), 0);
    return Math.max(0, Number(allocation?.montantDisponible || 0) - totalOther);
  }
  canCreateValid(): boolean {
    return (
      this.draft.lignes.length > 0 &&
      this.draft.lignes.every(
        (line) =>
          Number(line.montant) > 0 &&
          !!line.allocationDestinationId &&
          (line.typeMouvement === 'EXTENSION' ||
            (!!line.allocationSourceId &&
              line.allocationSourceId !== line.allocationDestinationId &&
              Number(line.montant) <=
                this.sourceAvailable(line.allocationSourceId) + Number(line.montant))),
      )
    );
  }
  create(): void {
    if (this.saving()) return;
    const editingId = this.editingDemandId();
    const requiredPermission = editingId ? 'revision.modifier' : 'revision.creer';
    if (!this.can(requiredPermission)) {
      this.createError.set(`Votre rôle ne possède pas le droit ${requiredPermission}.`);
    } else if (!this.auth.currentUser()?.id || !this.draft.demandeurId) {
      this.createError.set('Votre session utilisateur est introuvable. Reconnectez-vous puis réessayez.');
    } else if (!this.draft.numeroDemande.trim()) {
      this.createError.set('Le numéro de demande est obligatoire.');
    } else if (!this.draft.anneeExercice) {
      this.createError.set('Sélectionnez un exercice budgétaire.');
    } else if (!this.draft.motif.trim()) {
      this.createError.set('Saisissez le motif de la demande.');
    } else if (!this.draft.lignes.length) {
      this.createError.set('Ajoutez au moins un mouvement à la demande.');
    } else if (this.draft.lignes.some(
      (line) => !Number.isFinite(Number(line.montant)) || Number(line.montant) <= 0,
    )) {
      this.createError.set('Chaque mouvement doit avoir un montant supérieur à zéro.');
    } else if (this.draft.lignes.some((line) => !line.allocationDestinationId)) {
      this.createError.set('Sélectionnez une allocation de destination pour chaque mouvement.');
    } else if (this.draft.lignes.some(
      (line) =>
        line.typeMouvement === 'VIREMENT_INTERNE' &&
        (!line.allocationSourceId || line.allocationSourceId === line.allocationDestinationId),
    )) {
      this.createError.set('Pour chaque virement, choisissez une source différente de la destination.');
    } else if (!this.canCreateValid()) {
      this.createError.set('Le montant demandé dépasse le disponible de la source.');
    } else {
      this.createError.set('');
    }
    if (this.createError()) {
      this.notifications.error(this.createError());
      return;
    }
    const dto: DemandeRevisionRequestDto = {
      numeroDemande: this.draft.numeroDemande,
      anneeExercice: Number(this.draft.anneeExercice),
      demandeurId: this.draft.demandeurId,
      motif: this.draft.motif,
      lignes: this.draft.lignes.map((line) => ({
        typeMouvement: line.typeMouvement,
        montant: Number(line.montant),
        ...(line.allocationSourceId ? { allocationSourceId: line.allocationSourceId } : {}),
        allocationDestinationId: line.allocationDestinationId,
      })),
    };
    this.saving.set(true);
    const request = editingId
      ? this.budget.modifierRevision(editingId, dto)
      : this.budget.creerRevision(dto);
    request.subscribe({
      next: (result) => {
        this.saving.set(false);
        this.createError.set('');
        this.modal.set(false);
        this.editingDemandId.set(null);
        this.notifications.success(
          editingId ? 'Brouillon modifié avec succès.' : 'Demande enregistrée au statut brouillon.',
        );
        this.refresh();
        this.select(result);
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        this.createError.set(this.describeCreateError(error));
        this.notifications.error(this.createError());
      },
    });
  }
  submitDraft(): void {
    const demand = this.selected();
    if (!demand?.id || demand.statut !== 'BROUILLON' || !this.can('revision.soumettre')) return;
    this.saving.set(true);
    this.budget.soumettreRevision(demand.id).subscribe({
      next: (submitted) => {
        this.saving.set(false);
        this.notifications.success('Demande soumise pour décision.');
        this.select(submitted);
        this.refresh();
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        this.notifications.error(this.describeLifecycleError(error, 'soumettre'));
      },
    });
  }
  deleteDraft(): void {
    const demand = this.selected();
    if (!demand?.id || demand.statut !== 'BROUILLON' || !this.can('revision.supprimer')) return;
    if (!window.confirm(`Supprimer définitivement le brouillon ${demand.numeroDemande} ?`)) return;
    this.saving.set(true);
    this.budget.supprimerRevision(demand.id).subscribe({
      next: () => {
        this.saving.set(false);
        this.selected.set(null);
        this.lines.set([]);
        this.notifications.success('Brouillon supprimé.');
        this.refresh();
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        this.notifications.error(this.describeLifecycleError(error, 'supprimer'));
      },
    });
  }
  private describeLifecycleError(error: HttpErrorResponse, action: 'soumettre' | 'supprimer'): string {
    if (error.status === 0) return 'Le serveur est injoignable. Vérifiez la connexion puis réessayez.';
    if (error.status === 401) return 'Votre session a expiré. Reconnectez-vous puis réessayez.';
    if (error.status === 403) {
      return action === 'soumettre'
        ? 'Vous n’avez pas le droit revision.soumettre.'
        : 'Vous n’avez pas le droit revision.supprimer.';
    }
    const body = error.error as { message?: unknown; detail?: unknown } | null;
    const detail =
      typeof body?.message === 'string'
        ? body.message
        : typeof body?.detail === 'string'
          ? body.detail
          : '';
    return detail || `Impossible de ${action} cette demande (HTTP ${error.status}).`;
  }
  private describeCreateError(error: HttpErrorResponse): string {
    if (error.status === 0) return 'Le serveur est injoignable. Vérifiez la connexion puis réessayez.';
    if (error.status === 401) return 'Votre session a expiré. Reconnectez-vous puis réessayez.';
    if (error.status === 403) {
      const requiredPermission = this.editingDemandId() ? 'revision.modifier' : 'revision.creer';
      return `Vous n’avez pas le droit ${requiredPermission}. Demandez à un administrateur de l’ajouter à votre rôle.`;
    }
    const body = error.error as { message?: unknown; detail?: unknown } | null;
    const detail =
      typeof body?.message === 'string'
        ? body.message
        : typeof body?.detail === 'string'
          ? body.detail
          : '';
    return detail || `Création impossible (HTTP ${error.status}). Vérifiez le numéro de demande et les allocations sélectionnées.`;
  }
  validate(): void {
    const demand = this.selected();
    if (!demand?.id || !this.canDecide()) return;
    const decision = this.validationStatus;
    const dto: ValidationRevisionRequestDto = {
      statut: decision,
      commentaire: this.validationComment,
    };
    this.saving.set(true);
    this.budget.validerRevision(demand.id, dto).subscribe({
      next: (updated) => {
        this.saving.set(false);
        this.notifications.success(
          decision === 'VALIDE'
            ? 'Demande validée. Les crédits ont été appliqués et journalisés.'
            : 'Demande rejetée. Les lignes budgétaires n’ont pas été modifiées.',
        );
        this.select(updated);
        this.refresh();
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        this.notifications.error(this.describeDecisionError(error, decision));
      },
    });
  }
  private yearOf(item: BudgetAllocationStructure): number | undefined {
    return item.budgetAllocationActivite?.budgetAllocationOp?.budgetAllocationOs?.exerciceBudgetaire
      ?.annee;
  }
  private describeDecisionError(error: HttpErrorResponse, decision: 'VALIDE' | 'REJETE'): string {
    if (error.status === 0) return 'Le serveur est injoignable. Vérifiez la connexion puis réessayez.';
    if (error.status === 401) return 'Votre session a expiré. Reconnectez-vous puis réessayez.';
    if (error.status === 403) return 'Vous n’avez pas les droits nécessaires pour décider cette demande.';
    const body = error.error as { message?: unknown; detail?: unknown } | null;
    const detail =
      typeof body?.message === 'string'
        ? body.message
        : typeof body?.detail === 'string'
          ? body.detail
          : '';
    return detail || (decision === 'VALIDE'
      ? `Validation impossible (HTTP ${error.status}).`
      : `Rejet impossible (HTTP ${error.status}).`);
  }
  private newLine(): RevisionLineForm {
    return {
      typeMouvement: 'VIREMENT_INTERNE',
      montant: 0,
      allocationSourceId: '',
      allocationDestinationId: '',
    };
  }
  private emptyDraft() {
    return {
      numeroDemande: '',
      anneeExercice: null as number | null,
      demandeurId: '',
      motif: '',
      lignes: [] as RevisionLineForm[],
    };
  }
  private download(blob: Blob, name: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = name;
    a.click();
    URL.revokeObjectURL(url);
  }
}
