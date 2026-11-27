import { Component, Input } from '@angular/core';

/** Photo de profil ronde ; sans photo, silhouette d'utilisateur par défaut. */
@Component({
  selector: 'app-avatar',
  standalone: true,
  template: `
    <span [class]="'avatar avatar-' + taille" [attr.aria-label]="libelle || null" [attr.role]="libelle ? 'img' : null">
      @if (photoUrl) {
        <img [src]="photoUrl" alt="" class="h-full w-full object-cover" />
      } @else {
        <svg aria-hidden="true" class="avatar-silhouette" viewBox="0 0 24 24" fill="currentColor">
          <circle cx="12" cy="8.6" r="4.1" />
          <path d="M3.6 21.2c.7-4.3 4.2-7.1 8.4-7.1s7.7 2.8 8.4 7.1a.8.8 0 01-.8.9H4.4a.8.8 0 01-.8-.9z" />
        </svg>
      }
    </span>
  `,
})
export class AvatarComponent {
  @Input() photoUrl: string | null = null;
  @Input() taille: 'sm' | 'md' | 'lg' | 'xl' = 'md';
  /** Texte alternatif, ex. « Photo de profil de Awa Ouédraogo ». */
  @Input() libelle = '';
}
