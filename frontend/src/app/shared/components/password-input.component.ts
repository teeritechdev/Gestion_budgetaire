import { Component, Input, forwardRef, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

/** Champ mot de passe avec bouton afficher / masquer, utilisable avec [(ngModel)]. */
@Component({
  selector: 'app-password-input',
  standalone: true,
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => PasswordInputComponent), multi: true }],
  template: `
    <div class="password-input relative">
      <input [type]="visible() ? 'text' : 'password'" [value]="valeur()" [attr.autocomplete]="autocomplete"
        [attr.id]="inputId || null" [placeholder]="placeholder" [disabled]="disabled()" spellcheck="false"
        (input)="onSaisie($event)" (blur)="onTouched()"
        class="w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 pr-11 text-sm text-slate-900">
      <button type="button" (click)="visible.set(!visible())" [disabled]="disabled()"
        [attr.aria-label]="visible() ? 'Masquer le mot de passe' : 'Afficher le mot de passe'" [attr.aria-pressed]="visible()"
        class="absolute right-1.5 top-1/2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-md text-slate-500 hover:bg-slate-100 hover:text-slate-800">
        <svg aria-hidden="true" class="h-[18px] w-[18px]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          @if (visible()) {
            <path d="M3 3l18 18M10.6 10.6a2 2 0 002.8 2.8M9.9 5.1A9.8 9.8 0 0112 5c5 0 8.5 4.5 9.5 7-.4 1-1.3 2.5-2.6 3.8M6.2 6.2C4.3 7.5 3 9.5 2.5 12c1 2.5 4.5 7 9.5 7 1.7 0 3.2-.5 4.5-1.2" />
          } @else {
            <path d="M2.5 12S6 5 12 5s9.5 7 9.5 7-3.5 7-9.5 7-9.5-7-9.5-7z" /><circle cx="12" cy="12" r="3" />
          }
        </svg>
      </button>
    </div>
  `,
})
export class PasswordInputComponent implements ControlValueAccessor {
  @Input() placeholder = '';
  /** « current-password » ou « new-password » : aide les gestionnaires de mots de passe. */
  @Input() autocomplete = 'current-password';
  @Input() inputId = '';

  readonly valeur = signal('');
  readonly visible = signal(false);
  readonly disabled = signal(false);
  private onChange: (valeur: string) => void = () => {};
  onTouched: () => void = () => {};

  onSaisie(event: Event): void {
    const valeur = (event.target as HTMLInputElement).value;
    this.valeur.set(valeur);
    this.onChange(valeur);
  }

  writeValue(valeur: unknown): void {
    this.valeur.set(typeof valeur === 'string' ? valeur : '');
  }

  registerOnChange(fn: (valeur: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(estDesactive: boolean): void {
    this.disabled.set(estDesactive);
  }
}
