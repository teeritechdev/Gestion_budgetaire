import { PageHeaderComponent } from '../../../shared/components/page-header.component';
import { RecordDetailsDialogComponent } from '../../../shared/components/record-details-dialog.component';
import { RowActionsComponent } from '../../../shared/components/row-actions.component';
import { IMPORT_FORMATS } from '../../../core/models/import-formats';
import { ImportCsvPageComponent } from '../../../shared/components/import-csv-page.component';
import { DataActionsComponent } from '../../../shared/components/data-actions.component';
import { FormPageComponent } from '../../../shared/components/form-page.component';
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SearchOptionDirective, SearchSelectComponent } from '../../../shared/components/search-select.component';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { DirectionService } from '../../../core/services/direction.service';
import { StructureService } from '../../../core/services/structure.service';
import { ServiceOrganisationnelService } from '../../../core/services/service-organisationnel.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Direction, DirectionRequest } from '../../../core/models/direction.model';
import { Structure } from '../../../core/models/structure.model';
import { ServiceOrganisationnel } from '../../../core/models/service-organisationnel.model';

@Component({
  selector: 'app-directions-list',
  standalone: true,
  imports: [SearchSelectComponent, SearchOptionDirective, PageHeaderComponent, RecordDetailsDialogComponent, RowActionsComponent, DataActionsComponent, ImportCsvPageComponent, FormPageComponent, CommonModule, FormsModule],
  template: `
@if (importOpen()) {
<app-import-csv-page section="Directions" [format]="importFormats.directions" (back)="importOpen.set(false)" (imported)="refresh()" />
} @else {
@if (formOpen()) {
<app-form-page section="Directions" [title]="editingId() ? 'Modifier la direction' : 'Créer une direction'" [mode]="editingId() ? 'Modification' : 'Création'" subtitle="Une direction est rattachée à une seule Structure et regroupe des services." (back)="formOpen.set(false)">
<form (ngSubmit)="save()">
  <section class="space-y-4">
    <label class="block space-y-1.5 text-xs font-medium text-slate-700">Structure de rattachement <span class="champ-obligatoire" aria-hidden="true">*</span>
      <app-search-select required [(ngModel)]="draft.structureCode" name="structureCode" placeholder="Choisir une structure…" [isDisabled]="!structures().length">
        @for (structure of structures(); track structure.code) { <app-search-option [value]="structure.code" [label]="structure.nom + ' (' + structure.code + ')'" /> }
      </app-search-select>
      @if (!structures().length) {
        <span class="block text-[11px] text-amber-700">Aucune Structure disponible : créez-en une ou vérifiez la permission entite.lire.</span>
      }
    </label>
    <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
      <label class="block space-y-1.5 text-xs font-medium text-slate-700">Code <span class="champ-obligatoire" aria-hidden="true">*</span><input required maxlength="50" [(ngModel)]="draft.code" name="code" class="w-full rounded-xl border border-slate-300 bg-slate-50 px-3 py-2.5 text-sm text-slate-900 focus:outline-none focus:ring-1 focus:ring-blue-500 font-mono uppercase"><span class="block text-[11px] font-normal text-slate-500">Code unique, ex. DIR-FIN.</span></label>
      <label class="block space-y-1.5 text-xs font-medium text-slate-700">Libellé <span class="champ-obligatoire" aria-hidden="true">*</span><input required maxlength="150" [(ngModel)]="draft.libelle" name="libelle" class="w-full rounded-xl border border-slate-300 bg-slate-50 px-3 py-2.5 text-sm text-slate-900 focus:outline-none focus:ring-1 focus:ring-blue-500"></label>
    </div>
    <label class="flex items-center gap-2 text-xs text-slate-700"><input type="checkbox" [(ngModel)]="draft.actif" name="actif" class="h-4 w-4"> Direction active</label>
  </section>
  <div class="form-page-actions">
    <button type="button" (click)="formOpen.set(false)" class="rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-100">Annuler</button>
    <button [disabled]="saving()" class="rounded-xl bg-green-700 px-5 py-2 text-xs font-semibold text-white hover:bg-green-600 disabled:opacity-50">{{ saving() ? 'Enregistrement…' : editingId() ? 'Enregistrer les modifications' : 'Créer la direction' }}</button>
  </div>
</form>
        
</app-form-page>
} @else {
    <div class="mx-auto max-w-7xl space-y-6">
      <app-page-header icon="directions" module="Administration" title="Directions" description="Les directions de chaque structure et les services qu’elles regroupent.">
<app-data-actions ressource="directions" exportPermission="direction.exporter" [ids]="exportIds()" [importPermissions]="['direction.importer']" [importFormat]="importFormats.directions" (importer)="importOpen.set(true)" />@if (auth.hasPermission('direction.creer')) {
          <button type="button" (click)="openForm()" class="whitespace-nowrap rounded-xl bg-green-700 px-4 py-2 text-xs font-semibold text-white hover:bg-green-600">＋ Nouvelle direction</button>
        }
</app-page-header>

      @if (error()) {
        <div role="alert" class="rounded-xl border border-rose-300 bg-rose-50 p-3 text-sm text-rose-700">{{ error() }}</div>
      }

      <div class="flex flex-wrap items-center gap-3 rounded-2xl border border-slate-200 bg-white p-4">
        <input type="text" [(ngModel)]="search" placeholder="Rechercher par code ou libellé…" class="min-w-[220px] flex-1 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs" />
        <select [(ngModel)]="structureFilter" class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-700">
          <option value="">Toutes les Structures</option>
          @for (structure of structures(); track structure.code) { <option [value]="structure.code">{{ structure.nom }} ({{ structure.code }})</option> }
        </select>
      </div>

      <section class="overflow-hidden rounded-2xl border border-slate-200 bg-white">
        <div class="overflow-x-auto">
          <table class="w-full text-left text-xs">
            <thead class="bg-slate-50 text-slate-600"><tr><th class="p-3">Code</th><th class="p-3">Libellé</th><th class="p-3">Structure</th><th class="p-3">Services</th><th class="p-3">État</th><th class="p-3 text-right">Actions</th></tr></thead>
            <tbody class="divide-y divide-slate-200">
              @for (direction of visible(); track direction.id) {
                <tr>
                  <td class="p-3 font-mono">{{ direction.code }}</td>
                  <td class="p-3 font-medium text-white">{{ direction.libelle }}</td>
                  <td class="p-3">{{ structureLabel(direction) }}</td>
                  <td class="p-3">{{ serviceCount(direction.id) }}</td>
                  <td class="p-3">{{ direction.actif ? 'Active' : 'Inactive' }}</td>
                  <td class="p-3">
                    <app-row-actions [label]="direction.code"
                      [canEdit]="auth.hasPermission('direction.modifier')" [canDelete]="auth.hasPermission('direction.supprimer')"
                      (view)="detailRecord = direction" (edit)="edit(direction)" (remove)="remove(direction)" />
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="6" class="p-6 text-center text-slate-500">{{ loading() ? 'Chargement des directions…' : 'Aucune direction trouvée.' }}</td></tr>
              }
            </tbody>
          </table>
          @if (detailRecord) { <app-record-details-dialog title="Direction" [record]="detailRecord" (close)="detailRecord = null" /> }
        </div>
      </section>

      
    </div>
}
}
`,
})
export class DirectionsListComponent implements OnInit {
  readonly importOpen = signal(false);
  readonly importFormats = IMPORT_FORMATS;
  /** Lignes affichées (filtres et tri appliqués) : ce sont elles qui sont exportées. */
  exportIds(): (string | undefined)[] {
    return this.visible().map((item) => item.id);
  }
  readonly auth = inject(AuthService);
  private readonly directionService = inject(DirectionService);
  private readonly structureService = inject(StructureService);
  private readonly serviceService = inject(ServiceOrganisationnelService);
  private readonly notifications = inject(NotificationService);

