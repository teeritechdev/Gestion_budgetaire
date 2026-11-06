import { PageHeaderComponent } from '../../../shared/components/page-header.component';
import { IMPORT_FORMATS } from '../../../core/models/import-formats';
import { ImportCsvPageComponent } from '../../../shared/components/import-csv-page.component';
import { DataActionsComponent } from '../../../shared/components/data-actions.component';
import { FormPageComponent } from '../../../shared/components/form-page.component';
import { RecordDetailsDialogComponent } from '../../../shared/components/record-details-dialog.component';
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SearchOptionDirective, SearchSelectComponent } from '../../../shared/components/search-select.component';
import { forkJoin } from 'rxjs';
import { UtilisateurService } from '../../../core/services/utilisateur.service';
import { ServiceOrganisationnelService } from '../../../core/services/service-organisationnel.service';
import { SalarieService } from '../../../core/services/salarie.service';
import { Salarie } from '../../../core/models/salarie.model';
import { RoleService } from '../../../core/services/role.service';
import { NotificationService } from '../../../core/services/notification.service';
import { AuthService } from '../../../core/services/auth.service';
import { of } from 'rxjs';
import { Utilisateur, UtilisateurRequest } from '../../../core/models/utilisateur.model';
import { ServiceOrganisationnel } from '../../../core/models/service-organisationnel.model';
import { Role } from '../../../core/models/role.model';

