import { Injectable, signal } from '@angular/core';

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'info' | 'warning';
  title?: string;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private readonly _toasts = signal<ToastMessage[]>([]);
  public readonly toasts = this._toasts.asReadonly();

  show(type: 'success' | 'error' | 'info' | 'warning', message: string, title?: string, durationMs: number = 4000) {
    const id = Math.random().toString(36).substring(2, 9);
    const toast: ToastMessage = { id, type, title, message };
    this._toasts.update(current => [...current, toast]);

    setTimeout(() => {
      this.dismiss(id);
    }, durationMs);
  }

  success(message: string, title: string = 'Succès') {
    this.show('success', message, title);
  }

  error(message: string, title: string = 'Erreur') {
    this.show('error', message, title, 6000);
  }

  info(message: string, title: string = 'Information') {
    this.show('info', message, title);
  }

  warning(message: string, title: string = 'Attention') {
    this.show('warning', message, title);
  }

  dismiss(id: string) {
    this._toasts.update(current => current.filter(t => t.id !== id));
  }
}
