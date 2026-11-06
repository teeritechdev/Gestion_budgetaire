import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { PageHeaderComponent } from '../../../shared/components/page-header.component';
import { RecordDetailsDialogComponent } from '../../../shared/components/record-details-dialog.component';
import { RowActionsComponent } from '../../../shared/components/row-actions.component';
import { DataActionsComponent } from '../../../shared/components/data-actions.component';
import { FormPageComponent } from '../../../shared/components/form-page.component';
import { ImportCsvPageComponent } from '../../../shared/components/import-csv-page.component';
import { IMPORT_FORMATS } from '../../../core/models/import-formats';
import { DatePickerComponent } from '../../../shared/components/date-picker.component';
import { SearchOptionDirective, SearchSelectComponent } from '../../../shared/components/search-select.component';
import { AuthService } from '../../../core/services/auth.service';
import { AffectationService } from '../../../core/services/affectation.service';
import { SalarieService } from '../../../core/services/salarie.service';
import { StructureService } from '../../../core/services/structure.service';
import { DirectionService } from '../../../core/services/direction.service';
import { ServiceOrganisationnelService } from '../../../core/services/service-organisationnel.service';
import { NotificationService } from '../../../core/services/notification.service';
import {
  Affectation, AffectationRequest, NIVEAUX_AFFECTATION, NiveauAffectation, STATUTS_AFFECTATION, StatutAffectation,
} from '../../../core/models/affectation.model';
import { Salarie } from '../../../core/models/salarie.model';
import { Structure } from '../../../core/models/structure.model';
import { Direction } from '../../../core/models/direction.model';
import { ServiceOrganisationnel } from '../../../core/models/service-organisationnel.model';
import { errorMessage } from '../structures/directions-list.component';

/** Entité proposée dans la liste « Entité d'affectation », quel que soit le niveau. */
interface Cible {
  id: string;
  libelle: string;
  chemin: string;
}

const AIDE = 'block text-[11px] font-normal text-slate-500';
const CHAMP = 'w-full rounded-lg border border-slate-300 p-2';