@Component({
  selector: 'app-utilisateurs-list',
  standalone: true,
  imports: [SearchSelectComponent, SearchOptionDirective, PageHeaderComponent, DataActionsComponent, ImportCsvPageComponent, FormPageComponent, RecordDetailsDialogComponent, CommonModule, FormsModule],
  template: `
@if (importOpen()) {
<app-import-csv-page section="Utilisateurs" [upload]="uploadImport" [format]="importFormats.utilisateurs" (back)="importOpen.set(false)" (imported)="loadInitialData()" />
} @else {
@if (showFormModal()) {
<app-form-page section="Utilisateurs" [title]="isEditing() ? 'Modifier le compte utilisateur' : 'Créer un compte utilisateur'" [mode]="isEditing() ? 'Modification' : 'Création'" subtitle="Salarié titulaire, identifiant de connexion, mot de passe et rôle système." (back)="closeFormModal()">
<div>
            <form (ngSubmit)="saveUser()" class="space-y-4">
              <section class="space-y-4 rounded-xl border border-slate-200 p-4">
                <div><h4 class="text-sm font-semibold text-slate-900">Salarié titulaire du compte</h4><p class="mt-1 text-xs text-slate-500">Le dossier du salarié se modifie dans l’onglet Salariés, son rattachement dans l’onglet Affectations.</p></div>
                @if (isEditing()) {
                  <div class="rounded-xl bg-slate-50 p-3 text-xs text-slate-700">
                    <p class="font-semibold text-slate-900">{{ editingUser?.prenom }} {{ editingUser?.nom }}</p>
                    <p>{{ editingUser?.matricule }} · {{ editingUser?.email }}</p>
                    <p>{{ editingUser?.affectation?.cibleLibelle || 'Non affecté' }}</p>
                  </div>
                } @else {
                  <div>
                    <label class="mb-1.5 block text-xs font-medium text-slate-700">Salarié <span class="champ-obligatoire" aria-hidden="true">*</span></label>
                    <app-search-select required [(ngModel)]="selectedSalarieId" (ngModelChange)="onSalarieChange($event)" name="salarieId" placeholder="Sélectionner un salarié sans compte" [emptyValue]="null" triggerClass="w-full rounded-xl border border-slate-300 bg-slate-50 px-3 py-2 text-sm text-slate-900 focus:outline-none focus:ring-1 focus:ring-blue-500">
                      @for (salarie of salariesSansCompte(); track salarie.id) {
                        <app-search-option [value]="salarie.id" [label]="salarie.prenom + ' ' + salarie.nom + ' — ' + salarie.matricule + (salarie.affectation ? ' (' + salarie.affectation.cibleLibelle + ')' : '')" />
                      }
                    </app-search-select>
                    @if (!auth.hasPermission('salarie.lire')) {
                      <p class="mt-1 text-xs text-rose-600">La permission salarie.lire est nécessaire pour choisir un salarié.</p>
                    } @else if (!salariesSansCompte().length) {
                      <p class="mt-1 text-xs text-amber-700">Tous les salariés actifs ont déjà un compte. Créez d’abord le salarié dans l’onglet Salariés.</p>
                    }
                  </div>
                }
                <div>
                  <label class="mb-1.5 block text-xs font-medium text-slate-700">Nom d’utilisateur <span class="champ-obligatoire" aria-hidden="true">*</span></label>
                  <input type="text" required maxlength="150" [(ngModel)]="username" name="username" autocomplete="username" class="w-full rounded-xl border border-slate-300 bg-slate-50 px-3 py-2 text-sm text-slate-900 focus:outline-none focus:ring-1 focus:ring-blue-500" />
                  <p class="mt-1 text-[11px] text-slate-500">Identifiant de connexion, généralement l’adresse e-mail (ex. prenom.nom@uab.bf).</p>
                </div>
              </section>

              <!-- Le mot de passe est saisi en clair dans le formulaire puis haché par le backend. -->
              <section class="space-y-4 rounded-xl border border-slate-200 p-4">
                <div><h4 class="text-sm font-semibold text-slate-900">Sécurité du compte</h4><p class="mt-1 text-xs text-slate-500">Le mot de passe est protégé avant son enregistrement.</p></div>
                <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label class="block text-xs font-medium text-slate-700 mb-1.5">
                    {{ isEditing() ? 'Nouveau mot de passe (laisser vide pour conserver)' : 'Mot de passe' }}@if (!isEditing()) { <span class="champ-obligatoire" aria-hidden="true">*</span> }
                  </label>
                  <input
                    type="password"
                    autocomplete="new-password"
                    [required]="!isEditing()"
                    minlength="8"
                    [(ngModel)]="passwordInput"
                    name="password"
                    class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs text-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                  />
                  <p class="mt-1 text-[11px] text-slate-500">8 caractères minimum.</p>
                </div>
                @if (!isEditing() || passwordInput) {
                  <div>
                    <label class="block text-xs font-medium text-slate-700 mb-1.5">Confirmer le mot de passe <span class="champ-obligatoire" aria-hidden="true">*</span></label>
                    <input
                      type="password"
                      autocomplete="new-password"
                      required
                      minlength="8"
                      [(ngModel)]="passwordConfirmation"
                      name="passwordConfirmation"
                      class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs text-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                    />
                    @if (passwordConfirmation && passwordInput !== passwordConfirmation) {
                      <p class="mt-1 text-xs font-medium text-rose-600">Les deux mots de passe ne correspondent pas.</p>
                    }
                  </div>
                }
                </div>
              </section>

              <section class="space-y-4 rounded-xl border border-slate-200 p-4">
                <div><h4 class="text-sm font-semibold text-slate-900">Accès</h4><p class="mt-1 text-xs text-slate-500">Rôle système attribué au compte.</p></div>
                <div>
                  <label class="block text-xs font-medium text-slate-700 mb-1.5">Rôle Système <span class="champ-obligatoire" aria-hidden="true">*</span></label>
                  <select
                    required
                    [(ngModel)]="selectedRoleId"
                    name="roleId"
                    class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs text-white focus:outline-none focus:ring-1 focus:ring-blue-500">
                    <option [ngValue]="null" disabled>Sélectionner un rôle</option>
                    @for (r of roles(); track r.id) {
                      <option [value]="r.id">{{ r.libelle }}</option>
                    }
                  </select>
                </div>

              <!-- Ligne 7 : Statut Actif -->
              <div class="flex items-center gap-2 pt-1">
                <input
                  type="checkbox"
                  id="actifCheckbox"
                  [(ngModel)]="actif"
                  name="actif"
                  class="w-4 h-4 rounded text-blue-600 bg-slate-50 border-slate-300 focus:ring-blue-500"
                />
                <label for="actifCheckbox" class="text-xs text-slate-700 cursor-pointer">
                  Compte actif (autorisé à se connecter à la plateforme budgétaire)
                </label>
              </div>
              </section>

              <!-- Boutons d'action -->
              <div class="form-page-actions">
                <button
                  type="button"
                  (click)="closeFormModal()"
                  class="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-700 text-slate-700 text-xs font-medium transition-colors">
                  Annuler
                </button>
                <button
                  type="submit"
                  [disabled]="modalSaving()"
                  [attr.aria-busy]="modalSaving()"
                  class="px-5 py-2 rounded-xl bg-green-700 hover:bg-green-600 text-white text-xs font-medium shadow-lg shadow-blue-600/30 disabled:opacity-50 flex items-center gap-2 transition-all">
                  {{ modalSaving() ? (isEditing() ? 'Enregistrement…' : 'Création en cours…') : (isEditing() ? 'Enregistrer les modifications' : 'Créer le compte') }}
                </button>
              </div>
            </form>
          </div>
        
</app-form-page>
} @else {
    <div class="space-y-6 max-w-7xl mx-auto">
      <app-page-header icon="utilisateurs" module="Administration" title="Utilisateurs" description="Les comptes de connexion : identifiant, rôle et accès à la plateforme.">
<app-data-actions ressource="utilisateurs" exportPermission="utilisateur.exporter" [ids]="exportIds()" [importPermissions]="['utilisateur.importer']" [importFormat]="importFormats.utilisateurs" (importer)="importOpen.set(true)" />

          <button
            [hidden]="!auth.hasPermission('utilisateur.creer')"
            (click)="openCreateModal()"
            class="px-4 py-2 rounded-xl bg-green-700 hover:bg-green-600 text-white text-xs font-medium shadow-lg shadow-blue-600/30 transition-all flex items-center gap-1.5">
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            Créer un compte
          </button>
</app-page-header>

      <!-- Barre de Filtres -->
      <div class="bg-white border border-slate-200 rounded-2xl p-4 shadow-lg flex flex-wrap items-center gap-3">
        <!-- Recherche textuelle -->
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
              (input)="applyFilters()"
              placeholder="Rechercher par identifiant, nom, prénom, email ou matricule..."
              class="w-full pl-9 pr-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
          </div>
        </div>

        <!-- Filtre Rôle -->
        <select
          [(ngModel)]="roleFilter"
          (change)="applyFilters()"
          class="bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-700 py-2 px-3 focus:outline-none focus:ring-1 focus:ring-blue-500">
          <option [ngValue]="null">Tous les Rôles</option>
          @for (role of roles(); track role.id) {
            <option [ngValue]="role.id">{{ role.libelle }}</option>
          }
        </select>

        <!-- Filtre Service -->
        <select
          [(ngModel)]="serviceFilter"
          (change)="applyFilters()"
          class="bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-700 py-2 px-3 focus:outline-none focus:ring-1 focus:ring-blue-500">
          <option [ngValue]="null">Tous les services</option>
          @for (service of services(); track service.id) {
            <option [ngValue]="service.id">{{ service.libelle }} ({{ service.code }})</option>
          }
        </select>

        <!-- Filtre Statut Actif -->
        <select
          [(ngModel)]="actifFilter"
          (change)="applyFilters()"
          class="bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-700 py-2 px-3 focus:outline-none focus:ring-1 focus:ring-blue-500">
          <option [ngValue]="null">Tous statuts</option>
          <option [ngValue]="true">Actif</option>
          <option [ngValue]="false">Inactif</option>
        </select>

        <button
          (click)="resetFilters()"
          class="p-2 rounded-xl bg-slate-100 hover:bg-slate-700 text-slate-600 hover:text-white transition-colors"
          title="Réinitialiser filtres">
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- Tableau des Utilisateurs -->
      <div class="bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-xl">
        <div class="overflow-x-auto">
          <table class="w-full text-left text-xs">
            <thead class="bg-slate-50/80 text-slate-600 uppercase tracking-wider border-b border-slate-200">
              <tr>
                <th class="py-3.5 px-4">Compte</th>
                <th class="py-3.5 px-4">Salarié</th>
                <th class="py-3.5 px-4">Matricule</th>
                <th class="py-3.5 px-4">Service</th>
                <th class="py-3.5 px-4">Rôle Système</th>
                <th class="py-3.5 px-4">Statut</th>
                <th class="py-3.5 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-200/60">
              @if (loading()) {
                <tr>
                  <td colspan="7" class="py-8 text-center text-slate-600">
                    <svg class="animate-spin h-6 w-6 text-blue-500 mx-auto mb-2" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Chargement des comptes...
                  </td>
                </tr>
              } @else if (utilisateurs().length === 0) {
                <tr>
                  <td colspan="7" class="py-8 text-center text-slate-500">
                    Aucun utilisateur trouvé.
                  </td>
                </tr>
              } @else {
                @for (user of utilisateurs(); track user.id) {
                  <tr class="hover:bg-slate-100/40 transition-colors">
                    <td class="py-3.5 px-4">
                      <div class="flex items-center gap-3">
                        <div class="w-8 h-8 rounded-lg bg-blue-500/20 text-blue-300 font-bold flex items-center justify-center shrink-0">
                          {{ (user.prenom?.charAt(0) || '') + (user.nom?.charAt(0) || '') }}
                        </div>
                        <p class="font-semibold text-white">{{ user.username }}</p>
                      </div>
                    </td>
                    <td class="py-3.5 px-4">
                      <p class="text-slate-800">{{ user.prenom }} {{ user.nom }}</p>
                      <p class="text-slate-500 text-[11px]">{{ user.email }}</p>
                    </td>
                    <td class="py-3.5 px-4 font-mono text-slate-700 font-medium">{{ user.matricule }}</td>
                    <td class="py-3.5 px-4 text-slate-700">
                      {{ user.affectation?.cibleLibelle || 'Non affecté' }}
                    </td>
                    <td class="py-3.5 px-4">
                      <span class="px-2 py-0.5 rounded-full text-[11px] font-medium bg-blue-500/10 text-blue-300 border border-blue-500/20">
                        {{ user.role?.libelle || user.role?.code || 'Aucun' }}
                      </span>
                    </td>
                    <td class="py-3.5 px-4">
                      <span
                        class="px-2 py-0.5 rounded-full text-[11px] font-medium inline-flex items-center gap-1"
                        [ngClass]="user.actif ? 'bg-blue-500/10 text-blue-400 border border-blue-500/20' : 'bg-rose-500/10 text-rose-400 border border-rose-500/20'">
                        <span class="w-1.5 h-1.5 rounded-full" [ngClass]="user.actif ? 'bg-blue-400' : 'bg-rose-400'"></span>
                        {{ user.actif ? 'Actif' : 'Inactif' }}
                      </span>
                    </td>
                    <td class="py-3.5 px-4 text-right">
                      <div class="flex items-center justify-end gap-1.5">
                        <button type="button" (click)="showDetails('Utilisateur', user)" class="inline-flex h-10 w-10 items-center justify-center rounded-lg text-blue-700 transition-colors hover:bg-blue-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2" title="Voir les détails" aria-label="Voir les détails"><svg aria-hidden="true" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M2.25 12s3.5-6.5 9.75-6.5 9.75 6.5 9.75 6.5-3.5 6.5-9.75 6.5S2.25 12 2.25 12Z"/><circle cx="12" cy="12" r="3"/></svg></button><button
                          [hidden]="!auth.hasPermission('utilisateur.modifier')"
                          (click)="openEditModal(user)"
                          class="p-1.5 rounded-lg text-slate-600 hover:text-blue-400 hover:bg-green-600/10 transition-colors"
                          title="Modifier l'utilisateur">
                          <svg aria-hidden="true" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                          </svg>
                        </button>
                        <button
                          [hidden]="!auth.hasPermission('utilisateur.supprimer')"
                          (click)="confirmDelete(user)"
                          class="p-1.5 rounded-lg text-rose-600 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
                          title="Supprimer l'utilisateur">
                          <svg aria-hidden="true" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                          </svg>
                        </button>
                      </div>
                    </td>
                  </tr>
                }
              }
            </tbody>
          </table>
        </div>
      </div>

      <!-- MODAL COMPLET DE CRÉATION / MODIFICATION (Conforme à Utilisateur.java) -->
      

      <!-- Modal de Confirmation de Suppression -->
      @if (userToDelete()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
          <div class="bg-white border border-slate-200 rounded-2xl w-full max-w-md p-6 shadow-2xl space-y-4">
            <h3 class="text-lg font-semibold text-rose-400">Confirmer la suppression</h3>
            <p class="text-xs text-slate-700 leading-relaxed">
              Êtes-vous sûr de vouloir supprimer le compte
              <strong class="text-white">{{ userToDelete()?.username }}</strong>
              de {{ userToDelete()?.prenom }} {{ userToDelete()?.nom }} ? Le dossier salarié est conservé.
              Cette action est irréversible et sera consignée dans l'audit.
            </p>
            <div class="flex items-center justify-end gap-3 pt-3">
              <button
                (click)="userToDelete.set(null)"
                class="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-700 text-slate-700 text-xs font-medium">
                Annuler
              </button>
              <button
                (click)="executeDelete()"
                class="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-medium">
                Supprimer Définitivement
              </button>
            </div>
          </div>
        </div>
      }

      <!-- Modal Import CSV -->
      
    </div>
    @if (detailRecord) { <app-record-details-dialog [title]="detailTitle" [record]="detailRecord" (close)="detailRecord = null" /> }
}
}
`
})
export class UtilisateursListComponent implements OnInit {
  readonly importOpen = signal(false);
  readonly importFormats = IMPORT_FORMATS;
  readonly uploadImport = (fichier: File) => this.userService.importerCsv(fichier);
  /** Lignes affichées (filtres et tri appliqués) : ce sont elles qui sont exportées. */
  exportIds(): (string | undefined)[] {
    return this.utilisateurs().map((item) => item.id);
  }
  detailTitle = '';
  detailRecord: object | null = null;
  showDetails(title: string, record: object): void { this.detailTitle = title; this.detailRecord = record; }

