import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="fixed top-5 right-5 z-50 flex flex-col gap-3 max-w-sm w-full pointer-events-none">
      @for (toast of notify.toasts(); track toast.id) {
        <div
          class="pointer-events-auto transform transition-all duration-300 ease-out shadow-xl rounded-xl p-4 flex items-start gap-3 border backdrop-blur-md"
          [ngClass]="{
            'bg-blue-50 text-blue-900 border-blue-200': toast.type === 'success',
            'bg-rose-50 text-rose-900 border-rose-200': toast.type === 'error',
            'bg-amber-50 text-amber-900 border-amber-200': toast.type === 'warning',
            'bg-white text-slate-900 border-blue-200': toast.type === 'info'
          }">
          <div class="mt-0.5 shrink-0">
            @if (toast.type === 'success') {
              <svg class="w-5 h-5 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              </svg>
            } @else if (toast.type === 'error') {
              <svg class="w-5 h-5 text-rose-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            } @else if (toast.type === 'warning') {
              <svg class="w-5 h-5 text-amber-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            } @else {
              <svg class="w-5 h-5 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            }
          </div>
          <div class="flex-1 text-sm">
            @if (toast.title) {
              <p class="font-semibold text-white">{{ toast.title }}</p>
            }
            <p class="text-xs opacity-90 leading-relaxed">{{ toast.message }}</p>
          </div>
          <button (click)="notify.dismiss(toast.id)" class="text-gray-400 hover:text-white transition-colors">
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      }
    </div>
  `
})
export class ToastContainerComponent {
  readonly notify = inject(NotificationService);
}
