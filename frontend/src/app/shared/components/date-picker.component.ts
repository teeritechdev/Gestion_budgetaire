import { Component, ElementRef, HostListener, Input, ViewChild, computed, forwardRef, signal } from '@angular/core';
import { AbstractControl, ControlValueAccessor, NG_VALIDATORS, NG_VALUE_ACCESSOR, ValidationErrors, Validator } from '@angular/forms';

type Vue = 'jours' | 'mois' | 'annees';

interface Jour {
  iso: string;
  numero: number;
  horsMois: boolean;
  desactive: boolean;
}

const MOIS = ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'];
const MOIS_COURTS = ['Janv.', 'Févr.', 'Mars', 'Avr.', 'Mai', 'Juin', 'Juil.', 'Août', 'Sept.', 'Oct.', 'Nov.', 'Déc.'];
const JOURS_SEMAINE = ['Lu', 'Ma', 'Me', 'Je', 'Ve', 'Sa', 'Di'];

/**
 * Sélecteur de date utilisable avec [(ngModel)] : valeur au format ISO « AAAA-MM-JJ » (comme <input type="date">),
 * affichage et saisie au format « JJ/MM/AAAA ». Calendrier déroulant (semaine du lundi au dimanche),
 * vues mois et années, bornes [min] / [max], navigation au clavier.
 */
@Component({
  selector: 'app-date-picker',
  standalone: true,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => DatePickerComponent), multi: true },
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => DatePickerComponent), multi: true },
  ],
  template: `
    <div class="date-picker relative font-normal">
      <div class="relative">
        <input #champ type="text" inputmode="numeric" autocomplete="off" maxlength="10"
          [value]="texte()" [placeholder]="placeholder" [disabled]="disabled()"
          [attr.aria-invalid]="saisieInvalide() || null" aria-haspopup="dialog" [attr.aria-expanded]="open()"
          (input)="onSaisie($event)" (blur)="onBlur()" (click)="ouvrir()" (keydown)="onChampKeydown($event)"
          class="date-picker-input w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 pr-11 text-sm text-slate-900">
        <button type="button" tabindex="-1" [disabled]="disabled()" (mousedown)="$event.preventDefault()" (click)="basculer()"
          aria-label="Ouvrir le calendrier"
          class="date-picker-icone absolute right-1.5 top-1/2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-md text-slate-500 hover:bg-slate-100 hover:text-slate-800 disabled:opacity-40">
          <svg aria-hidden="true" class="h-[18px] w-[18px]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3.5" y="5" width="17" height="15.5" rx="2.5" /><path d="M3.5 10h17M8 3v4M16 3v4" />
          </svg>
        </button>
      </div>

      @if (open()) {
        <div #panneau role="dialog" aria-label="Choisir une date" tabindex="-1"
          (mousedown)="$event.preventDefault()" (click)="$event.preventDefault()" (keydown)="onPanneauKeydown($event)"
          class="date-picker-panneau absolute left-0 z-50 mt-2 w-[19rem] rounded-xl border border-slate-200 bg-white p-3 shadow-lg outline-none">
          <div class="mb-2 flex items-center justify-between">
            <button type="button" (click)="precedent()" [attr.aria-label]="vue() === 'jours' ? 'Mois précédent' : vue() === 'mois' ? 'Année précédente' : 'Années précédentes'"
              class="flex h-8 w-8 items-center justify-center rounded-md text-slate-600 hover:bg-slate-100">
              <svg aria-hidden="true" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 18l-6-6 6-6" /></svg>
            </button>
            <button type="button" (click)="changerVue()" class="rounded-md px-2.5 py-1 text-sm font-semibold text-slate-900 hover:bg-slate-100">
              {{ titre() }}
            </button>
            <button type="button" (click)="suivant()" [attr.aria-label]="vue() === 'jours' ? 'Mois suivant' : vue() === 'mois' ? 'Année suivante' : 'Années suivantes'"
              class="flex h-8 w-8 items-center justify-center rounded-md text-slate-600 hover:bg-slate-100">
              <svg aria-hidden="true" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 6l6 6-6 6" /></svg>
            </button>
          </div>

          @switch (vue()) {
            @case ('jours') {
              <div class="grid grid-cols-7 text-center text-[11px] font-semibold uppercase text-slate-400">
                @for (j of joursSemaine; track j) { <span class="py-1">{{ j }}</span> }
              </div>
              <div role="grid" class="grid grid-cols-7 gap-0.5">
                @for (jour of jours(); track jour.iso) {
                  <button type="button" role="gridcell" [disabled]="jour.desactive" (click)="choisir(jour.iso)"
                    [attr.aria-selected]="jour.iso === valeur()" [attr.aria-current]="jour.iso === aujourdhui ? 'date' : null"
                    class="date-picker-jour h-9 rounded-md text-sm"
                    [class.date-picker-jour-hors-mois]="jour.horsMois"
                    [class.date-picker-jour-aujourdhui]="jour.iso === aujourdhui"
                    [class.date-picker-jour-actif]="jour.iso === actif()"
                    [class.date-picker-jour-choisi]="jour.iso === valeur()">{{ jour.numero }}</button>
                }
              </div>
            }
            @case ('mois') {
              <div class="grid grid-cols-3 gap-1.5 py-1">
                @for (m of moisCourts; track $index) {
                  <button type="button" (click)="choisirMois($index)"
                    class="date-picker-cellule h-11 rounded-md text-sm"
                    [class.date-picker-jour-choisi]="$index === moisVue() && anneeChoisie() === anneeVue()">{{ m }}</button>
                }
              </div>
            }
            @case ('annees') {
              <div class="grid grid-cols-3 gap-1.5 py-1">
                @for (a of annees(); track a) {
                  <button type="button" (click)="choisirAnnee(a)"
                    class="date-picker-cellule h-11 rounded-md text-sm"
                    [class.date-picker-jour-aujourdhui]="a === anneeCourante"
                    [class.date-picker-jour-choisi]="a === anneeChoisie()">{{ a }}</button>
                }
              </div>
            }
          }

          <div class="mt-2 flex items-center justify-between border-t border-slate-100 pt-2">
            <button type="button" (click)="effacer()" class="rounded-md px-2.5 py-1.5 text-xs font-medium text-slate-500 hover:bg-slate-100">Effacer</button>
            <button type="button" (click)="choisirAujourdhui()" [disabled]="horsBornes(aujourdhui)"
              class="rounded-md px-2.5 py-1.5 text-xs font-semibold text-green-700 hover:bg-green-50 disabled:opacity-40">Aujourd’hui</button>
          </div>
        </div>
      }
    </div>
  `,
})
export class DatePickerComponent implements ControlValueAccessor, Validator {
  @Input() placeholder = '';
  /** Bornes au format ISO « AAAA-MM-JJ » ; vides = pas de borne. */
  @Input() set min(valeur: string | null | undefined) {
    this.borneMin.set(valeur || null);
  }
  @Input() set max(valeur: string | null | undefined) {
    this.borneMax.set(valeur || null);
  }

