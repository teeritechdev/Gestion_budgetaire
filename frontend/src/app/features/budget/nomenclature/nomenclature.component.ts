import { PageHeaderComponent } from '../../../shared/components/page-header.component';
import { AppIconComponent } from '../../../shared/components/app-icon.component';
import { RowActionsComponent } from '../../../shared/components/row-actions.component';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { NomenclatureService } from '../../../core/services/nomenclature.service';
import { NotificationService } from '../../../core/services/notification.service';
import { AuthService } from '../../../core/services/auth.service';
import { ObjectifStrategique } from '../../../core/models/budget/objectif-strategique.model';
import { ObjectifOperationnel } from '../../../core/models/budget/objectif-operationnel.model';
import { ActiviteCatalogue } from '../../../core/models/budget/activite-catalogue.model';
import { FormatImport, IMPORT_FORMATS } from '../../../core/models/import-formats';
import { DataActionsComponent } from '../../../shared/components/data-actions.component';
import { FormPageComponent } from '../../../shared/components/form-page.component';
import { ImportCsvPageComponent } from '../../../shared/components/import-csv-page.component';
import { RecordDetailsDialogComponent } from '../../../shared/components/record-details-dialog.component';

type NodeType = 'OS' | 'OP' | 'AC';

interface SousOnglet {
  type: NodeType;
  libelle: string;
  libelleSingulier: string;
  lire: string;
  creer: string;
  modifier: string;
  supprimer: string;
  exportRessource: string;
  exportPermission: string;
  importRessource: string;
  importPermissions: readonly string[];
  importFormat: FormatImport;
  description: string;
}

const SOUS_ONGLETS: Record<NodeType, SousOnglet> = {
  OS: {
    type: 'OS', libelle: 'Objectifs stratégiques', libelleSingulier: 'objectif stratégique',
    lire: 'os.lire', creer: 'os.creer', modifier: 'os.modifier', supprimer: 'os.supprimer',
    exportRessource: 'objectifs-strategiques', exportPermission: 'os.exporter',
    importRessource: 'objectifs-strategiques', importPermissions: ['os.importer'],
    importFormat: IMPORT_FORMATS.objectifsStrategiques,
    description: 'Les grands axes du plan budgétaire, déclinés ensuite en objectifs opérationnels.',
  },
  OP: {
    type: 'OP', libelle: 'Objectifs opérationnels', libelleSingulier: 'objectif opérationnel',
    lire: 'op.lire', creer: 'op.creer', modifier: 'op.modifier', supprimer: 'op.supprimer',
    exportRessource: 'objectifs-operationnels', exportPermission: 'op.exporter',
    importRessource: 'objectifs-operationnels', importPermissions: ['op.importer'],
    importFormat: IMPORT_FORMATS.objectifsOperationnels,
    description: 'Les objectifs concrets rattachés à un objectif stratégique.',
  },
  AC: {
    type: 'AC', libelle: 'Activités catalogue', libelleSingulier: 'activité',
    lire: 'activite.lire', creer: 'activite.creer', modifier: 'activite.modifier', supprimer: 'activite.supprimer',
    exportRessource: 'activites-catalogue', exportPermission: 'activite.exporter',
    importRessource: 'activites-catalogue', importPermissions: ['activite.importer'],
    importFormat: IMPORT_FORMATS.activitesCatalogue,
    description: 'Les actions budgétisables qui contribuent à un objectif opérationnel.',
  },
};

const INPUT = 'w-full rounded-xl border border-slate-300 bg-slate-50 px-3 py-2.5 text-sm text-slate-900 focus:outline-none focus:ring-1 focus:ring-blue-500';
const FILTRE = 'bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-700 py-2 px-3 focus:outline-none focus:ring-1 focus:ring-blue-500';

