import { PageHeaderComponent } from '../../../shared/components/page-header.component';
import { RowActionsComponent } from '../../../shared/components/row-actions.component';
import { IMPORT_FORMATS } from '../../../core/models/import-formats';
import { ImportCsvPageComponent } from '../../../shared/components/import-csv-page.component';
import { DataActionsComponent } from '../../../shared/components/data-actions.component';
import { FormPageComponent } from '../../../shared/components/form-page.component';
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DatePickerComponent } from '../../../shared/components/date-picker.component';
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
  imports: [RouterLink, DatePickerComponent, PageHeaderComponent, RowActionsComponent, DataActionsComponent, ImportCsvPageComponent, FormPageComponent, CommonModule, FormsModule, RecordDetailsDialogComponent],
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
                <label class="block space-y-1 text-xs">Nom de famille <span class="champ-obligatoire" aria-hidden="true">*</span><input required maxlength="100" [(ngModel)]="draft.nom" name="nom" class="w-full rounded-lg border border-slate-300 p-2"></label>
                <label class="block space-y-1 text-xs">Prénom <span class="champ-obligatoire" aria-hidden="true">*</span><input required maxlength="100" [(ngModel)]="draft.prenom" name="prenom" class="w-full rounded-lg border border-slate-300 p-2"></label>
                <label class="block space-y-1 text-xs">Sexe <span class="champ-obligatoire" aria-hidden="true">*</span>
                  <select required [(ngModel)]="draft.sexe" name="sexe" class="w-full rounded-lg border border-slate-300 p-2">
                    <option value="">Sélectionner</option>
                    <option value="HOMME">Masculin</option>
                    <option value="FEMME">Féminin</option>
                  </select>
                </label>
                <label class="block space-y-1 text-xs">Téléphone<input type="tel" maxlength="30" [(ngModel)]="draft.telephone" name="telephone" class="w-full rounded-lg border border-slate-300 p-2"><span class="block text-[11px] font-normal text-slate-500">Avec l’indicatif, ex. +226 70 00 00 00.</span></label>
                <label class="block space-y-1 text-xs">Date de naissance<app-date-picker [(ngModel)]="draft.dateNaissance" name="dateNaissance" [max]="hier" /></label>
                <label class="block space-y-1 text-xs">Lieu de naissance<input maxlength="100" [(ngModel)]="draft.lieuNaissance" name="lieuNaissance" class="w-full rounded-lg border border-slate-300 p-2"></label>
                <label class="block space-y-1 text-xs">N° CNIB
                  <input #cnib="ngModel" maxlength="16" [(ngModel)]="draft.numeroCnib" (ngModelChange)="draft.numeroCnib = normaliserCnib($event)" name="numeroCnib"
                    pattern="B[0-9]{5,15}" autocomplete="off" class="w-full rounded-lg border border-slate-300 p-2 font-mono uppercase">
                  @if (cnib.invalid && (cnib.touched || cnib.dirty) && draft.numeroCnib) {
                    <span class="block text-[11px] font-normal text-rose-700">Le numéro CNIB commence par la lettre B suivie de chiffres (ex. B12345678).</span>
                  } @else {
                    <span class="block text-[11px] font-normal text-slate-500">Carte nationale d’identité burkinabè : B suivi de chiffres.</span>
                  }
                </label>
              </div>
            </section>

            <section class="space-y-3 rounded-xl border border-slate-200 p-4">
              <h3 class="text-sm font-semibold text-slate-900">Informations professionnelles</h3>
              <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <label class="block space-y-1 text-xs">Matricule <span class="champ-obligatoire" aria-hidden="true">*</span><input required maxlength="50" [(ngModel)]="draft.matricule" name="matricule" class="w-full rounded-lg border border-slate-300 p-2 uppercase"><span class="block text-[11px] font-normal text-slate-500">Identifiant RH unique, ex. EMP-2026-001.</span></label>
                <label class="block space-y-1 text-xs">Adresse e-mail professionnelle<input type="email" maxlength="150" [(ngModel)]="draft.email" name="email" class="w-full rounded-lg border border-slate-300 p-2"></label>
                <label class="block space-y-1 text-xs">Fonction / poste <span class="champ-obligatoire" aria-hidden="true">*</span><input required maxlength="100" [(ngModel)]="draft.poste" name="poste" class="w-full rounded-lg border border-slate-300 p-2"></label>
                <label class="block space-y-1 text-xs">Date d’embauche<app-date-picker [(ngModel)]="draft.dateEmbauche" name="dateEmbauche"/></label>
              </div>
              <label class="flex items-center gap-2 text-xs"><input type="checkbox" [(ngModel)]="draft.actif" name="actif"> Salarié actif (un salarié inactif ne peut pas se connecter)</label>
              @if (!editingId()) {
                <p class="rounded-lg bg-slate-50 px-3 py-2 text-[11px] text-slate-600">Après l’enregistrement, rattachez le salarié à sa structure, direction ou service depuis l’onglet <span class="font-semibold">Affectations</span>.</p>
              }
            </section>

            <div class="form-page-actions">
              <button type="button" (click)="formOpen.set(false)" class="rounded-lg border px-3 py-2 text-xs">Annuler</button>
              <button [disabled]="saving()" class="rounded-lg bg-green-700 px-4 py-2 text-xs font-semibold text-white disabled:opacity-50">{{ saving() ? 'Enregistrement…' : 'Enregistrer' }}</button>
            </div>
          </form>
        
