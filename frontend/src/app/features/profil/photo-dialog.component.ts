import { Component, EventEmitter, Input, OnDestroy, Output, inject, signal } from '@angular/core';
import { ProfilService } from '../../core/services/profil.service';
import { NotificationService } from '../../core/services/notification.service';
import { AvatarComponent } from '../../shared/components/avatar.component';

const CADRE = 280;
const SORTIE = 512;
const TYPES_ACCEPTES = ['image/jpeg', 'image/png', 'image/webp'];
const TAILLE_MAX_SOURCE = 10 * 1024 * 1024;

/**
 * Choix et recadrage de la photo de profil : glisser-déposer ou sélection, recadrage circulaire
 * (déplacement à la souris / au doigt / aux flèches, zoom), export carré 512 px en JPEG.
 */
@Component({
  selector: 'app-photo-dialog',
  standalone: true,
  imports: [AvatarComponent],
  template: `
    <div class="fixed inset-0 z-50 flex items-end justify-center bg-slate-900/60 p-0 sm:items-center sm:p-4" (click)="fermer()">
      <div role="dialog" aria-modal="true" aria-labelledby="photo-titre" (click)="$event.stopPropagation()"
        class="w-full max-w-lg rounded-t-2xl border border-slate-200 bg-white shadow-xl sm:rounded-2xl">
        <header class="flex items-center justify-between border-b border-slate-200 px-5 py-4">
          <div>
            <h2 id="photo-titre" class="text-base font-semibold text-slate-900">Photo de profil</h2>
            <p class="text-xs text-slate-500">JPEG, PNG ou WebP. Elle est visible dans l’en-tête et le menu de l’application.</p>
          </div>
          <button type="button" (click)="fermer()" aria-label="Fermer" class="rounded-lg p-2 text-slate-500 hover:bg-slate-100">
            <svg aria-hidden="true" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path d="M6 18L18 6M6 6l12 12" /></svg>
          </button>
        </header>

        <div class="px-5 py-5">
          @if (!image()) {
            <div class="flex flex-col items-center gap-5 sm:flex-row sm:items-start">
              <app-avatar [photoUrl]="photoActuelle" taille="xl" />
              <label class="photo-depot flex w-full flex-1 cursor-pointer flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed px-4 py-8 text-center"
                [class.photo-depot-actif]="survol()"
                (dragover)="$event.preventDefault(); survol.set(true)" (dragleave)="survol.set(false)" (drop)="onDepot($event)">
                <svg aria-hidden="true" class="h-8 w-8 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M4 16.5V18a2 2 0 002 2h12a2 2 0 002-2v-1.5M12 15V4m0 0L7.5 8.5M12 4l4.5 4.5" />
                </svg>
                <span class="text-sm font-semibold text-slate-800">Glissez une image ici</span>
                <span class="text-xs text-slate-500">ou <span class="font-semibold text-green-700 underline">parcourez vos fichiers</span></span>
                <input type="file" accept="image/jpeg,image/png,image/webp" class="sr-only" (change)="onChoix($event)">
              </label>
            </div>
          } @else {
            <div class="flex flex-col items-center gap-4">
              <div class="photo-cadre relative touch-none select-none overflow-hidden rounded-xl bg-slate-900"
                [style.width.px]="cadre" [style.height.px]="cadre" tabindex="0"
                aria-label="Zone de recadrage : faites glisser l’image ou utilisez les flèches du clavier ; + et - pour zoomer"
                (pointerdown)="debutGlisser($event)" (pointermove)="glisser($event)" (pointerup)="finGlisser($event)" (pointercancel)="finGlisser($event)"
                (wheel)="onMolette($event)" (keydown)="onClavier($event)">
                <img [src]="image()!.src" alt="" draggable="false" class="pointer-events-none absolute left-0 top-0 max-w-none origin-top-left"
                  [style.width.px]="largeurAffichee()" [style.height.px]="hauteurAffichee()"
                  [style.transform]="'translate(' + x() + 'px,' + y() + 'px)'" />
                <div class="photo-masque pointer-events-none absolute inset-0 rounded-full"></div>
              </div>
              <div class="flex w-full max-w-[280px] items-center gap-3">
                <svg aria-hidden="true" class="h-4 w-4 text-slate-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="6.5" /><path d="M8 11h6M20 20l-4.2-4.2" /></svg>
                <input type="range" min="1" max="4" step="0.01" [value]="zoom()" (input)="changerZoom(+$any($event.target).value)"
                  aria-label="Zoom" class="photo-zoom flex-1">
                <svg aria-hidden="true" class="h-4 w-4 text-slate-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="6.5" /><path d="M8 11h6M11 8v6M20 20l-4.2-4.2" /></svg>
              </div>
              <p class="text-xs text-slate-500">Faites glisser l’image pour la positionner dans le cercle.</p>
            </div>
          }
        </div>

        <footer class="flex flex-col-reverse gap-2 border-t border-slate-200 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            @if (image()) {
              <button type="button" (click)="annulerImage()" class="w-full rounded-lg px-3 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 sm:w-auto">Choisir une autre image</button>
            } @else if (photoActuelle) {
              <button type="button" (click)="supprimer()" [disabled]="enCours()" class="w-full rounded-lg px-3 py-2 text-xs font-semibold text-rose-700 hover:bg-rose-50 disabled:opacity-50 sm:w-auto">Supprimer la photo</button>
            }
          </div>
          <div class="flex gap-2">
            <button type="button" (click)="fermer()" class="flex-1 rounded-lg border border-slate-300 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-100 sm:flex-none">Annuler</button>
            <button type="button" (click)="enregistrer()" [disabled]="!image() || enCours()"
              class="flex-1 rounded-lg bg-green-700 px-4 py-2 text-xs font-semibold text-white hover:bg-green-600 disabled:opacity-50 sm:flex-none">
              {{ enCours() ? 'Enregistrement…' : 'Enregistrer la photo' }}
            </button>
          </div>
        </footer>
      </div>
    </div>
  `,
})
export class PhotoDialogComponent implements OnDestroy {
  @Input() photoActuelle: string | null = null;
  @Output() fermeture = new EventEmitter<void>();

