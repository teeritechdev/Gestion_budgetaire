import { PageHeaderComponent } from '../../../shared/components/page-header.component';
import { IMPORT_FORMATS } from '../../../core/models/import-formats';
import { ImportCsvPageComponent } from '../../../shared/components/import-csv-page.component';
import { DataActionsComponent } from '../../../shared/components/data-actions.component';
import { FormPageComponent } from '../../../shared/components/form-page.component';
import { RecordDetailsDialogComponent } from '../../../shared/components/record-details-dialog.component';
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { RoleService } from '../../../core/services/role.service';
import { PermissionService } from '../../../core/services/permission.service';
import { NotificationService } from '../../../core/services/notification.service';
import { AuthService } from '../../../core/services/auth.service';
import { of } from 'rxjs';
import { Role, RoleRequestDto } from '../../../core/models/role.model';
import { Permission } from '../../../core/models/permission.model';

@Component({
  selector: 'app-roles-permissions',
  standalone: true,
  imports: [PageHeaderComponent, DataActionsComponent, ImportCsvPageComponent, FormPageComponent, RecordDetailsDialogComponent, CommonModule, FormsModule],
  template: `
@if (importOpen()) {
<app-import-csv-page section="Rôles & permissions" [upload]="uploadImport" [format]="activeTab() === 'roles' ? importFormats.roles : importFormats.permissions" (back)="importOpen.set(false)" (imported)="loadData()" />
} @else {
@if (showRoleModal()) {
<app-form-page section="Rôles & permissions" [title]="isEditingRole() ? 'Modifier le rôle' : 'Créer un rôle'" [mode]="isEditingRole() ? 'Modification' : 'Création'" subtitle="Code, libellé et permissions accordées aux utilisateurs portant ce rôle." (back)="closeRoleModal()">
<div class="form-page-card space-y-5">
            

            <div class="flex-1 overflow-y-auto space-y-4 pr-1">
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="block text-xs font-medium text-slate-700 mb-1">Code Rôle (Unique) <span class="champ-obligatoire" aria-hidden="true">*</span></label>
                  <input
                    type="text"
                    required
                    [disabled]="isEditingRole()"
                    [(ngModel)]="roleFormData.code"
                    class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs text-white uppercase disabled:opacity-60"
                  />
                  <p class="mt-1 text-[11px] text-slate-500">Majuscules et tirets bas, ex. CHEF_DEPARTEMENT.</p>
                </div>
                <div>
                  <label class="block text-xs font-medium text-slate-700 mb-1">Libellé du Rôle <span class="champ-obligatoire" aria-hidden="true">*</span></label>
                  <input
                    type="text"
                    required
                    [(ngModel)]="roleFormData.libelle"
                    class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs text-white"
                  />
                </div>
              </div>

              <div>
                <label class="block text-xs font-medium text-slate-700 mb-1">Description</label>
                <textarea
                  rows="2"
                  [(ngModel)]="roleFormData.description"
                  class="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs text-white"
                ></textarea>
                <p class="mt-1 text-[11px] text-slate-500">Responsabilités et périmètre d’action du rôle.</p>
              </div>

              <!-- Interactive Permissions Matrix -->
              <div>
                <div class="flex items-center justify-between mb-2">
                  <label class="block text-xs font-semibold text-white uppercase tracking-wider">
                    Permissions Accordées ({{ selectedPermissionIds.size }} sélectionnées)
                  </label>
                  <div class="flex flex-wrap items-center gap-2">
                    <button
                      type="button"
                      (click)="selectAllPermissions()"
                      class="text-[11px] text-blue-400 hover:text-blue-300">
                      Tout cocher
                    </button>
                    <span class="text-slate-600">|</span>
                    <button
                      type="button"
                      (click)="deselectAllPermissions()"
                      class="text-[11px] text-slate-600 hover:text-slate-700">
                      Tout décocher
                    </button>
                  </div>
                </div>

                <div class="space-y-4 max-h-72 overflow-y-auto border border-slate-200 rounded-xl p-3 bg-white">
                  @for (mod of availableModules(); track mod) {
                    <div class="border-b border-slate-200/80 pb-3 last:border-0 last:pb-0">
                      <div class="flex items-center justify-between py-1">
                        <span class="text-xs font-bold text-blue-300 tracking-wide">{{ mod }}</span>
                        <button
                          type="button"
                          (click)="toggleModulePermissions(mod)"
                          class="text-[11px] text-slate-600 hover:text-white">
                          Inverser module
                        </button>
                      </div>

                      <div class="grid grid-cols-1 sm:grid-cols-2 gap-2 mt-2">
                        @for (p of getPermissionsByModule(mod); track p.id) {
                          <label class="flex items-center gap-2 p-1.5 rounded-lg bg-white/50 hover:bg-white border border-slate-200/50 cursor-pointer">
                            <input
                              type="checkbox"
                              [checked]="p.id ? selectedPermissionIds.has(p.id) : false"
                              (change)="p.id && togglePermission(p.id)"
                              class="w-3.5 h-3.5 rounded text-blue-600 bg-slate-50 border-slate-300"
                            />
                            <div class="min-w-0 flex-1">
                              <p class="text-xs text-slate-800 font-mono truncate">{{ p.code }}</p>
                              <p class="text-[10px] text-slate-500 truncate">{{ p.libelle }}</p>
                            </div>
                          </label>
                        }
                      </div>
                    </div>
                  }
                </div>
              </div>
            </div>

            <div class="form-page-actions">
              <button
                type="button"
                (click)="closeRoleModal()"
                class="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-700 text-slate-700 text-xs font-medium">
                Annuler
              </button>
              <button
                type="button"
                (click)="saveRole()"
                [disabled]="roleSaving()"
                class="px-4 py-2 rounded-xl bg-green-700 hover:bg-green-600 text-white text-xs font-medium disabled:opacity-50 flex items-center gap-2">
                @if (roleSaving()) {
                  <svg class="animate-spin h-3.5 w-3.5" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                }
                Enregistrer le Rôle
              </button>
            </div>
          </div>
        
</app-form-page>
} @else {
    <div class="space-y-6 max-w-7xl mx-auto">
      <app-page-header icon="roles" module="Administration" title="Rôles & permissions" description="Définissez qui peut faire quoi, avec des droits précis et maîtrisés.">
<app-data-actions [ressource]="activeTab() === 'roles' ? 'roles' : 'permissions'" [exportPermission]="activeTab() === 'roles' ? 'role.exporter' : 'permission.exporter'" [ids]="exportIds()" [importPermissions]="[activeTab() === 'roles' ? 'role.importer' : 'permission.importer']" [importFormat]="activeTab() === 'roles' ? importFormats.roles : importFormats.permissions" (importer)="importOpen.set(true)" />

          @if (activeTab() === 'roles' && auth.hasPermission('role.creer')) {
            <button
              (click)="openRoleModal()"
              class="px-4 py-2 rounded-xl bg-green-700 hover:bg-green-600 text-white text-xs font-medium shadow-lg shadow-blue-600/30 transition-all flex items-center gap-1.5">
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>
              Nouveau rôle
            </button>
          }
</app-page-header>

      <!-- Navigation Tabs -->
      <div class="border-b border-slate-200 flex gap-4">
        <button
          [hidden]="!auth.hasPermission('role.lire')"
          (click)="activeTab.set('roles')"
          [class.border-blue-500]="activeTab() === 'roles'"
          [class.text-white]="activeTab() === 'roles'"
          [class.border-transparent]="activeTab() !== 'roles'"
          [class.text-slate-600]="activeTab() !== 'roles'"
          class="pb-3 text-sm font-semibold border-b-2 hover:text-white transition-colors flex items-center gap-2">
          <svg class="w-4 h-4 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
          </svg>
          Gestion des Rôles ({{ roles().length }})
        </button>

        <button
          [hidden]="!auth.hasPermission('permission.lire')"
          (click)="activeTab.set('permissions')"
          [class.border-blue-500]="activeTab() === 'permissions'"
          [class.text-white]="activeTab() === 'permissions'"
          [class.border-transparent]="activeTab() !== 'permissions'"
          [class.text-slate-600]="activeTab() !== 'permissions'"
          class="pb-3 text-sm font-semibold border-b-2 hover:text-white transition-colors flex items-center gap-2">
          <svg class="w-4 h-4 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
          </svg>
          Catalogue des Permissions ({{ allPermissions().length }})
        </button>
      </div>

      <!-- TAB 1 : GESTION DES RÔLES -->
      @if (activeTab() === 'roles') {
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          @for (role of roles(); track role.id) {
            <div class="bg-white border border-slate-200 rounded-2xl p-5 shadow-lg flex flex-col justify-between hover:border-slate-300 transition-all group">
              <div>
                <div class="flex items-start justify-between gap-3 mb-2">
                  <span class="px-2.5 py-1 rounded-lg text-xs font-mono font-semibold bg-blue-500/10 text-blue-400 border border-blue-500/20">
                    {{ role.code }}
                  </span>
                  <div class="flex items-center gap-1 opacity-80 group-hover:opacity-100 transition-opacity">
                    <button type="button" (click)="showDetails('Rôle', role)" class="inline-flex h-10 w-10 items-center justify-center rounded-lg text-blue-700 transition-colors hover:bg-blue-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2" title="Voir les détails" aria-label="Voir les détails"><svg aria-hidden="true" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M2.25 12s3.5-6.5 9.75-6.5 9.75 6.5 9.75 6.5-3.5 6.5-9.75 6.5S2.25 12 2.25 12Z"/><circle cx="12" cy="12" r="3"/></svg></button><button
                      (click)="editRole(role)"
                      class="p-1.5 rounded-lg text-slate-600 hover:text-blue-400 hover:bg-green-600/10 transition-colors"
                      title="Modifier les permissions">
                      <svg aria-hidden="true" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                    </button>
                    @if (role.code !== 'ADMIN_SYSTEME') {
                      <button
                        [hidden]="!auth.hasPermission('role.supprimer')"
                        (click)="confirmDeleteRole(role)"
                        class="p-1.5 rounded-lg text-rose-600 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
                        title="Supprimer le rôle">
                        <svg aria-hidden="true" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                        </svg>
                      </button>
                    }
                  </div>
                </div>

                <h3 class="text-base font-semibold text-white mt-1">{{ role.libelle }}</h3>
                <p class="text-xs text-slate-600 mt-1 line-clamp-2 leading-relaxed">
                  {{ role.description || 'Aucune description disponible.' }}
                </p>
              </div>

              <div class="mt-5 pt-4 border-t border-slate-200 flex items-center justify-between">
                <div class="flex items-center gap-1.5 text-xs text-slate-600">
                  <svg class="w-4 h-4 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                  </svg>
                  <span class="font-medium text-blue-300">{{ role.permissions?.length || 0 }}</span>
                  <span>permissions actives</span>
                </div>

                <button type="button" (click)="showDetails('R?le', role)" class="inline-flex h-10 w-10 items-center justify-center rounded-lg text-blue-700 transition-colors hover:bg-blue-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2" title="Voir les d?tails" aria-label="Voir les d?tails"><svg aria-hidden="true" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M2.25 12s3.5-6.5 9.75-6.5 9.75 6.5 9.75 6.5-3.5 6.5-9.75 6.5S2.25 12 2.25 12Z"/><circle cx="12" cy="12" r="3"/></svg></button>
                <button
                  [hidden]="!auth.hasPermission('role.modifier')"
                  (click)="editRole(role)"
                  class="text-xs text-blue-400 hover:text-blue-300 font-medium">
                  Configurer &rarr;
                </button>
              </div>
            </div>
          }
        </div>
      }

      <!-- TAB 2 : CATALOGUE DES PERMISSIONS -->
      @if (activeTab() === 'permissions' && auth.hasPermission('permission.lire')) {
        <div class="space-y-4">
          <!-- Filter Module -->
          <div class="flex items-center gap-3">
            <span class="text-xs text-slate-600">Filtrer par module :</span>
            <div class="flex flex-wrap items-center gap-2">
              <button
                (click)="selectedModuleFilter.set(null)"
                [class.bg-green-700]="selectedModuleFilter() === null"
                [class.text-white]="selectedModuleFilter() === null"
                [class.bg-white]="selectedModuleFilter() !== null"
                [class.text-slate-600]="selectedModuleFilter() !== null"
                class="px-3 py-1 rounded-lg text-xs font-medium border border-slate-200 hover:text-white transition-all">
                Tous ({{ allPermissions().length }})
              </button>
              @for (mod of availableModules(); track mod) {
                <button
                  (click)="selectedModuleFilter.set(mod)"
                  [class.bg-green-700]="selectedModuleFilter() === mod"
                  [class.text-white]="selectedModuleFilter() === mod"
                  [class.bg-white]="selectedModuleFilter() !== mod"
                  [class.text-slate-600]="selectedModuleFilter() !== mod"
                  class="px-3 py-1 rounded-lg text-xs font-medium border border-slate-200 hover:text-white transition-all">
                  {{ mod }}
                </button>
              }
            </div>
          </div>

          <!-- Permissions Table -->
          <div class="bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-xl">
            <table class="w-full text-left text-xs">
              <thead class="bg-slate-50 text-slate-600 uppercase tracking-wider border-b border-slate-200">
                <tr>
                  <th class="py-3 px-4">Code Action</th>
                  <th class="py-3 px-4">Libellé / Description</th>
                  <th class="py-3 px-4">Module Applicatif</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-200/60">
                @for (perm of filteredPermissions(); track perm.id) {
                  <tr class="hover:bg-slate-50 transition-colors">
                    <td class="py-3 px-4 font-mono font-medium text-blue-300">{{ perm.code }}</td>
                    <td class="py-3 px-4 text-slate-800">{{ perm.libelle }}</td>
                    <td class="py-3 px-4">
                      <span class="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-slate-100 text-slate-700 border border-slate-300">
                        {{ perm.module }}
                      </span>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </div>
      }

      <!-- Role Create / Edit Modal with Permission Matrix -->
      

      <!-- Delete Role Confirmation -->
      @if (roleToDelete()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
          <div class="bg-white border border-slate-200 rounded-2xl w-full max-w-md p-6 shadow-2xl space-y-4">
            <h3 class="text-lg font-semibold text-rose-400">Confirmer la suppression</h3>
            <p class="text-xs text-slate-700 leading-relaxed">
              Voulez-vous vraiment supprimer le rôle <strong class="text-white">{{ roleToDelete()?.libelle }}</strong> ?
              Cette action est impossible si des utilisateurs y sont encore rattachés.
            </p>
            <div class="flex items-center justify-end gap-3 pt-3">
              <button
                (click)="roleToDelete.set(null)"
                class="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-700 text-slate-700 text-xs font-medium">
                Annuler
              </button>
              <button
                (click)="executeDeleteRole()"
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
export class RolesPermissionsComponent implements OnInit {
  readonly importOpen = signal(false);
  readonly importFormats = IMPORT_FORMATS;
  readonly uploadImport = (fichier: File) => this.activeTab() === 'roles' ? this.roleService.importerCsv(fichier) : this.permissionService.importerCsv(fichier);
  /** Lignes affichées (filtres et tri appliqués) : ce sont elles qui sont exportées. */
  exportIds(): (string | undefined)[] {
    return this.activeTab() === 'roles' ? this.roles().map((item) => item.id) : this.filteredPermissions().map((item) => item.id);
  }
  detailTitle = '';
  detailRecord: object | null = null;
  showDetails(title: string, record: object): void { this.detailTitle = title; this.detailRecord = record; }

  private readonly roleService = inject(RoleService);
  private readonly permissionService = inject(PermissionService);
  private readonly notify = inject(NotificationService);
  readonly auth = inject(AuthService);

  activeTab = signal<'roles' | 'permissions'>(
    this.auth.hasPermission('role.lire') ? 'roles' : 'permissions',
  );
  roles = signal<Role[]>([]);
  allPermissions = signal<Permission[]>([]);
  availableModules = signal<string[]>([]);
  selectedModuleFilter = signal<string | null>(null);

  // Modal State
  showRoleModal = signal(false);
  isEditingRole = signal(false);
  roleSaving = signal(false);
  currentRoleId: string | null = null;
  roleFormData: Partial<Role> = { code: '', libelle: '', description: '' };
  selectedPermissionIds = new Set<string>();

  // Delete State
  roleToDelete = signal<Role | null>(null);

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    forkJoin({
      roles: this.auth.hasPermission('role.lire')
        ? this.roleService.listerOuFiltrer()
        : of([] as Role[]),
      permissions: this.auth.hasPermission('permission.lire')
        ? this.permissionService.listerOuFiltrer()
        : of([] as Permission[])
    }).subscribe({
      next: (res) => {
        this.roles.set(res.roles);
        this.allPermissions.set(res.permissions);
        const modules = Array.from(new Set(res.permissions.map(p => p.module))).sort();
        this.availableModules.set(modules);
      },
      error: () => this.notify.error('Erreur de chargement des rôles et permissions')
    });
  }

  filteredPermissions() {
    const mod = this.selectedModuleFilter();
    if (!mod) return this.allPermissions();
    return this.allPermissions().filter(p => p.module === mod);
  }

  getPermissionsByModule(module: string) {
    return this.allPermissions().filter(p => p.module === module);
  }

  openRoleModal() {
    this.isEditingRole.set(false);
    this.currentRoleId = null;
    this.roleFormData = { code: '', libelle: '', description: '' };
    this.selectedPermissionIds.clear();
    this.showRoleModal.set(true);
  }

  editRole(role: Role) {
    this.isEditingRole.set(true);
    this.currentRoleId = role.id || null;
    this.roleFormData = {
      code: role.code,
      libelle: role.libelle,
      description: role.description
    };
    this.selectedPermissionIds.clear();
    if (role.permissions) {
      role.permissions.forEach(p => {
        if (p.id) this.selectedPermissionIds.add(p.id);
      });
    }
    this.showRoleModal.set(true);
  }

  closeRoleModal() {
    this.showRoleModal.set(false);
  }

  togglePermission(id: string) {
    if (this.selectedPermissionIds.has(id)) {
      this.selectedPermissionIds.delete(id);
    } else {
      this.selectedPermissionIds.add(id);
    }
  }

  selectAllPermissions() {
    this.allPermissions().forEach(p => {
      if (p.id) this.selectedPermissionIds.add(p.id);
    });
  }

  deselectAllPermissions() {
    this.selectedPermissionIds.clear();
  }

  toggleModulePermissions(module: string) {
    const modulePerms = this.getPermissionsByModule(module);
    const allChecked = modulePerms.every(p => p.id && this.selectedPermissionIds.has(p.id));
    modulePerms.forEach(p => {
      if (!p.id) return;
      if (allChecked) {
        this.selectedPermissionIds.delete(p.id);
      } else {
        this.selectedPermissionIds.add(p.id);
      }
    });
  }

  saveRole() {
    if (!this.roleFormData.code || !this.roleFormData.libelle) {
      this.notify.warning('Code et Libellé sont obligatoires.');
      return;
    }

    this.roleSaving.set(true);
    const payload: RoleRequestDto = {
      role: {
        code: this.roleFormData.code,
        libelle: this.roleFormData.libelle,
        description: this.roleFormData.description
      },
      permissionIds: Array.from(this.selectedPermissionIds)
    };

    if (this.isEditingRole() && this.currentRoleId) {
      this.roleService.modifier(this.currentRoleId, payload).subscribe({
        next: () => {
          this.notify.success('Rôle modifié avec succès');
          this.roleSaving.set(false);
          this.closeRoleModal();
          this.loadData();
        },
        error: (err) => {
          this.notify.error(err.error?.message || 'Erreur lors de la modification');
          this.roleSaving.set(false);
        }
      });
    } else {
      this.roleService.creer(payload).subscribe({
        next: () => {
          this.notify.success('Nouveau rôle créé avec succès');
          this.roleSaving.set(false);
          this.closeRoleModal();
          this.loadData();
        },
        error: (err) => {
          this.notify.error(err.error?.message || 'Erreur lors de la création');
          this.roleSaving.set(false);
        }
      });
    }
  }

  confirmDeleteRole(role: Role) {
    this.roleToDelete.set(role);
  }

  executeDeleteRole() {
    const r = this.roleToDelete();
    if (!r || !r.id) return;

    this.roleService.supprimer(r.id).subscribe({
      next: () => {
        this.notify.success('Rôle supprimé avec succès');
        this.roleToDelete.set(null);
        this.loadData();
      },
      error: (err) => {
        this.notify.error(err.error?.message || 'Erreur lors de la suppression');
      }
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
    this.notify.success(`Téléchargement de ${filename} lancé.`);
  }
}
