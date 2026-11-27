import { Component } from '@angular/core';
import { AppIconComponent } from '../../shared/components/app-icon.component';

/**
 * Gabarit des pages publiques (connexion, mot de passe oublié) : panneau de marque à gauche
 * sur grand écran, formulaire centré à droite ; sur mobile, marque compacte au-dessus du formulaire.
 */
@Component({
  selector: 'app-auth-shell',
  standalone: true,
  imports: [AppIconComponent],
  template: `
    <div class="auth-shell flex min-h-dvh bg-slate-50">
      <aside class="auth-marque relative hidden w-[44%] max-w-[640px] flex-col justify-between overflow-hidden p-10 text-white lg:flex xl:p-14">
        <div class="relative flex items-center gap-3">
          <span class="flex h-11 w-11 items-center justify-center rounded-xl bg-white text-lg font-bold text-green-800">U</span>
          <div>
            <p class="text-base font-bold leading-tight">UAB Assurances</p>
            <p class="text-sm text-green-100">Gestion intégrée</p>
          </div>
        </div>

        <div class="relative max-w-md">
          <h2 class="text-3xl font-bold leading-tight xl:text-4xl">Pilotez votre budget en toute maîtrise.</h2>
          <p class="mt-4 text-base text-green-50/90">Planification stratégique, allocations et suivi de l’exécution réunis sur une seule plateforme.</p>
          <ul class="mt-8 space-y-4 text-sm">
            @for (point of points; track point.texte) {
              <li class="flex items-center gap-3">
                <span class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-white/15">
                  <app-icon [name]="point.icone" classe="h-[18px] w-[18px]" />
                </span>
                <span class="text-green-50">{{ point.texte }}</span>
              </li>
            }
          </ul>
        </div>

        <p class="relative text-xs text-green-100/80">© {{ annee }} UAB Assurances · Accès réservé au personnel autorisé</p>
      </aside>

      <main class="flex flex-1 flex-col">
        <div class="flex flex-1 items-center justify-center px-5 py-10 sm:px-8">
          <div class="w-full max-w-[400px]">
            <div class="mb-8 flex items-center gap-3 lg:hidden">
              <span class="flex h-10 w-10 items-center justify-center rounded-xl bg-green-700 text-base font-bold text-white">U</span>
              <div>
                <p class="text-sm font-bold leading-tight text-slate-900">UAB Assurances</p>
                <p class="text-xs text-slate-500">Plateforme de gestion budgétaire</p>
              </div>
            </div>
            <ng-content />
          </div>
        </div>
        <p class="px-5 pb-6 text-center text-xs text-slate-500 lg:hidden">© {{ annee }} UAB Assurances · Accès réservé au personnel autorisé</p>
      </main>
    </div>
  `,
})
export class AuthShellComponent {
  readonly annee = new Date().getFullYear();
  readonly points = [
    { icone: 'plans', texte: 'Plans stratégiques et exercices budgétaires' },
    { icone: 'allocations', texte: 'Allocations, révisions et répartitions' },
    { icone: 'audit', texte: 'Traçabilité complète des opérations' },
  ];
}
