import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FormPageComponent } from './form-page.component';
import { NotificationService } from '../../core/services/notification.service';
import { ResultatImport, TransfertService } from '../../core/services/transfert.service';
import type { FormatImport } from '../../core/models/import-formats';

export interface ColonneImport {
  nom: string;
  obligatoire?: boolean;
  aide: string;
}

/** Page complète d'import CSV : format attendu, modèle téléchargeable, envoi et compte rendu. */
@Component({
  selector: 'app-import-csv-page',
  standalone: true,
  imports: [FormPageComponent],
  template: `
    <app-form-page [section]="section" [title]="titre" mode="Import" [subtitle]="description" (back)="back.emit()">
      <form (submit)="$event.preventDefault(); envoyer()">
        <section class="space-y-4">
          <div class="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
            <div>
              <h3>Format attendu</h3>
              <p class="mt-1 text-xs text-slate-600">
                Fichier CSV encodé en UTF-8, séparateur « ; » ou « , ». La première ligne contient les noms de
                colonnes (ordre libre, colonnes supplémentaires ignorées). Dates au format AAAA-MM-JJ ou JJ/MM/AAAA,
                valeurs oui/non pour les cases à cocher. Un fichier exporté depuis cet onglet peut être réimporté.
              </p>
            </div>
            <button type="button" (click)="telechargerModele()"
              class="shrink-0 rounded-xl border border-green-700 bg-white px-4 py-2 text-xs font-semibold text-green-800 hover:bg-green-50">
              ⤓ Télécharger le modèle
            </button>
          </div>
          <div class="overflow-x-auto rounded-xl border border-slate-200">
            <table class="w-full text-left text-xs">
              <thead class="bg-slate-50 text-slate-600"><tr><th class="p-3">Colonne</th><th class="p-3">Obligatoire</th><th class="p-3">Contenu</th></tr></thead>
              <tbody class="divide-y divide-slate-200">
                @for (colonne of colonnes; track colonne.nom) {
                  <tr>
                    <td class="p-3 font-mono font-semibold text-slate-800">{{ colonne.nom }}</td>
                    <td class="p-3">{{ colonne.obligatoire ? 'Oui' : 'Non' }}</td>
                    <td class="p-3 text-slate-600">{{ colonne.aide }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </section>

        <section class="space-y-4">
          <h3>Fichier à importer</h3>
          <input type="file" accept=".csv,text/csv" (change)="choisir($event)"
            class="w-full cursor-pointer text-xs text-slate-600 file:mr-4 file:rounded-xl file:border-0 file:bg-green-700 file:px-4 file:py-2 file:text-xs file:font-semibold file:text-white hover:file:bg-green-600" />
          @if (fichier()) {
            <p class="text-xs text-slate-600">Fichier sélectionné : <strong>{{ fichier()!.name }}</strong> ({{ taille() }})</p>
          }
          <p class="text-xs text-slate-500">
            L’import est réalisé en une seule opération : si une ligne est invalide, aucune donnée n’est enregistrée
            et la ligne en cause est indiquée.
          </p>
          @if (erreur()) {
            <p role="alert" class="whitespace-pre-line rounded-xl border border-rose-300 bg-rose-50 p-3 text-sm text-rose-800">{{ erreur() }}</p>
          }
          @if (resultat()) {
            <p role="status" class="rounded-xl border border-green-300 bg-green-50 p-3 text-sm text-green-800">{{ resultat() }}</p>
          }
        </section>

        <div class="form-page-actions">
          <button type="button" (click)="back.emit()" class="rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-100">
            {{ resultat() ? 'Retour à la liste' : 'Annuler' }}
          </button>
          <button type="submit" [disabled]="!fichier() || envoi()" class="rounded-xl bg-green-700 px-5 py-2 text-xs font-semibold text-white hover:bg-green-600 disabled:opacity-50">
            {{ envoi() ? 'Import en cours…' : 'Lancer l’import' }}
          </button>
        </div>
      </form>
    </app-form-page>
  `,
})
export class ImportCsvPageComponent {
  private readonly transfert = inject(TransfertService);
  private readonly notifications = inject(NotificationService);

  @Input({ required: true }) section = '';
  @Input() titre = '';
  @Input() description = '';
  /** Ressource de l'import commun (/api/imports/{ressource}) ; ignorée si `upload` est fourni. */
  @Input() ressource = '';
  /** Envoi spécifique (imports historiques propres à un écran). */
  @Input() upload?: (fichier: File) => Observable<ResultatImport>;
  @Input() colonnes: readonly ColonneImport[] = [];
  @Input() exemples: readonly (readonly string[])[] = [];
  @Input() nomModele = 'modele_import.csv';
  /** Raccourci : titre, description, colonnes, exemples et nom du modèle en un seul objet. */
  @Input() set format(format: FormatImport) {
    this.titre = format.titre;
    this.description = format.description;
    this.colonnes = format.colonnes;
    this.exemples = format.exemples;
    this.nomModele = format.nomModele;
  }
  @Output() back = new EventEmitter<void>();
  @Output() imported = new EventEmitter<number>();

  readonly fichier = signal<File | null>(null);
  readonly envoi = signal(false);
  readonly erreur = signal('');
  readonly resultat = signal('');

  choisir(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.fichier.set(input.files?.[0] ?? null);
    this.erreur.set('');
    this.resultat.set('');
  }

  taille(): string {
    const octets = this.fichier()?.size ?? 0;
    return octets < 1024 ? `${octets} o` : `${(octets / 1024).toFixed(1)} Ko`;
  }

  telechargerModele(): void {
    this.transfert.telechargerModele(this.nomModele, this.colonnes.map((c) => c.nom), this.exemples);
  }

  envoyer(): void {
    const fichier = this.fichier();
    if (!fichier || this.envoi()) return;
    this.envoi.set(true);
    this.erreur.set('');
    this.resultat.set('');
    const requete = this.upload ? this.upload(fichier) : this.transfert.importer(this.ressource, fichier);
    requete.subscribe({
      next: (res) => {
        this.envoi.set(false);
        const message = `${res.elementsImportes} élément(s) importé(s) avec succès.`;
        this.resultat.set(message);
        this.notifications.success(message);
        this.imported.emit(res.elementsImportes);
      },
      error: (error: HttpErrorResponse) => {
        this.envoi.set(false);
        const message = (error.error as { message?: string } | null)?.message
          || (error.status === 403 ? 'Vous n’avez pas la permission d’importer ces données.' : 'L’import a échoué.');
        this.erreur.set(message);
        this.notifications.error(message);
      },
    });
  }
}
