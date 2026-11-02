import { Directive, ElementRef, HostListener, inject } from '@angular/core';

@Directive({
  selector: 'input[type="date"]',
  standalone: true,
})
export class NativeDatePickerDirective {
  private readonly input = inject(ElementRef<HTMLInputElement>).nativeElement;

  @HostListener('pointerdown', ['$event'])
  openPicker(event: PointerEvent): void {
    if (this.input.disabled || typeof this.input.showPicker !== 'function') {
      return;
    }

    this.input.focus();
    try {
      this.input.showPicker();
      event.preventDefault();
    } catch {
      this.input.focus();
    }
  }
}
