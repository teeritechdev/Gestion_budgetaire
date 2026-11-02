import { PageHeaderComponent } from '../../../shared/components/page-header.component';
import { DataActionsComponent } from '../../../shared/components/data-actions.component';
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuditLogService } from '../../../core/services/audit-log.service';
import { NotificationService } from '../../../core/services/notification.service';
import { AuthService } from '../../../core/services/auth.service';
import { AuditLog } from '../../../core/models/audit-log.model';

@Component({
  selector: 'app-audit-logs-list',
  standalone: true,
  imports: [PageHeaderComponent, DataActionsComponent, CommonModule, FormsModule],
  template: `
    <div class="space-y-6 max-w-7xl mx-auto">
      <app-page-header icon="audit" module="Administration" title="Journal d’audit" description="Chaque action sensible, horodatée et traçable.">
<app-data-actions ressource="audit-logs" exportPermission="audit.exporter" [ids]="exportIds()" />
</app-page-header>

      <!-- Filters -->
      <div class="bg-white border border-slate-200 rounded-2xl p-4 shadow-lg flex flex-wrap items-center gap-3">
        <!-- Type Action -->
        <select
          [(ngModel)]="filterTypeAction"
          (change)="applyFilters()"
          class="bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-700 py-2 px-3 focus:outline-none focus:ring-1 focus:ring-blue-500">
          <option [ngValue]="null">Toutes les Actions</option>
          <option value="CONNEXION">CONNEXION</option>
          <option value="INSERTION">INSERTION</option>
          <option value="MODIFICATION">MODIFICATION</option>
          <option value="SUPPRESSION">SUPPRESSION</option>
        </select>

        <!-- Table name -->
        <input
          type="text"
          [(ngModel)]="filterNomTable"
          (input)="applyFilters()"
          placeholder="Filtrer par table (ex: utilisateurs)..."
          class="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        />

        <button
          (click)="resetFilters()"
          class="p-2 rounded-xl bg-slate-100 hover:bg-slate-700 text-slate-600 hover:text-white transition-colors"
          title="Réinitialiser filtres">
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- Audit Logs Table -->
      <div class="bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-xl">
        <table class="w-full text-left text-xs">
          <thead class="bg-slate-50/80 text-slate-600 uppercase tracking-wider border-b border-slate-200">
            <tr>
              <th class="py-3.5 px-4">Date & Heure</th>
              <th class="py-3.5 px-4">Action</th>
              <th class="py-3.5 px-4">Entité / Table Cible</th>
              <th class="py-3.5 px-4">ID Enregistrement</th>
              <th class="py-3.5 px-4">Adresse IP</th>
              <th class="py-3.5 px-4 text-right">Détails</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-200/60">
            @if (logs().length === 0) {
              <tr>
                <td colspan="6" class="py-8 text-center text-slate-500">
                  Aucun log d'audit ne correspond aux filtres actuels.
                </td>
              </tr>
            } @else {
              @for (log of logs(); track log.id) {
                <tr class="hover:bg-slate-100/40 transition-colors">
                  <td class="py-3.5 px-4 font-mono text-slate-700">
                    {{ log.createdAt | date:'dd/MM/yyyy HH:mm:ss' }}
                  </td>
                  <td class="py-3.5 px-4">
                    <span
                      class="px-2 py-0.5 rounded-full font-medium"
                      [ngClass]="{
                        'bg-blue-50 text-blue-800 border border-blue-200': log.typeAction === 'CONNEXION' || log.typeAction === 'INSERTION' || log.typeAction === 'MODIFICATION',
                        'bg-rose-500/10 text-rose-400 border border-rose-500/20': log.typeAction === 'SUPPRESSION'
                      }">
                      {{ log.typeAction }}
                    </span>
                  </td>
                  <td class="py-3.5 px-4 font-mono text-slate-800">{{ log.nomTable }}</td>
                  <td class="py-3.5 px-4 font-mono text-slate-600 truncate max-w-[150px]">{{ log.enregistrementId }}</td>
                  <td class="py-3.5 px-4 text-slate-600">{{ log.adresseIp || 'Localhost' }}</td>
                  <td class="py-3.5 px-4 text-right">
                    <button
                      (click)="inspectLog(log)"
                      class="px-2.5 py-1 rounded-lg bg-slate-100 hover:bg-slate-700 text-blue-300 hover:text-white transition-colors text-[11px] font-medium">
                      Examiner
                    </button>
                  </td>
                </tr>
              }
            }
          </tbody>
        </table>
      </div>

      <!-- JSON Diff / Detail Modal -->
      @if (selectedLog()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm">
          <div class="bg-white border border-slate-200 rounded-2xl w-full max-w-3xl p-6 shadow-2xl space-y-4 max-h-[90vh] flex flex-col">
            <div class="flex items-center justify-between border-b border-slate-200 pb-3">
              <div>
                <h3 class="text-lg font-semibold text-white">Détails de l'Audit</h3>
                <p class="text-xs text-slate-600 font-mono">
                  {{ selectedLog()?.typeAction }} sur {{ selectedLog()?.nomTable }} (ID: {{ selectedLog()?.enregistrementId }})
                </p>
              </div>
              <button (click)="selectedLog.set(null)" class="text-slate-600 hover:text-white">
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <div class="flex-1 overflow-y-auto space-y-4 text-xs">
              <div class="grid grid-cols-2 gap-4">
                <div class="p-3 bg-slate-50 rounded-xl border border-slate-200">
                  <span class="text-slate-500 uppercase font-semibold text-[10px]">Utilisateur ID</span>
                  <p class="text-slate-800 font-mono mt-0.5 truncate">{{ selectedLog()?.utilisateurId || 'Système / Anonyme' }}</p>
                </div>
                <div class="p-3 bg-slate-50 rounded-xl border border-slate-200">
                  <span class="text-slate-500 uppercase font-semibold text-[10px]">User-Agent</span>
                  <p class="text-slate-800 mt-0.5 truncate">{{ selectedLog()?.userAgent || 'Non renseigné' }}</p>
                </div>
              </div>

              <!-- JSON Comparison (Anciennes vs Nouvelles Valeurs) -->
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <h4 class="text-xs font-semibold text-rose-400 uppercase tracking-wider mb-1.5">
                    Anciennes Valeurs
                  </h4>
                  <pre class="bg-slate-50 border border-slate-200 rounded-xl p-3 text-[11px] font-mono text-slate-700 overflow-x-auto max-h-64">{{ selectedLog()?.anciennesValeurs ? (selectedLog()?.anciennesValeurs | json) : selectedLog()?.typeAction === 'INSERTION' ? 'Aucune (création)' : 'Valeurs antérieures non capturées.' }}</pre>
                </div>

                <div>
                  <h4 class="text-xs font-semibold text-blue-400 uppercase tracking-wider mb-1.5">
                    Nouvelles Valeurs
                  </h4>
                  <pre class="bg-slate-50 border border-slate-200 rounded-xl p-3 text-[11px] font-mono text-slate-700 overflow-x-auto max-h-64">{{ selectedLog()?.nouvellesValeurs ? (selectedLog()?.nouvellesValeurs | json) : selectedLog()?.typeAction === 'SUPPRESSION' ? 'Aucune (suppression)' : 'Aucune valeur enregistrée.' }}</pre>
                </div>
              </div>
            </div>

            <div class="flex items-center justify-end pt-3 border-t border-slate-200">
              <button
                (click)="selectedLog.set(null)"
                class="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-700 text-slate-700 text-xs font-medium">
                Fermer
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  `
})
export class AuditLogsListComponent implements OnInit {
  /** Lignes affichées (filtres et tri appliqués) : ce sont elles qui sont exportées. */
  exportIds(): (string | undefined)[] {
    return this.logs().map((item) => item.id);
  }
  private readonly auditLogService = inject(AuditLogService);
  private readonly notify = inject(NotificationService);
  readonly auth = inject(AuthService);

  logs = signal<AuditLog[]>([]);
  selectedLog = signal<AuditLog | null>(null);

  filterTypeAction: string | null = null;
  filterNomTable = '';

  ngOnInit() {
    this.loadLogs();
  }

  loadLogs() {
    this.auditLogService.listerOuFiltrer().subscribe({
      next: (res) => this.logs.set(res),
      error: () => this.notify.error('Erreur chargement des logs d\'audit')
    });
  }

  applyFilters() {
    this.auditLogService.listerOuFiltrer(
      undefined,
      this.filterTypeAction || undefined,
      this.filterNomTable || undefined
    ).subscribe({
      next: (res) => this.logs.set(res),
      error: () => {}
    });
  }

  resetFilters() {
    this.filterTypeAction = null;
    this.filterNomTable = '';
    this.loadLogs();
  }

  inspectLog(log: AuditLog) {
    this.selectedLog.set(log);
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
