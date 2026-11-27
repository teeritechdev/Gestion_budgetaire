import { AppIconComponent } from '../../shared/components/app-icon.component';
import { AvatarComponent } from '../../shared/components/avatar.component';
import { ProfilService } from '../../core/services/profil.service';
import { Component, DestroyRef, HostListener, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../core/services/auth.service';
import { FEATURE_ACCESS, FeaturePath } from '../../core/guards/feature-access';

/** Sous-onglet : même page, distingué par le paramètre « onglet » de l'adresse. */
interface NavigationChild { label: string; code: string; onglet: string; permission: string; }
interface NavigationItem { label: string; path: FeaturePath; icon: string; children?: NavigationChild[]; }
interface NavigationGroup { label: string; items: NavigationItem[]; }

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [AppIconComponent, AvatarComponent, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <!-- Seule la zone <main> défile : en-tête et menu restent fixes, sans décalage. -->
    <div class="h-dvh overflow-hidden bg-slate-50 text-slate-900">
      @if (mobileOpen()) {
        <button aria-label="Fermer le menu" class="fixed inset-0 z-30 bg-black/60 md:hidden" (click)="mobileOpen.set(false)"></button>
      }
      <aside [class.translate-x-0]="mobileOpen()" [class.-translate-x-full]="!mobileOpen()" class="fixed inset-y-0 left-0 z-40 flex w-72 flex-col border-r border-green-100 bg-green-50 transition-transform md:translate-x-0">
        <div class="sidebar-brand flex h-16 items-center gap-3 border-b border-green-800 px-5">
          <div class="brand-mark flex h-10 w-10 items-center justify-center rounded-xl text-lg font-bold">U</div>
          <div><p class="font-bold text-white">UAB Assurances</p><p class="text-xs text-blue-400">Gestion intégrée</p></div>
          <button aria-label="Fermer le menu" (click)="mobileOpen.set(false)" class="ml-auto text-white md:hidden">✕</button>
        </div>
        <nav class="flex-1 space-y-6 overflow-y-auto px-3 py-5">
          @if (canAccessDashboard()) {
          <div>
            <p class="mb-2 px-3 text-[10px] font-semibold uppercase tracking-[.16em] text-slate-500">Pilotage</p>
            <a routerLink="/dashboard" routerLinkActive="bg-green-700 text-white font-semibold" [routerLinkActiveOptions]="{ exact: true }" (click)="mobileOpen.set(false)" class="flex items-center gap-3 rounded-xl whitespace-nowrap px-3 py-2.5 text-[15px] font-bold text-slate-700 transition hover:bg-green-700 hover:text-white"><span class="sidebar-nav-icon flex w-6 justify-center text-green-700"><app-icon name="dashboard" /></span>Tableau de bord</a>
          </div>
          }
          @for (group of visibleGroups(); track group.label) {
            <div>
              <p class="mb-2 px-3 text-[10px] font-semibold uppercase tracking-[.16em] text-slate-500">{{ group.label }}</p>
              <div class="space-y-1">
                @for (item of group.items; track item.path) {
                  @if (item.children?.length) {
                    <a [routerLink]="item.path" [queryParams]="{ onglet: item.children![0].onglet }" routerLinkActive="bg-green-700 text-white font-semibold" [routerLinkActiveOptions]="{ paths: 'subset', queryParams: 'ignored', matrixParams: 'ignored', fragment: 'ignored' }" [attr.aria-expanded]="isOpen(item)" (click)="mobileOpen.set(false)" class="flex items-center gap-3 rounded-xl whitespace-nowrap px-3 py-2.5 text-[15px] font-bold text-slate-700 transition hover:bg-green-700 hover:text-white"><span class="sidebar-nav-icon flex w-6 justify-center text-green-700"><app-icon [name]="item.icon" /></span><span class="flex-1">{{ item.label }}</span><span class="transition-transform" [class.rotate-90]="isOpen(item)"><app-icon name="chevron" classe="h-4 w-4" /></span></a>
                    @if (isOpen(item)) {
                      <div class="sidebar-subnav ml-6 mt-1 space-y-1 border-l-2 border-green-200 pl-3" role="group" [attr.aria-label]="'Sous-onglets ' + item.label">
                        @for (child of item.children; track child.onglet) {
                          <a [routerLink]="item.path" [queryParams]="{ onglet: child.onglet }" routerLinkActive="sidebar-subnav-active" [routerLinkActiveOptions]="{ paths: 'exact', queryParams: 'exact', matrixParams: 'ignored', fragment: 'ignored' }" (click)="mobileOpen.set(false)" class="block rounded-lg whitespace-nowrap px-3 py-2 text-sm font-semibold text-slate-600 transition hover:bg-green-100 hover:text-green-900">{{ child.label }}</a>
                        }
                      </div>
                    }
                  } @else {
                    <a [routerLink]="item.path" routerLinkActive="bg-green-700 text-white font-semibold" (click)="mobileOpen.set(false)" class="flex items-center gap-3 rounded-xl whitespace-nowrap px-3 py-2.5 text-[15px] font-bold text-slate-700 transition hover:bg-green-700 hover:text-white"><span class="sidebar-nav-icon flex w-6 justify-center text-green-700"><app-icon [name]="item.icon" /></span>{{ item.label }}</a>
                  }
                }
              </div>
            </div>
          }
        </nav>
        <div class="border-t border-slate-200 p-4">
          <a routerLink="/profil" (click)="mobileOpen.set(false)" class="sidebar-utilisateur flex items-center gap-3 rounded-xl bg-slate-100 p-3 hover:bg-slate-200/70" title="Mon profil">
            <app-avatar [photoUrl]="profilService.photoUrl()" taille="sm" />
            <div class="min-w-0 flex-1"><p class="truncate text-xs font-medium text-slate-900">{{ auth.currentUser()?.prenom }} {{ auth.currentUser()?.nom }}</p><p class="truncate text-[10px] text-slate-600">{{ auth.role() }}</p></div>
            <svg aria-hidden="true" class="h-4 w-4 shrink-0 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 6l6 6-6 6" /></svg>
          </a>
        </div>
      </aside>
      <div class="flex h-dvh min-w-0 flex-col md:pl-72">
        <header class="app-header z-20 flex h-16 shrink-0 items-center justify-between border-b-2 border-green-800 bg-green-700 px-4 md:px-8">
          <div class="flex items-center gap-3"><button aria-label="Ouvrir le menu" (click)="mobileOpen.set(true)" class="rounded-lg border border-slate-300 px-2.5 py-1.5 text-slate-700 md:hidden">☰</button><span class="text-green-700"><app-icon name="brand" classe="h-5 w-5" /></span><span class="text-sm font-semibold text-slate-800">Plateforme de Gestion Budgétaire</span></div>
          <div class="menu-utilisateur relative">
            <button type="button" (click)="menuOuvert.set(!menuOuvert())" aria-haspopup="menu" [attr.aria-expanded]="menuOuvert()"
              [attr.aria-label]="'Compte de ' + (auth.currentUser()?.prenom || '') + ' ' + (auth.currentUser()?.nom || '')"
              [title]="(auth.currentUser()?.prenom || '') + ' ' + (auth.currentUser()?.nom || '')"
              class="menu-utilisateur-bouton flex items-center justify-center rounded-full">
              <app-avatar [photoUrl]="profilService.photoUrl()" taille="sm" />
            </button>
            @if (menuOuvert()) {
              <div role="menu" class="menu-utilisateur-panneau absolute right-0 z-50 mt-2 w-72 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-lg" (keydown.escape)="menuOuvert.set(false)">
                <div class="flex items-center gap-3 border-b border-slate-200 px-4 py-3">
                  <app-avatar [photoUrl]="profilService.photoUrl()" taille="md" />
                  <div class="min-w-0">
                    <p class="truncate text-sm font-semibold text-slate-900">{{ auth.currentUser()?.prenom }} {{ auth.currentUser()?.nom }}</p>
                    <p class="truncate text-xs text-slate-500">{{ auth.currentUser()?.email }}</p>
                  </div>
                </div>
                <div class="py-1">
                  <a role="menuitem" routerLink="/profil" [queryParams]="{ onglet: 'informations' }" (click)="menuOuvert.set(false)" class="menu-utilisateur-item">
                    <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="8" r="4" /><path d="M4.5 20a7.5 7.5 0 0115 0" /></svg>Mon profil
                  </a>
                  <a role="menuitem" routerLink="/profil" [queryParams]="{ onglet: 'securite' }" (click)="menuOuvert.set(false)" class="menu-utilisateur-item">
                    <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="5" y="10.5" width="14" height="9.5" rx="2" /><path d="M8 10.5V8a4 4 0 018 0v2.5" /></svg>Sécurité et mot de passe
                  </a>
                </div>
                <div class="border-t border-slate-200 py-1">
                  <button type="button" role="menuitem" (click)="logout()" class="menu-utilisateur-item menu-utilisateur-deconnexion">
                    <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M15 4h3a2 2 0 012 2v12a2 2 0 01-2 2h-3M10 16l-4-4 4-4M6 12h10" /></svg>Se déconnecter
                  </button>
                </div>
              </div>
            }
          </div>
        </header>
        <main id="app-main" class="app-main min-w-0 flex-1 overflow-auto p-4 md:p-8"><router-outlet /></main>
      </div>
    </div>`
})
export class AppLayoutComponent {
  readonly auth = inject(AuthService);
  readonly mobileOpen = signal(false);
  readonly menuOuvert = signal(false);
  readonly profilService = inject(ProfilService);
  private readonly router = inject(Router);
  /** Adresse courante (signal) : déplie les sous-onglets de la page active. */
  private readonly currentUrl = signal(this.router.url);

  constructor() {
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd), takeUntilDestroyed(inject(DestroyRef)))
      .subscribe(event => this.currentUrl.set(event.urlAfterRedirects));
  }

  isOpen(item: NavigationItem): boolean {
    const chemin = this.currentUrl().split('?')[0];
    return chemin === item.path || chemin.startsWith(item.path + '/');
  }
  readonly groups: NavigationGroup[] = [
    { label: 'Gestion budgétaire', items: [
      { label: 'Nomenclature', path: '/budget/nomenclature', icon: 'nomenclature', children: [
        { label: 'Objectifs stratégiques', code: 'OS', onglet: 'os', permission: 'os.lire' },
        { label: 'Objectifs opérationnels', code: 'OP', onglet: 'op', permission: 'op.lire' },
        { label: 'Activités catalogue', code: 'AC', onglet: 'ac', permission: 'activite.lire' }
      ] },
      { label: 'Plans budgétaires', path: '/budget/plans', icon: 'plans' },
      { label: 'Exercices budgétaires', path: '/budget/exercices', icon: 'exercices' },
      { label: 'Allocations budgétaires', path: '/budget/allocations', icon: 'allocations' }
    ] },
    { label: 'Répartition & contrôle', items: [
      { label: 'Règles de répartition CSP', path: '/budget/repartition-csp', icon: 'repartition' },
      { label: 'Clés statistiques', path: '/budget/statistiques-cles', icon: 'statistiques' },
      { label: 'Révisions budgétaires', path: '/budget/revisions', icon: 'revisions' },
      { label: 'Journal des mouvements', path: '/budget/journal-mouvements', icon: 'journal' }
    ] },
    { label: 'Administration', items: [
      { label: 'Structure', path: '/security/structures', icon: 'structure' },
      { label: 'Directions', path: '/security/directions', icon: 'directions' },
      { label: 'Services', path: '/security/services', icon: 'services' },
      { label: 'Salariés', path: '/security/salaries', icon: 'salaries' },
      { label: 'Affectations', path: '/security/affectations', icon: 'affectations' },
      { label: 'Utilisateurs', path: '/security/utilisateurs', icon: 'utilisateurs' },
      { label: 'Rôles & permissions', path: '/security/roles-permissions', icon: 'roles' },
      { label: 'Journal d’audit', path: '/security/audit-logs', icon: 'audit' }
    ] }
  ];
  readonly visibleGroups = computed(() => this.groups
    .map(group => ({
      ...group,
      items: group.items
        .filter(item => this.auth.hasAnyPermission([...FEATURE_ACCESS[item.path]]))
        .map(item => ({ ...item, children: item.children?.filter(child => this.auth.hasPermission(child.permission)) }))
    }))
    .filter(group => group.items.length > 0));
  canAccessDashboard(): boolean {
    return this.visibleGroups().length > 0;
  }
  logout(): void { this.menuOuvert.set(false); this.auth.logout(); }

  /** Ferme le menu utilisateur au clic en dehors. */
  @HostListener('document:click', ['$event'])
  fermerMenu(event: MouseEvent): void {
    if (this.menuOuvert() && !(event.target as HTMLElement).closest('.menu-utilisateur')) this.menuOuvert.set(false);
  }
}
