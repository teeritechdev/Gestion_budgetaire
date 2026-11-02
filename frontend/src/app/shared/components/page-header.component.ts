import { Component, Input } from '@angular/core';
import { AppIconComponent } from './app-icon.component';

/**
 * En-tête commun des onglets : pastille d'icône, rubrique, titre et accroche courte,
 * avec les actions de la page alignées à droite (sur une seule ligne).
 * Les boutons sont projetés tels quels : <app-page-header ...> boutons </app-page-header>.
 */
@Component({
  selector: 'app-page-header',
  standalone: true,
  imports: [AppIconComponent],
  // Espace sous l'en-tête (padding : il s'ajoute à l'espacement de la page au lieu de se confondre avec lui).
  styles: [':host { display: block; padding-bottom: 1rem; }'],
  template: `
    <header class="page-header relative overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <!-- Décor discret : halo aux couleurs de l'application -->
      <span aria-hidden="true" class="pointer-events-none absolute -right-16 -top-20 h-56 w-56 rounded-full bg-gradient-to-br from-green-100 to-transparent opacity-80"></span>
      <span aria-hidden="true" class="pointer-events-none absolute -bottom-24 right-40 h-40 w-40 rounded-full bg-gradient-to-tr from-emerald-50 to-transparent"></span>

      <div class="relative flex flex-col gap-5 px-5 py-5 md:px-6 md:py-6 xl:flex-row xl:items-center xl:justify-between">
        <div class="flex min-w-0 items-start gap-4">
          <span class="flex h-12 w-12 shrink-0 items-center justify-center text-green-700">
            <app-icon [name]="icon" classe="h-9 w-9" [epaisseur]="1.6" />
          </span>
          <div class="min-w-0">
            @if (module) {
              <p class="text-[11px] font-semibold uppercase tracking-[.16em] text-green-700">{{ module }}</p>
            }
            <h1 class="mt-0.5 text-2xl font-bold tracking-tight text-slate-900 md:text-[1.7rem]">{{ title }}</h1>
            @if (description) {
              <p class="mt-1.5 max-w-2xl text-sm leading-relaxed text-slate-600">{{ description }}</p>
            }
          </div>
        </div>
        <div class="page-header-actions action-toolbar flex shrink-0 flex-nowrap items-center gap-2 xl:justify-end">
          <ng-content />
        </div>
      </div>
    </header>
  `,
})
export class PageHeaderComponent {
  @Input({ required: true }) icon = '';
  @Input({ required: true }) title = '';
  /** Rubrique au-dessus du titre (module). */
  @Input() module = '';
  /** Accroche d'une phrase : ce que l'on fait dans l'onglet. */
  @Input() description = '';
}