  private readonly profilService = inject(ProfilService);
  private readonly notify = inject(NotificationService);

  readonly cadre = CADRE;
  readonly image = signal<HTMLImageElement | null>(null);
  readonly zoom = signal(1);
  readonly x = signal(0);
  readonly y = signal(0);
  readonly survol = signal(false);
  readonly enCours = signal(false);
  private urlSource: string | null = null;
  private glissement: { id: number; departX: number; departY: number; x: number; y: number } | null = null;

  // --- Choix du fichier ---

  onChoix(event: Event): void {
    const input = event.target as HTMLInputElement;
    const fichier = input.files?.[0];
    input.value = '';
    if (fichier) this.charger(fichier);
  }

  onDepot(event: DragEvent): void {
    event.preventDefault();
    this.survol.set(false);
    const fichier = event.dataTransfer?.files?.[0];
    if (fichier) this.charger(fichier);
  }

  private charger(fichier: File): void {
    if (!TYPES_ACCEPTES.includes(fichier.type)) {
      this.notify.warning('Choisissez une image JPEG, PNG ou WebP.');
      return;
    }
    if (fichier.size > TAILLE_MAX_SOURCE) {
      this.notify.warning('L’image est trop lourde (10 Mo maximum).');
      return;
    }
    this.libererSource();
    this.urlSource = URL.createObjectURL(fichier);
    const image = new Image();
    image.onload = () => {
      this.image.set(image);
      this.zoom.set(1);
      this.x.set((CADRE - this.largeurAffichee()) / 2);
      this.y.set((CADRE - this.hauteurAffichee()) / 2);
    };
    image.onerror = () => this.notify.error('Cette image ne peut pas être lue.');
    image.src = this.urlSource;
  }

  annulerImage(): void {
    this.libererSource();
    this.image.set(null);
  }

  // --- Recadrage ---

  /** Échelle « couvrir » : l'image remplit toujours le cadre. */
  private echelle(): number {
    const image = this.image();
    if (!image) return 1;
    return Math.max(CADRE / image.naturalWidth, CADRE / image.naturalHeight) * this.zoom();
  }

  largeurAffichee(): number {
    return (this.image()?.naturalWidth ?? 0) * this.echelle();
  }

  hauteurAffichee(): number {
    return (this.image()?.naturalHeight ?? 0) * this.echelle();
  }