  @ViewChild('champ') private champ?: ElementRef<HTMLInputElement>;
  @ViewChild('panneau') private set panneau(ref: ElementRef<HTMLElement> | undefined) {
    if (ref && this.focusPanneau) {
      this.focusPanneau = false;
      ref.nativeElement.focus();
    }
  }

  readonly joursSemaine = JOURS_SEMAINE;
  readonly moisCourts = MOIS_COURTS;
  readonly aujourdhui = versIso(new Date());
  readonly anneeCourante = new Date().getFullYear();

  readonly valeur = signal('');
  readonly texte = signal('');
  readonly saisieInvalide = signal(false);
  readonly open = signal(false);
  readonly vue = signal<Vue>('jours');
  readonly anneeVue = signal(this.anneeCourante);
  readonly moisVue = signal(new Date().getMonth());
  readonly actif = signal(this.aujourdhui);
  readonly disabled = signal(false);
  private readonly borneMin = signal<string | null>(null);
  private readonly borneMax = signal<string | null>(null);
  private focusPanneau = false;

  private onChange: (valeur: string) => void = () => {};
  private onTouched: () => void = () => {};
  private onValidatorChange: () => void = () => {};

  readonly anneeChoisie = computed(() => (this.valeur() ? Number(this.valeur().slice(0, 4)) : null));

  readonly titre = computed(() => {
    if (this.vue() === 'jours') return `${MOIS[this.moisVue()]} ${this.anneeVue()}`;
    if (this.vue() === 'mois') return String(this.anneeVue());
    const debut = this.debutDecennie();
    return `${debut} – ${debut + 11}`;
  });

  /** 6 semaines complètes, du lundi au dimanche. */
  readonly jours = computed<Jour[]>(() => {
    const annee = this.anneeVue();
    const mois = this.moisVue();
    const decalage = (new Date(annee, mois, 1).getDay() + 6) % 7;
    return Array.from({ length: 42 }, (_, i) => {
      const date = new Date(annee, mois, 1 - decalage + i);
      const iso = versIso(date);
      return { iso, numero: date.getDate(), horsMois: date.getMonth() !== mois, desactive: this.horsBornes(iso) };
    });
  });

