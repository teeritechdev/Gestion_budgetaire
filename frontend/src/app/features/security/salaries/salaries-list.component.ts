import { PageHeaderComponent } from '../../../shared/components/page-header.component';
import { RowActionsComponent } from '../../../shared/components/row-actions.component';
import { IMPORT_FORMATS } from '../../../core/models/import-formats';
import { ImportCsvPageComponent } from '../../../shared/components/import-csv-page.component';
import { DataActionsComponent } from '../../../shared/components/data-actions.component';
import { FormPageComponent } from '../../../shared/components/form-page.component';
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { SalarieService } from '../../../core/services/salarie.service';
import { ServiceOrganisationnelService } from '../../../core/services/service-organisationnel.service';
import { UtilisateurService } from '../../../core/services/utilisateur.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Salarie, SalarieRequest, SexeSalarie } from '../../../core/models/salarie.model';
import { ServiceOrganisationnel } from '../../../core/models/service-organisationnel.model';
import { Utilisateur } from '../../../core/models/utilisateur.model';
import { RecordDetailsDialogComponent } from '../../../shared/components/record-details-dialog.component';
import { errorMessage } from '../structures/directions-list.component';

type SalarieDraft = Omit<SalarieRequest, 'sexe'> & { sexe: SexeSalarie | '' };

