import { Component, Directive, booleanAttribute, ElementRef, HostListener, Input, ViewChild, computed, contentChildren, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

export interface SearchSelectOption {
  value: unknown;
  label: string;
}

/** Option d'une <app-search-select>, déclarée comme un <option> : <app-search-option [value]="x.id" [label]="..." />. */
@Directive({ selector: 'app-search-option', standalone: true })
export class SearchOptionDirective {
  readonly value = input<unknown>();
  readonly label = input('');
}

const DEFAULT_TRIGGER_CLASS =
  'w-full rounded-xl border border-slate-300 bg-slate-50 px-3 py-2.5 text-sm text-slate-900 focus:outline-none focus:ring-1 focus:ring-blue-500';

/**
 * Liste de sélection avec recherche par saisie, utilisable comme un <select> avec [(ngModel)]
 * (et les validateurs de formulaire, dont required). La recherche ignore la casse et les accents.
 * Les options sont déclarées dans le contenu avec <app-search-option>.
 */
@Component({
  selector: 'app-search-select',
  standalone: true,
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => SearchSelectComponent), multi: true }],
  template: `
    <div class="relative font-normal">
      <button #trigger type="button" [disabled]="disabled()" (click)="toggle()" (keydown)="onTriggerKeydown($event)"
        [attr.aria-expanded]="open()" aria-haspopup="listbox"
        [class]="triggerClasses() + ' flex items-center justify-between gap-2 text-left disabled:cursor-not-allowed disabled:opacity-60'">
        <span class="truncate" [class.text-slate-400]="!hasSelection()">{{ displayLabel() }}</span>
        <svg aria-hidden="true" class="h-4 w-4 shrink-0 text-slate-400" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.06l3.71-3.83a.75.75 0 111.08 1.04l-4.25 4.39a.75.75 0 01-1.08 0L5.21 8.27a.75.75 0 01.02-1.06z" clip-rule="evenodd" />
        </svg>
      </button>
      @if (open()) {
        <!-- preventDefault : dans un <label>, un clic sur la liste ne doit pas réactiver le bouton. -->
        <div class="absolute left-0 right-0 z-50 mt-1 rounded-xl border border-slate-200 bg-white p-2 shadow-lg" (click)="$event.preventDefault()">
          <input #searchInput type="text" [value]="term()" (input)="onSearch($event)" (keydown)="onSearchKeydown($event)"
            placeholder="Rechercher…" aria-label="Rechercher dans la liste"
            class="mb-2 w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2 text-xs text-slate-900 focus:outline-none focus:ring-1 focus:ring-blue-500">
          <ul role="listbox" class="max-h-60 overflow-y-auto text-xs">
            @if (emptyLabel !== null) {
              <li role="option" [attr.aria-selected]="isSelected(emptyValue)" (mousedown)="$event.preventDefault()" (click)="choose(emptyValue)"
                class="cursor-pointer rounded-lg px-3 py-2 italic text-slate-500 hover:bg-slate-100">{{ emptyLabel }}</li>
            }
            @for (option of filtered(); track $index; let i = $index) {
              <li role="option" [attr.aria-selected]="isSelected(option.value)" (mousedown)="$event.preventDefault()" (click)="choose(option.value)"
                (mouseenter)="active.set(i)"
                class="flex cursor-pointer items-center gap-2 rounded-lg px-3 py-2 text-slate-900"
                [class.bg-blue-50]="active() === i" [class.font-semibold]="isSelected(option.value)">
                @if (multiple) {
                  <input type="checkbox" tabindex="-1" aria-hidden="true" [checked]="isSelected(option.value)" class="pointer-events-none h-4 w-4 shrink-0">
                }
                <span>{{ option.label }}</span>
              </li>
            } @empty {
              <li class="px-3 py-2 text-slate-500">Aucun résultat.</li>
            }
          </ul>
        </div>
      }
    </div>
  `,
})
export class SearchSelectComponent implements ControlValueAccessor {
  /** Texte affiché quand aucune valeur n'est choisie. */
  @Input() placeholder = 'Sélectionner…';
  /** Option « vide » proposée en tête de liste (ex. « Aucun manager ») ; null pour ne pas la proposer. */
  @Input() emptyLabel: string | null = null;
  /** Valeur correspondant à « aucune sélection » ('' ou null selon le formulaire). */
  @Input() emptyValue: unknown = '';
  /** Choix multiples : la valeur est un tableau et la liste reste ouverte après chaque choix. */
  @Input({ transform: booleanAttribute }) multiple = false;
  /** Classes du champ fermé (reprend celles des <select> de l'écran). */
  @Input() triggerClass = DEFAULT_TRIGGER_CLASS;
  @Input() set isDisabled(value: boolean) {
    this.disabled.set(!!value);
  }