  readonly annees = computed(() => Array.from({ length: 12 }, (_, i) => this.debutDecennie() + i));

  constructor(private readonly host: ElementRef<HTMLElement>) {}

  // --- Ouverture / fermeture ---

  ouvrir(parClavier = false): void {
    if (this.disabled() || this.open()) return;
    const reference = this.valeur() || this.dansBornes(this.aujourdhui);
    this.afficherMois(reference);
    this.actif.set(reference);
    this.vue.set('jours');
    this.focusPanneau = parClavier;
    this.open.set(true);
  }

  basculer(): void {
    if (this.open()) this.fermer();
    else {
      this.ouvrir();
      this.champ?.nativeElement.focus();
    }
  }

  fermer(focusChamp = true): void {
    this.open.set(false);
    if (focusChamp) this.champ?.nativeElement.focus();
  }

  @HostListener('document:mousedown', ['$event'])
  onDocumentMousedown(event: MouseEvent): void {
    if (this.open() && !this.host.nativeElement.contains(event.target as Node)) {
      this.fermer(false);
      this.onTouched();
    }
  }

  // --- Saisie au clavier dans le champ ---

  onSaisie(event: Event): void {
    const champ = event.target as HTMLInputElement;
    const chiffres = champ.value.replace(/\D/g, '').slice(0, 8);
    let texte = chiffres;
    if (chiffres.length > 4) texte = `${chiffres.slice(0, 2)}/${chiffres.slice(2, 4)}/${chiffres.slice(4)}`;
    else if (chiffres.length > 2) texte = `${chiffres.slice(0, 2)}/${chiffres.slice(2)}`;
    champ.value = texte;
    this.texte.set(texte);

    if (!texte) {
      this.saisieInvalide.set(false);
      this.mettreAJour('');
    } else if (chiffres.length === 8) {
      const iso = depuisTexte(texte);
      this.saisieInvalide.set(!iso);
      if (iso) {
        this.mettreAJour(iso);
        this.afficherMois(iso);
        this.actif.set(iso);
      }
    }
  }

  onBlur(): void {
    const texte = this.texte();
    if (texte && !depuisTexte(texte)) this.saisieInvalide.set(true);
    if (!this.open()) this.onTouched();
    this.onValidatorChange();
  }