@Component({
  selector: 'app-salaries-list',
  standalone: true,
  imports: [PageHeaderComponent, RowActionsComponent, DataActionsComponent, ImportCsvPageComponent, FormPageComponent, CommonModule, FormsModule, RecordDetailsDialogComponent],
  template: `
@if (importOpen()) {
<app-import-csv-page section="Salariés" [format]="importFormats.salaries" (back)="importOpen.set(false)" (imported)="loadReferences(); refresh()" />
} @else {
@if (formOpen()) {
<app-form-page section="Salariés" [title]="editingId() ? 'Modifier le salarié' : 'Créer un salarié'" [mode]="editingId() ? 'Modification' : 'Création'" subtitle="Identité, informations professionnelles et affectation du collaborateur." (back)="formOpen.set(false)">
<form (ngSubmit)="save()">

            <section class="space-y-3 rounded-xl border border-slate-200 p-4">
              <h3 class="text-sm font-semibold text-slate-900">Identité</h3>
              <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <label class="block space-y-1 text-xs">Nom de famille *<input required maxlength="100" [(ngModel)]="draft.nom" name="nom" placeholder="Ex. : Traoré" class="w-full rounded-lg border border-slate-300 p-2"></label>
                <label class="block space-y-1 text-xs">Prénom *<input required maxlength="100" [(ngModel)]="draft.prenom" name="prenom" placeholder="Ex. : Aïssata" class="w-full rounded-lg border border-slate-300 p-2"></label>
                <label class="block space-y-1 text-xs">Sexe *
                  <select required [(ngModel)]="draft.sexe" name="sexe" class="w-full rounded-lg border border-slate-300 p-2">
                    <option value="">Sélectionner</option>
                    <option value="HOMME">Masculin</option>
                    <option value="FEMME">Féminin</option>
                    <option value="AUTRE">Autre</option>
                    <option value="NON_RENSEIGNE">Non renseigné</option>
                  </select>
                </label>
                <label class="block space-y-1 text-xs">Téléphone<input type="tel" maxlength="30" [(ngModel)]="draft.telephone" name="telephone" placeholder="Ex. : +226 70 00 00 00" class="w-full rounded-lg border border-slate-300 p-2"></label>
              </div>
            </section>

            <section class="space-y-3 rounded-xl border border-slate-200 p-4">
              <h3 class="text-sm font-semibold text-slate-900">Informations professionnelles</h3>
              <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <label class="block space-y-1 text-xs">Matricule *<input required maxlength="50" [(ngModel)]="draft.matricule" name="matricule" placeholder="Ex. : EMP-2026-001" class="w-full rounded-lg border border-slate-300 p-2 uppercase"></label>
                <label class="block space-y-1 text-xs">Adresse e-mail professionnelle *<input type="email" required maxlength="150" [(ngModel)]="draft.email" name="email" placeholder="prenom.nom@uab.bf" class="w-full rounded-lg border border-slate-300 p-2"></label>
                <label class="block space-y-1 text-xs">Fonction / poste<input maxlength="100" [(ngModel)]="draft.poste" name="poste" placeholder="Ex. : Contrôleur de gestion" class="w-full rounded-lg border border-slate-300 p-2"></label>
                <label class="block space-y-1 text-xs">Date d’embauche *<input type="date" required [(ngModel)]="draft.dateEmbauche" name="dateEmbauche" class="w-full rounded-lg border border-slate-300 p-2"></label>
              </div>
            </section>

            <section class="space-y-3 rounded-xl border border-slate-200 p-4">
              <h3 class="text-sm font-semibold text-slate-900">Affectation</h3>
              <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <label class="block space-y-1 text-xs">Service de rattachement *
                  <select required [(ngModel)]="draft.serviceId" name="serviceId" class="w-full rounded-lg border border-slate-300 p-2">
                    <option value="">Choisir un service</option>
                    @for (service of services(); track service.id) { <option [value]="service.id">{{ service.libelle }} ({{ service.code }})</option> }
                  </select>
                </label>
                <label class="block space-y-1 text-xs">Manager direct (N+1)
                  <select [(ngModel)]="draft.managerId" name="managerId" class="w-full rounded-lg border border-slate-300 p-2">
                    <option [ngValue]="null">Aucun manager</option>
                    @for (manager of availableManagers(); track manager.id) { <option [ngValue]="manager.id">{{ manager.prenom }} {{ manager.nom }} — {{ manager.matricule }}</option> }
                  </select>
                </label>
              </div>
              <label class="flex items-center gap-2 text-xs"><input type="checkbox" [(ngModel)]="draft.actif" name="actif"> Salarié actif (un salarié inactif ne peut pas se connecter)</label>
            </section>

            <div class="form-page-actions">
              <button type="button" (click)="formOpen.set(false)" class="rounded-lg border px-3 py-2 text-xs">Annuler</button>
              <button [disabled]="saving()" class="rounded-lg bg-green-700 px-4 py-2 text-xs font-semibold text-white disabled:opacity-50">{{ saving() ? 'Enregistrement…' : 'Enregistrer' }}</button>
            </div>
          </form>
        
</app-form-page>
} @else {
    <div class="mx-auto max-w-7xl space-y-6">
      <app-page-header icon="salaries" module="Administration" title="Salariés" description="Les dossiers des collaborateurs : identité, poste, service et manager.">
<app-data-actions ressource="salaries" exportPermission="salarie.exporter" [ids]="exportIds()" [importPermissions]="['salarie.importer']" (importer)="importOpen.set(true)" />@if (auth.hasPermission('salarie.creer')) {
          <button type="button" (click)="openForm()" [disabled]="!services().length" class="whitespace-nowrap rounded-xl bg-green-700 px-4 py-2 text-xs font-semibold text-white hover:bg-green-600 disabled:opacity-50">＋ Nouveau salarié</button>
        }
</app-page-header>

      @if (error()) {
        <div role="alert" class="rounded-xl border border-rose-300 bg-rose-50 p-3 text-sm text-rose-700">{{ error() }}</div>
      }
      @if (auth.hasPermission('salarie.creer') && !loading() && !services().length) {
        <div class="rounded-xl border border-amber-300 bg-amber-50 p-3 text-sm text-amber-800">
          Créez d’abord un service (onglet Services) pour pouvoir y rattacher des salariés, ou vérifiez la permission service.lire.
        </div>
      }

      <div class="flex flex-wrap items-center gap-3 rounded-2xl border border-slate-200 bg-white p-4">
        <input type="text" [(ngModel)]="search" (keyup.enter)="refresh()" placeholder="Rechercher par nom, prénom, matricule ou e-mail…" class="min-w-[220px] flex-1 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs" />
        <select [(ngModel)]="serviceFilter" (change)="refresh()" class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-700">
          <option value="">Tous les services</option>
          @for (service of services(); track service.id) { <option [value]="service.id">{{ service.libelle }} ({{ service.code }})</option> }
        </select>
        <select [(ngModel)]="actifFilter" (change)="refresh()" class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-700">
          <option value="">Tous statuts</option>
          <option value="true">Actif</option>
          <option value="false">Inactif</option>
        </select>
        <button type="button" (click)="refresh()" class="rounded-xl bg-blue-700 px-3 py-2 text-xs font-medium text-white hover:bg-blue-800">Rechercher</button>
        <button type="button" (click)="resetFilters()" class="rounded-xl bg-slate-100 px-3 py-2 text-xs text-slate-600 hover:bg-slate-200">Réinitialiser</button>
      </div>

      <section class="overflow-hidden rounded-2xl border border-slate-200 bg-white">
        <div class="overflow-x-auto">
          <table class="w-full text-left text-xs">
            <thead class="bg-slate-50 text-slate-600">
              <tr>
                <th class="p-3">Salarié</th><th class="p-3">Matricule</th><th class="p-3">Poste</th><th class="p-3">Service</th>
                <th class="p-3">Manager (N+1)</th><th class="p-3">Compte</th><th class="p-3">Statut</th><th class="p-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-200">
              @for (salarie of salaries(); track salarie.id) {
                <tr>
                  <td class="p-3">
                    <p class="font-semibold text-white">{{ salarie.prenom }} {{ salarie.nom }}</p>
                    <p class="text-[11px] text-slate-500">{{ salarie.email }}</p>
                    @if (salarie.telephone) { <p class="text-[11px] text-slate-500">{{ salarie.telephone }}</p> }
                  </td>
                  <td class="p-3 font-mono">{{ salarie.matricule }}</td>
                  <td class="p-3">{{ salarie.poste || '—' }}</td>
                  <td class="p-3">{{ salarie.service.libelle || 'Non affecté' }}</td>
                  <td class="p-3">{{ managerName(salarie.managerId) || '— (Premier niveau)' }}</td>
                  <td class="p-3">
                    @if (!canSeeAccounts) { <span class="text-slate-400">—</span> }
                    @else if (accountFor(salarie.id); as compte) { <span class="text-blue-700">{{ compte.username }}</span> }
                    @else { <span class="text-slate-500">Aucun compte</span> }
                  </td>
                  <td class="p-3">{{ salarie.actif ? 'Actif' : 'Inactif' }}</td>
                  <td class="p-3">
                    <app-row-actions [label]="salarie.prenom + ' ' + salarie.nom"
                      [canEdit]="auth.hasPermission('salarie.modifier')" [canDelete]="auth.hasPermission('salarie.supprimer')"
                      [deleteDisabled]="!!accountFor(salarie.id)" deleteDisabledReason="Supprimez d’abord son compte utilisateur"
                      (view)="detailRecord = salarie" (edit)="edit(salarie)" (remove)="remove(salarie)" />
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="8" class="p-6 text-center text-slate-500">{{ loading() ? 'Chargement des salariés…' : 'Aucun salarié trouvé.' }}</td></tr>
              }
            </tbody>
          </table>
        </div>
      </section>

      
    </div>
    @if (detailRecord) { <app-record-details-dialog title="Salarié" [record]="detailRecord" (close)="detailRecord = null" /> }
}
}
`,
})
export class SalariesListComponent implements OnInit {
  readonly importOpen = signal(false);
  readonly importFormats = IMPORT_FORMATS;
  /** Lignes affichées (filtres et tri appliqués) : ce sont elles qui sont exportées. */
  exportIds(): (string | undefined)[] {
    return this.salaries().map((item) => item.id);
  }
  readonly auth = inject(AuthService);
  private readonly salarieService = inject(SalarieService);
  private readonly serviceService = inject(ServiceOrganisationnelService);
  private readonly utilisateurService = inject(UtilisateurService);
  private readonly notifications = inject(NotificationService);