  private readonly userService = inject(UtilisateurService);
  private readonly serviceService = inject(ServiceOrganisationnelService);
  private readonly roleService = inject(RoleService);
  private readonly salarieService = inject(SalarieService);
  private readonly notify = inject(NotificationService);
  readonly auth = inject(AuthService);

  utilisateurs = signal<Utilisateur[]>([]);
  services = signal<ServiceOrganisationnel[]>([]);
  roles = signal<Role[]>([]);
  salaries = signal<Salarie[]>([]);
  loading = signal(false);

  // Filtres
  searchFilter = '';
  roleFilter: string | null = null;
  serviceFilter: string | null = null;
  actifFilter: boolean | null = null;

  // Modal formulaire
  showFormModal = signal(false);
  isEditing = signal(false);
  modalSaving = signal(false);
  currentEditingId: string | null = null;
  editingUser: Utilisateur | null = null;

  username = '';
  actif = true;
  passwordInput = '';
  passwordConfirmation = '';
  selectedSalarieId: string | null = null;
  selectedRoleId: string | null = null;

  // Modal suppression
  userToDelete = signal<Utilisateur | null>(null);


  ngOnInit() {
    this.loadInitialData();
  }

  loadInitialData() {
    this.loading.set(true);
    forkJoin({
      users: this.userService.listerOuFiltrer(),
      services: this.auth.hasPermission('service.lire')
        ? this.serviceService.lister()
        : of([] as ServiceOrganisationnel[]),
      roles: this.auth.hasPermission('role.lire')
        ? this.roleService.listerOuFiltrer()
        : of([] as Role[]),
      salaries: this.auth.hasPermission('salarie.lire')
        ? this.salarieService.lister()
        : of([] as Salarie[])
    }).subscribe({
      next: (res) => {
        this.utilisateurs.set(res.users);
        this.services.set(res.services);
        this.roles.set(res.roles);
        this.salaries.set(res.salaries);
        this.loading.set(false);
      },
      error: () => {
        this.notify.error('Erreur lors du chargement des données');
        this.loading.set(false);
      }
    });
  }

