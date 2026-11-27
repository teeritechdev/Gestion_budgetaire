import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { FormatExport, TransfertService } from '../../core/services/transfert.service';
import type { FormatImport } from '../../core/models/import-formats';

/**
 * Boutons Export CSV / Export PDF / Modèle d'import / Import CSV d'un onglet.
 * L'export porte sur `ids` : les identifiants des lignes actuellement affichées (filtres et tri appliqués).
 */
@Component({
  selector: 'app-data-actions',
  standalone: true,
  template: `
    <div class="action-toolbar flex flex-nowrap items-center gap-2">
      @if (canExport()) {
        <button type="button" (click)="exporter('csv')" [disabled]="busy() || !ids.length" [title]="exportTitle()"
          class="flex shrink-0 items-center gap-1.5 whitespace-nowrap rounded-xl border border-blue-800 bg-blue-700 px-3.5 py-2 text-xs font-medium text-white transition-colors hover:bg-blue-800 disabled:cursor-not-allowed disabled:opacity-50">
          <svg aria-hidden="true" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
          {{ busy() === 'csv' ? 'Export…' : 'Export CSV' }}
        </button>
        <button type="button" (click)="exporter('pdf')" [disabled]="busy() || !ids.length" [title]="exportTitle()"
          class="flex shrink-0 items-center gap-1.5 whitespace-nowrap rounded-xl border border-blue-800 bg-blue-700 px-3.5 py-2 text-xs font-medium text-white transition-colors hover:bg-blue-800 disabled:cursor-not-allowed disabled:opacity-50">
          <svg aria-hidden="true" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" /></svg>
          {{ busy() === 'pdf' ? 'Export…' : 'Export PDF' }}
        </button>
      }
      @if (canImport() && importFormat) {
        <button type="button" (click)="telechargerModele()" [title]="'Télécharger le fichier modèle à remplir (' + importFormat.nomModele + ')'"
          class="flex shrink-0 items-center gap-1.5 whitespace-nowrap rounded-xl border border-blue-700 bg-white px-3.5 py-2 text-xs font-medium text-blue-700 transition-colors hover:bg-blue-50">
          <svg aria-hidden="true" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 3H7a2 2 0 00-2 2v14a2 2 0 002 2h10a2 2 0 002-2V8z" /><path d="M14 3v5h5M9 13h6M9 17h4" /></svg>
          Modèle d’import
        </button>
      }
      @if (canImport()) {
        <button type="button" (click)="importer.emit()"
          class="flex shrink-0 items-center gap-1.5 whitespace-nowrap rounded-xl border border-blue-800 bg-blue-700 px-3.5 py-2 text-xs font-medium text-white transition-colors hover:bg-blue-800">
          <svg aria-hidden="true" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" /></svg>
          Import CSV
        </button>
      }
    </div>
  `,
})
export class DataActionsComponent {
  private readonly auth = inject(AuthService);
  private readonly transfert = inject(TransfertService);
  private readonly notifications = inject(NotificationService);

  /** Ressource côté API, ex. « directions ». */
  @Input({ required: true }) ressource = '';
  /** Identifiants des lignes affichées, dans l'ordre d'affichage. */
  @Input() ids: readonly (string | undefined | null)[] = [];
  @Input() exportPermission = '';
  /** Une des permissions suffit pour afficher le bouton d'import (vide : pas d'import). */
  @Input() importPermissions: readonly string[] = [];
  /** Format d'import de l'onglet : permet de télécharger le fichier modèle à remplir. */
  @Input() importFormat: FormatImport | null = null;
  @Output() importer = new EventEmitter<void>();

  readonly busy = signal<FormatExport | null>(null);

  canExport(): boolean {
    return !!this.exportPermission && this.auth.hasPermission(this.exportPermission);
  }

  canImport(): boolean {
    return this.importPermissions.length > 0 && this.auth.hasAnyPermission([...this.importPermissions]);
  }

  /** Fichier CSV vierge (ligne d'en-tête avec les colonnes attendues) à remplir puis importer. */
  telechargerModele(): void {
    if (!this.importFormat) return;
    this.transfert.telechargerModele(this.importFormat.nomModele, this.importFormat.colonnes.map((c) => c.nom), []);
    this.notifications.success(`Modèle « ${this.importFormat.nomModele} » téléchargé : remplissez-le puis utilisez Import CSV.`);
  }

  exportTitle(): string {
    const n = this.ids.filter(Boolean).length;
    return n ? `Exporter les ${n} ligne(s) affichée(s) avec les filtres en cours` : 'Aucune ligne affichée à exporter';
  }

  exporter(format: FormatExport): void {
    const n = this.ids.filter(Boolean).length;
    if (!n || this.busy()) return;
    this.busy.set(format);
    this.transfert.exporter(this.ressource, format, this.ids).subscribe({
      next: () => {
        this.busy.set(null);
        this.notifications.success(`Export ${format.toUpperCase()} généré : ${n} ligne(s).`);
      },
      error: (error: HttpErrorResponse) => {
        this.busy.set(null);
        this.notifications.error(messageErreurBlob(error) || `L’export ${format.toUpperCase()} a échoué.`);
      },
    });
  }
}

/** Les erreurs d'une requête « blob » arrivent sous forme de Blob : on ne peut pas lire le message directement. */
function messageErreurBlob(error: HttpErrorResponse): string {
  if (error.status === 403) return 'Vous n’avez pas la permission d’exporter ces données.';
  if (error.status === 0) return 'Le serveur est injoignable.';
  return '';
}