  readonly directions = signal<Direction[]>([]);
  readonly structures = signal<Structure[]>([]);
  readonly servicesParDirection = signal<Record<string, number>>({});
  readonly formOpen = signal(false);
  readonly saving = signal(false);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly editingId = signal('');

  detailRecord: object | null = null;
  search = '';
  structureFilter = '';
  draft: DirectionRequest = this.emptyDraft();

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set('');
    forkJoin({
      directions: this.directionService.lister().pipe(catchError(() => this.failedLoad('les directions'))),
      structures: this.auth.hasPermission('entite.lire')
        ? this.structureService.listerOuFiltrer().pipe(catchError(() => this.failedLoad('les Structures')))
        : of([] as Structure[]),
      services: this.auth.hasPermission('service.lire')
        ? this.serviceService.lister().pipe(catchError(() => of([] as ServiceOrganisationnel[])))
        : of([] as ServiceOrganisationnel[]),
    }).subscribe((data) => {
      this.directions.set(data.directions as Direction[]);
      this.structures.set(data.structures as Structure[]);
      const compteurs: Record<string, number> = {};
      if (this.auth.hasPermission('service.lire')) {
        for (const direction of data.directions as Direction[]) compteurs[direction.id] = 0;
        for (const service of data.services) {
          compteurs[service.directionId] = (compteurs[service.directionId] ?? 0) + 1;
        }
      }
      this.servicesParDirection.set(compteurs);
      this.loading.set(false);
    });
  }

  visible(): Direction[] {
    const terme = this.search.trim().toLowerCase();
    return this.directions().filter(
      (direction) =>
        (!this.structureFilter || direction.structureCode === this.structureFilter) &&
        (!terme || direction.code.toLowerCase().includes(terme) || direction.libelle.toLowerCase().includes(terme)),
    );
  }

  serviceCount(directionId: string): number | string {
    return this.servicesParDirection()[directionId] ?? '—';
  }

  openForm(): void {
    this.editingId.set('');
    this.draft = this.emptyDraft();
    this.formOpen.set(true);
  }

  edit(direction: Direction): void {
    this.editingId.set(direction.id);
    this.draft = {
      code: direction.code,
      libelle: direction.libelle,
      structureCode: direction.structureCode,
      actif: direction.actif,
    };
    this.formOpen.set(true);
  }

  save(): void {
    const request = {
      ...this.draft,
      code: this.draft.code.trim(),
      libelle: this.draft.libelle.trim(),
    };
    if (!request.code || !request.libelle) {
      this.notifications.warning('Renseignez le code et le libellé.');
      return;
    }
    if (!request.structureCode) {
      this.notifications.warning('Choisissez la structure de rattachement.');
      return;
    }
    this.saving.set(true);
    const request$ = this.editingId()
      ? this.directionService.modifier(this.editingId(), request)
      : this.directionService.creer(request);
    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.formOpen.set(false);
        this.notifications.success('Direction enregistrée.');
        this.refresh();
      },
      error: (error: unknown) => this.failedSave(error),
    });
  }

  remove(direction: Direction): void {
    if (!confirm(`Supprimer la direction « ${direction.libelle} » ?`)) return;
    this.directionService.supprimer(direction.id).subscribe({
      next: () => {
        this.notifications.success('Direction supprimée.');
        this.refresh();
      },
      error: (error: unknown) => this.failedSave(error),
    });
  }

  structureLabel(direction: Direction): string {
    if (!direction.structureCode) return '—';
    return direction.structureNom ? `${direction.structureNom} (${direction.structureCode})` : direction.structureCode;
  }

  private emptyDraft(): DirectionRequest {
    return { code: '', libelle: '', structureCode: '', actif: true };
  }

  private failedLoad(label: string) {
    this.error.set(`Impossible de charger ${label}.`);
    return of([]);
  }

  private failedSave(error: unknown): void {
    this.saving.set(false);
    const message = errorMessage(error);
    this.error.set(message);
    this.notifications.error(message);
  }
}

export function errorMessage(error: unknown): string {
  if (error && typeof error === 'object' && 'error' in error) {
    const body = (error as { error?: { message?: string } }).error;
    if (body?.message) return body.message;
  }
  return 'Opération impossible. Vérifiez vos droits et les rattachements hiérarchiques.';
}
