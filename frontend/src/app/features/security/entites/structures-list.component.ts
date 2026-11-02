import { PageHeaderComponent } from '../../../shared/components/page-header.component';
import { IMPORT_FORMATS } from '../../../core/models/import-formats';
import { ImportCsvPageComponent } from '../../../shared/components/import-csv-page.component';
import { DataActionsComponent } from '../../../shared/components/data-actions.component';
import { FormPageComponent } from '../../../shared/components/form-page.component';
import { RecordDetailsDialogComponent } from '../../../shared/components/record-details-dialog.component';
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { StructureService } from '../../../core/services/structure.service';
import { NotificationService } from '../../../core/services/notification.service';
import { AuthService } from '../../../core/services/auth.service';
import { Structure } from '../../../core/models/structure.model';

@Component({
  selector: 'app-entites-list',
  standalone: true,
  imports: [PageHeaderComponent, DataActionsComponent, ImportCsvPageComponent, FormPageComponent, RecordDetailsDialogComponent, CommonModule, FormsModule],
  template: `
@if (importOpen()) {
<app-import-csv-page section="Structures" [upload]="uploadImport" [format]="importFormats.structures" (back)="importOpen.set(false)" (imported)="loadStructures()" />
} @else {
@if (showModal()) {
<app-form-page section="Structures" [title]="isEditing() ? 'Modifier la structure' : 'Créer une structure'" [mode]="isEditing() ? 'Modification' : 'Création'" subtitle="Identité juridique, coordonnées et rôle de centre de services partagés (CSP)." (back)="closeModal()">
<div class="form-page-card space-y-5">
            

            <form (ngSubmit)="saveStructure()" class="space-y-4">
              <div>
                <label class="block text-xs font-medium text-slate-700 mb-1">Code Structure *</label>
                <input
                  type="text"
                  required
                  minlength="2"
                  maxlength="20"
                  [disabled]="isEditing()"
                  [(ngModel)]="formData.code"
                  name="code"
                  placeholder="UAB_VIE"
                  class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs text-slate-900 uppercase disabled:opacity-50"
                />
              </div>

              <div>
                <label class="block text-xs font-medium text-slate-700 mb-1">Nom / Raison Sociale *</label>
                <input
                  type="text"
                  required
                  minlength="2"
                  maxlength="150"
                  [(ngModel)]="formData.nom"
                  name="nom"
                  placeholder="UAB Vie Assurances"
                  class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs text-slate-900"
                />
              </div>

              <div class="grid grid-cols-1 gap-3 md:grid-cols-2">
                <div>
                  <label class="block text-xs font-medium text-slate-700 mb-1">Sigle</label>
                  <input [(ngModel)]="formData.sigle" name="sigle" maxlength="30" class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs text-slate-900" />
                </div>
                <div>
                  <label class="block text-xs font-medium text-slate-700 mb-1">Téléphone</label>
                  <input [(ngModel)]="formData.telephone" name="telephone" maxlength="30" type="tel" class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs text-slate-900" />
                </div>
                <div>
                  <label class="block text-xs font-medium text-slate-700 mb-1">Email</label>
                  <input [(ngModel)]="formData.email" name="email" maxlength="150" type="email" class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs text-slate-900" />
                </div>
                <div>
                  <label class="block text-xs font-medium text-slate-700 mb-1">Adresse</label>
                  <input [(ngModel)]="formData.adresse" name="adresse" maxlength="255" class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs text-slate-900" />
                </div>
                <div>
                  <label class="block text-xs font-medium text-slate-700 mb-1">Ville</label>
                  <input [(ngModel)]="formData.ville" name="ville" maxlength="100" class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs text-slate-900" />
                </div>
                <div>
                  <label class="block text-xs font-medium text-slate-700 mb-1">Pays</label>
                  <input [(ngModel)]="formData.pays" name="pays" maxlength="100" class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs text-slate-900" />
                </div>
                <div>
                  <label class="block text-xs font-medium text-slate-700 mb-1">Secteur d'activité</label>
                  <input [(ngModel)]="formData.secteurActivite" name="secteurActivite" maxlength="100" class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs text-slate-900" />
                </div>
                <div>
                  <label class="block text-xs font-medium text-slate-700 mb-1">Forme juridique</label>
                  <input [(ngModel)]="formData.formeJuridique" name="formeJuridique" maxlength="100" class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs text-slate-900" />
                </div>
                <div>
                  <label class="block text-xs font-medium text-slate-700 mb-1">Numéro fiscal</label>
                  <input [(ngModel)]="formData.numeroFiscal" name="numeroFiscal" maxlength="100" class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs text-slate-900" />
                </div>
                <div>
                  <label class="block text-xs font-medium text-slate-700 mb-1">Site web</label>
                  <input [(ngModel)]="formData.siteWeb" name="siteWeb" maxlength="255" type="url" class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs text-slate-900" />
                </div>
                <div class="md:col-span-2">
                  <label class="block text-xs font-medium text-slate-700 mb-1">Logo (URL ou chemin)</label>
                  <input [(ngModel)]="formData.logo" name="logo" maxlength="512" class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs text-slate-900" />
                </div>
              </div>


              @if (formError()) {
                <p class="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-xs text-rose-700" role="alert">
                  {{ formError() }}
                </p>
              }

              <div class="form-page-actions">
                <button
                  type="button"
                  (click)="closeModal()"
                  [disabled]="saving()"
                  class="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-700 text-slate-700 text-xs font-medium">
                  Annuler
                </button>
                <button
                  type="submit"
                  [disabled]="saving()"
                  class="px-4 py-2 rounded-xl bg-green-700 hover:bg-green-600 text-white text-xs font-medium disabled:cursor-not-allowed disabled:opacity-50">
                  {{ saving() ? 'Enregistrement…' : 'Enregistrer' }}
                </button>
              </div>
            </form>
          </div>
        
</app-form-page>
} @else {
    <div class="space-y-6 max-w-7xl mx-auto">
      <app-page-header icon="structure" module="Administration" title="Structures" description="Les sociétés du groupe, leur identité juridique et leurs coordonnées.">
<app-data-actions ressource="structures" exportPermission="entite.exporter" [ids]="exportIds()" [importPermissions]="['entite.importer']" (importer)="importOpen.set(true)" />

          <button
            [hidden]="!auth.hasPermission('entite.creer')"
            (click)="openCreateModal()"
            class="px-4 py-2 rounded-xl bg-green-700 hover:bg-green-600 text-white text-xs font-medium shadow-lg shadow-blue-600/30 transition-all flex items-center gap-1.5">
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            Nouvelle structure
          </button>
</app-page-header>

      <!-- Critères de filtrage -->
      <div class="bg-white border border-slate-200 rounded-2xl p-4 shadow-lg flex flex-wrap items-center gap-3">
        <div class="flex-1 min-w-[220px]">
          <div class="relative">
            <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-500">
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
            <input
              type="text"
              [(ngModel)]="searchFilter"
              placeholder="Rechercher par code, nom, sigle, e-mail ou n° fiscal..."
              aria-label="Rechercher une structure"
              class="w-full pl-9 pr-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
          </div>
        </div>


        <select [(ngModel)]="villeFilter" aria-label="Filtrer par ville"
          class="bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-700 py-2 px-3 focus:outline-none focus:ring-1 focus:ring-blue-500">
          <option value="">Toutes les villes</option>
          @for (ville of villes(); track ville) { <option [value]="ville">{{ ville }}</option> }
        </select>

        <select [(ngModel)]="paysFilter" aria-label="Filtrer par pays"
          class="bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-700 py-2 px-3 focus:outline-none focus:ring-1 focus:ring-blue-500">
          <option value="">Tous les pays</option>
          @for (pays of paysListe(); track pays) { <option [value]="pays">{{ pays }}</option> }
        </select>

        <button type="button" (click)="resetFilters()" title="Réinitialiser les filtres" aria-label="Réinitialiser les filtres"
          class="p-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-600 transition-colors">
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>

        <p class="w-full text-[11px] text-slate-500">
          {{ filtered().length }} structure(s) affichée(s) sur {{ structures().length }}
          @if (hasActiveFilters()) { · filtres actifs : les exports porteront sur ces lignes uniquement }
        </p>
      </div>

      <!-- Table des Entités -->
      <div class="bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-xl">
        <table class="w-full text-left text-xs">
          <thead class="bg-slate-50/80 text-slate-600 uppercase tracking-wider border-b border-slate-200">
            <tr>
              <th class="py-3.5 px-4">Code Structure</th>
              <th class="py-3.5 px-4">Raison Sociale / Nom</th>
              <th class="py-3.5 px-4">Sigle</th>
              <th class="py-3.5 px-4">Ville</th>
              <th class="py-3.5 px-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-200/60">
            @for (structure of filtered(); track structure.code) {
              <tr class="hover:bg-slate-100/40 transition-colors">
                <td class="py-3.5 px-4 font-mono font-bold text-blue-400">{{ structure.code }}</td>
                <td class="py-3.5 px-4 font-medium text-white">{{ structure.nom }}</td>
                <td class="py-3.5 px-4">{{ structure.sigle || '—' }}</td>
                <td class="py-3.5 px-4">{{ structure.ville || '—' }}</td>
                <td class="py-3.5 px-4 text-right">
                  <div class="flex items-center justify-end gap-1.5">
                    <button type="button" (click)="showDetails('Structure', structure)" class="inline-flex h-10 w-10 items-center justify-center rounded-lg text-blue-700 transition-colors hover:bg-blue-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2" title="Voir les détails" aria-label="Voir les détails"><svg aria-hidden="true" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M2.25 12s3.5-6.5 9.75-6.5 9.75 6.5 9.75 6.5-3.5 6.5-9.75 6.5S2.25 12 2.25 12Z"/><circle cx="12" cy="12" r="3"/></svg></button><button
                      [hidden]="!auth.hasPermission('entite.modifier')"
                      (click)="openEditModal(structure)"
                      class="p-1.5 rounded-lg text-slate-600 hover:text-blue-400 hover:bg-green-600/10 transition-colors"
                      title="Modifier">
                      <svg aria-hidden="true" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                    </button>
                    <button
                      [hidden]="!auth.hasPermission('entite.supprimer')"
                      (click)="confirmDelete(structure)"
                      class="p-1.5 rounded-lg text-rose-600 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
                      title="Supprimer">
                      <svg aria-hidden="true" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="5" class="py-8 text-center text-slate-500">
                  {{ structures().length ? 'Aucune structure ne correspond aux critères de filtrage.' : 'Aucune structure enregistrée.' }}
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <!-- Modal de création et de modification d'une structure -->
      

      <!-- Delete Confirmation -->
      @if (structureToDelete()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
          <div class="bg-white border border-slate-200 rounded-2xl w-full max-w-md p-6 shadow-2xl space-y-4">
            <h3 class="text-lg font-semibold text-rose-400">Confirmer la suppression</h3>
            <p class="text-xs text-slate-700 leading-relaxed">
              Confirmez-vous la suppression de la structure <strong class="text-white">{{ structureToDelete()?.nom }}</strong> ({{ structureToDelete()?.code }}) ?
            </p>
            <div class="flex items-center justify-end gap-3 pt-3">
              <button
                (click)="structureToDelete.set(null)"
                class="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-700 text-slate-700 text-xs font-medium">
                Annuler
              </button>
              <button
                (click)="executeDelete()"
                class="rounded-xl bg-rose-600 px-4 py-2 text-xs font-semibold text-white hover:bg-rose-700">Supprimer</button>
            </div>
          </div>
        </div>
      }
    </div>
    @if (detailRecord) { <app-record-details-dialog [title]="detailTitle" [record]="detailRecord" (close)="detailRecord = null" /> }
}
}
`
})
export class StructuresListComponent implements OnInit {
  readonly importOpen = signal(false);
  readonly importFormats = IMPORT_FORMATS;
  readonly uploadImport = (fichier: File) => this.structureService.importerCsv(fichier);
  /** Lignes affichées (filtres et tri appliqués) : ce sont elles qui sont exportées. */
  exportIds(): (string | undefined)[] {
    return this.filtered().map((item) => item.code);
  }
  detailTitle = '';
  detailRecord: object | null = null;
  showDetails(title: string, record: object): void { this.detailTitle = title; this.detailRecord = record; }

