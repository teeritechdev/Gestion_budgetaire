import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { UtilisateurService } from '../../../core/services/utilisateur.service';
import { RoleService } from '../../../core/services/role.service';
import { UniteOrganisationnelleService } from '../../../core/services/unite-organisationnelle.service';
import { StructureService } from '../../../core/services/structure.service';
import { PermissionService } from '../../../core/services/permission.service';
import { AuditLogService } from '../../../core/services/audit-log.service';
import { AuthService } from '../../../core/services/auth.service';
import { AuditLog } from '../../../core/models/audit-log.model';

@Component({
  selector: 'app-security-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="space-y-8 max-w-7xl mx-auto">
      <!-- Page Title & Greeting -->
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 class="text-2xl font-bold text-white tracking-tight">Tableau de Bord de Sécurité</h1>
          <p class="text-sm text-slate-600 mt-1">
            Supervision de l'organigramme, des comptes utilisateurs, du contrôle d'accès RBAC et des audits.
          </p>
        </div>

        <div class="action-toolbar flex items-center gap-3">
          <button
            (click)="loadMetrics()"
            [disabled]="loading()"
            class="px-3.5 py-2 rounded-xl bg-slate-100 hover:bg-slate-700 text-slate-800 text-xs font-medium border border-slate-300/80 transition-all flex items-center gap-2">
            <svg class="w-4 h-4 text-blue-400" [class.animate-spin]="loading()" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            Actualiser
          </button>
          <a routerLink="/security/utilisateurs" class="px-4 py-2 rounded-xl bg-green-700 hover:bg-green-600 text-white text-xs font-medium shadow-lg shadow-blue-600/30 transition-all flex items-center gap-2">
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
            </svg>
            Nouvel Utilisateur
          </a>
        </div>
      </div>

      <!-- KPI Metrics Cards Grid -->
      <div class="stats-grid grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <!-- Card Utilisateurs -->
        <div class="p-6 rounded-2xl bg-white border border-slate-200/80 shadow-lg relative overflow-hidden group hover:border-blue-500/40 transition-all">
          <div class="flex items-center justify-between mb-4">
            <span class="text-xs font-semibold uppercase tracking-wider text-slate-600">Utilisateurs</span>
            <div class="w-10 h-10 rounded-xl bg-blue-500/10 text-blue-400 flex items-center justify-center border border-blue-500/20">
              <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
              </svg>
            </div>
          </div>
          <div class="text-3xl font-extrabold text-white tracking-tight">{{ totalUsers() }}</div>
          <div class="mt-2 flex items-center gap-2 text-xs text-slate-600">
            <span class="text-blue-400 font-medium">{{ activeUsers() }} actifs</span>
            <span>•</span>
            <a routerLink="/security/utilisateurs" class="hover:text-blue-400 transition-colors">Gérer &rarr;</a>
          </div>
        </div>

        <!-- Card Rôles & RBAC -->
        <div class="p-6 rounded-2xl bg-white border border-slate-200/80 shadow-lg relative overflow-hidden group hover:border-blue-500/40 transition-all">
          <div class="flex items-center justify-between mb-4">
            <span class="text-xs font-semibold uppercase tracking-wider text-slate-600">Rôles RBAC</span>
            <div class="w-10 h-10 rounded-xl bg-blue-500/10 text-blue-400 flex items-center justify-center border border-blue-500/20">
              <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
              </svg>
            </div>
          </div>
          <div class="text-3xl font-extrabold text-white tracking-tight">{{ totalRoles() }}</div>
          <div class="mt-2 flex items-center gap-2 text-xs text-slate-600">
            <span class="text-blue-300 font-medium">{{ totalPermissions() }} permissions</span>
            <span>•</span>
            <a routerLink="/security/roles-permissions" class="hover:text-blue-400 transition-colors">Configurer &rarr;</a>
          </div>
        </div>

        <!-- Card Structures & Organigramme -->
        <div class="p-6 rounded-2xl bg-white border border-slate-200/80 shadow-lg relative overflow-hidden group hover:border-blue-500/40 transition-all">
          <div class="flex items-center justify-between mb-4">
            <span class="text-xs font-semibold uppercase tracking-wider text-slate-600">Structures</span>
            <div class="w-10 h-10 rounded-xl bg-blue-500/10 text-blue-400 flex items-center justify-center border border-blue-500/20">
              <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
              </svg>
            </div>
          </div>
          <div class="text-3xl font-extrabold text-white tracking-tight">{{ totalStructures() }}</div>
          <div class="mt-2 flex items-center gap-2 text-xs text-slate-600">
            <span class="text-blue-300 font-medium">{{ totalEntites() }} entités</span>
            <span>•</span>
            <a routerLink="/security/structures" class="hover:text-blue-400 transition-colors">Organigramme &rarr;</a>
          </div>
        </div>

        <!-- Card Audits Inaltérables -->
        <div class="p-6 rounded-2xl bg-white border border-slate-200/80 shadow-lg relative overflow-hidden group hover:border-blue-500/40 transition-all">
          <div class="flex items-center justify-between mb-4">
            <span class="text-xs font-semibold uppercase tracking-wider text-slate-600">Journaux d'Audit</span>
            <div class="w-10 h-10 rounded-xl bg-amber-500/10 text-amber-400 flex items-center justify-center border border-amber-500/20">
              <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
            </div>
          </div>
          <div class="text-3xl font-extrabold text-white tracking-tight">{{ totalAuditLogs() }}</div>
          <div class="mt-2 flex items-center gap-2 text-xs text-slate-600">
            <span class="text-amber-400 font-medium">Historique immuable</span>
            <span>•</span>
            <a routerLink="/security/audit-logs" class="hover:text-blue-400 transition-colors">Traçabilité &rarr;</a>
          </div>
        </div>
      </div>

      <!-- Quick Actions and Recent Audits -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <!-- Quick Action Shortcuts -->
        <div class="lg:col-span-1 space-y-4">
          <h2 class="text-lg font-semibold text-white">Raccourcis & Gestion</h2>
          <div class="space-y-3">
            <a routerLink="/security/utilisateurs" class="p-4 rounded-xl bg-white border border-slate-200 hover:border-blue-500/50 hover:bg-slate-100/40 transition-all flex items-center justify-between group">
              <div class="action-toolbar flex items-center gap-3">
                <div class="w-10 h-10 rounded-lg bg-blue-500/10 text-blue-400 flex items-center justify-center">
                  <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
                  </svg>
                </div>
                <div>
                  <p class="text-sm font-medium text-slate-800 group-hover:text-white">Gestion des Comptes</p>
                  <p class="text-xs text-slate-500">Ajout, activation, réinitialisation mot de passe</p>
                </div>
              </div>
              <svg class="w-4 h-4 text-slate-500 group-hover:text-blue-400 transform group-hover:translate-x-1 transition-all" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
            </a>

            <a routerLink="/security/roles-permissions" class="p-4 rounded-xl bg-white border border-slate-200 hover:border-blue-500/50 hover:bg-slate-100/40 transition-all flex items-center justify-between group">
              <div class="action-toolbar flex items-center gap-3">
                <div class="w-10 h-10 rounded-lg bg-blue-500/10 text-blue-400 flex items-center justify-center">
                  <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
                  </svg>
                </div>
                <div>
                  <p class="text-sm font-medium text-slate-800 group-hover:text-white">Matrice des Permissions</p>
                  <p class="text-xs text-slate-500">Délégations de droits par module</p>
                </div>
              </div>
              <svg class="w-4 h-4 text-slate-500 group-hover:text-blue-400 transform group-hover:translate-x-1 transition-all" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
            </a>

            <a routerLink="/security/structures" class="p-4 rounded-xl bg-white border border-slate-200 hover:border-blue-500/50 hover:bg-slate-100/40 transition-all flex items-center justify-between group">
              <div class="action-toolbar flex items-center gap-3">
                <div class="w-10 h-10 rounded-lg bg-blue-500/10 text-blue-400 flex items-center justify-center">
                  <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5" />
                  </svg>
                </div>
                <div>
                  <p class="text-sm font-medium text-slate-800 group-hover:text-white">Organigramme d'Entreprise</p>
                  <p class="text-xs text-slate-500">Directions, Départements, Services</p>
                </div>
              </div>
              <svg class="w-4 h-4 text-slate-500 group-hover:text-blue-400 transform group-hover:translate-x-1 transition-all" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
            </a>

            <a routerLink="/security/entites" class="p-4 rounded-xl bg-white border border-slate-200 hover:border-blue-500/50 hover:bg-slate-100/40 transition-all flex items-center justify-between group">
              <div class="action-toolbar flex items-center gap-3">
                <div class="w-10 h-10 rounded-lg bg-blue-500/10 text-blue-400 flex items-center justify-center">
                  <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                  </svg>
                </div>
                <div>
                  <p class="text-sm font-medium text-slate-800 group-hover:text-white">Filiales & CSP</p>
                  <p class="text-xs text-slate-500">UAB Vie, Holding, Services Partagés</p>
                </div>
              </div>
              <svg class="w-4 h-4 text-slate-500 group-hover:text-blue-400 transform group-hover:translate-x-1 transition-all" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
            </a>
          </div>
        </div>

        <!-- Recent Audit Activity Table -->
        <div class="lg:col-span-2 space-y-4">
          <div class="flex items-center justify-between">
            <h2 class="text-lg font-semibold text-white">Activité Récente (Audits)</h2>
            <a routerLink="/security/audit-logs" class="text-xs text-blue-400 hover:text-blue-300 transition-colors">
              Voir tout l'historique &rarr;
            </a>
          </div>

          <div class="bg-white border border-slate-200/80 rounded-2xl overflow-hidden shadow-xl">
            @if (recentAudits().length === 0) {
              <div class="p-8 text-center text-slate-500 text-sm">
                Aucun journal d'audit enregistré pour le moment.
              </div>
            } @else {
              <div class="overflow-x-auto">
                <table class="w-full text-left text-xs">
                  <thead class="bg-slate-50/60 text-slate-600 uppercase tracking-wider border-b border-slate-200">
                    <tr>
                      <th class="py-3 px-4">Action</th>
                      <th class="py-3 px-4">Table</th>
                      <th class="py-3 px-4">ID Cible</th>
                      <th class="py-3 px-4">Adresse IP</th>
                      <th class="py-3 px-4 text-right">Date & Heure</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-slate-200/60">
                    @for (log of recentAudits(); track log.id) {
                      <tr class="hover:bg-slate-50 transition-colors">
                        <td class="py-3 px-4">
                          <span
                            class="px-2 py-0.5 rounded-full font-medium"
                            [ngClass]="{
                              'bg-blue-50 text-blue-800 border border-blue-200': log.typeAction === 'CONNEXION' || log.typeAction === 'INSERTION' || log.typeAction === 'MODIFICATION',
                              'bg-rose-500/10 text-rose-400 border border-rose-500/20': log.typeAction === 'SUPPRESSION'
                            }">
                            {{ log.typeAction }}
                          </span>
                        </td>
                        <td class="py-3 px-4 text-slate-700 font-mono">{{ log.nomTable }}</td>
                        <td class="py-3 px-4 text-slate-600 font-mono truncate max-w-[120px]">{{ log.enregistrementId }}</td>
                        <td class="py-3 px-4 text-slate-600">{{ log.adresseIp || 'Localhost' }}</td>
                        <td class="py-3 px-4 text-slate-600 text-right">{{ log.createdAt | date:'dd/MM/yyyy HH:mm:ss' }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            }
          </div>
        </div>
      </div>
    </div>
  `
})
export class SecurityDashboardComponent implements OnInit {
  private readonly userService = inject(UtilisateurService);
  private readonly roleService = inject(RoleService);
  private readonly structureService = inject(UniteOrganisationnelleService);
  private readonly entiteService = inject(StructureService);
  private readonly permissionService = inject(PermissionService);
  private readonly auditLogService = inject(AuditLogService);

  totalUsers = signal(0);
  activeUsers = signal(0);
  totalRoles = signal(0);
  totalStructures = signal(0);
  totalEntites = signal(0);
  totalPermissions = signal(0);
  totalAuditLogs = signal(0);
  recentAudits = signal<AuditLog[]>([]);
  loading = signal(false);

  ngOnInit() {
    this.loadMetrics();
  }

  loadMetrics() {
    this.loading.set(true);

    forkJoin({
      users: this.userService.listerOuFiltrer(),
      roles: this.roleService.listerOuFiltrer(),
      structures: this.structureService.listerOuFiltrer(),
      entites: this.entiteService.listerOuFiltrer(),
      permissions: this.permissionService.listerOuFiltrer(),
      auditLogs: this.auditLogService.listerOuFiltrer()
    }).subscribe({
      next: (res) => {
        this.totalUsers.set(res.users.length);
        this.activeUsers.set(res.users.filter(u => u.actif).length);
        this.totalRoles.set(res.roles.length);
        this.totalStructures.set(res.structures.length);
        this.totalEntites.set(res.entites.length);
        this.totalPermissions.set(res.permissions.length);
        this.totalAuditLogs.set(res.auditLogs.length);
        this.recentAudits.set(res.auditLogs.slice(0, 7));
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }
}
