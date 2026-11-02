import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';

/**
 * Mise en page commune des formulaires en pleine page : fil d'Ariane, titre, retour à la liste.
 * Le formulaire est projeté tel quel ; sa barre d'actions utilise la classe `form-page-actions`
 * (barre fixée en bas de l'écran, voir styles.css).
 */
@Component({
  selector: 'app-form-page',
  standalone: true,
  template: `
    <section class="form-page mx-auto w-full max-w-5xl" [attr.aria-label]="title">
      <nav aria-label="Fil d'Ariane" class="mb-4 flex flex-wrap items-center gap-2 text-xs text-slate-500">
        <button type="button" (click)="back.emit()" class="font-medium text-slate-600 hover:text-green-700 hover:underline">{{ section }}</button>
        <span aria-hidden="true">›</span>
        <span class="font-semibold text-slate-800">{{ title }}</span>
      </nav>

      <header class="mb-6 flex flex-col gap-4 border-b border-slate-200 pb-5 sm:flex-row sm:items-end sm:justify-between">
        <div>
          @if (mode) {
            <p class="text-xs font-semibold uppercase tracking-wider text-green-700">{{ mode }}</p>
          }
          <h1 class="mt-1 text-2xl font-bold text-slate-900">{{ title }}</h1>
          @if (subtitle) {
            <p class="mt-1 max-w-3xl text-sm text-slate-600">{{ subtitle }}</p>
          }
          <p class="mt-2 text-xs text-slate-500">Les champs marqués d’un astérisque (*) sont obligatoires.</p>
        </div>
        <button type="button" (click)="back.emit()" class="inline-flex items-center gap-2 self-start rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-100 sm:self-auto">
          <span aria-hidden="true">←</span> Retour à la liste
        </button>
      </header>

      <div class="form-page-body">
        <ng-content />
      </div>
    </section>
  `,
})
export class FormPageComponent implements OnInit {
  /** Titre de la page, ex. « Créer une direction ». */
  @Input({ required: true }) title = '';
  /** Libellé de la liste parente affiché dans le fil d'Ariane, ex. « Directions ». */
  @Input({ required: true }) section = '';
  @Input() subtitle = '';
  /** « Création » ou « Modification ». */
  @Input() mode = '';
  @Output() back = new EventEmitter<void>();

  ngOnInit(): void {
    window.scrollTo({ top: 0 });
  }
}