  readonly salaries = signal<Salarie[]>([]);
  readonly tousLesSalaries = signal<Salarie[]>([]);
  readonly services = signal<ServiceOrganisationnel[]>([]);
  readonly comptes = signal<Utilisateur[]>([]);
  readonly formOpen = signal(false);
  readonly saving = signal(false);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly editingId = signal('');
  readonly canSeeAccounts = this.auth.hasPermission('utilisateur.lire');

  detailRecord: object | null = null;
  search = '';
  serviceFilter = '';
  actifFilter = '';
  draft: SalarieDraft = this.emptyDraft();

  ngOnInit(): void {
    this.loadReferences();
    this.refresh();
  }

  loadReferences(): void {
    forkJoin({
      services: this.auth.hasPermission('service.lire')
        ? this.serviceService.lister().pipe(catchError(() => of([] as ServiceOrganisationnel[])))
        : of([] as ServiceOrganisationnel[]),
      comptes: this.canSeeAccounts
        ? this.utilisateurService.listerOuFiltrer().pipe(catchError(() => of([] as Utilisateur[])))
        : of([] as Utilisateur[]),
      tous: this.salarieService.lister().pipe(catchError(() => of([] as Salarie[]))),
    }).subscribe((data) => {
      this.services.set(data.services);
      this.comptes.set(data.comptes);
      this.tousLesSalaries.set(data.tous);
    });
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set('');
    const actif = this.actifFilter === '' ? undefined : this.actifFilter === 'true';
    this.salarieService.lister(this.serviceFilter || undefined, actif, this.search.trim() || undefined).subscribe({
      next: (salaries) => {
        this.salaries.set(salaries);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.error.set(errorMessage(error));
      },
    });
  }