  changerZoom(valeur: number): void {
    const zoom = Math.min(4, Math.max(1, valeur));
    // Zoom centré : le point au centre du cadre reste au centre.
    const ancienne = this.echelle();
    const centreX = (CADRE / 2 - this.x()) / ancienne;
    const centreY = (CADRE / 2 - this.y()) / ancienne;
    this.zoom.set(zoom);
    const nouvelle = this.echelle();
    this.positionner(CADRE / 2 - centreX * nouvelle, CADRE / 2 - centreY * nouvelle);
  }

  debutGlisser(event: PointerEvent): void {
    (event.currentTarget as HTMLElement).setPointerCapture(event.pointerId);
    this.glissement = { id: event.pointerId, departX: event.clientX, departY: event.clientY, x: this.x(), y: this.y() };
  }

  glisser(event: PointerEvent): void {
    if (!this.glissement || this.glissement.id !== event.pointerId) return;
    this.positionner(this.glissement.x + event.clientX - this.glissement.departX,
      this.glissement.y + event.clientY - this.glissement.departY);
  }

  finGlisser(event: PointerEvent): void {
    if (this.glissement?.id === event.pointerId) this.glissement = null;
  }

  onMolette(event: WheelEvent): void {
    event.preventDefault();
    this.changerZoom(this.zoom() - event.deltaY * 0.002);
  }

  onClavier(event: KeyboardEvent): void {
    const pas = event.shiftKey ? 20 : 5;
    const deplacements: Record<string, [number, number]> = {
      ArrowLeft: [pas, 0], ArrowRight: [-pas, 0], ArrowUp: [0, pas], ArrowDown: [0, -pas],
    };
    if (event.key in deplacements) {
      event.preventDefault();
      const [dx, dy] = deplacements[event.key];
      this.positionner(this.x() + dx, this.y() + dy);
    } else if (event.key === '+' || event.key === '=') {
      event.preventDefault();
      this.changerZoom(this.zoom() + 0.1);
    } else if (event.key === '-') {
      event.preventDefault();
      this.changerZoom(this.zoom() - 0.1);
    }
  }

  /** L'image ne doit jamais laisser de vide dans le cadre. */
  private positionner(x: number, y: number): void {
    this.x.set(Math.min(0, Math.max(CADRE - this.largeurAffichee(), x)));
    this.y.set(Math.min(0, Math.max(CADRE - this.hauteurAffichee(), y)));
  }

  // --- Enregistrement ---

  enregistrer(): void {
    const image = this.image();
    if (!image) return;
    const canvas = document.createElement('canvas');
    canvas.width = SORTIE;
    canvas.height = SORTIE;
    const contexte = canvas.getContext('2d');
    if (!contexte) return;
    const echelle = this.echelle();
    contexte.imageSmoothingQuality = 'high';
    contexte.fillStyle = '#fff';
    contexte.fillRect(0, 0, SORTIE, SORTIE);
    contexte.drawImage(image, -this.x() / echelle, -this.y() / echelle, CADRE / echelle, CADRE / echelle, 0, 0, SORTIE, SORTIE);
    this.enCours.set(true);
    canvas.toBlob((blob) => {
      if (!blob) {
        this.enCours.set(false);
        this.notify.error('Impossible de préparer l’image.');
        return;
      }
      this.profilService.envoyerPhoto(blob).subscribe({
        next: () => {
          this.enCours.set(false);
          this.notify.success('Photo de profil mise à jour.');
          this.fermer();
        },
        error: (erreur) => {
          this.enCours.set(false);
          this.notify.error(erreur?.error?.message || 'La photo n’a pas pu être enregistrée.');
        },
      });
    }, 'image/jpeg', 0.9);
  }

  supprimer(): void {
    if (!confirm('Supprimer votre photo de profil ?')) return;
    this.enCours.set(true);
    this.profilService.supprimerPhoto().subscribe({
      next: () => {
        this.enCours.set(false);
        this.notify.success('Photo de profil supprimée.');
        this.fermer();
      },
      error: () => {
        this.enCours.set(false);
        this.notify.error('La photo n’a pas pu être supprimée.');
      },
    });
  }

  fermer(): void {
    this.fermeture.emit();
  }

  ngOnDestroy(): void {
    this.libererSource();
  }

  private libererSource(): void {
    if (this.urlSource) URL.revokeObjectURL(this.urlSource);
    this.urlSource = null;
  }
}
