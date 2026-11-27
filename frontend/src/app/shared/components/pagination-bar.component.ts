import { Component, EventEmitter, Input, Output } from '@angular/core';

/** Barre de pagination pour les listes paginées côté serveur (page à partir de 0). */
@Component({
  selector: 'app-pagination-bar',
  standalone: true,
  template: `
    <div class="flex flex-wrap items-center justify-between gap-2 border-t border-slate-200 px-4 py-3 text-xs text-slate-600">
      <span>{{ total }} résultat(s) — page {{ totalPages ? page + 1 : 0 }} / {{ totalPages }}</span>
      <div class="flex gap-2">
        <button type="button" [disabled]="page <= 0" (click)="pageChange.emit(page - 1)" class="rounded-lg border border-slate-300 px-3 py-1.5 font-semibold disabled:opacity-40">← Précédent</button>
        <button type="button" [disabled]="page + 1 >= totalPages" (click)="pageChange.emit(page + 1)" class="rounded-lg border border-slate-300 px-3 py-1.5 font-semibold disabled:opacity-40">Suivant →</button>
      </div>
    </div>
  `,
})
export class PaginationBarComponent {
  @Input() page = 0;
  @Input() totalPages = 0;
  @Input() total = 0;
  @Output() pageChange = new EventEmitter<number>();
}