  onChampKeydown(event: KeyboardEvent): void {
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      if (this.open()) this.focusPanneauMaintenant();
      else this.ouvrir(true);
    } else if (event.key === 'Escape' && this.open()) {
      event.preventDefault();
      this.fermer();
    } else if (event.key === 'Tab' && this.open()) {
      this.fermer(false);
    }
  }

  // --- Navigation dans le calendrier ---

  onPanneauKeydown(event: KeyboardEvent): void {
    if (this.vue() !== 'jours') {
      if (event.key === 'Escape') { event.preventDefault(); this.fermer(); }
      return;
    }
    const deplacements: Record<string, number> = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -7, ArrowDown: 7 };
    if (event.key in deplacements) {
      event.preventDefault();
      this.deplacerActif(deplacements[event.key]);
    } else if (event.key === 'PageUp' || event.key === 'PageDown') {
      event.preventDefault();
      const date = depuisIso(this.actif());
      date.setMonth(date.getMonth() + (event.key === 'PageUp' ? -1 : 1));
      this.actif.set(versIso(date));
      this.afficherMois(this.actif());
    } else if (event.key === 'Home' || event.key === 'End') {
      event.preventDefault();
      const date = depuisIso(this.actif());
      date.setDate(date.getDate() + (event.key === 'Home' ? -((date.getDay() + 6) % 7) : 6 - ((date.getDay() + 6) % 7)));
      this.actif.set(versIso(date));
      this.afficherMois(this.actif());
    } else if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      if (!this.horsBornes(this.actif())) this.choisir(this.actif());
    } else if (event.key === 'Escape') {
      event.preventDefault();
      this.fermer();
    } else if (event.key === 'Tab') {
      this.fermer(false);
    }
  }

  precedent(): void {
    if (this.vue() === 'jours') this.decalerMois(-1);
    else this.anneeVue.update((a) => a - (this.vue() === 'mois' ? 1 : 12));
  }

  suivant(): void {
    if (this.vue() === 'jours') this.decalerMois(1);
    else this.anneeVue.update((a) => a + (this.vue() === 'mois' ? 1 : 12));
  }

  changerVue(): void {
    this.vue.update((v) => (v === 'jours' ? 'mois' : v === 'mois' ? 'annees' : 'jours'));
  }

  choisirMois(mois: number): void {
    this.moisVue.set(mois);
    this.vue.set('jours');
  }

  choisirAnnee(annee: number): void {
    this.anneeVue.set(annee);
    this.vue.set('mois');
  }

  choisir(iso: string): void {
    if (this.horsBornes(iso)) return;
    this.saisieInvalide.set(false);
    this.texte.set(versTexte(iso));
    this.mettreAJour(iso);
    this.fermer();
    this.onTouched();
  }

  choisirAujourdhui(): void {
    this.choisir(this.aujourdhui);
  }

  effacer(): void {
    this.saisieInvalide.set(false);
    this.texte.set('');
    this.mettreAJour('');
    this.fermer();
    this.onTouched();
  }

  horsBornes(iso: string): boolean {
    const min = this.borneMin();
    const max = this.borneMax();
    return (!!min && iso < min) || (!!max && iso > max);
  }

  // --- ControlValueAccessor / Validator ---

  writeValue(valeur: unknown): void {
    const iso = typeof valeur === 'string' && /^\d{4}-\d{2}-\d{2}/.test(valeur) ? valeur.slice(0, 10) : '';
    this.valeur.set(iso);
    this.texte.set(iso ? versTexte(iso) : '');
    this.saisieInvalide.set(false);
  }

  registerOnChange(fn: (valeur: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(estDesactive: boolean): void {
    this.disabled.set(estDesactive);
    if (estDesactive) this.open.set(false);
  }

  validate(_: AbstractControl): ValidationErrors | null {
    if (this.saisieInvalide() || (this.texte() && !this.valeur())) return { dateInvalide: true };
    const valeur = this.valeur();
    if (valeur && this.borneMin() && valeur < this.borneMin()!) return { dateMin: { min: this.borneMin() } };
    if (valeur && this.borneMax() && valeur > this.borneMax()!) return { dateMax: { max: this.borneMax() } };
    return null;
  }

  registerOnValidatorChange(fn: () => void): void {
    this.onValidatorChange = fn;
  }

  // --- Outils ---

  private mettreAJour(iso: string): void {
    if (iso !== this.valeur()) {
      this.valeur.set(iso);
      this.onChange(iso);
    }
    this.onValidatorChange();
  }

  private afficherMois(iso: string): void {
    const date = depuisIso(iso);
    this.anneeVue.set(date.getFullYear());
    this.moisVue.set(date.getMonth());
  }

  private decalerMois(pas: number): void {
    const date = new Date(this.anneeVue(), this.moisVue() + pas, 1);
    this.anneeVue.set(date.getFullYear());
    this.moisVue.set(date.getMonth());
  }

  private deplacerActif(jours: number): void {
    const date = depuisIso(this.actif());
    date.setDate(date.getDate() + jours);
    this.actif.set(versIso(date));
    this.afficherMois(this.actif());
  }

  private focusPanneauMaintenant(): void {
    this.host.nativeElement.querySelector<HTMLElement>('.date-picker-panneau')?.focus();
  }

  private dansBornes(iso: string): string {
    if (this.borneMin() && iso < this.borneMin()!) return this.borneMin()!;
    if (this.borneMax() && iso > this.borneMax()!) return this.borneMax()!;
    return iso;
  }

  private debutDecennie(): number {
    return this.anneeVue() - (((this.anneeVue() % 12) + 12) % 12);
  }
}

function versIso(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function depuisIso(iso: string): Date {
  const [annee, mois, jour] = iso.split('-').map(Number);
  return new Date(annee, mois - 1, jour);
}

function versTexte(iso: string): string {
  const [annee, mois, jour] = iso.split('-');
  return `${jour}/${mois}/${annee}`;
}

/** « JJ/MM/AAAA » → ISO, ou null si la date n'existe pas (ex. 31/02/2026). */
function depuisTexte(texte: string): string | null {
  const morceaux = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(texte);
  if (!morceaux) return null;
  const [, jour, mois, annee] = morceaux.map(Number);
  const date = new Date(annee, mois - 1, jour);
  if (annee < 1900 || date.getFullYear() !== annee || date.getMonth() !== mois - 1 || date.getDate() !== jour) return null;
  return versIso(date);
}
