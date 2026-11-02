import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="min-h-screen bg-slate-50 flex flex-col justify-center items-center px-4 relative overflow-hidden">
      <div class="w-full max-w-md z-10">
        <!-- Logo & Header -->
        <div class="text-center mb-8">
          <div class="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-green-700 shadow-sm shadow-blue-500/30 mb-4 border border-blue-400/30">
            <svg class="w-8 h-8 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
          </div>
          <h1 class="text-2xl font-bold text-white tracking-tight">Système Budgétaire</h1>
          <p class="text-slate-600 text-sm mt-1">Module 1 : Sécurité & Gouvernance des Accès</p>
        </div>

        <!-- Login Card -->
        <div class="bg-white/80 backdrop-blur-xl border border-slate-200 rounded-2xl p-8 shadow-2xl shadow-black/50">
          <h2 class="text-xl font-semibold text-white mb-6">Connexion</h2>

          @if (errorMessage()) {
            <div class="mb-5 p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-sm flex items-start gap-2.5">
              <svg class="w-5 h-5 text-rose-400 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span>{{ errorMessage() }}</span>
            </div>
          }

          <form (ngSubmit)="onSubmit()" class="space-y-5">
            <div>
              <label class="block text-xs font-medium text-slate-700 mb-1.5 uppercase tracking-wider">Nom d'utilisateur</label>
              <div class="relative">
                <div class="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                  <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M16 12a4 4 0 10-8 0 4 4 0 008 0zm0 0v1.5a2.5 2.5 0 005 0V12a9 9 0 10-9 9m4.5-1.206a8.959 8.959 0 01-4.5 1.207" />
                  </svg>
                </div>
                <input
                  type="text"
                  name="username"
                  required
                  [(ngModel)]="username"
                  placeholder="admin@uab.bf"
                  class="w-full pl-10 pr-4 py-2.5 bg-slate-50/60 border border-slate-300/80 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm transition-all"
                />
              </div>
            </div>

            <div>
              <label class="block text-xs font-medium text-slate-700 mb-1.5 uppercase tracking-wider">Mot de Passe</label>
              <div class="relative">
                <div class="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                  <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                  </svg>
                </div>
                <input
                  [type]="showPassword ? 'text' : 'password'"
                  name="password"
                  required
                  [(ngModel)]="password"
                  placeholder="••••••••••••"
                  class="w-full pl-10 pr-10 py-2.5 bg-slate-50/60 border border-slate-300/80 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm transition-all"
                />
                <button
                  type="button"
                  (click)="showPassword = !showPassword"
                  class="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-500 hover:text-slate-700 transition-colors"
                >
                  <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    @if (showPassword) {
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l18 18" />
                    } @else {
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    }
                  </svg>
                </button>
              </div>
            </div>

            <button
              type="submit"
              [disabled]="loading()"
              class="w-full py-3 px-4 bg-green-700     text-white font-medium rounded-xl shadow-lg shadow-blue-600/30 transition-all duration-200 flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              @if (loading()) {
                <svg class="animate-spin h-5 w-5 text-white" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <span>Authentification en cours...</span>
              } @else {
                <span>Se Connecter</span>
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
                </svg>
              }
            </button>
          </form>

          <!-- Quick Test Helper Button -->
          <div class="mt-6 pt-5 border-t border-slate-200 text-center">
            <button
              type="button"
              (click)="fillAdminCredentials()"
              class="text-xs text-blue-400 hover:text-blue-300 transition-colors inline-flex items-center gap-1.5"
            >
              <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
              Remplir avec le compte par défaut (Admin UAB)
            </button>
          </div>
        </div>

        <p class="text-center text-xs text-slate-600 mt-6">
          Protection par JWT & Contrôle d'accès granulaire RBAC
        </p>
      </div>
    </div>
  `
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notify = inject(NotificationService);

  username = 'admin@uab.bf';
  password = 'Password123!';
  showPassword = false;
  loading = signal(false);
  errorMessage = signal<string | null>(null);

  fillAdminCredentials() {
    this.username = 'admin@uab.bf';
    this.password = 'Password123!';
  }

  onSubmit() {
    if (!this.username || !this.password) {
      this.errorMessage.set('Veuillez renseigner votre nom d’utilisateur et mot de passe.');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    this.authService.login({ username: this.username, password: this.password }).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.notify.success(`Bienvenue, ${res.prenom} ${res.nom} !`);
        this.router.navigate(['/security/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        if (err.status === 401 || err.status === 403) {
          this.errorMessage.set('Identifiants incorrects. Veuillez vérifier votre nom d’utilisateur et votre mot de passe.');
        } else {
          this.errorMessage.set(err.error?.message || 'Impossible de se connecter au serveur backend.');
        }
      }
    });
  }
}
