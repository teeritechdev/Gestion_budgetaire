import { Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * Actions d'une ligne de tableau (Détails, Modifier, Supprimer) sous forme d'icônes,
 * avec infobulle et libellé accessible. Même style que les autres listes de l'application.
 */
@Component({
  selector: 'app-row-actions',
  standalone: true,
  template: `
    <div class="flex items-center justify-end gap-1.5">
      @if (showView) {
        <button type="button" (click)="view.emit()" [title]="'Voir les détails' + suffixe()" [attr.aria-label]="'Voir les détails' + suffixe()"
          class="inline-flex h-10 w-10 items-center justify-center rounded-lg text-blue-700 transition-colors hover:bg-blue-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2">
          <svg aria-hidden="true" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M2.25 12s3.5-6.5 9.75-6.5 9.75 6.5 9.75 6.5-3.5 6.5-9.75 6.5S2.25 12 2.25 12Z" />
            <circle cx="12" cy="12" r="3" />
          </svg>
        </button>
      }
      @if (canEdit) {
        <button type="button" (click)="edit.emit()" [title]="'Modifier' + suffixe()" [attr.aria-label]="'Modifier' + suffixe()"
          class="inline-flex h-10 w-10 items-center justify-center rounded-lg text-slate-600 transition-colors hover:bg-green-50 hover:text-green-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-green-600 focus-visible:ring-offset-2">
          <svg aria-hidden="true" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
          </svg>
        </button>
      }
      @if (canDelete) {
        <button type="button" (click)="remove.emit()" [disabled]="deleteDisabled"
          [title]="deleteDisabled ? deleteDisabledReason : 'Supprimer' + suffixe()" [attr.aria-label]="'Supprimer' + suffixe()"
          class="inline-flex h-10 w-10 items-center justify-center rounded-lg text-rose-600 transition-colors hover:bg-rose-50 hover:text-rose-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-600 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-30 disabled:hover:bg-transparent">
          <svg aria-hidden="true" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 6h18M8 6V4h8v2m2 0-.8 14H6.8L6 6m4 4v6m4-6v6" />
          </svg>
        </button>
      }
    </div>
  `,
})
export class RowActionsComponent {
  /** Libellé de la ligne (ex. « DIR-FIN »), ajouté aux infobulles et libellés accessibles. */
  @Input() label = '';
  @Input() showView = true;
  @Input() canEdit = false;
  @Input() canDelete = false;
  @Input() deleteDisabled = false;
  @Input() deleteDisabledReason = 'Suppression impossible';
  @Output() view = new EventEmitter<void>();
  @Output() edit = new EventEmitter<void>();
  @Output() remove = new EventEmitter<void>();

  suffixe(): string {
    return this.label ? ` : ${this.label}` : '';
  }
}