  @ViewChild('trigger') private trigger?: ElementRef<HTMLButtonElement>;
  @ViewChild('searchInput') private set searchInput(ref: ElementRef<HTMLInputElement> | undefined) {
    ref?.nativeElement.focus();
  }

  private readonly optionItems = contentChildren(SearchOptionDirective, { descendants: true });
  readonly allOptions = computed<SearchSelectOption[]>(() =>
    this.optionItems().map((item) => ({ value: item.value(), label: item.label() })),
  );
  readonly value = signal<unknown>('');
  readonly open = signal(false);
  readonly term = signal('');
  readonly active = signal(0);
  readonly disabled = signal(false);

  private onChange: (value: unknown) => void = () => {};
  private onTouched: () => void = () => {};

  constructor(private readonly host: ElementRef<HTMLElement>) {}

  triggerClasses(): string {
    // Les boutons « text-white » restent blancs dans le thème clair : on force un texte lisible.
    return this.triggerClass.replace(/\btext-white\b/g, 'text-slate-900');
  }

  selectedOptions(): SearchSelectOption[] {
    return this.allOptions().filter((option) => this.isSelected(option.value));
  }

  hasSelection(): boolean {
    return this.selectedOptions().length > 0;
  }

  displayLabel(): string {
    const choisies = this.selectedOptions();
    if (!choisies.length) return this.placeholder;
    if (!this.multiple || choisies.length === 1) return choisies[0].label;
    return `${choisies.length} sélectionnés : ${choisies.map((option) => option.label).join(', ')}`;
  }

  filtered(): SearchSelectOption[] {
    const terme = normaliser(this.term().trim());
    if (!terme) return this.allOptions();
    return this.allOptions().filter((option) => normaliser(option.label).includes(terme));
  }

  isSelected(value: unknown): boolean {
    if (this.multiple) return this.values().some((choisie) => this.same(choisie, value));
    return this.same(value, this.value());
  }

  toggle(): void {
    if (this.open()) this.close();
    else this.openPanel();
  }

  onSearch(event: Event): void {
    this.term.set((event.target as HTMLInputElement).value);
    this.active.set(0);
  }

  onTriggerKeydown(event: KeyboardEvent): void {
    if (!this.open() && (event.key === 'ArrowDown' || event.key === 'Enter' || event.key === ' ')) {
      event.preventDefault();
      this.openPanel();
    } else if (!this.open() && event.key.length === 1 && !event.ctrlKey && !event.metaKey && !event.altKey) {
      // Une saisie sur le champ fermé ouvre la liste et démarre la recherche.
      event.preventDefault();
      this.openPanel(event.key);
    }
  }

  onSearchKeydown(event: KeyboardEvent): void {
    const options = this.filtered();
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.active.set(Math.min(this.active() + 1, options.length - 1));
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.active.set(Math.max(this.active() - 1, 0));
    } else if (event.key === 'Enter') {
      event.preventDefault();
      const option = options[this.active()];
      if (option) this.choose(option.value);
    } else if (event.key === 'Escape') {
      event.preventDefault();
      this.close();
    } else if (event.key === 'Tab') {
      this.close(false);
    }
  }

  choose(value: unknown): void {
    if (this.multiple) {
      const valeurs = this.isSelected(value)
        ? this.values().filter((choisie) => !this.same(choisie, value))
        : [...this.values(), value];
      this.value.set(valeurs);
      this.onChange(valeurs);
      return;
    }
    if (!this.same(value, this.value())) {
      this.value.set(value);
      this.onChange(value);
    }
    this.close();
  }

  @HostListener('document:mousedown', ['$event'])
  onDocumentMousedown(event: MouseEvent): void {
    if (this.open() && !this.host.nativeElement.contains(event.target as Node)) this.close(false);
  }

  writeValue(value: unknown): void {
    this.value.set(this.multiple ? (Array.isArray(value) ? value : []) : (value ?? this.emptyValue));
  }

  registerOnChange(fn: (value: unknown) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
  }

  private openPanel(initialTerm = ''): void {
    this.term.set(initialTerm);
    const index = this.multiple ? -1 : this.allOptions().findIndex((option) => this.same(option.value, this.value()));
    this.active.set(initialTerm || index < 0 ? 0 : index);
    this.open.set(true);
  }

  private values(): unknown[] {
    const valeur = this.value();
    return Array.isArray(valeur) ? valeur : [];
  }

  private close(refocus = true): void {
    this.open.set(false);
    this.onTouched();
    if (refocus) this.trigger?.nativeElement.focus();
  }

  /** Les <option [value]> des formulaires renvoient des chaînes : on compare donc aussi en texte. */
  private same(a: unknown, b: unknown): boolean {
    if (a === b) return true;
    if (a === null || a === undefined || b === null || b === undefined) return false;
    return String(a) === String(b);
  }
}

function normaliser(texte: string): string {
  return texte.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
}
