import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-record-details-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (record) {
      <div class="fixed inset-0 z-[70] flex items-center justify-center bg-slate-950/50 p-4" (click)="$event.target === $event.currentTarget && close.emit()">
        <section role="dialog" aria-modal="true" [attr.aria-label]="title" class="max-h-[85vh] w-full max-w-2xl overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl">
          <header class="flex items-center justify-between border-b border-slate-200 px-6 py-4">
            <div><p class="text-xs font-semibold uppercase tracking-wider text-green-700">Consultation</p><h2 class="mt-1 text-xl font-semibold text-slate-900">{{ title }}</h2></div>
            <button type="button" (click)="close.emit()" aria-label="Fermer" title="Fermer" class="rounded-lg p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-900">
              <svg aria-hidden="true" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="m18 6-12 12M6 6l12 12"/></svg>
            </button>
          </header>
          <div class="grid max-h-[65vh] gap-3 overflow-y-auto p-6 sm:grid-cols-2">
            @for (field of fields; track field.key) {
              <article class="min-w-0 rounded-xl border border-slate-200 bg-slate-50 p-3" [ngClass]="field.wide ? 'sm:col-span-2' : ''">
                <p class="text-xs font-medium text-slate-500">{{ field.label }}</p>
                <p class="mt-1 break-words text-base font-medium text-slate-900">{{ field.value }}</p>
              </article>
            }
          </div>
          <footer class="flex justify-end border-t border-slate-200 px-6 py-4"><button type="button" (click)="close.emit()" class="rounded-lg bg-green-700 px-4 py-2 text-sm font-semibold text-white hover:bg-green-800">Fermer</button></footer>
        </section>
      </div>
    }
  `
})
export class RecordDetailsDialogComponent {
  @Input() title = 'Détails';
  @Input() record: object | null = null;
  @Output() close = new EventEmitter<void>();

  get fields(): Array<{ key: string; label: string; value: string; wide: boolean }> {
    if (!this.record) return [];
    return Object.entries(this.record as Record<string, unknown>)
      .filter(([key, value]) => !/^(?:id|.*Id)$/i.test(key) && !/mot.?de.?passe|password|hash/i.test(key) && value !== undefined)
      .map(([key, value]) => ({
        key,
        label: key.replace(/([a-z0-9])([A-Z])/g, '$1 $2').replace(/Id\b/g, 'Identifiant').replace(/^./, letter => letter.toLocaleUpperCase('fr')),
        value: this.format(value),
        wide: value !== null && typeof value === 'object'
      }));
  }

  private format(value: unknown): string {
    if (value == null || value === '') return 'Non renseigné';
    if (typeof value === 'boolean') return value ? 'Oui' : 'Non';
    if (Array.isArray(value)) return value.length ? value.map(item => this.format(item)).join(', ') : 'Aucun';
    if (typeof value === 'object') {
      const item = value as Record<string, unknown>;
      const summary = item['libelle'] ?? item['nom'] ?? item['code'] ?? item['email'];
      return summary == null ? JSON.stringify(value) : String(summary);
    }
    return String(value);
  }
}


