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
import { ServiceOrganisationnelService } from '../../../core/services/service-organisationnel.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Direction } from '../../../core/models/direction.model';
import {
  ServiceOrganisationnel,
  ServiceOrganisationnelRequest,
} from '../../../core/models/service-organisationnel.model';
import { errorMessage } from './directions-list.component';

@Component({
  selector: 'app-services-list',
  standalone: true,
  imports: [SearchSelectComponent, SearchOptionDirective, PageHeaderComponent, RecordDetailsDialogComponent, RowActionsComponent, DataActionsComponent, ImportCsvPageComponent, FormPageComponent, CommonModule, FormsModule],
  template: `
@if (importOpen()) {
<app-import-csv-page section="Services" [format]="importFormats.services" (back)="importOpen.set(false)" (imported)="refresh()" />
} @else {
@if (formOpen()) {
<app-form-page section="Services" [title]="editingId() ? 'Modifier le service' : 'Créer un service'" [mode]="editingId() ? 'Modification' : 'Création'" subtitle="Un service est rattaché à une direction ; les salariés y sont affectés." (back)="formOpen.set(false)">
<form (ngSubmit)="save()">
  <section class="space-y-4">
    <label class="block space-y-1.5 text-xs font-medium text-slate-700">Direction <span class="champ-obligatoire" aria-hidden="true">*</span>
      <app-search-select required [(ngModel)]="draft.directionId" name="directionId" placeholder="Choisir une direction">
        @for (direction of directions(); track direction.id) { <app-search-option [value]="direction.id" [label]="direction.libelle + ' (' + direction.code + ') · ' + structuresLabel(direction)" /> }
      </app-search-select>
    </label>
    <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
      <label class="block space-y-1.5 text-xs font-medium text-slate-700">Code <span class="champ-obligatoire" aria-hidden="true">*</span><input required maxlength="50" [(ngModel)]="draft.code" name="code" class="w-full rounded-xl border border-slate-300 bg-slate-50 px-3 py-2.5 text-sm text-slate-900 focus:outline-none focus:ring-1 focus:ring-blue-500 font-mono uppercase"><span class="block text-[11px] font-normal text-slate-500">Code unique, ex. SVC-COMPTA.</span></label>
      <label class="block space-y-1.5 text-xs font-medium text-slate-700">Libellé <span class="champ-obligatoire" aria-hidden="true">*</span><input required maxlength="150" [(ngModel)]="draft.libelle" name="libelle" class="w-full rounded-xl border border-slate-300 bg-slate-50 px-3 py-2.5 text-sm text-slate-900 focus:outline-none focus:ring-1 focus:ring-blue-500"></label>
    </div>
    <label class="flex items-center gap-2 text-xs text-slate-700"><input type="checkbox" [(ngModel)]="draft.actif" name="actif" class="h-4 w-4"> Service actif</label>
  </section>
  <div class="form-page-actions">
    <button type="button" (click)="formOpen.set(false)" class="rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-100">Annuler</button>
    <button [disabled]="saving()" class="rounded-xl bg-green-700 px-5 py-2 text-xs font-semibold text-white hover:bg-green-600 disabled:opacity-50">{{ saving() ? 'Enregistrement…' : editingId() ? 'Enregistrer les modifications' : 'Créer le service' }}</button>
  </div>
</form>
        
</app-form-page>
} @else {
    <div class="mx-auto max-w-7xl space-y-6">
      <app-page-header icon="services" module="Administration" title="Services" description="Les services opérationnels rattachés à chaque direction.">
<app-data-actions ressource="services" exportPermission="service.exporter" [ids]="exportIds()" [importPermissions]="['service.importer']" [importFormat]="importFormats.services" (importer)="importOpen.set(true)" />@if (auth.hasPermission('service.creer')) {
          <button type="button" (click)="openForm()" [disabled]="!directions().length" class="whitespace-nowrap rounded-xl bg-green-700 px-4 py-2 text-xs font-semibold text-white hover:bg-green-600 disabled:opacity-50">＋ Nouveau service</button>
        }
</app-page-header>

      @if (error()) {
        <div role="alert" class="rounded-xl border border-rose-300 bg-rose-50 p-3 text-sm text-rose-700">{{ error() }}</div>
      }
      @if (auth.hasPermission('service.creer') && !loading() && !directions().length) {
        <div class="rounded-xl border border-amber-300 bg-amber-50 p-3 text-sm text-amber-800">
          Créez d’abord une direction (onglet Directions) avant d’ajouter un service, ou vérifiez la permission direction.lire.
        </div>
      }

      <div class="flex flex-wrap items-center gap-3 rounded-2xl border border-slate-200 bg-white p-4">
        <input type="text" [(ngModel)]="search" placeholder="Rechercher par code ou libellé…" class="min-w-[220px] flex-1 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs" />
        <select [(ngModel)]="directionFilter" class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-700">
          <option value="">Toutes les directions</option>
          @for (direction of directions(); track direction.id) { <option [value]="direction.id">{{ direction.libelle }} ({{ direction.code }})</option> }
        </select>
      </div>

      <section class="overflow-hidden rounded-2xl border border-slate-200 bg-white">
        <div class="overflow-x-auto">
          <table class="w-full text-left text-xs">
            <thead class="bg-slate-50 text-slate-600"><tr><th class="p-3">Code</th><th class="p-3">Libellé</th><th class="p-3">Direction</th><th class="p-3">Structure</th><th class="p-3">État</th><th class="p-3 text-right">Actions</th></tr></thead>
            <tbody class="divide-y divide-slate-200">
              @for (service of visible(); track service.id) {
                <tr>
                  <td class="p-3 font-mono">{{ service.code }}</td>
                  <td class="p-3 font-medium text-white">{{ service.libelle }}</td>
                  <td class="p-3">{{ service.directionLibelle }} ({{ service.directionCode }})</td>
                  <td class="p-3">{{ service.structureCode || '—' }}</td>
                  <td class="p-3">{{ service.actif ? 'Actif' : 'Inactif' }}</td>
                  <td class="p-3">
                    <app-row-actions [label]="service.code"
                      [canEdit]="auth.hasPermission('service.modifier')" [canDelete]="auth.hasPermission('service.supprimer')"
                      (view)="detailRecord = service" (edit)="edit(service)" (remove)="remove(service)" />
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="6" class="p-6 text-center text-slate-500">{{ loading() ? 'Chargement des services…' : 'Aucun service trouvé.' }}</td></tr>
              }
            </tbody>
          </table>
          @if (detailRecord) { <app-record-details-dialog title="Service" [record]="detailRecord" (close)="detailRecord = null" /> }
        </div>
      </section>

      
    </div>
}
}
`,
})
export class ServicesListComponent implements OnInit {
  readonly importOpen = signal(false);
  readonly importFormats = IMPORT_FORMATS;
  /** Lignes affichées (filtres et tri appliqués) : ce sont elles qui sont exportées. */
  exportIds(): (string | undefined)[] {
    return this.visible().map((item) => item.id);
  }
  readonly auth = inject(AuthService);
  private readonly directionService = inject(DirectionService);
  private readonly serviceService = inject(ServiceOrganisationnelService);
  private readonly notifications = inject(NotificationService);