  private readonly structureService = inject(StructureService);
  private readonly notify = inject(NotificationService);
  readonly auth = inject(AuthService);

  structures = signal<Structure[]>([]);

  // Critères de filtrage (appliqués côté écran, repris tels quels par les exports)
  searchFilter = '';
  villeFilter = '';
  paysFilter = '';

  filtered(): Structure[] {
    const terme = this.normaliser(this.searchFilter);
    return this.structures().filter((s) =>
      (!terme || [s.code, s.nom, s.sigle, s.email, s.numeroFiscal]
        .some((valeur) => this.normaliser(valeur).includes(terme))) &&
      (!this.villeFilter || (s.ville || '').trim() === this.villeFilter) &&
      (!this.paysFilter || (s.pays || '').trim() === this.paysFilter));
  }

  villes(): string[] {
    return this.valeursDistinctes((s) => s.ville);
  }

  paysListe(): string[] {
    return this.valeursDistinctes((s) => s.pays);
  }

  hasActiveFilters(): boolean {
    return !!(this.searchFilter.trim() || this.villeFilter || this.paysFilter);
  }

  resetFilters(): void {
    this.searchFilter = '';
    this.villeFilter = '';
    this.paysFilter = '';
  }

  private valeursDistinctes(champ: (s: Structure) => string | null | undefined): string[] {
    const valeurs = this.structures().map((s) => (champ(s) || '').trim()).filter(Boolean);
    return [...new Set(valeurs)].sort((a, b) => a.localeCompare(b, 'fr'));
  }

