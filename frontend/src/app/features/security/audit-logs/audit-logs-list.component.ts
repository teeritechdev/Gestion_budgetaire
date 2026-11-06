import { PageHeaderComponent } from '../../../shared/components/page-header.component';
import { DataActionsComponent } from '../../../shared/components/data-actions.component';
import { RowActionsComponent } from '../../../shared/components/row-actions.component';
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuditLogService } from '../../../core/services/audit-log.service';
import { NotificationService } from '../../../core/services/notification.service';
import { AuthService } from '../../../core/services/auth.service';
import { AuditLog, AuditLogDetail } from '../../../core/models/audit-log.model';

@Component({
  selector: 'app-audit-logs-list',
  standalone: true,
  imports: [PageHeaderComponent, DataActionsComponent, RowActionsComponent, CommonModule, FormsModule],
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
              <th class="py-3.5 px-4 text-right">Actions</th>
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
                    <span class="px-2 py-0.5 rounded-full font-medium" [ngClass]="actionClass(log.typeAction)">
                      {{ log.typeAction }}
                    </span>
                  </td>
                  <td class="py-3.5 px-4 font-mono text-slate-800">{{ log.nomTable }}</td>
                  <td class="py-3.5 px-4 font-mono text-slate-600 truncate max-w-[150px]">{{ log.enregistrementId }}</td>
                  <td class="py-3.5 px-4 text-slate-600">{{ log.adresseIp || 'Localhost' }}</td>
                  <td class="py-3.5 px-4 text-right">
                    <app-row-actions [label]="log.nomTable + ' du ' + (log.createdAt | date:'dd/MM/yyyy HH:mm')" (view)="inspectLog(log)" />
                  </td>
                </tr>
              }
            }
          </tbody>
        </table>
      </div>

      <!-- Vue détaillée d'une entrée -->
      @if (selectedLog(); as log) {
        <div class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4" (click)="fermerDetail()">
          <div role="dialog" aria-modal="true" aria-labelledby="audit-detail-titre" (click)="$event.stopPropagation()"
            class="flex max-h-[90vh] w-full max-w-4xl flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl">
            <header class="flex items-start justify-between gap-4 border-b border-slate-200 px-6 py-4">
              <div class="min-w-0">
                <div class="flex flex-wrap items-center gap-2">
                  <span class="rounded-full px-2.5 py-0.5 text-xs font-semibold" [ngClass]="actionClass(log.typeAction)">{{ log.typeAction }}</span>
                  <span class="font-mono text-sm text-slate-700">{{ log.nomTable }}</span>
                </div>
                <h3 id="audit-detail-titre" class="mt-1.5 text-lg font-semibold text-slate-900">Détail de l’événement d’audit</h3>
                <p class="text-xs text-slate-500">Le {{ log.createdAt | date:"dd/MM/yyyy 'à' HH:mm:ss" }}</p>
              </div>
              <button type="button" (click)="fermerDetail()" aria-label="Fermer" class="rounded-lg p-2 text-slate-500 hover:bg-slate-100">
                <svg aria-hidden="true" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </header>

            <div class="flex-1 space-y-5 overflow-y-auto px-6 py-5 text-sm">
              @if (detailLoading()) {
                <p class="text-xs text-slate-500">Chargement des informations détaillées…</p>
              }
              <div class="grid gap-4 md:grid-cols-2">
                <section class="audit-detail-bloc">
                  <h4>Événement</h4>
                  <dl>
                    <div><dt>Type d’action</dt><dd>{{ log.typeAction }}</dd></div>
                    <div><dt>Ressource</dt><dd class="font-mono">{{ log.nomTable }}</dd></div>
                    <div><dt>ID enregistrement</dt><dd class="font-mono break-all">{{ log.enregistrementId || '—' }}</dd></div>
                    <div><dt>Date et heure</dt><dd>{{ log.createdAt | date:'dd/MM/yyyy HH:mm:ss' }}</dd></div>
                    <div><dt>ID de l’entrée</dt><dd class="font-mono break-all text-slate-500">{{ log.id }}</dd></div>
                  </dl>
                </section>

                <section class="audit-detail-bloc">
                  <h4>Auteur</h4>
                  <dl>
                    <div><dt>Utilisateur</dt><dd>{{ detail()?.auteur || (log.utilisateurId ? '—' : 'Système / anonyme') }}</dd></div>
                    <div><dt>Identifiant</dt><dd>{{ detail()?.auteurUsername || '—' }}</dd></div>
                    <div><dt>Matricule</dt><dd>{{ detail()?.auteurMatricule || '—' }}</dd></div>
                    <div><dt>Rôle</dt><dd>{{ detail()?.auteurRole || '—' }}</dd></div>
                    <div><dt>ID utilisateur</dt><dd class="font-mono break-all">{{ log.utilisateurId || '— (aucun compte)' }}</dd></div>
                  </dl>
                </section>

                <section class="audit-detail-bloc">
                  <h4>Poste de travail</h4>
                  <dl>
                    <div><dt>Adresse IP</dt><dd class="font-mono">{{ log.adresseIp || '—' }}@if (estPosteLocal(log.adresseIp)) {<span class="ml-1 font-sans text-xs text-slate-500">(poste local)</span>}</dd></div>
                    <div><dt>Navigateur</dt><dd>{{ detail()?.navigateur || '—' }}</dd></div>
                    <div><dt>User agent</dt><dd class="break-all font-mono text-xs text-slate-600">{{ log.userAgent || 'Non renseigné' }}</dd></div>
                  </dl>
                </section>

                <section class="audit-detail-bloc">
                  <h4>Requête</h4>
                  <dl>
                    <div><dt>Appel</dt><dd class="font-mono break-all">{{ detail()?.requete || '—' }}</dd></div>
                  </dl>
                </section>
              </div>

              <section class="audit-detail-bloc">
                <h4>Valeurs enregistrées</h4>
                @if (detail()?.champs?.length) {
                  <div class="overflow-x-auto">
                    <table class="w-full text-left text-xs">
                      <thead class="text-slate-600"><tr><th class="py-2 pr-3">Champ</th><th class="py-2 pr-3">Ancienne valeur</th><th class="py-2">Nouvelle valeur</th></tr></thead>
                      <tbody class="divide-y divide-slate-200">
                        @for (champ of detail()!.champs; track champ.nom) {
                          <tr [class.bg-amber-50]="champ.modifie">
                            <td class="py-2 pr-3 font-semibold text-slate-800">{{ champ.nom }}@if (champ.modifie) {<span class="ml-1.5 rounded bg-amber-100 px-1.5 py-0.5 text-[10px] font-semibold text-amber-800">modifié</span>}</td>
                            <td class="break-all py-2 pr-3 font-mono text-slate-600">{{ champ.ancienneValeur ?? '—' }}</td>
                            <td class="break-all py-2 font-mono text-slate-900">{{ champ.nouvelleValeur ?? '—' }}</td>
                          </tr>
                        }
                      </tbody>
                    </table>
                  </div>
                } @else if (!detailLoading()) {
                  <p class="text-xs text-slate-500">Aucune valeur enregistrée pour cet événement.</p>
                }
                <details class="mt-3 text-xs">
                  <summary class="cursor-pointer font-medium text-slate-600">Voir les données brutes (JSON)</summary>
                  <div class="mt-2 grid gap-3 md:grid-cols-2">
                    <pre class="max-h-64 overflow-auto rounded-lg border border-slate-200 bg-slate-50 p-3 font-mono text-[11px] text-slate-700">{{ log.anciennesValeurs ? (log.anciennesValeurs | json) : 'Aucune ancienne valeur' }}</pre>
                    <pre class="max-h-64 overflow-auto rounded-lg border border-slate-200 bg-slate-50 p-3 font-mono text-[11px] text-slate-700">{{ log.nouvellesValeurs ? (log.nouvellesValeurs | json) : 'Aucune nouvelle valeur' }}</pre>
                  </div>
                </details>
              </section>
            </div>

            <footer class="flex justify-end border-t border-slate-200 px-6 py-3">
              <button type="button" (click)="fermerDetail()" class="rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-100">Fermer</button>
            </footer>
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
  readonly detail = signal<AuditLogDetail | null>(null);
  readonly detailLoading = signal(false);

  filterTypeAction: string | null = null;
  filterNomTable = '';

  ngOnInit() {
    this.loadLogs();
  }

  loadLogs() {
    this.auditLogService.listerOuFiltrer().subscribe({
      next: (res) => this.logs.set(plusRecentsDAbord(res)),
      error: () => this.notify.error('Erreur chargement des logs d\'audit')
    });
  }

  applyFilters() {
    this.auditLogService.listerOuFiltrer(
      undefined,
      this.filterTypeAction || undefined,
      this.filterNomTable || undefined
    ).subscribe({
      next: (res) => this.logs.set(plusRecentsDAbord(res)),
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
    this.detail.set(null);
    this.detailLoading.set(true);
    this.auditLogService.detail(log.id).subscribe({
      next: (detail) => {
        if (this.selectedLog()?.id === log.id) this.detail.set(detail);
        this.detailLoading.set(false);
      },
      error: () => {
        // Les données de base restent affichées ; seules les informations enrichies manquent.
        this.detailLoading.set(false);
        this.notify.error('Informations détaillées indisponibles pour cette entrée.');
      },
    });
  }

  fermerDetail() {
    this.selectedLog.set(null);
    this.detail.set(null);
  }

  actionClass(typeAction: string): string {
    switch (typeAction) {
      case 'INSERTION': return 'border border-green-200 bg-green-50 text-green-800';
      case 'MODIFICATION': return 'border border-amber-200 bg-amber-50 text-amber-800';
      case 'SUPPRESSION': return 'border border-rose-200 bg-rose-50 text-rose-700';
      case 'CONNEXION': return 'border border-blue-200 bg-blue-50 text-blue-800';
      default: return 'border border-slate-200 bg-slate-100 text-slate-700';
    }
  }

  estPosteLocal(ip?: string | null): boolean {
    return ip === '0:0:0:0:0:0:0:1' || ip === '::1' || !!ip?.startsWith('127.');
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

/** Journal affiché du plus récent au plus ancien. */
function plusRecentsDAbord(logs: AuditLog[]): AuditLog[] {
  const horodatage = (log: AuditLog) => (log.createdAt ? new Date(log.createdAt).getTime() : 0);
  return [...logs].sort((a, b) => horodatage(b) - horodatage(a));
}