@Component({
  selector: 'app-affectations-list',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHeaderComponent, RecordDetailsDialogComponent, RowActionsComponent, DataActionsComponent,
    FormPageComponent, ImportCsvPageComponent, DatePickerComponent, SearchSelectComponent, SearchOptionDirective],
  template: `
@if (importOpen()) {
<app-import-csv-page section="Affectations" [format]="importFormats.affectations" (back)="importOpen.set(false)" (imported)="refresh()" />
} @else if (formOpen()) {
<app-form-page section="Affectations" [title]="editingId() ? 'Modifier l’affectation' : 'Nouvelle affectation'" [mode]="editingId() ? 'Modification' : 'Création'"
  subtitle="Indiquez pour quelle structure, direction ou service le salarié travaille, et avec quelle fonction." (back)="fermer()">
  <form (ngSubmit)="save()">
    <section class="space-y-4">
      <label class="block space-y-1 text-xs">Salarié <span class="champ-obligatoire" aria-hidden="true">*</span>
        <app-search-select required [(ngModel)]="draft.salarieId" name="salarieId" triggerClass="${CHAMP}" [isDisabled]="!salaries().length">
          @for (salarie of salaries(); track salarie.id) {
            <app-search-option [value]="salarie.id" [label]="salarie.prenom + ' ' + salarie.nom + ' — ' + salarie.matricule + (salarie.poste ? ' · ' + salarie.poste : '')" />
          }
        </app-search-select>
        @if (!salaries().length) { <span class="block text-[11px] font-normal text-amber-700">Aucun salarié disponible : créez d’abord le dossier dans l’onglet Salariés.</span> }
      </label>

      <label class="block space-y-1 text-xs">Niveau d’affectation <span class="champ-obligatoire" aria-hidden="true">*</span>
        <select required [ngModel]="draft.niveau" (ngModelChange)="choisirNiveau($event)" name="niveau" class="${CHAMP}">
          @for (niveau of niveaux; track niveau.valeur) { <option [value]="niveau.valeur">{{ niveau.libelle }}</option> }
        </select>
        <span class="${AIDE}">{{ aideNiveau() }}</span>
      </label>

      <label class="block space-y-1 text-xs">{{ libelleNiveau(draft.niveau) }} d’affectation <span class="champ-obligatoire" aria-hidden="true">*</span>
        <app-search-select required [(ngModel)]="draft.cibleId" name="cibleId" triggerClass="${CHAMP}" [isDisabled]="!cibles().length">
          @for (cible of cibles(); track cible.id) { <app-search-option [value]="cible.id" [label]="cible.libelle" /> }
        </app-search-select>
        @if (cibleChoisie(); as cible) {
          <span class="${AIDE}">Rattachement : <span class="font-mono">{{ cible.chemin }}</span></span>
        } @else if (!cibles().length) {
          <span class="block text-[11px] font-normal text-amber-700">Aucun(e) {{ libelleNiveau(draft.niveau).toLowerCase() }} disponible, ou permission de lecture manquante.</span>
        }
      </label>

      <label class="block space-y-1 text-xs">Rôle / fonction
        <input maxlength="100" [(ngModel)]="draft.fonction" name="fonction" list="fonctions-affectation" class="${CHAMP}">
        <datalist id="fonctions-affectation">
          @for (fonction of fonctionsProposees; track fonction) { <option [value]="fonction"></option> }
        </datalist>
        <span class="${AIDE}">Rôle tenu dans l’entité : choisissez une proposition ou saisissez librement.</span>
      </label>

      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <label class="block space-y-1 text-xs">Date de début <span class="champ-obligatoire" aria-hidden="true">*</span>
          <app-date-picker required [(ngModel)]="draft.dateDebut" name="dateDebut" />
        </label>
        <label class="block space-y-1 text-xs">Date de fin
          <app-date-picker [(ngModel)]="draft.dateFin" name="dateFin" [min]="draft.dateDebut || null" />
          <span class="${AIDE}">Laissez vide tant que l’affectation est en cours.</span>
        </label>
      </div>


      <label class="block space-y-1 text-xs">Commentaire
        <textarea rows="2" maxlength="500" [(ngModel)]="draft.commentaire" name="commentaire" class="${CHAMP} resize-y"></textarea>
      </label>
    </section>

    <div class="form-page-actions">
      <button type="button" (click)="fermer()" class="rounded-lg border px-3 py-2 text-xs">Annuler</button>
      <button [disabled]="saving()" class="rounded-lg bg-green-700 px-4 py-2 text-xs font-semibold text-white disabled:opacity-50">
        {{ saving() ? 'Enregistrement…' : editingId() ? 'Enregistrer les modifications' : 'Affecter le salarié' }}
      </button>
    </div>
  </form>
</app-form-page>
} @else {
  <div class="mx-auto max-w-7xl space-y-6">
    <app-page-header icon="affectations" module="Administration" title="Affectations"
      description="Qui travaille pour quelle structure, direction ou service, et avec quelle fonction.">
      <app-data-actions ressource="affectations" exportPermission="affectation.exporter" [ids]="exportIds()"
        [importPermissions]="['affectation.importer']" [importFormat]="importFormats.affectations" (importer)="importOpen.set(true)" />
      @if (auth.hasPermission('affectation.creer')) {
        <button type="button" (click)="ouvrir()" class="whitespace-nowrap rounded-xl bg-green-700 px-4 py-2 text-xs font-semibold text-white hover:bg-green-600">＋ Nouvelle affectation</button>
      }
    </app-page-header>

    @if (error()) {
      <div role="alert" class="rounded-xl border border-rose-300 bg-rose-50 p-3 text-sm text-rose-700">{{ error() }}</div>
    }

    <div class="stats-grid grid gap-4 sm:grid-cols-3">
      <div class="rounded-2xl border border-slate-200 bg-white p-4">
        <p class="text-xs font-medium text-slate-500">Affectations en cours</p>
        <p class="mt-1 text-2xl font-bold text-slate-900">{{ indicateurs().enCours }}</p>
      </div>
      <div class="rounded-2xl border border-slate-200 bg-white p-4">
        <p class="text-xs font-medium text-slate-500">Salariés affectés</p>
        <p class="mt-1 text-2xl font-bold text-slate-900">{{ indicateurs().affectes }}</p>
      </div>
      <button type="button" (click)="voirSansAffectation()" [disabled]="!indicateurs().sansAffectation"
        class="rounded-2xl border border-slate-200 bg-white p-4 text-left disabled:cursor-default">
        <p class="text-xs font-medium text-slate-500">Salariés actifs sans affectation</p>
        <p class="mt-1 text-2xl font-bold" [class.text-amber-700]="indicateurs().sansAffectation" [class.text-slate-900]="!indicateurs().sansAffectation">{{ indicateurs().sansAffectation }}</p>
        @if (indicateurs().sansAffectation) { <p class="mt-1 text-[11px] font-medium text-green-700">Voir la liste →</p> }
      </button>
    </div>

    @if (sansAffectationOuvert() && salariesSansAffectation().length) {
      <div class="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-xs text-amber-900">
        <div class="mb-2 flex items-center justify-between gap-3">
          <p class="font-semibold">Salariés actifs sans affectation en cours</p>
          <button type="button" (click)="sansAffectationOuvert.set(false)" class="text-amber-800 underline">Masquer</button>
        </div>
        <ul class="flex flex-wrap gap-2">
          @for (salarie of salariesSansAffectation(); track salarie.id) {
            <li>
              @if (auth.hasPermission('affectation.creer')) {
                <button type="button" (click)="ouvrir(salarie.id)" class="rounded-full border border-amber-300 bg-white px-3 py-1 font-medium text-amber-900 hover:bg-amber-100">
                  {{ salarie.prenom }} {{ salarie.nom }} · Affecter
                </button>
              } @else {
                <span class="rounded-full border border-amber-300 bg-white px-3 py-1">{{ salarie.prenom }} {{ salarie.nom }}</span>
              }
            </li>
          }
        </ul>
      </div>
    }

    <div class="flex flex-wrap items-center gap-3 rounded-2xl border border-slate-200 bg-white p-4">
      <input type="text" [(ngModel)]="search" placeholder="Rechercher un salarié, une entité ou une fonction…" class="min-w-[220px] flex-1 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs" />
      <select [(ngModel)]="niveauFilter" aria-label="Filtrer par niveau" class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-700">
        <option value="">Tous les niveaux</option>
        @for (niveau of niveaux; track niveau.valeur) { <option [value]="niveau.valeur">{{ niveau.libelle }}</option> }
      </select>
      <select [(ngModel)]="structureFilter" aria-label="Filtrer par structure" class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-700">
        <option value="">Toutes les structures</option>
        @for (structure of structures(); track structure.code) { <option [value]="structure.code">{{ structure.nom }} ({{ structure.code }})</option> }
      </select>
      <select [(ngModel)]="statutFilter" aria-label="Filtrer par statut" class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-700">
        <option value="">Tous les statuts</option>
        @for (statut of statuts; track statut.valeur) { <option [value]="statut.valeur">{{ statut.libelle }}</option> }
      </select>
      @if (salarieFilter) {
        <span class="inline-flex items-center gap-2 rounded-full bg-green-50 px-3 py-1 text-xs font-medium text-green-800">
          {{ nomSalarie(salarieFilter) }}
          <button type="button" (click)="salarieFilter = ''" aria-label="Retirer le filtre salarié" class="font-bold">×</button>
        </span>
      }
    </div>

    <section class="overflow-hidden rounded-2xl border border-slate-200 bg-white">
      <div class="overflow-x-auto">
        <table class="w-full text-left text-xs">
          <thead class="bg-slate-50 text-slate-600">
            <tr><th class="p-3">Salarié</th><th class="p-3">Affecté à</th><th class="p-3">Rôle / fonction</th><th class="p-3">Période</th><th class="p-3">Statut</th><th class="p-3 text-right">Actions</th></tr>
          </thead>
          <tbody class="divide-y divide-slate-200">
            @for (affectation of visible(); track affectation.id) {
              <tr>
                <td class="p-3">
                  <button type="button" (click)="salarieFilter = affectation.salarieId" class="text-left font-semibold text-slate-900 hover:underline" title="Voir toutes ses affectations">{{ affectation.salarieNom }}</button>
                  <p class="text-[11px] text-slate-500"><span class="font-mono">{{ affectation.salarieMatricule }}</span>@if (affectation.salariePoste) { · {{ affectation.salariePoste }} }</p>
                </td>
                <td class="p-3">
                  <div class="flex items-center gap-2">
                    <span class="affectation-badge" [ngClass]="'affectation-badge-' + affectation.niveau.toLowerCase()">{{ libelleNiveau(affectation.niveau) }}</span>
                    <span class="font-medium text-slate-900">{{ affectation.cibleLibelle }}</span>
                  </div>
                  <p class="mt-1 font-mono text-[11px] text-slate-500">{{ affectation.chemin }}</p>
                </td>
                <td class="p-3">{{ affectation.fonction || '—' }}</td>
                <td class="p-3 whitespace-nowrap">
                  {{ affectation.dateDebut | date: 'dd/MM/yyyy' }} → {{ affectation.dateFin ? (affectation.dateFin | date: 'dd/MM/yyyy') : '…' }}
                </td>
                <td class="p-3">
                  <div class="flex flex-wrap items-center gap-1.5">
                    <span class="affectation-statut" [ngClass]="'affectation-statut-' + affectation.statut.toLowerCase()">{{ libelleStatut(affectation.statut) }}</span>
                  </div>
                </td>
                <td class="p-3">
                  <app-row-actions [label]="affectation.salarieNom + ' · ' + affectation.cibleCode"
                    [canEdit]="auth.hasPermission('affectation.modifier')" [canDelete]="auth.hasPermission('affectation.supprimer')"
                    (view)="detailRecord = affectation" (edit)="modifier(affectation)" (remove)="supprimer(affectation)" />
                </td>
              </tr>
            } @empty {
              <tr><td colspan="6" class="p-6 text-center text-slate-500">{{ loading() ? 'Chargement des affectations…' : 'Aucune affectation trouvée.' }}</td></tr>
            }
          </tbody>
        </table>
        @if (detailRecord) { <app-record-details-dialog title="Affectation" [record]="detailRecord" (close)="detailRecord = null" /> }
      </div>
    </section>
  </div>
}
`,
})
export class AffectationsListComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly affectationService = inject(AffectationService);
  private readonly salarieService = inject(SalarieService);
  private readonly structureService = inject(StructureService);
  private readonly directionService = inject(DirectionService);
  private readonly serviceService = inject(ServiceOrganisationnelService);
  private readonly notifications = inject(NotificationService);

  readonly niveaux = NIVEAUX_AFFECTATION;
  readonly importFormats = IMPORT_FORMATS;
  readonly importOpen = signal(false);
  readonly statuts = (Object.keys(STATUTS_AFFECTATION) as StatutAffectation[]).map((valeur) => ({ valeur, libelle: STATUTS_AFFECTATION[valeur] }));
  readonly fonctionsProposees = ['Directeur', 'Directeur adjoint', 'Chef de service', 'Responsable', 'Chargé d’études',
    'Assistant', 'Agent', 'Comptable', 'Contrôleur de gestion', 'Stagiaire'];

  readonly affectations = signal<Affectation[]>([]);
  readonly salaries = signal<Salarie[]>([]);
  readonly structures = signal<Structure[]>([]);
  readonly directions = signal<Direction[]>([]);
  readonly services = signal<ServiceOrganisationnel[]>([]);
  readonly formOpen = signal(false);
  readonly saving = signal(false);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly editingId = signal('');
  readonly sansAffectationOuvert = signal(false);
  /** Niveau courant du formulaire, en signal pour recalculer la liste des entités. */
  private readonly niveauCourant = signal<NiveauAffectation>('SERVICE');

  detailRecord: object | null = null;
  search = '';
  niveauFilter: NiveauAffectation | '' = '';
  structureFilter = '';
  statutFilter: StatutAffectation | '' = '';
  salarieFilter = '';
  draft: AffectationRequest = this.brouillonVide();

  /** Entités proposées pour le niveau choisi, avec leur chemin hiérarchique. */
  readonly cibles = computed<Cible[]>(() => {
    switch (this.niveauCourant()) {
      case 'STRUCTURE':
        return this.structures().map((s) => ({ id: s.code, libelle: `${s.nom} (${s.code})`, chemin: s.code }));
      case 'DIRECTION':
        return this.directions().map((d) => ({
          id: d.id, libelle: `${d.libelle} (${d.code}) · ${d.structureCode}`, chemin: `${d.structureCode} › ${d.code}`,
        }));
      default:
        return this.services().map((s) => ({
          id: s.id,
          libelle: `${s.libelle} (${s.code}) · ${s.directionCode}`,
          chemin: [s.structureCode, s.directionCode, s.code].filter(Boolean).join(' › '),
        }));
    }
  });

  readonly indicateurs = computed(() => {
    const enCours = this.affectations().filter((a) => a.statut === 'EN_COURS');
    const affectes = new Set(enCours.map((a) => a.salarieId));
    return {
      enCours: enCours.length,
      affectes: affectes.size,
      sansAffectation: this.salariesSansAffectation().length,
    };
  });

  readonly salariesSansAffectation = computed(() => {
    const affectes = new Set(this.affectations().filter((a) => a.statut === 'EN_COURS').map((a) => a.salarieId));
    return this.salaries().filter((s) => s.actif && !affectes.has(s.id));
  });

  ngOnInit(): void {
    // Lien depuis l'onglet Salariés : ?salarie=<id> filtre sur ce salarié.
    this.salarieFilter = this.route.snapshot.queryParamMap.get('salarie') ?? '';
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set('');
    const si = <T>(permission: string, source: () => Observable<T[]>, libelle: string) =>
      this.auth.hasPermission(permission)
        ? source().pipe(catchError(() => { this.error.set(`Impossible de charger ${libelle}.`); return of([] as T[]); }))
        : of([] as T[]);
    forkJoin({
      affectations: si('affectation.lire', () => this.affectationService.lister(), 'les affectations'),
      salaries: si('salarie.lire', () => this.salarieService.lister(), 'les salariés'),
      structures: si('entite.lire', () => this.structureService.listerOuFiltrer(), 'les structures'),
      directions: si('direction.lire', () => this.directionService.lister(), 'les directions'),
      services: si('service.lire', () => this.serviceService.lister(), 'les services'),
    }).subscribe((data) => {
      this.affectations.set(data.affectations);
      this.salaries.set(data.salaries);
      this.structures.set(data.structures);
      this.directions.set(data.directions);
      this.services.set(data.services);
      this.loading.set(false);
    });
  }

  visible(): Affectation[] {
    const terme = this.search.trim().toLowerCase();
    return this.affectations().filter((a) =>
      (!this.salarieFilter || a.salarieId === this.salarieFilter) &&
      (!this.niveauFilter || a.niveau === this.niveauFilter) &&
      (!this.structureFilter || a.structureCode === this.structureFilter) &&
      (!this.statutFilter || a.statut === this.statutFilter) &&
      (!terme || [a.salarieNom, a.salarieMatricule, a.cibleCode, a.cibleLibelle, a.fonction]
        .some((valeur) => valeur?.toLowerCase().includes(terme))));
  }

  /** Lignes affichées (filtres appliqués) : ce sont elles qui sont exportées. */
  exportIds(): string[] {
    return this.visible().map((a) => a.id);
  }

  ouvrir(salarieId = ''): void {
    this.editingId.set('');
    this.draft = { ...this.brouillonVide(), salarieId: salarieId || this.salarieFilter };
    this.niveauCourant.set(this.draft.niveau);
    this.formOpen.set(true);
  }

  modifier(affectation: Affectation): void {
    this.editingId.set(affectation.id);
    this.draft = {
      salarieId: affectation.salarieId,
      niveau: affectation.niveau,
      cibleId: affectation.cibleId,
      fonction: affectation.fonction ?? '',
      dateDebut: affectation.dateDebut,
      dateFin: affectation.dateFin ?? '',
      commentaire: affectation.commentaire ?? '',
    };
    this.niveauCourant.set(affectation.niveau);
    this.formOpen.set(true);
  }

  fermer(): void {
    this.formOpen.set(false);
  }

  choisirNiveau(niveau: NiveauAffectation): void {
    if (this.draft.niveau === niveau) return;
    this.draft.niveau = niveau;
    this.draft.cibleId = '';
    this.niveauCourant.set(niveau);
  }

  cibleChoisie(): Cible | undefined {
    return this.cibles().find((cible) => cible.id === this.draft.cibleId);
  }

  aideNiveau(): string {
    return this.niveaux.find((n) => n.valeur === this.draft.niveau)?.aide ?? '';
  }

  libelleNiveau(niveau: NiveauAffectation): string {
    return this.niveaux.find((n) => n.valeur === niveau)?.libelle ?? niveau;
  }

  libelleStatut(statut: StatutAffectation): string {
    return STATUTS_AFFECTATION[statut] ?? statut;
  }

  nomSalarie(id: string): string {
    const salarie = this.salaries().find((s) => s.id === id);
    return salarie ? `${salarie.prenom} ${salarie.nom}` : 'Salarié sélectionné';
  }

  voirSansAffectation(): void {
    this.sansAffectationOuvert.update((ouvert) => !ouvert);
  }

  save(): void {
    const d = this.draft;
    if (!d.salarieId || !d.niveau || !d.cibleId || !d.dateDebut) {
      this.notifications.warning('Renseignez le salarié, le niveau, l’entité d’affectation et la date de début.');
      return;
    }
    if (d.dateFin && d.dateFin < d.dateDebut) {
      this.notifications.warning('La date de fin ne peut pas précéder la date de début.');
      return;
    }
    const request: AffectationRequest = {
      ...d,
      fonction: d.fonction?.trim() || null,
      dateFin: d.dateFin || null,
      commentaire: d.commentaire?.trim() || null,
    };
    this.saving.set(true);
    const requete$ = this.editingId()
      ? this.affectationService.modifier(this.editingId(), request)
      : this.affectationService.creer(request);
    requete$.subscribe({
      next: () => {
        this.saving.set(false);
        this.formOpen.set(false);
        this.notifications.success('Affectation enregistrée.');
        this.refresh();
      },
      error: (erreur: unknown) => this.echec(erreur),
    });
  }

  supprimer(affectation: Affectation): void {
    if (!confirm(`Supprimer l’affectation de ${affectation.salarieNom} à « ${affectation.cibleLibelle} » ?`)) return;
    this.affectationService.supprimer(affectation.id).subscribe({
      next: () => {
        this.notifications.success('Affectation supprimée.');
        this.refresh();
      },
      error: (erreur: unknown) => this.echec(erreur),
    });
  }

  private brouillonVide(): AffectationRequest {
    return {
      salarieId: '', niveau: 'SERVICE', cibleId: '', fonction: '',
      dateDebut: aujourdHui(), dateFin: '', commentaire: '',
    };
  }

  private echec(erreur: unknown): void {
    this.saving.set(false);
    const message = errorMessage(erreur);
    this.error.set(message);
    this.notifications.error(message);
  }
}

/** Date du jour au format ISO (AAAA-MM-JJ), en heure locale. */
function aujourdHui(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}