  readonly services = signal<ServiceOrganisationnel[]>([]);
  readonly directions = signal<Direction[]>([]);
  readonly formOpen = signal(false);
  readonly saving = signal(false);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly editingId = signal('');

  detailRecord: object | null = null;
  search = '';
  directionFilter = '';
  draft: ServiceOrganisationnelRequest = this.emptyDraft();

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set('');
    forkJoin({
      services: this.serviceService.lister().pipe(catchError(() => this.failedLoad('les services'))),
      directions: this.auth.hasPermission('direction.lire')
        ? this.directionService.lister().pipe(catchError(() => this.failedLoad('les directions')))
        : of([] as Direction[]),
    }).subscribe((data) => {
      this.services.set(data.services as ServiceOrganisationnel[]);
      this.directions.set(data.directions as Direction[]);
      this.loading.set(false);
    });
  }

  structuresLabel(direction: Direction): string {
    return direction.structureCode || '—';
  }

  visible(): ServiceOrganisationnel[] {
    const terme = this.search.trim().toLowerCase();
    return this.services().filter(
      (service) =>
        (!this.directionFilter || service.directionId === this.directionFilter) &&
        (!terme || service.code.toLowerCase().includes(terme) || service.libelle.toLowerCase().includes(terme)),
    );
  }

  openForm(): void {
    this.editingId.set('');
    this.draft = { ...this.emptyDraft(), directionId: this.directionFilter || this.directions()[0]?.id || '' };
    this.formOpen.set(true);
  }

  edit(service: ServiceOrganisationnel): void {
    this.editingId.set(service.id);
    this.draft = {
      code: service.code,
      libelle: service.libelle,
      directionId: service.directionId,
      actif: service.actif,
    };
    this.formOpen.set(true);
  }

  save(): void {
    const request = { ...this.draft, code: this.draft.code.trim(), libelle: this.draft.libelle.trim() };
    if (!request.code || !request.libelle || !request.directionId) {
      this.notifications.warning('Renseignez le code, le libellé et la direction.');
      return;
    }
    this.saving.set(true);
    const request$ = this.editingId()
      ? this.serviceService.modifier(this.editingId(), request)
      : this.serviceService.creer(request);
    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.formOpen.set(false);
        this.notifications.success('Service enregistré.');
        this.refresh();
      },
      error: (error: unknown) => this.failedSave(error),
    });
  }

  remove(service: ServiceOrganisationnel): void {
    if (!confirm(`Supprimer le service « ${service.libelle} » ?`)) return;
    this.serviceService.supprimer(service.id).subscribe({
      next: () => {
        this.notifications.success('Service supprimé.');
        this.refresh();
      },
      error: (error: unknown) => this.failedSave(error),
    });
  }

  private emptyDraft(): ServiceOrganisationnelRequest {
    return { code: '', libelle: '', directionId: '', actif: true };
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