@Component({
  selector: 'app-nomenclature',
  standalone: true,
  imports: [PageHeaderComponent, AppIconComponent, RowActionsComponent, CommonModule, FormsModule, DataActionsComponent, FormPageComponent, ImportCsvPageComponent, RecordDetailsDialogComponent],
  template: `
@if (importOpen()) {
  <app-import-csv-page section="Nomenclature budgétaire" [ressource]="onglet().importRessource" [format]="onglet().importFormat"
    (back)="importOpen.set(false)" (imported)="refresh()" />
} @else if (formOpen()) {
  <app-form-page section="Nomenclature budgétaire" [title]="formTitle()" [mode]="editId() ? 'Modification' : 'Création'"
    [subtitle]="formOnglet().description" (back)="closeForm()">
    <form (ngSubmit)="save()">
      <section class="space-y-4">
        <h3>Identification</h3>
        <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
          <label class="block space-y-1.5 text-xs font-medium text-slate-700">Code *
            <input required maxlength="50" [(ngModel)]="form.code" name="code" [placeholder]="codeExemple()" class="${INPUT} font-mono uppercase">
            <span class="block text-[11px] font-normal text-slate-500">Unique dans la nomenclature, ex. {{ codeExemple() }}.</span>
          </label>
          <label class="block space-y-1.5 text-xs font-medium text-slate-700">Libellé *
            <input required maxlength="200" [(ngModel)]="form.libelle" name="libelle" placeholder="Intitulé de l’élément" class="${INPUT}">
          </label>
        </div>
        <label class="block space-y-1.5 text-xs font-medium text-slate-700">Description
          <textarea rows="4" maxlength="3000" [(ngModel)]="form.description" name="description" placeholder="Finalité, résultats attendus, périmètre…" class="${INPUT} resize-y"></textarea>
          <span class="block text-right text-[11px] font-normal text-slate-500">{{ form.description.length }}/3000</span>
        </label>
      </section>

      @if (formType() !== 'OS') {
        <section class="space-y-4">
          <h3>Rattachement</h3>
          <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
            <label class="block space-y-1.5 text-xs font-medium text-slate-700">{{ formType() === 'OP' ? 'Objectif stratégique parent *' : 'Objectif stratégique (filtre)' }}
              <select [(ngModel)]="form.osId" (ngModelChange)="onFormOsChange()" name="osId" [required]="formType() === 'OP'" class="${INPUT}">
                <option value="">{{ formType() === 'OP' ? 'Choisir un OS…' : 'Tous les OS' }}</option>
                @for (os of osList(); track os.id) { <option [value]="os.id">{{ os.codeOs }} — {{ os.libelle }}</option> }
              </select>
              @if (formType() === 'AC') {
                <span class="block text-[11px] font-normal text-slate-500">Sert uniquement à restreindre la liste des OP : l’activité est rattachée à l’OP, pas directement à l’OS.</span>
              }
            </label>
            @if (formType() === 'AC') {
              <label class="block space-y-1.5 text-xs font-medium text-slate-700">Objectif opérationnel parent *
                <select required [(ngModel)]="form.opId" (ngModelChange)="onFormOpChange()" name="opId" class="${INPUT}">
                  <option value="">{{ form.osId ? 'Choisir un OP de cet OS…' : 'Choisir un OP…' }}</option>
                  @for (op of opsDuFormulaire(); track op.id) { <option [value]="op.id">{{ op.codeOp }} — {{ op.libelle }}</option> }
                </select>
                @if (!opsDuFormulaire().length) {
                  <span class="block text-[11px] font-normal text-amber-700">{{ form.osId ? 'Aucun OP n’est rattaché à cet OS : choisissez un autre OS ou créez l’OP dans le sous-onglet Objectifs opérationnels.' : 'Aucun OP disponible : créez-en un dans le sous-onglet Objectifs opérationnels.' }}</span>
                } @else {
                  <span class="block text-[11px] font-normal text-slate-500">{{ opsDuFormulaire().length }} OP {{ form.osId ? 'rattaché(s) à l’OS choisi' : 'au total' }}.</span>
                }
              </label>
            }
          </div>
          @if (formType() === 'AC' && opDuFormulaire(); as op) {
            <p class="rounded-xl bg-green-50 px-3 py-2 text-xs text-green-900">
              Rattachement : <span class="font-mono">{{ osDeOp(op)?.codeOs || '—' }}</span> › <span class="font-mono font-semibold">{{ op.codeOp }}</span> {{ op.libelle }} › cette activité
            </p>
          }
        </section>
      }

      <div class="form-page-actions">
        <button type="button" (click)="closeForm()" [disabled]="saving()" class="rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-100">Annuler</button>
        <button type="submit" [disabled]="saving()" class="rounded-xl bg-green-700 px-5 py-2 text-xs font-semibold text-white hover:bg-green-600 disabled:opacity-50">
          {{ saving() ? 'Enregistrement…' : editId() ? 'Enregistrer les modifications' : 'Créer l’' + formOnglet().libelleSingulier }}
        </button>
      </div>
    </form>
  </app-form-page>
} @else {
  <div class="mx-auto max-w-7xl space-y-6">
    <app-page-header icon="nomenclature" [module]="'Nomenclature budgétaire · ' + tab()" [title]="onglet().libelle" [description]="onglet().description">
<app-data-actions [ressource]="onglet().exportRessource" [exportPermission]="onglet().exportPermission"
          [ids]="exportIds()" [importPermissions]="onglet().importPermissions" (importer)="importOpen.set(true)" />
        @if (can(onglet().creer)) {
          <button type="button" (click)="openCreate(tab())" class="whitespace-nowrap rounded-xl bg-green-700 px-4 py-2 text-xs font-semibold text-white hover:bg-green-600">
            ＋ Nouvel{{ tab() === 'AC' ? 'le activité' : ' ' + tab() }}
          </button>
        }
</app-page-header>


    <!-- Statistiques communes aux trois sous-onglets -->
    <section class="stats-grid grid gap-3 sm:grid-cols-3">
      <article class="flex items-center gap-4 rounded-2xl border border-slate-200 bg-white p-4"><span class="flex h-11 w-11 items-center justify-center rounded-xl bg-blue-500/10 text-lg text-blue-300"><app-icon name="objectifStrategique" classe="h-6 w-6" /></span><div><p class="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Objectifs stratégiques</p><p class="mt-1 text-2xl font-bold text-white">{{ osList().length }}</p></div><span class="ml-auto text-[10px] text-slate-500">Niveau 1</span></article>
      <article class="flex items-center gap-4 rounded-2xl border border-slate-200 bg-white p-4"><span class="flex h-11 w-11 items-center justify-center rounded-xl bg-blue-500/10 text-lg text-blue-300"><app-icon name="objectifOperationnel" classe="h-6 w-6" /></span><div><p class="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Objectifs opérationnels</p><p class="mt-1 text-2xl font-bold text-white">{{ opList().length }}</p></div><span class="ml-auto text-[10px] text-slate-500">Niveau 2</span></article>
      <article class="flex items-center gap-4 rounded-2xl border border-slate-200 bg-white p-4"><span class="flex h-11 w-11 items-center justify-center rounded-xl bg-blue-500/10 text-lg text-blue-300"><app-icon name="activite" classe="h-6 w-6" /></span><div><p class="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Activités catalogue</p><p class="mt-1 text-2xl font-bold text-white">{{ activiteList().length }}</p></div><span class="ml-auto text-[10px] text-slate-500">Niveau 3</span></article>
    </section>

    @if (error()) {
      <div role="alert" class="rounded-xl border border-rose-300 bg-rose-50 p-3 text-sm text-rose-700">{{ error() }}</div>
    }

    <!-- Critères de filtrage -->
    <div class="flex flex-wrap items-center gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-lg">
      <div class="min-w-[220px] flex-1">
        <input type="text" [(ngModel)]="search" [placeholder]="'Rechercher un ' + onglet().libelleSingulier + ' par code, libellé ou description…'"
          aria-label="Recherche" class="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
      </div>
      @if (tab() !== 'OS') {
        <select [(ngModel)]="osFilter" (ngModelChange)="opFilter = ''" aria-label="Filtrer par objectif stratégique" class="${FILTRE}">
          <option value="">Tous les OS</option>
          @for (os of osList(); track os.id) { <option [value]="os.id">{{ os.codeOs }} — {{ os.libelle }}</option> }
        </select>
      }
      @if (tab() === 'AC') {
        <select [(ngModel)]="opFilter" aria-label="Filtrer par objectif opérationnel" class="${FILTRE}">
          <option value="">Tous les OP</option>
          @for (op of opsDuFiltre(); track op.id) { <option [value]="op.id">{{ op.codeOp }} — {{ op.libelle }}</option> }
        </select>
      }
      @if (tab() !== 'AC') {
        <select [(ngModel)]="childFilter" aria-label="Filtrer par rattachements" class="${FILTRE}">
          <option value="">{{ tab() === 'OS' ? 'Avec ou sans OP' : 'Avec ou sans activités' }}</option>
          <option value="AVEC">{{ tab() === 'OS' ? 'Avec OP rattachés' : 'Avec activités rattachées' }}</option>
          <option value="SANS">{{ tab() === 'OS' ? 'Sans OP rattaché' : 'Sans activité rattachée' }}</option>
        </select>
      }
      <button type="button" (click)="resetFilters()" title="Réinitialiser les filtres" aria-label="Réinitialiser les filtres" class="rounded-xl bg-slate-100 p-2 text-slate-600 transition-colors hover:bg-slate-200">
        <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
      </button>
      <button type="button" (click)="refresh()" [disabled]="loading()" class="rounded-xl border border-slate-300 px-3 py-2 text-xs text-slate-700 hover:bg-slate-100 disabled:opacity-50">{{ loading() ? 'Chargement…' : '↻ Actualiser' }}</button>
      <p class="w-full text-[11px] text-slate-500">
        {{ exportIds().length }} {{ onglet().libelle.toLowerCase() }} affiché(s) sur {{ total(tab()) }}
        @if (hasActiveFilters()) { · filtres actifs : les exports porteront sur ces lignes uniquement }
      </p>
    </div>

    <section class="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl">
      <div class="overflow-x-auto">
        @switch (tab()) {
          @case ('OS') {
            <table class="w-full text-left text-xs">
              <thead class="border-b border-slate-200 bg-slate-50 uppercase tracking-wider text-slate-600">
                <tr><th class="px-4 py-3.5">Code</th><th class="px-4 py-3.5">Libellé</th><th class="px-4 py-3.5">Description</th><th class="px-4 py-3.5">OP rattachés</th><th class="px-4 py-3.5 text-right">Actions</th></tr>
              </thead>
              <tbody class="divide-y divide-slate-200/60">
                @for (os of filteredOs(); track os.id) {
                  <tr class="hover:bg-slate-100/40">
                    <td class="px-4 py-3.5 font-mono font-bold text-blue-400">{{ os.codeOs }}</td>
                    <td class="px-4 py-3.5 font-medium text-white">{{ os.libelle }}</td>
                    <td class="max-w-md px-4 py-3.5 text-slate-600"><span class="line-clamp-2">{{ os.description || '—' }}</span></td>
                    <td class="px-4 py-3.5">
                      <button type="button" (click)="voirEnfants('OS', os.id)" class="rounded-full bg-blue-500/10 px-2.5 py-1 text-[11px] font-semibold text-blue-300 hover:underline" title="Voir les OP de cet OS">{{ opsFor(os.id).length }} OP</button>
                    </td>
                    <td class="px-4 py-3.5 text-right whitespace-nowrap">
                      <ng-container *ngTemplateOutlet="actions; context: { $implicit: os, type: 'OS' }" />
                    </td>
                  </tr>
                } @empty { <ng-container *ngTemplateOutlet="vide; context: { colonnes: 5 }" /> }
              </tbody>
            </table>
          }
          @case ('OP') {
            <table class="w-full text-left text-xs">
              <thead class="border-b border-slate-200 bg-slate-50 uppercase tracking-wider text-slate-600">
                <tr><th class="px-4 py-3.5">Code</th><th class="px-4 py-3.5">Libellé</th><th class="px-4 py-3.5">Objectif stratégique</th><th class="px-4 py-3.5">Activités</th><th class="px-4 py-3.5 text-right">Actions</th></tr>
              </thead>
              <tbody class="divide-y divide-slate-200/60">
                @for (op of filteredOp(); track op.id) {
                  <tr class="hover:bg-slate-100/40">
                    <td class="px-4 py-3.5 font-mono font-bold text-blue-400">{{ op.codeOp }}</td>
                    <td class="px-4 py-3.5"><p class="font-medium text-white">{{ op.libelle }}</p>@if (op.description) {<p class="mt-0.5 line-clamp-1 text-[11px] text-slate-500">{{ op.description }}</p>}</td>
                    <td class="px-4 py-3.5 text-slate-700"><span class="font-mono">{{ op.objectifStrategique?.codeOs }}</span> — {{ op.objectifStrategique?.libelle }}</td>
                    <td class="px-4 py-3.5">
                      <button type="button" (click)="voirEnfants('OP', op.id)" class="rounded-full bg-blue-500/10 px-2.5 py-1 text-[11px] font-semibold text-blue-300 hover:underline" title="Voir les activités de cet OP">{{ activitesFor(op.id).length }} AC</button>
                    </td>
                    <td class="px-4 py-3.5 text-right whitespace-nowrap">
                      <ng-container *ngTemplateOutlet="actions; context: { $implicit: op, type: 'OP' }" />
                    </td>
                  </tr>
                } @empty { <ng-container *ngTemplateOutlet="vide; context: { colonnes: 5 }" /> }
              </tbody>
            </table>
          }
          @case ('AC') {
            <table class="w-full text-left text-xs">
              <thead class="border-b border-slate-200 bg-slate-50 uppercase tracking-wider text-slate-600">
                <tr><th class="px-4 py-3.5">Code</th><th class="px-4 py-3.5">Libellé</th><th class="px-4 py-3.5">Objectif opérationnel</th><th class="px-4 py-3.5">Objectif stratégique</th><th class="px-4 py-3.5 text-right">Actions</th></tr>
              </thead>
              <tbody class="divide-y divide-slate-200/60">
                @for (ac of filteredAc(); track ac.id) {
                  <tr class="hover:bg-slate-100/40">
                    <td class="px-4 py-3.5 font-mono font-bold text-blue-400">{{ ac.codeActivite }}</td>
                    <td class="px-4 py-3.5"><p class="font-medium text-white">{{ ac.libelle }}</p>@if (ac.description) {<p class="mt-0.5 line-clamp-1 text-[11px] text-slate-500">{{ ac.description }}</p>}</td>
                    <td class="px-4 py-3.5 text-slate-700"><span class="font-mono">{{ ac.objectifOperationnel?.codeOp }}</span> — {{ ac.objectifOperationnel?.libelle }}</td>
                    <td class="px-4 py-3.5 text-slate-700 font-mono">{{ osDeActivite(ac)?.codeOs || '—' }}</td>
                    <td class="px-4 py-3.5 text-right whitespace-nowrap">
                      <ng-container *ngTemplateOutlet="actions; context: { $implicit: ac, type: 'AC' }" />
                    </td>
                  </tr>
                } @empty { <ng-container *ngTemplateOutlet="vide; context: { colonnes: 5 }" /> }
              </tbody>
            </table>
          }
        }
      </div>
    </section>
  </div>

  <ng-template #actions let-node let-type="type">
    <app-row-actions [label]="codeOf(type, node)"
      [canEdit]="can(config(type).modifier)" [canDelete]="can(config(type).supprimer)"
      [deleteDisabled]="hasChildren(type, node)" deleteDisabledReason="Supprimez d’abord les éléments rattachés"
      (view)="detailRecord = node" (edit)="openEdit(type, node)" (remove)="askDelete(type, node)" />
  </ng-template>

  <ng-template #vide let-colonnes="colonnes">
    <tr><td [attr.colspan]="colonnes" class="py-10 text-center text-slate-500">
      {{ loading() ? 'Chargement…' : total(tab()) ? 'Aucun élément ne correspond aux critères de filtrage.' : 'Aucun ' + onglet().libelleSingulier + ' enregistré.' }}
    </td></tr>
  </ng-template>

  @if (toDelete(); as cible) {
    <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4 backdrop-blur-sm">
      <div class="w-full max-w-md space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl" role="dialog" aria-modal="true">
        <h3 class="text-lg font-semibold text-rose-600">Confirmer la suppression</h3>
        <p class="text-xs leading-relaxed text-slate-700">Supprimer l’{{ config(cible.type).libelleSingulier }} <strong>{{ codeOf(cible.type, cible.node) }} — {{ cible.node.libelle }}</strong> ? Cette action est irréversible.</p>
        <div class="flex justify-end gap-3 pt-3">
          <button type="button" (click)="toDelete.set(null)" class="rounded-xl bg-slate-100 px-4 py-2 text-xs font-medium text-slate-700">Annuler</button>
          <button type="button" (click)="confirmDelete()" class="rounded-xl bg-rose-600 px-4 py-2 text-xs font-semibold text-white hover:bg-rose-700">Supprimer</button>
        </div>
      </div>
    </div>
  }
  @if (detailRecord) { <app-record-details-dialog [title]="'Détail — ' + onglet().libelleSingulier" [record]="detailRecord" (close)="detailRecord = null" /> }
}
`,
})
export class NomenclatureComponent implements OnInit {
  private readonly service = inject(NomenclatureService);
  private readonly notifications = inject(NotificationService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly osList = signal<ObjectifStrategique[]>([]);
  readonly opList = signal<ObjectifOperationnel[]>([]);
  readonly activiteList = signal<ActiviteCatalogue[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly tab = signal<NodeType>('OS');
  readonly onglet = computed(() => SOUS_ONGLETS[this.tab()]);
  readonly importOpen = signal(false);
  readonly formOpen = signal(false);
  readonly formType = signal<NodeType>('OS');
  readonly formOnglet = computed(() => SOUS_ONGLETS[this.formType()]);
  readonly editId = signal('');
  readonly toDelete = signal<{ type: NodeType; node: ObjectifStrategique | ObjectifOperationnel | ActiviteCatalogue } | null>(null);

  detailRecord: object | null = null;

  // Critères de filtrage (propres au sous-onglet affiché, repris par les exports)
  search = '';
  osFilter = '';
  opFilter = '';
  childFilter: '' | 'AVEC' | 'SANS' = '';

  form = { code: '', libelle: '', description: '', osId: '', opId: '' };

  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    // Le sous-onglet est piloté par l'adresse (?onglet=os|op|ac), choisie depuis la barre latérale.
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const demande = (params.get('onglet') || '').toUpperCase() as NodeType;
      const visibles = this.ongletsVisibles().map((o) => o.type);
      const type = visibles.includes(demande) ? demande : visibles[0] ?? 'OS';
      this.appliquerOnglet(type);
      this.importOpen.set(false);
      this.formOpen.set(false);
      if (type !== demande) {
        this.router.navigate([], { relativeTo: this.route, queryParams: { onglet: type.toLowerCase() }, replaceUrl: true });
      }
    });
    this.refresh();
  }

  can(permission: string): boolean {
    return this.auth.hasPermission(permission);
  }

  config(type: NodeType): SousOnglet {
    return SOUS_ONGLETS[type];
  }

  ongletsVisibles(): SousOnglet[] {
    return Object.values(SOUS_ONGLETS).filter((o) => this.can(o.lire));
  }

  setTab(type: NodeType): void {
    this.appliquerOnglet(type);
    this.router.navigate([], { relativeTo: this.route, queryParams: { onglet: type.toLowerCase() } });
  }

  /** Change de sous-onglet en réinitialisant les filtres qui ne le concernent pas. */
  private appliquerOnglet(type: NodeType): void {
    if (type === this.tab()) return;
    this.tab.set(type);
    this.childFilter = '';
    this.search = '';
    if (type === 'OS') { this.osFilter = ''; this.opFilter = ''; }
    if (type === 'OP') this.opFilter = '';
  }

  /** Depuis un compteur : ouvre le sous-onglet enfant filtré sur l'élément parent. */
  voirEnfants(type: 'OS' | 'OP', id?: string): void {
    if (!id) return;
    if (type === 'OS') {
      this.setTab('OP');
      this.osFilter = id;
    } else {
      const op = this.opList().find((item) => item.id === id);
      this.setTab('AC');
      this.osFilter = op?.objectifStrategique?.id || '';
      this.opFilter = id;
    }
  }

  total(type: NodeType): number {
    return type === 'OS' ? this.osList().length : type === 'OP' ? this.opList().length : this.activiteList().length;
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set('');
    const charger = <T>(peutLire: boolean, source: () => Observable<T[]>, niveau: string): Observable<T[]> =>
      !peutLire ? of([] as T[]) : source().pipe(catchError(() => {
        this.error.set(`Impossible de charger les ${niveau}. Vérifiez vos droits et la disponibilité du serveur.`);
        return of([] as T[]);
      }));
    forkJoin({
      os: charger(this.can('os.lire'), () => this.service.listerOs(), 'objectifs stratégiques'),
      op: charger(this.can('op.lire'), () => this.service.listerOp(), 'objectifs opérationnels'),
      ac: charger(this.can('activite.lire'), () => this.service.listerActivites(), 'activités'),
    }).subscribe((data) => {
      const parCode = <T>(liste: T[], code: (t: T) => string) => [...liste].sort((a, b) => code(a).localeCompare(code(b), 'fr', { numeric: true }));
      this.osList.set(parCode(data.os, (o) => o.codeOs));
      this.opList.set(parCode(data.op, (o) => o.codeOp));
      this.activiteList.set(parCode(data.ac, (o) => o.codeActivite));
      this.loading.set(false);
    });
  }

  // --- Relations ---
  opsFor(osId?: string): ObjectifOperationnel[] {
    return this.opList().filter((op) => op.objectifStrategique?.id === osId);
  }

  activitesFor(opId?: string): ActiviteCatalogue[] {
    return this.activiteList().filter((ac) => ac.objectifOperationnel?.id === opId);
  }

  osDeActivite(ac: ActiviteCatalogue): ObjectifStrategique | undefined {
    const osId = ac.objectifOperationnel?.objectifStrategique?.id
      ?? this.opList().find((op) => op.id === ac.objectifOperationnel?.id)?.objectifStrategique?.id;
    return this.osList().find((os) => os.id === osId);
  }

  hasChildren(type: NodeType, node: { id?: string }): boolean {
    return type === 'OS' ? this.opsFor(node.id).length > 0 : type === 'OP' ? this.activitesFor(node.id).length > 0 : false;
  }

  codeOf(type: NodeType, node: ObjectifStrategique | ObjectifOperationnel | ActiviteCatalogue): string {
    return type === 'OS' ? (node as ObjectifStrategique).codeOs
      : type === 'OP' ? (node as ObjectifOperationnel).codeOp : (node as ActiviteCatalogue).codeActivite;
  }

  // --- Filtres ---
  opsDuFiltre(): ObjectifOperationnel[] {
    return this.osFilter ? this.opsFor(this.osFilter) : this.opList();
  }

  filteredOs(): ObjectifStrategique[] {
    return this.osList().filter((os) =>
      this.correspond(os.codeOs, os.libelle, os.description) && this.filtreEnfants(this.opsFor(os.id).length));
  }

  filteredOp(): ObjectifOperationnel[] {
    return this.opList().filter((op) =>
      this.correspond(op.codeOp, op.libelle, op.description)
      && (!this.osFilter || op.objectifStrategique?.id === this.osFilter)
      && this.filtreEnfants(this.activitesFor(op.id).length));
  }

  filteredAc(): ActiviteCatalogue[] {
    return this.activiteList().filter((ac) =>
      this.correspond(ac.codeActivite, ac.libelle, ac.description)
      && (!this.opFilter || ac.objectifOperationnel?.id === this.opFilter)
      && (!this.osFilter || this.osDeActivite(ac)?.id === this.osFilter));
  }

  /** Lignes affichées dans le sous-onglet courant : ce sont elles qui sont exportées. */
  exportIds(): (string | undefined)[] {
    const tab = this.tab();
    const lignes = tab === 'OS' ? this.filteredOs() : tab === 'OP' ? this.filteredOp() : this.filteredAc();
    return lignes.map((item) => item.id);
  }

  hasActiveFilters(): boolean {
    return !!(this.search.trim() || (this.tab() !== 'OS' && this.osFilter) || (this.tab() === 'AC' && this.opFilter)
      || (this.tab() !== 'AC' && this.childFilter));
  }

  resetFilters(): void {
    this.search = '';
    this.osFilter = '';
    this.opFilter = '';
    this.childFilter = '';
  }

  private filtreEnfants(nombre: number): boolean {
    return !this.childFilter || (this.childFilter === 'AVEC' ? nombre > 0 : nombre === 0);
  }

  private correspond(...valeurs: (string | undefined)[]): boolean {
    const terme = normaliser(this.search);
    return !terme || valeurs.some((valeur) => normaliser(valeur).includes(terme));
  }

  // --- Formulaire (pleine page) ---
  formTitle(): string {
    const libelle = this.formOnglet().libelleSingulier;
    return this.editId() ? `Modifier l’${libelle}` : `Créer un${this.formType() === 'AC' ? 'e' : ''} ${libelle}`;
  }

  codeExemple(): string {
    return ({ OS: 'OS-01', OP: 'OP-01.01', AC: 'AC-01.01.01' })[this.formType()];
  }

  opsDuFormulaire(): ObjectifOperationnel[] {
    return this.form.osId ? this.opsFor(this.form.osId) : this.opList();
  }

  opDuFormulaire(): ObjectifOperationnel | undefined {
    return this.opList().find((op) => op.id === this.form.opId);
  }

  osDeOp(op: ObjectifOperationnel): ObjectifStrategique | undefined {
    return this.osList().find((os) => os.id === op.objectifStrategique?.id);
  }

  /** Choisir un OP aligne le filtre OS sur son objectif stratégique (affichage cohérent). */
  onFormOpChange(): void {
    const op = this.opDuFormulaire();
    if (this.formType() === 'AC' && op?.objectifStrategique?.id) {
      this.form.osId = op.objectifStrategique.id;
    }
  }

  onFormOsChange(): void {
    if (this.formType() === 'AC' && this.form.opId && !this.opsDuFormulaire().some((op) => op.id === this.form.opId)) {
      this.form.opId = '';
    }
  }

  openCreate(type: NodeType): void {
    this.formType.set(type);
    this.editId.set('');
    // Les filtres en cours servent de rattachement par défaut.
    this.form = { code: '', libelle: '', description: '', osId: this.osFilter, opId: type === 'AC' ? this.opFilter : '' };
    this.formOpen.set(true);
  }

  openEdit(type: NodeType, node: ObjectifStrategique | ObjectifOperationnel | ActiviteCatalogue): void {
    this.formType.set(type);
    this.editId.set(node.id || '');
    const op = type === 'AC' ? (node as ActiviteCatalogue).objectifOperationnel : undefined;
    this.form = {
      code: this.codeOf(type, node),
      libelle: node.libelle,
      description: node.description || '',
      osId: type === 'OP' ? (node as ObjectifOperationnel).objectifStrategique?.id || ''
        : type === 'AC' ? this.osDeActivite(node as ActiviteCatalogue)?.id || '' : '',
      opId: op?.id || '',
    };
    this.formOpen.set(true);
  }

  closeForm(): void {
    if (!this.saving()) this.formOpen.set(false);
  }

  save(): void {
    if (this.saving()) return;
    const type = this.formType();
    const code = this.form.code.trim().toUpperCase();
    const libelle = this.form.libelle.trim();
    const description = this.form.description.trim();
    if (!code || !libelle) {
      this.notifications.warning('Le code et le libellé sont obligatoires.');
      return;
    }
    if (type === 'OP' && !this.osList().some((os) => os.id === this.form.osId)) {
      this.notifications.warning('Sélectionnez l’objectif stratégique parent.');
      return;
    }
    if (type === 'AC' && !this.opList().some((op) => op.id === this.form.opId)) {
      this.notifications.warning('Sélectionnez l’objectif opérationnel parent.');
      return;
    }
    const id = this.editId();
    const requete: Observable<unknown> = type === 'OS'
      ? (id ? this.service.modifierOs(id, { codeOs: code, libelle, description }) : this.service.creerOs({ codeOs: code, libelle, description }))
      : type === 'OP'
        ? (id ? this.service.modifierOp(id, { osId: this.form.osId, codeOp: code, libelle, description })
          : this.service.creerOp({ osId: this.form.osId, codeOp: code, libelle, description }))
        : (id ? this.service.modifierActivite(id, { opId: this.form.opId, codeActivite: code, libelle, description })
          : this.service.creerActivite({ opId: this.form.opId, codeActivite: code, libelle, description }));
    this.saving.set(true);
    requete.subscribe({
      next: () => {
        this.saving.set(false);
        this.formOpen.set(false);
        const libelleType = SOUS_ONGLETS[type].libelleSingulier;
        this.notifications.success(`${libelleType.charAt(0).toUpperCase() + libelleType.slice(1)} ${id ? 'mis à jour' : 'créé'} : ${code}.`);
        if (this.tab() !== type) this.setTab(type);
        this.refresh();
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        this.notifications.error(messageErreur(error,
          'Enregistrement impossible. Vérifiez que le code est unique, le rattachement choisi et vos droits.'));
      },
    });
  }

  // --- Suppression ---
  askDelete(type: NodeType, node: ObjectifStrategique | ObjectifOperationnel | ActiviteCatalogue): void {
    if (!node.id || this.hasChildren(type, node)) return;
    this.toDelete.set({ type, node });
  }

  confirmDelete(): void {
    const cible = this.toDelete();
    if (!cible?.node.id) return;
    const id = cible.node.id;
    const requete: Observable<void> = cible.type === 'OS' ? this.service.supprimerOs(id)
      : cible.type === 'OP' ? this.service.supprimerOp(id) : this.service.supprimerActivite(id);
    requete.subscribe({
      next: () => {
        this.toDelete.set(null);
        this.notifications.success(`${this.codeOf(cible.type, cible.node)} supprimé.`);
        this.refresh();
      },
      error: (error: HttpErrorResponse) => {
        this.toDelete.set(null);
        this.notifications.error(messageErreur(error,
          'Suppression impossible : cet élément est peut-être utilisé dans une allocation budgétaire.'));
      },
    });
  }
}

function normaliser(valeur: string | null | undefined): string {
  return (valeur || '').normalize('NFD').replace(/\p{M}/gu, '').toLocaleLowerCase('fr').trim();
}

function messageErreur(error: HttpErrorResponse, defaut: string): string {
  const message = (error.error as { message?: string } | null)?.message;
  return typeof message === 'string' && message ? message : defaut;
}
