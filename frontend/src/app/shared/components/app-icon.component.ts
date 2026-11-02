import { Component, Input, computed, inject, signal } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

/**
 * Icônes de l'application : jeu unique au trait (24×24, trait 1,75, coins arrondis),
 * inspiré de Lucide. Le contenu est statique (défini ici), il peut donc être marqué sûr.
 */
const ICONES: Record<string, string> = {
  // Pilotage
  dashboard: '<rect x="3" y="3" width="7" height="9" rx="1.5"/><rect x="14" y="3" width="7" height="5" rx="1.5"/><rect x="14" y="12" width="7" height="9" rx="1.5"/><rect x="3" y="16" width="7" height="5" rx="1.5"/>',
  // Gestion budgétaire
  plans: '<path d="M9 4h6a1 1 0 0 1 1 1v1a1 1 0 0 1-1 1H9a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1Z"/><path d="M16 5h1.5A2.5 2.5 0 0 1 20 7.5v11a2.5 2.5 0 0 1-2.5 2.5h-11A2.5 2.5 0 0 1 4 18.5v-11A2.5 2.5 0 0 1 6.5 5H8"/><path d="M8 12.5h1.5l1.5-2 2 4 1.5-2H16"/><path d="M8 17h8"/>',
  nomenclature: '<rect x="3" y="3" width="6" height="5" rx="1.2"/><rect x="15" y="10" width="6" height="4" rx="1.2"/><rect x="15" y="17" width="6" height="4" rx="1.2"/><path d="M6 8v8a2 2 0 0 0 2 2h7"/><path d="M6 12h9"/>',
  allocations: '<circle cx="12" cy="5" r="2.5"/><path d="M12 7.5V11"/><path d="M5 17v-2.5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2V17"/><path d="M12 11v6"/><circle cx="5" cy="19" r="2"/><circle cx="12" cy="19" r="2"/><circle cx="19" cy="19" r="2"/>',
  exercices: '<rect x="3" y="4.5" width="18" height="16.5" rx="2.5"/><path d="M16 2.5v4"/><path d="M8 2.5v4"/><path d="M3 10h18"/><path d="M8 14h2"/><path d="M14 14h2"/><path d="M8 17.5h2"/><path d="M14 17.5h2"/>',
  // Répartition & contrôle
  repartition: '<path d="M21 12.5A9 9 0 1 1 11.5 3v9.5Z"/><path d="M15 2.6A9 9 0 0 1 21.4 9H15Z"/>',
  statistiques: '<path d="M3 3v16.5A1.5 1.5 0 0 0 4.5 21H21"/><rect x="7" y="12" width="3" height="5" rx="0.8"/><rect x="12" y="7" width="3" height="10" rx="0.8"/><rect x="17" y="10" width="3" height="7" rx="0.8"/>',
  revisions: '<path d="M7 4 3 8l4 4"/><path d="M3 8h13.5A4.5 4.5 0 0 1 21 12.5"/><path d="m17 20 4-4-4-4"/><path d="M21 16H7.5A4.5 4.5 0 0 1 3 11.5"/>',
  journal: '<path d="M3.5 12a8.5 8.5 0 1 0 2.6-6.1L3.5 8.5"/><path d="M3.5 3.5v5h5"/><path d="M12 7.5V12l3 2"/>',
  // Administration
  structure: '<path d="M6 21V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v16"/><path d="M6 11H4.5A1.5 1.5 0 0 0 3 12.5V21"/><path d="M18 9h1.5A1.5 1.5 0 0 1 21 10.5V21"/><path d="M2 21h20"/><path d="M10 7h4"/><path d="M10 11h4"/><path d="M10 15h4"/>',
  directions: '<rect x="9" y="2.5" width="6" height="5" rx="1.2"/><rect x="2.5" y="16.5" width="6" height="5" rx="1.2"/><rect x="15.5" y="16.5" width="6" height="5" rx="1.2"/><path d="M12 7.5v4.5"/><path d="M5.5 16.5V14a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v2.5"/>',
  services: '<path d="M12.8 2.4a2 2 0 0 0-1.6 0L3.1 6.1a.9.9 0 0 0 0 1.6l8.1 3.7a2 2 0 0 0 1.6 0l8.1-3.7a.9.9 0 0 0 0-1.6Z"/><path d="m21.5 12-8.7 4a2 2 0 0 1-1.6 0L2.5 12"/><path d="m21.5 16.5-8.7 4a2 2 0 0 1-1.6 0l-8.7-4"/>',
  salaries: '<path d="M16 20v-1.5a4 4 0 0 0-4-4H6.5a4 4 0 0 0-4 4V20"/><circle cx="9.25" cy="7.5" r="3.75"/><path d="M21.5 20v-1.5a4 4 0 0 0-3-3.9"/><path d="M15.5 3.9a3.75 3.75 0 0 1 0 7.2"/>',
  utilisateurs: '<circle cx="10" cy="7.5" r="4"/><path d="M3 20.5v-1A5.5 5.5 0 0 1 8.5 14h3"/><circle cx="17.5" cy="17" r="2.25"/><path d="M17.5 13.25v1.5"/><path d="M17.5 19.25v1.5"/><path d="m14.25 15.1 1.3.75"/><path d="m19.45 18.15 1.3.75"/><path d="m14.25 18.9 1.3-.75"/><path d="m19.45 15.85 1.3-.75"/>',
  roles: '<path d="M20 12.5c0 5-3.5 7.6-7.6 9a1.2 1.2 0 0 1-.8 0C7.5 20.1 4 17.5 4 12.5V6a1 1 0 0 1 1-1c2 0 4.4-1.1 6.2-2.6a1.2 1.2 0 0 1 1.6 0C14.6 3.9 17 5 19 5a1 1 0 0 1 1 1Z"/><path d="m9 12 2 2 4-4"/>',
  audit: '<path d="M14 2.5H6.5A2.5 2.5 0 0 0 4 5v14a2.5 2.5 0 0 0 2.5 2.5H11"/><path d="M14 2.5V7a1 1 0 0 0 1 1h5"/><path d="M20 8 14 2.5"/><path d="M8 9h3"/><path d="M8 13h5"/><circle cx="16.5" cy="16.5" r="3"/><path d="m21 21-2.4-2.4"/>',
  // Divers
  brand: '<path d="M3 10.5 12 4l9 6.5"/><path d="M5 10v8"/><path d="M9.5 10v8"/><path d="M14.5 10v8"/><path d="M19 10v8"/><path d="M3 21h18"/><path d="M3.5 18h17"/>',
  chevron: '<path d="m9 6 6 6-6 6"/>',
  objectifStrategique: '<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="5.5"/><circle cx="12" cy="12" r="2"/>',
  objectifOperationnel: '<path d="M12 3v3"/><path d="M12 18v3"/><path d="M3 12h3"/><path d="M18 12h3"/><circle cx="12" cy="12" r="6"/><circle cx="12" cy="12" r="1.5"/>',
  activite: '<rect x="3" y="3" width="18" height="18" rx="2.5"/><path d="m7.5 12 3 3 6-6"/>',
};

@Component({
  selector: 'app-icon',
  standalone: true,
  template: `<svg aria-hidden="true" focusable="false" [attr.class]="classe" viewBox="0 0 24 24" fill="none"
    stroke="currentColor" [attr.stroke-width]="epaisseur" stroke-linecap="round" stroke-linejoin="round" [innerHTML]="contenu()"></svg>`,
  styles: [':host { display: inline-flex; flex-shrink: 0; }'],
})
export class AppIconComponent {
  private readonly sanitizer = inject(DomSanitizer);
  private readonly nom = signal('');

  @Input({ required: true }) set name(valeur: string) {
    this.nom.set(valeur);
  }
  @Input() classe = 'h-5 w-5';
  @Input() epaisseur = 1.75;

  readonly contenu = computed<SafeHtml>(() =>
    this.sanitizer.bypassSecurityTrustHtml(ICONES[this.nom()] ?? ICONES['dashboard']));
}