</app-form-page>
} @else {
    <div class="mx-auto max-w-7xl space-y-6">
      <app-page-header icon="salaries" module="Administration" title="Salariés" description="Les dossiers des collaborateurs : identité, poste et affectation.">
<app-data-actions ressource="salaries" exportPermission="salarie.exporter" [ids]="exportIds()" [importPermissions]="['salarie.importer']" [importFormat]="importFormats.salaries" (importer)="importOpen.set(true)" />@if (auth.hasPermission('salarie.creer')) {
          <button type="button" (click)="openForm()" class="whitespace-nowrap rounded-xl bg-green-700 px-4 py-2 text-xs font-semibold text-white hover:bg-green-600">＋ Nouveau salarié</button>
        }
</app-page-header>

      @if (error()) {
        <div role="alert" class="rounded-xl border border-rose-300 bg-rose-50 p-3 text-sm text-rose-700">{{ error() }}</div>
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
                <th class="p-3">Salarié</th><th class="p-3">Matricule</th><th class="p-3">Poste</th><th class="p-3">Affectation</th>
                <th class="p-3">Compte</th><th class="p-3">Statut</th><th class="p-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-200">
              @for (salarie of salaries(); track salarie.id) {
                <tr>
                  <td class="p-3">
                    <p class="font-semibold text-white">{{ salarie.prenom }} {{ salarie.nom }}</p>
                    @if (salarie.email) { <p class="text-[11px] text-slate-500">{{ salarie.email }}</p> }
                    @if (salarie.telephone) { <p class="text-[11px] text-slate-500">{{ salarie.telephone }}</p> }
                  </td>
                  <td class="p-3 font-mono">{{ salarie.matricule }}</td>
                  <td class="p-3">{{ salarie.poste || '—' }}</td>
                  <td class="p-3">
                    @if (salarie.affectation; as affectation) {
                      <p class="font-medium text-slate-900">{{ affectation.cibleLibelle }}</p>
                      <p class="font-mono text-[11px] text-slate-500">{{ affectation.chemin }}</p>
                    } @else {
                      <span class="text-amber-700">Non affecté</span>
                    }
                    @if (auth.hasPermission('affectation.lire')) {
                      <a [routerLink]="['/security/affectations']" [queryParams]="{ salarie: salarie.id }" class="mt-0.5 inline-block text-[11px] font-semibold text-green-700 hover:underline">Affectations →</a>
                    }
                  </td>
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
    }).subscribe((data) => {
      this.services.set(data.services);
      this.comptes.set(data.comptes);
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

  accountFor(salarieId: string): Utilisateur | undefined {
    return this.comptes().find((compte) => compte.salarieId === salarieId);
  }

  openForm(): void {
    this.editingId.set('');
    this.draft = this.emptyDraft();
    this.formOpen.set(true);
  }

  edit(salarie: Salarie): void {
    this.editingId.set(salarie.id);
    this.draft = {
      matricule: salarie.matricule,
      nom: salarie.nom,
      prenom: salarie.prenom,
      // Anciennes valeurs (Autre, Non renseigné) : à ressaisir, seuls Masculin et Féminin sont proposés.
      sexe: salarie.sexe === 'HOMME' || salarie.sexe === 'FEMME' ? salarie.sexe : '',
      telephone: salarie.telephone || '',
      email: salarie.email || '',
      poste: salarie.poste || '',
      dateEmbauche: salarie.dateEmbauche || '',
      dateNaissance: salarie.dateNaissance || '',
      lieuNaissance: salarie.lieuNaissance || '',
      numeroCnib: salarie.numeroCnib || '',
      managerId: salarie.managerId || null,
      actif: salarie.actif,
    };
    this.formOpen.set(true);
  }

  save(): void {
    const d = this.draft;
    if (!d.matricule.trim() || !d.nom.trim() || !d.prenom.trim() || !d.sexe || !d.poste?.trim()) {
      this.notifications.warning('Veuillez remplir tous les champs obligatoires.');
      return;
    }
    if (d.numeroCnib && !/^B\d{5,15}$/.test(d.numeroCnib)) {
      this.notifications.warning('Le numéro CNIB doit commencer par B suivi de chiffres (ex. B12345678).');
      return;
    }
    if (d.dateNaissance && d.dateEmbauche && d.dateNaissance >= d.dateEmbauche) {
      this.notifications.warning('La date de naissance doit être antérieure à la date d’embauche.');
      return;
    }
    const request: SalarieRequest = {
      matricule: d.matricule.trim().toUpperCase(),
      nom: d.nom.trim(),
      prenom: d.prenom.trim(),
      sexe: d.sexe,
      telephone: d.telephone?.trim() || null,
      email: d.email?.trim() || null,
      poste: d.poste!.trim(),
      dateEmbauche: d.dateEmbauche || null,
      dateNaissance: d.dateNaissance || null,
      lieuNaissance: d.lieuNaissance?.trim() || null,
      numeroCnib: d.numeroCnib || null,
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

  /** Veille du jour : borne haute de la date de naissance. */
  readonly hier = (() => {
    const date = new Date();
    date.setDate(date.getDate() - 1);
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  })();

  /** Majuscules, sans espaces (saisie « b 1234 5678 » → « B12345678 »). */
  normaliserCnib(valeur: string | null): string {
    return (valeur ?? '').replace(/\s/g, '').toUpperCase();
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
      dateEmbauche: '',
      dateNaissance: '',
      lieuNaissance: '',
      numeroCnib: '',
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