  resetFilters(): void {
    this.search = '';
    this.serviceFilter = '';
    this.actifFilter = '';
    this.refresh();
  }

  managerName(managerId?: string | null): string | null {
    if (!managerId) return null;
    const manager = this.tousLesSalaries().find((salarie) => salarie.id === managerId);
    return manager ? `${manager.prenom} ${manager.nom}` : null;
  }

  accountFor(salarieId: string): Utilisateur | undefined {
    return this.comptes().find((compte) => compte.salarieId === salarieId);
  }

  availableManagers(): Salarie[] {
    return this.tousLesSalaries().filter((salarie) => salarie.actif && salarie.id !== this.editingId());
  }

  openForm(): void {
    this.editingId.set('');
    this.draft = { ...this.emptyDraft(), serviceId: this.serviceFilter || this.services()[0]?.id || '' };
    this.formOpen.set(true);
  }

  edit(salarie: Salarie): void {
    this.editingId.set(salarie.id);
    this.draft = {
      matricule: salarie.matricule,
      nom: salarie.nom,
      prenom: salarie.prenom,
      sexe: salarie.sexe,
      telephone: salarie.telephone || '',
      email: salarie.email,
      poste: salarie.poste || '',
      dateEmbauche: salarie.dateEmbauche,
      serviceId: salarie.service?.id || '',
      managerId: salarie.managerId || null,
      actif: salarie.actif,
    };
    this.formOpen.set(true);
  }

  save(): void {
    const d = this.draft;
    if (!d.matricule.trim() || !d.nom.trim() || !d.prenom.trim() || !d.sexe || !d.email.trim()
      || !d.dateEmbauche || !d.serviceId) {
      this.notifications.warning('Veuillez remplir tous les champs obligatoires.');
      return;
    }
    const request: SalarieRequest = {
      matricule: d.matricule.trim().toUpperCase(),
      nom: d.nom.trim(),
      prenom: d.prenom.trim(),
      sexe: d.sexe,
      telephone: d.telephone?.trim() || null,
      email: d.email.trim(),
      poste: d.poste?.trim() || null,
      dateEmbauche: d.dateEmbauche,
      serviceId: d.serviceId,
      managerId: d.managerId || null,
      actif: d.actif,
    };
    this.saving.set(true);
    const request$ = this.editingId()
      ? this.salarieService.modifier(this.editingId(), request)
      : this.salarieService.creer(request);
    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.formOpen.set(false);
        this.notifications.success('Salarié enregistré.');
        this.loadReferences();
        this.refresh();
      },
      error: (error: unknown) => this.failedSave(error),
    });
  }

  remove(salarie: Salarie): void {
    if (!confirm(`Supprimer le salarié ${salarie.prenom} ${salarie.nom} (${salarie.matricule}) ?`)) return;
    this.salarieService.supprimer(salarie.id).subscribe({
      next: () => {
        this.notifications.success('Salarié supprimé.');
        this.loadReferences();
        this.refresh();
      },
      error: (error: unknown) => this.failedSave(error),
    });
  }

  private emptyDraft(): SalarieDraft {
    return {
      matricule: `EMP-${new Date().getFullYear()}-${Math.floor(100 + Math.random() * 900)}`,
      nom: '',
      prenom: '',
      sexe: '',
      telephone: '',
      email: '',
      poste: '',
      dateEmbauche: new Date().toISOString().slice(0, 10),
      serviceId: '',
      managerId: null,
      actif: true,
    };
  }

  private failedSave(error: unknown): void {
    this.saving.set(false);
    const message = errorMessage(error);
    this.error.set(message);
    this.notifications.error(message);
  }
}
