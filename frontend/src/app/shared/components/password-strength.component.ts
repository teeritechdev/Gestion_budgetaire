import { Component, Input } from '@angular/core';

export interface RegleMotDePasse {
  libelle: string;
  respectee: boolean;
}

/** Mêmes règles que le backend (PolitiqueMotDePasse). */
export function reglesMotDePasse(motDePasse: string, identifiant = ''): RegleMotDePasse[] {
  const valeur = motDePasse ?? '';
  const id = (identifiant ?? '').trim().toLowerCase().split('@')[0];
  return [
    { libelle: '8 caractères minimum', respectee: valeur.length >= 8 && valeur.length <= 128 },
    { libelle: 'Une lettre majuscule', respectee: /\p{Lu}/u.test(valeur) },
    { libelle: 'Une lettre minuscule', respectee: /\p{Ll}/u.test(valeur) },
    { libelle: 'Un chiffre', respectee: /\d/.test(valeur) },
    { libelle: 'Un caractère spécial (ex. ! ? @ # -)', respectee: /[^\p{L}\p{N}]/u.test(valeur) },
    { libelle: 'Ne contient pas votre identifiant', respectee: !valeur || id.length < 3 || !valeur.toLowerCase().includes(id) },
  ];
}

export function motDePasseConforme(motDePasse: string, identifiant = ''): boolean {
  return valeurSansEspacesAuxBords(motDePasse) && reglesMotDePasse(motDePasse, identifiant).every((r) => r.respectee);
}

function valeurSansEspacesAuxBords(valeur: string): boolean {
  return !!valeur && valeur === valeur.trim();
}

/** Jauge de robustesse et liste des règles, mises à jour pendant la saisie. */
@Component({
  selector: 'app-password-strength',
  standalone: true,
  template: `
    <div class="mt-2 space-y-2" aria-live="polite">
      <div class="flex items-center gap-3">
        <div class="flex flex-1 gap-1" aria-hidden="true">
          @for (segment of [1, 2, 3, 4]; track segment) {
            <span class="h-1.5 flex-1 rounded-full transition-colors" [class]="segment <= niveau() ? couleur() : 'bg-slate-200'"></span>
          }
        </div>
        <span class="min-w-[5.5rem] text-right text-xs font-semibold" [class]="couleurTexte()">{{ libelleNiveau() }}</span>
      </div>
      <ul class="grid gap-x-4 gap-y-1 text-xs sm:grid-cols-2">
        @for (regle of regles(); track regle.libelle) {
          <li class="flex items-center gap-1.5" [class.text-green-700]="regle.respectee" [class.text-slate-500]="!regle.respectee">
            @if (regle.respectee) {
              <svg aria-hidden="true" class="h-3.5 w-3.5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M5 12.5l4.5 4.5L19 7.5" /></svg>
            } @else {
              <svg aria-hidden="true" class="h-3.5 w-3.5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="8" /></svg>
            }
            <span>{{ regle.libelle }}</span>
            <span class="sr-only">{{ regle.respectee ? '(respectée)' : '(non respectée)' }}</span>
          </li>
        }
      </ul>
    </div>
  `,
})
export class PasswordStrengthComponent {
  @Input() motDePasse = '';
  @Input() identifiant = '';

  regles(): RegleMotDePasse[] {
    return reglesMotDePasse(this.motDePasse, this.identifiant);
  }

  /** 0 à 4 : règles respectées, avec un bonus de longueur (12 caractères et plus). */
  niveau(): number {
    if (!this.motDePasse) return 0;
    const respectees = this.regles().filter((r) => r.respectee).length;
    if (respectees < 4) return 1;
    if (respectees < 6) return 2;
    return this.motDePasse.length >= 12 ? 4 : 3;
  }

  libelleNiveau(): string {
    return ['', 'Faible', 'Moyen', 'Bon', 'Excellent'][this.niveau()];
  }

  couleur(): string {
    return ['bg-slate-200', 'bg-rose-500', 'bg-amber-500', 'bg-green-600', 'bg-green-700'][this.niveau()];
  }

  couleurTexte(): string {
    return ['text-slate-400', 'text-rose-600', 'text-amber-700', 'text-green-700', 'text-green-800'][this.niveau()];
  }
}