  applyFilters() {
    this.loading.set(true);
    this.userService.listerOuFiltrer(
      this.serviceFilter || undefined,
      this.roleFilter || undefined,
      this.actifFilter !== null ? this.actifFilter : undefined,
      this.searchFilter || undefined
    ).subscribe({
      next: (res) => {
        this.utilisateurs.set(res);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  resetFilters() {
    this.searchFilter = '';
    this.roleFilter = null;
    this.serviceFilter = null;
    this.actifFilter = null;
    this.applyFilters();
  }

  /** Salariés actifs ne possédant pas encore de compte (un salarié = au plus un compte). */
  salariesSansCompte(): Salarie[] {
    const avecCompte = new Set(this.utilisateurs().map(u => u.salarieId));
    return this.salaries().filter(s => s.actif && !avecCompte.has(s.id));
  }

  onSalarieChange(salarieId: string | null) {
    const salarie = this.salaries().find(s => s.id === salarieId);
    // Identifiant proposé par défaut : l'e-mail professionnel du salarié.
    if (salarie?.email && !this.username) {
      this.username = salarie.email;
    }
  }

  openCreateModal() {
    this.isEditing.set(false);
    this.currentEditingId = null;
    this.editingUser = null;
    this.username = '';
    this.actif = true;
    this.passwordInput = '';
    this.passwordConfirmation = '';
    this.selectedSalarieId = null;
    this.selectedRoleId = this.roles()[0]?.id || null;
    this.showFormModal.set(true);
  }

  openEditModal(user: Utilisateur) {
    this.isEditing.set(true);
    this.currentEditingId = user.id || null;
    this.editingUser = user;
    this.username = user.username;
    this.actif = user.actif;
    this.passwordInput = '';
    this.passwordConfirmation = '';
    this.selectedSalarieId = user.salarieId || null;
    this.selectedRoleId = user.role?.id || null;
    this.showFormModal.set(true);
  }

  closeFormModal() {
    this.showFormModal.set(false);
  }

  saveUser() {
    const role = this.roles().find(r => r.id === this.selectedRoleId);
    if (!this.selectedSalarieId) {
      this.notify.warning('Veuillez sélectionner le salarié titulaire du compte.');
      return;
    }
    if (!role) {
      this.notify.warning('Veuillez sélectionner un rôle.');
      return;
    }
    if (!this.username.trim()) {
      this.notify.warning('Le nom d’utilisateur est obligatoire.');
      return;
    }

    if (!this.isEditing() || this.passwordInput) {
      if (this.passwordInput.length < 8) {
        this.notify.warning('Le mot de passe doit contenir au moins 8 caractères.');
        return;
      }
      if (this.passwordInput !== this.passwordConfirmation) {
        this.notify.warning('La confirmation du mot de passe ne correspond pas.');
        return;
      }
    }

    this.modalSaving.set(true);

    const payload: UtilisateurRequest = {
      username: this.username.trim(),
      salarieId: this.selectedSalarieId,
      roleId: role.id!,
      actif: this.actif
    };

    const request$ = this.isEditing() && this.currentEditingId
      ? this.userService.modifier(this.currentEditingId, payload, this.passwordInput || undefined)
      : this.userService.creer(payload, this.passwordInput);
    request$.subscribe({
      next: () => {
        this.notify.success(this.isEditing() ? 'Compte modifié avec succès.' : 'Compte créé avec succès.');
        this.modalSaving.set(false);
        this.closeFormModal();
        this.loadInitialData();
      },
      error: (err) => {
        this.notify.error(err.error?.message || 'Erreur lors de l’enregistrement du compte.');
        this.modalSaving.set(false);
      }
    });
  }

  confirmDelete(user: Utilisateur) {
    this.userToDelete.set(user);
  }

  executeDelete() {
    const user = this.userToDelete();
    if (!user || !user.id) return;

    this.userService.supprimer(user.id).subscribe({
      next: () => {
        this.notify.success('Utilisateur supprimé avec succès.');
        this.userToDelete.set(null);
        this.loadInitialData();
      },
      error: (err) => {
        this.notify.error(err.error?.message || 'Erreur lors de la suppression.');
      }
    });
  }






}