  private normaliser(valeur: string | null | undefined): string {
    return (valeur || '').normalize('NFD').replace(/\p{M}/gu, '').toLocaleLowerCase('fr').trim();
  }

  showModal = signal(false);
  isEditing = signal(false);
  saving = signal(false);
  formError = signal('');
  formData: Partial<Structure> = { code: '', nom: '' };

  structureToDelete = signal<Structure | null>(null);

  ngOnInit() {
    this.loadStructures();
  }


  loadStructures() {
    this.structureService.listerOuFiltrer().subscribe({
      next: (res) => this.structures.set(res),
      error: () => this.notify.error('Erreur de chargement des structures')
    });
  }

  openCreateModal() {
    this.isEditing.set(false);
    this.formData = { code: '', nom: '' };
    this.formError.set('');
    this.showModal.set(true);
  }

  openEditModal(e: Structure) {
    this.isEditing.set(true);
    this.formData = { ...e };
    this.formError.set('');
    this.showModal.set(true);
  }

  closeModal() {
    if (!this.saving()) this.showModal.set(false);
  }

  saveStructure() {
    if (this.saving()) return;
    const code = this.formData.code?.trim().toUpperCase() ?? '';
    const nom = this.formData.nom?.trim() ?? '';
    if (code.length < 2 || nom.length < 2) {
      this.formError.set('Le code et le nom doivent contenir au moins 2 caractères.');
      return;
    }

    const payload: Partial<Structure> = {
      ...this.formData,
      code,
      nom
    };

    this.formError.set('');
    this.saving.set(true);
    const handleError = (err: { error?: { message?: string; detail?: string } | string; status?: number }) => {
      this.saving.set(false);
      const errorBody = err.error as { message?: string; detail?: string } | string | null | undefined;
      const message =
        typeof errorBody === 'string'
          ? errorBody
          : errorBody?.message || errorBody?.detail;
      const displayMessage = message || `Enregistrement impossible (HTTP ${err.status ?? 'inconnu'}).`;
      this.formError.set(displayMessage);
      this.notify.error(displayMessage);
    };

    if (this.isEditing()) {
      this.structureService.modifier(this.formData.code!, payload).subscribe({
        next: () => {
          this.saving.set(false);
          this.notify.success('Structure modifiée avec succès');
          this.closeModal();
          this.loadStructures();
        },
        error: handleError
      });
    } else {
      this.structureService.creer(payload).subscribe({
        next: () => {
          this.saving.set(false);
          this.notify.success('Structure créée avec succès');
          this.closeModal();
          this.loadStructures();
        },
        error: handleError
      });
    }
  }

  confirmDelete(e: Structure) {
    this.structureToDelete.set(e);
  }

  executeDelete() {
    const e = this.structureToDelete();
    if (!e) return;

    this.structureService.supprimer(e.code).subscribe({
      next: () => {
        this.notify.success('Structure supprimée');
        this.structureToDelete.set(null);
        this.loadStructures();
      },
      error: (err) => this.notify.error(err.error?.message || 'Suppression impossible')
    });
  }



  private downloadFile(blob: Blob, filename: string) {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
    this.notify.success(`Fichier ${filename} téléchargé`);
  }
}
