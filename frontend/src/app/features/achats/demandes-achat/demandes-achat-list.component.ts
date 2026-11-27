import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { AchatService } from '../../../core/services/achat.service';
import { NomenclatureService } from '../../../core/services/nomenclature.service';
import { StockService } from '../../../core/services/stock.service';
import { ServiceOrganisationnelService } from '../../../core/services/service-organisationnel.service';
import { StructureService } from '../../../core/services/structure.service';
import { ProfilService } from '../../../core/services/profil.service';
import {
  DemandeAchat,
  DemandeAchatRequest,
  LigneDemandeAchatRequest,
  StatutDemandeAchat,
  TypeAchat,
  HistoriqueValidation,
  DecisionValidationRequest,
  VentilationImpact,
  VentilationImpactRequest,
} from '../../../core/models/achat/demande-achat.model';
import { Fournisseur } from '../../../core/models/achat/fournisseur.model';
import { ActiviteCatalogue } from '../../../core/models/budget/activite-catalogue.model';
import { Article } from '../../../core/models/stock/article.model';
import { ServiceOrganisationnel } from '../../../core/models/service-organisationnel.model';
import { Structure } from '../../../core/models/structure.model';
import { PageHeaderComponent } from '../../../shared/components/page-header.component';
import { FormPageComponent } from '../../../shared/components/form-page.component';
import { PaginationBarComponent } from '../../../shared/components/pagination-bar.component';
import { SearchOptionDirective, SearchSelectComponent } from '../../../shared/components/search-select.component';
import { errorMessage } from '../../../core/utils/error-message';

const STATUTS_DA: Record<StatutDemandeAchat, { label: string; classe: string }> = {
  BROUILLON: { label: 'Brouillon', classe: 'bg-slate-100 text-slate-700 border border-slate-200' },
  SOUMIS: { label: 'Soumise (N+1)', classe: 'bg-blue-50 text-blue-700 border border-blue-200' },
  EN_COURS_VALIDATION: { label: 'En validation', classe: 'bg-amber-50 text-amber-700 border border-amber-200' },
  APPROUVE: { label: 'Approuvée', classe: 'bg-emerald-50 text-emerald-700 border border-emerald-200' },
  REJETE: { label: 'Rejetée', classe: 'bg-rose-50 text-rose-700 border border-rose-200' },
  COMMANDE: { label: 'Commandée (BC émis)', classe: 'bg-purple-50 text-purple-700 border border-purple-200' },
};

const TYPES_ACHAT: { code: TypeAchat; label: string }[] = [
  { code: 'STOCKABLE', label: 'Fournitures de stock (consommables magasin)' },
  { code: 'PRESTATION_SERVICE', label: 'Prestation de service / Étude / Expertise' },
  { code: 'INVESTISSEMENT_IMMO', label: 'Investissement & Immobilisation (matériel, mobilier)' },
  { code: 'TRAVAUX', label: 'Travaux, aménagement et maintenance' },
];

@Component({
  selector: 'app-demandes-achat-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    PageHeaderComponent,
    FormPageComponent,
    PaginationBarComponent,
    SearchSelectComponent,
    SearchOptionDirective,
  ],
  template: `
@if (formOpen()) {
<app-form-page section="Demandes d'achat" [title]="editingId() ? 'Modifier la demande d’achat' : 'Nouvelle Demande d’Achat (DA)'" [mode]="editingId() ? 'Modification' : 'Création'" subtitle="Expression formelle du besoin d'achat externe avec imputation budgétaire." (back)="formOpen.set(false)">
  <form (ngSubmit)="save()">
    <!-- En-tête Dossier -->
    <section class="space-y-4 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <h3 class="text-sm font-semibold text-slate-900">En-tête de la demande</h3>
      <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <label class="block space-y-1 text-xs">Entité émettrice <span class="champ-obligatoire" aria-hidden="true">*</span>
          <app-search-select required [(ngModel)]="draft.entiteEmettrice" name="entiteEmettrice" triggerClass="w-full rounded-lg border border-slate-300 p-2 text-xs">
            @for (s of structures(); track s.code) { <app-search-option [value]="s.code" [label]="s.nom + ' (' + s.code + ')'" /> }
          </app-search-select>
        </label>
        <label class="block space-y-1 text-xs">Entité bénéficiaire <span class="champ-obligatoire" aria-hidden="true">*</span>
          <app-search-select required [(ngModel)]="draft.entiteBeneficiaire" name="entiteBeneficiaire" triggerClass="w-full rounded-lg border border-slate-300 p-2 text-xs">
            @for (s of structures(); track s.code) { <app-search-option [value]="s.code" [label]="s.nom + ' (' + s.code + ')'" /> }
          </app-search-select>
        </label>
        <label class="block space-y-1 text-xs">Nature / Type d’achat <span class="champ-obligatoire" aria-hidden="true">*</span>
          <select required [(ngModel)]="draft.typeAchat" name="typeAchat" class="w-full rounded-lg border border-slate-300 bg-white p-2 text-xs">
            @for (t of typesAchat; track t.code) { <option [value]="t.code">{{ t.label }}</option> }
          </select>
        </label>
      </div>

      <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <label class="block space-y-1 text-xs">Service demandeur
          <app-search-select [(ngModel)]="draft.serviceDemandeurId" name="serviceDemandeurId" triggerClass="w-full rounded-lg border border-slate-300 p-2 text-xs">
            @for (s of services(); track s.id) { <app-search-option [value]="s.id" [label]="s.libelle + ' (' + s.code + ')'" /> }
          </app-search-select>
        </label>
        <label class="block space-y-1 text-xs">Année budgétaire <span class="champ-obligatoire" aria-hidden="true">*</span>
          <input type="number" required min="2020" max="2050" [(ngModel)]="draft.exerciceAnnee" name="exerciceAnnee" class="w-full rounded-lg border border-slate-300 p-2 text-xs font-mono">
        </label>
      </div>

      <label class="block space-y-1 text-xs">Objet de l'achat <span class="champ-obligatoire" aria-hidden="true">*</span>
        <input required maxlength="255" [(ngModel)]="draft.objet" name="objet" placeholder="Ex. : Acquisition de 10 ordinateurs portables pour le service sinistres" class="w-full rounded-lg border border-slate-300 p-2 text-xs">
      </label>

      <label class="block space-y-1 text-xs">Justification du besoin opérationnel
        <textarea rows="2" [(ngModel)]="draft.justification" name="justification" placeholder="Précisez le contexte, l'urgence ou les contraintes opérationnelles…" class="w-full rounded-lg border border-slate-300 p-2 text-xs"></textarea>
      </label>
    </section>

    <!-- Lignes de DA -->
    <section class="mt-4 space-y-4 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <div class="flex items-center justify-between">
        <div>
          <h3 class="text-sm font-semibold text-slate-900">Lignes d'expression du besoin</h3>
          <p class="text-xs text-slate-500">Chaque ligne est adossée à une activité budgétaire (vérification de disponibilité).</p>
        </div>
        <button type="button" (click)="addLigne()" class="rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-xs font-semibold hover:bg-slate-50">
          ＋ Ajouter une ligne
        </button>
      </div>

      @for (l of draft.lignes; track $index) {
        <div class="space-y-3 rounded-xl border border-slate-200 bg-slate-50/60 p-3">
          <div class="flex items-center justify-between border-b border-slate-200 pb-2">
            <span class="text-xs font-bold text-slate-700">Ligne {{ $index + 1 }}</span>
            <button type="button" (click)="removeLigne($index)" [disabled]="draft.lignes.length === 1" class="text-xs font-medium text-rose-600 hover:underline disabled:opacity-30">
              Supprimer cette ligne
            </button>
          </div>

          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <label class="block space-y-1 text-xs">Activité budgétaire d’imputation <span class="champ-obligatoire" aria-hidden="true">*</span>
              <app-search-select required [(ngModel)]="l.activiteId" [name]="'act' + $index" triggerClass="w-full rounded-lg border border-slate-300 bg-white p-2 text-xs">
                @for (act of activites(); track act.id) { <app-search-option [value]="act.id" [label]="act.codeActivite + ' — ' + act.libelle" /> }
              </app-search-select>
            </label>

            @if (draft.typeAchat === 'STOCKABLE') {
              <label class="block space-y-1 text-xs">Article du magasin (consommable)
                <app-search-select [(ngModel)]="l.articleId" (ngModelChange)="onArticleChange(l, $event)" [name]="'art' + $index" triggerClass="w-full rounded-lg border border-slate-300 bg-white p-2 text-xs">
                  <app-search-option [value]="null" label="-- Aucun (prestation ou référence libre) --" />
                  @for (art of articles(); track art.id) { <app-search-option [value]="art.id" [label]="art.codeArticle + ' — ' + art.designation + ' (stock: ' + art.quantiteStock + ')'" /> }
                </app-search-select>
              </label>
            }
          </div>

          <label class="block space-y-1 text-xs">Description détaillée / Spécifications <span class="champ-obligatoire" aria-hidden="true">*</span>
            <input required [(ngModel)]="l.description" [name]="'desc' + $index" placeholder="Ex. : PC portable Core i7, 16 Go RAM, 512 Go SSD" class="w-full rounded-lg border border-slate-300 bg-white p-2 text-xs">
          </label>

          <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <label class="block space-y-1 text-xs">Unité de mesure
              <input [(ngModel)]="l.uniteMesure" [name]="'unite' + $index" placeholder="Ex. : Unité, Lot, Forfait" class="w-full rounded-lg border border-slate-300 bg-white p-2 text-xs">
            </label>
            <label class="block space-y-1 text-xs">Quantité demandée <span class="champ-obligatoire" aria-hidden="true">*</span>
              <input type="number" min="1" step="1" required [(ngModel)]="l.quantite" [name]="'qte' + $index" class="w-full rounded-lg border border-slate-300 bg-white p-2 text-xs font-mono">
            </label>
            <label class="block space-y-1 text-xs">Prix unitaire estimé HT (FCFA) <span class="champ-obligatoire" aria-hidden="true">*</span>
              <input type="number" min="0" step="100" required [(ngModel)]="l.prixUnitaireEstime" [name]="'pu' + $index" class="w-full rounded-lg border border-slate-300 bg-white p-2 text-xs font-mono">
            </label>
          </div>

          <div class="text-right text-xs font-medium text-slate-700">
            Total estimé de la ligne : <strong class="font-mono text-emerald-700">{{ ((l.quantite || 0) * (l.prixUnitaireEstime || 0) | number:'1.0-0') }} FCFA</strong>
          </div>
        </div>
      }

      <!-- Synthèse totale -->
      <div class="flex items-center justify-end gap-6 rounded-xl border border-slate-200 bg-slate-50 p-4">
        <div class="text-right">
          <p class="text-xs text-slate-500">Montant total estimé HT</p>
          <p class="text-lg font-bold font-mono text-slate-900">{{ (totalHtCalcul() | number:'1.0-0') }} FCFA</p>
        </div>
      </div>
    </section>

    <div class="form-page-actions mt-4 flex items-center justify-end gap-2">
      <button type="button" (click)="formOpen.set(false)" class="rounded-lg border border-slate-300 px-3 py-2 text-xs">Annuler</button>
      <button [disabled]="saving()" class="rounded-lg bg-green-700 px-4 py-2 text-xs font-semibold text-white shadow-sm transition hover:bg-green-800 disabled:opacity-50">
        {{ saving() ? 'Enregistrement…' : 'Soumettre la Demande d’Achat' }}
      </button>
    </div>
  </form>
</app-form-page>
} @else {
<div class="mx-auto max-w-7xl space-y-6">
  <app-page-header icon="achats" module="Gestion des achats" title="Demandes d’Achat (DA)" description="Expression des besoins d'approvisionnement externe, imputation budgétaire, ventilation inter-filiales, circuit d'approbation et émission de BC.">
    @if (auth.hasPermission('demande_achat.creer')) {
      <button type="button" (click)="openForm()" class="whitespace-nowrap rounded-xl bg-green-700 px-4 py-2 text-xs font-semibold text-white shadow-sm transition hover:bg-green-800">
        ＋ Nouvelle demande d’achat
      </button>
    }
  </app-page-header>

  @if (error()) { <div role="alert" class="rounded-xl border border-rose-300 bg-rose-50 p-3 text-sm text-rose-700">{{ error() }}</div> }

  <!-- Cartes KPI (Design Djibril stats-grid avec liseré supérieur) -->
  <section class="stats-grid grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
    <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" (click)="statutFilter = ''; search()">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Demandes d'achat</p>
      <p class="mt-2 text-2xl font-bold font-mono text-slate-900">{{ total() }}</p>
      <p class="mt-1 text-[11px] text-slate-500">Total des dossiers DA</p>
    </article>

    <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" (click)="statutFilter = 'EN_COURS_VALIDATION'; search()">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">En attente de visa</p>
      <p class="mt-2 text-2xl font-bold font-mono" [class.text-amber-500]="statsEnAttente() > 0" [class.text-slate-900]="statsEnAttente() === 0">{{ statsEnAttente() }}</p>
      <p class="mt-1 text-[11px] text-slate-500">Soumises ou en validation</p>
    </article>

    <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" (click)="statutFilter = 'APPROUVE'; search()">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Approuvées / Validées</p>
      <p class="mt-2 text-2xl font-bold font-mono text-emerald-700">{{ statsApprouves() }}</p>
      <p class="mt-1 text-[11px] text-slate-500">Prêtes pour bon de commande</p>
    </article>

    <article class="rounded-2xl border border-slate-200 bg-white p-4">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Volume estimé total</p>
      <p class="mt-2 text-2xl font-bold font-mono text-purple-700">{{ statsMontantTotal() | number:'1.0-0' }} <span class="text-xs font-normal text-slate-500">FCFA</span></p>
      <p class="mt-1 text-[11px] text-slate-500">Montant total HT des dossiers</p>
    </article>
  </section>

  <div class="flex flex-wrap items-center gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
    <input type="text" [(ngModel)]="recherche" (keyup.enter)="search()" placeholder="Recherche par code DA ou objet…" class="min-w-[240px] flex-1 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs" />
    <select [(ngModel)]="statutFilter" (change)="search()" class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs">
      <option value="">Tous les statuts</option>
      @for (s of statutKeys; track s) { <option [value]="s">{{ statuts[s].label }}</option> }
    </select>
    <select [(ngModel)]="typeFilter" (change)="search()" class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs">
      <option value="">Tous les types d'achat</option>
      @for (t of typesAchat; track t.code) { <option [value]="t.code">{{ t.label }}</option> }
    </select>
    <button type="button" (click)="search()" class="rounded-xl bg-blue-700 px-3 py-2 text-xs font-medium text-white hover:bg-blue-800">Rechercher</button>
    <button type="button" (click)="reset()" class="rounded-xl bg-slate-100 px-3 py-2 text-xs text-slate-600 hover:bg-slate-200">Réinitialiser</button>
  </div>

  <section class="overflow-hidden rounded-2xl border border-slate-200 bg-white">
    <div class="overflow-x-auto">
      <table class="w-full min-w-[900px] text-left text-xs">
        <thead class="bg-slate-50 text-slate-600 whitespace-nowrap">
          <tr>
            <th class="p-3 whitespace-nowrap">N° Dossier</th>
            <th class="p-3 whitespace-nowrap">Objet</th>
            <th class="p-3 whitespace-nowrap">Demandeur</th>
            <th class="p-3 whitespace-nowrap">Type</th>
            <th class="p-3 text-right whitespace-nowrap">Lignes</th>
            <th class="p-3 text-right whitespace-nowrap">Montant HT</th>
            <th class="p-3 whitespace-nowrap">Statut</th>
            <th class="p-3 whitespace-nowrap">Date</th>
            <th class="p-3 text-right whitespace-nowrap">Actions</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-200">
          @for (d of demandes(); track d.id) {
            <tr class="hover:bg-slate-50/60 transition">
              <td class="p-3 font-mono font-semibold text-blue-700 cursor-pointer hover:underline" (click)="openDetail(d)">{{ d.codeDossier }}</td>
              <td class="p-3 font-semibold text-slate-900 max-w-xs truncate">{{ d.objet }}</td>
              <td class="p-3">{{ d.demandeur.prenom }} {{ d.demandeur.nom }}</td>
              <td class="p-3 text-[11px] font-medium text-slate-600">{{ d.typeAchat }}</td>
              <td class="p-3 text-right font-mono">{{ d.lignes.length }}</td>
              <td class="p-3 text-right font-mono font-bold text-slate-900 whitespace-nowrap">{{ (d.montantEstimeHt | number:'1.0-0') }} FCFA</td>
              <td class="p-3">
                <span class="rounded-full px-2 py-0.5 text-[11px] font-semibold {{ statuts[d.statut]?.classe }}">
                  {{ statuts[d.statut]?.label || d.statut }}
                </span>
                @if (d.etapeCourante && d.statut !== 'APPROUVE' && d.statut !== 'REJETE') {
                  <div class="mt-0.5 text-[10px] font-medium text-amber-800 flex items-center gap-1">
                    <span class="inline-block h-1.5 w-1.5 rounded-full bg-amber-500"></span>
                    {{ d.etapeCourante }}
                  </div>
                }
              </td>
              <td class="p-3 text-slate-500 whitespace-nowrap">{{ d.createdAt | date:'dd/MM/yyyy' }}</td>
              <td class="p-3 text-right space-x-1.5 whitespace-nowrap">
                <button type="button" (click)="openDetail(d)" class="rounded-lg border border-slate-300 bg-white px-2.5 py-1 text-[11px] font-medium text-slate-700 hover:bg-slate-50">
                  Consulter
                </button>
                @if (d.statut === 'APPROUVE' && auth.hasPermission('bon_commande.creer')) {
                  <button type="button" (click)="openCreerBcModal(d)" class="rounded-lg bg-purple-700 px-2.5 py-1 text-[11px] font-semibold text-white hover:bg-purple-800">
                    Émettre BC
                  </button>
                }
              </td>
            </tr>
          } @empty {
            <tr><td colspan="9" class="p-6 text-center text-slate-500">{{ loading() ? 'Chargement des demandes d’achat…' : 'Aucune demande d’achat trouvée.' }}</td></tr>
          }
        </tbody>
      </table>
    </div>
    <app-pagination-bar [page]="page()" [totalPages]="totalPages()" [total]="total()" (pageChange)="goTo($event)" />
  </section>
</div>

<!-- Modal Détail & Circuit de Validation DA -->
@if (detailRecord; as da) {
  <div class="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-slate-900/60 p-4 backdrop-blur-sm" (click)="closeDetail()">
    <div class="relative w-full max-w-4xl max-h-[92vh] flex flex-col rounded-2xl bg-white shadow-2xl overflow-hidden" (click)="$event.stopPropagation()">
      <div class="flex items-center justify-between border-b border-slate-200 px-6 py-4 bg-slate-50/80">
        <div>
          <div class="flex items-center gap-2">
            <h2 class="text-base font-bold text-slate-900 font-mono">{{ da.codeDossier }}</h2>
            <span class="rounded-full px-2.5 py-0.5 text-[11px] font-semibold {{ statuts[da.statut]?.classe }}">
              {{ statuts[da.statut]?.label || da.statut }}
            </span>
          </div>
          <p class="text-xs text-slate-500 mt-0.5">{{ da.objet }}</p>
        </div>
        <button type="button" (click)="closeDetail()" class="rounded-lg p-1.5 text-slate-400 hover:bg-slate-200">✕</button>
      </div>

      <div class="overflow-y-auto p-6 space-y-5">
        <!-- Informations générales -->
        <div class="grid grid-cols-2 gap-3 sm:grid-cols-4 rounded-xl border border-slate-200 bg-slate-50/50 p-3.5 text-xs">
          <div><span class="text-slate-500">Demandeur :</span> <strong class="block text-slate-900">{{ da.demandeur.prenom }} {{ da.demandeur.nom }}</strong></div>
          <div><span class="text-slate-500">Service :</span> <strong class="block text-slate-900">{{ da.serviceDemandeurLibelle || '—' }}</strong></div>
          <div><span class="text-slate-500">Exercice :</span> <strong class="block text-slate-900">{{ da.exerciceAnnee }}</strong></div>
          <div><span class="text-slate-500">Type d'achat :</span> <strong class="block text-slate-900">{{ da.typeAchat }}</strong></div>
        </div>

        @if (da.justification) {
          <div class="rounded-xl border border-slate-200 bg-slate-50/30 p-3 text-xs">
            <span class="font-semibold text-slate-700">Justification :</span>
            <p class="mt-1 text-slate-600">{{ da.justification }}</p>
          </div>
        }

        <!-- Lignes du dossier -->
        <div>
          <h4 class="text-xs font-bold text-slate-900 mb-2">Lignes d'imputation budgétaire & articles</h4>
          <div class="overflow-x-auto rounded-xl border border-slate-200">
            <table class="w-full text-left text-xs">
              <thead class="bg-slate-50 text-slate-600">
                <tr><th class="p-2.5">Activité budget</th><th class="p-2.5">Description</th><th class="p-2.5 text-right">Quantité</th><th class="p-2.5 text-right">P.U. HT</th><th class="p-2.5 text-right">Total HT</th></tr>
              </thead>
              <tbody class="divide-y divide-slate-200">
                @for (l of da.lignes; track l.id) {
                  <tr>
                    <td class="p-2.5 font-mono text-slate-700">{{ l.activiteCode || '—' }}</td>
                    <td class="p-2.5 font-medium text-slate-900">{{ l.description }}</td>
                    <td class="p-2.5 text-right font-mono">{{ l.quantite }} {{ l.uniteMesure }}</td>
                    <td class="p-2.5 text-right font-mono">{{ (l.prixUnitaireEstime | number:'1.0-0') }} FCFA</td>
                    <td class="p-2.5 text-right font-mono font-bold text-slate-900">{{ (l.montantTotalHt | number:'1.0-0') }} FCFA</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <div class="mt-2 text-right text-xs">
            Total estimé HT : <strong class="text-sm font-bold font-mono text-emerald-700">{{ (da.montantEstimeHt | number:'1.0-0') }} FCFA</strong>
          </div>
        </div>

        <!-- Section 3 : Ventilation Analytique & Inter-Filiales -->
        <div class="rounded-xl border border-blue-200 bg-blue-50/30 p-4 space-y-3">
          <div class="flex items-center justify-between">
            <div>
              <h4 class="text-xs font-bold text-blue-950 flex items-center gap-1.5">
                <span>Ventilation analytique & Impacts inter-filiales</span>
                <span class="rounded-full bg-blue-100 text-blue-800 px-2 py-0.5 text-[10px] font-bold">
                  {{ ventilations().length }} répartition(s)
                </span>
              </h4>
              <p class="text-[11px] text-blue-700">Imputation analytique du coût du dossier entre entités et activités budgétaires.</p>
            </div>
            @if (da.statut !== 'COMMANDE') {
              <button type="button" (click)="openVentilationModal(da)" class="rounded-lg bg-blue-700 px-2.5 py-1 text-[11px] font-semibold text-white shadow-xs hover:bg-blue-800">
                ⚙ Gérer la ventilation
              </button>
            }
          </div>

          @if (ventilations().length > 0) {
            <div class="overflow-x-auto rounded-xl border border-blue-200 bg-white">
              <table class="w-full text-left text-xs">
                <thead class="bg-blue-50 text-blue-900">
                  <tr>
                    <th class="p-2.5">Entité impactée</th>
                    <th class="p-2.5">Activité budget ciblée</th>
                    <th class="p-2.5 text-right">Pourcentage</th>
                    <th class="p-2.5 text-right">Montant imputé</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-blue-100">
                  @for (v of ventilations(); track v.id) {
                    <tr>
                      <td class="p-2.5 font-bold text-blue-900">{{ v.entiteImpactee }}</td>
                      <td class="p-2.5 text-slate-700">
                        {{ v.activiteImpacteeCode ? (v.activiteImpacteeCode + ' — ' + v.activiteImpacteeLibelle) : '—' }}
                      </td>
                      <td class="p-2.5 text-right font-mono font-bold text-blue-800">{{ v.pourcentageApplique }}%</td>
                      <td class="p-2.5 text-right font-mono font-bold text-slate-900">{{ (v.montantImpute | number:'1.0-0') }} FCFA</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          } @else {
            <div class="rounded-lg border border-dashed border-blue-200 bg-white/60 p-3 text-center text-xs text-blue-700">
              Aucune ventilation spécifique définie (100% du coût imputé sur la société émettrice <strong class="font-bold">{{ da.entiteEmettrice }}</strong>).
            </div>
          }
        </div>

        <!-- Circuit d'Audit / Historique des Validations -->
        <div class="rounded-xl border border-slate-200 bg-slate-50/40 p-4">
          <h4 class="text-xs font-bold text-slate-900 mb-3 flex items-center gap-1.5">
            <span>Circuit de validation & Traçabilité</span>
            <span class="rounded-full bg-slate-200 px-2 py-0.5 text-[10px] text-slate-700">{{ validations().length }} décision(s)</span>
          </h4>

          @if (validations().length > 0) {
            <div class="space-y-2">
              @for (v of validations(); track v.id) {
                <div class="flex items-start justify-between rounded-lg border border-slate-200 bg-white p-3 text-xs shadow-xs">
                  <div>
                    <div class="flex items-center gap-2">
                      <span class="font-bold text-slate-900">{{ v.valideur.prenom }} {{ v.valideur.nom }}</span>
                      <span class="text-slate-400">({{ v.etape }})</span>
                      <span class="rounded-md px-2 py-0.5 text-[10px] font-bold"
                        [ngClass]="v.decision === 'APPROUVE' ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800'">
                        {{ v.decision === 'APPROUVE' ? '✓ APPROUVÉ' : '✕ REJETÉ' }}
                      </span>
                    </div>
                    @if (v.commentaire) {
                      <p class="mt-1 text-slate-600 italic font-sans">« {{ v.commentaire }} »</p>
                    }
                  </div>
                  <span class="text-[11px] text-slate-400 font-mono">{{ v.dateDecision | date:'dd/MM/yyyy HH:mm' }}</span>
                </div>
              }
            </div>
          } @else {
            <p class="text-xs text-slate-500 italic">Aucune étape de validation n'a encore été enregistrée pour cette demande.</p>
          }
        </div>

        <!-- Panel de Décision (pour les valideurs autorisés) -->
        @if ((da.statut === 'SOUMIS' || da.statut === 'EN_COURS_VALIDATION') && auth.hasPermission('demande_achat.valider')) {
          <div class="rounded-xl border-2 border-dashed border-blue-300 bg-blue-50/50 p-4 space-y-3">
            <h4 class="text-xs font-bold text-blue-900 flex items-center gap-1.5">
              <span>Examiner et valider la demande d’achat</span>
            </h4>
            <label class="block space-y-1 text-xs">Commentaire / Motif d’approbation ou de rejet
              <textarea rows="2" [(ngModel)]="decisionCommentaire" placeholder="Ex. : Besoin validé, imputation budgétaire disponible sur l'activité…" class="w-full rounded-lg border border-slate-300 bg-white p-2 text-xs"></textarea>
            </label>
            <div class="flex items-center justify-end gap-2">
              <button type="button" [disabled]="validing()" (click)="submitValidation(da.id, 'REJETE')" class="rounded-xl border border-rose-300 bg-white px-3 py-1.5 text-xs font-semibold text-rose-700 hover:bg-rose-50 disabled:opacity-50">
                ✕ Rejeter la DA
              </button>
              <button type="button" [disabled]="validing()" (click)="submitValidation(da.id, 'APPROUVE')" class="rounded-xl bg-emerald-700 px-4 py-1.5 text-xs font-semibold text-white shadow-sm hover:bg-emerald-800 disabled:opacity-50">
                ✓ Approuver la DA
              </button>
            </div>
          </div>
        }
      </div>

      <div class="flex items-center justify-between border-t border-slate-200 bg-slate-50 px-6 py-3">
        <div>
          @if (da.statut === 'APPROUVE' && auth.hasPermission('bon_commande.creer')) {
            <button type="button" (click)="openCreerBcModal(da)" class="rounded-xl bg-purple-700 px-4 py-2 text-xs font-semibold text-white shadow-sm hover:bg-purple-800">
              ⚡ Émettre le Bon de Commande Fournisseur
            </button>
          }
        </div>
        <button type="button" (click)="closeDetail()" class="rounded-xl bg-slate-800 px-4 py-2 text-xs font-semibold text-white hover:bg-slate-700">Fermer</button>
      </div>
    </div>
  </div>
}

<!-- Modal Gestion de la Ventilation Analytique -->
@if (ventilationDaTarget; as targetDa) {
  <div class="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-slate-900/60 p-4 backdrop-blur-sm" (click)="ventilationDaTarget = null">
    <div class="relative w-full max-w-2xl rounded-2xl bg-white shadow-2xl overflow-hidden" (click)="$event.stopPropagation()">
      <div class="flex items-center justify-between border-b border-slate-200 px-6 py-4 bg-blue-50">
        <div>
          <h3 class="text-sm font-bold text-blue-950">Ventilation Analytique & Inter-Filiales</h3>
          <p class="text-xs text-blue-700">Dossier : <strong class="font-mono">{{ targetDa.codeDossier }}</strong> — Montant HT : <strong class="font-mono">{{ (targetDa.montantEstimeHt | number:'1.0-0') }} FCFA</strong></p>
        </div>
        <button type="button" (click)="ventilationDaTarget = null" class="rounded-lg p-1 text-slate-400 hover:bg-slate-200">✕</button>
      </div>

      <form (ngSubmit)="saveVentilation()" class="p-6 space-y-4">
        <div class="flex items-center justify-between">
          <p class="text-xs text-slate-600">Répartissez le montant total entre les filiales / activités participantes (Total requis : 100%).</p>
          <button type="button" (click)="addVentilationRow()" class="rounded-lg border border-slate-300 bg-white px-2.5 py-1 text-xs font-semibold hover:bg-slate-50">
            ＋ Ajouter une filiale
          </button>
        </div>

        <div class="space-y-3">
          @for (row of ventilationRows; track $index) {
            <div class="rounded-xl border border-slate-200 bg-slate-50/60 p-3 space-y-2">
              <div class="flex items-center justify-between">
                <span class="text-xs font-bold text-slate-700">Imputation #{{ $index + 1 }}</span>
                <button type="button" (click)="removeVentilationRow($index)" [disabled]="ventilationRows.length === 1" class="text-xs text-rose-600 hover:underline disabled:opacity-30">
                  Supprimer
                </button>
              </div>

              <div class="grid grid-cols-1 gap-2 sm:grid-cols-2">
                <label class="block space-y-1 text-xs">Entité impactée <span class="champ-obligatoire">*</span>
                  <app-search-select required [(ngModel)]="row.entiteImpactee" [name]="'ventEnt' + $index" triggerClass="w-full rounded-lg border border-slate-300 bg-white p-2 text-xs">
                    @for (s of structures(); track s.code) { <app-search-option [value]="s.code" [label]="s.nom + ' (' + s.code + ')'" /> }
                  </app-search-select>
                </label>
                <label class="block space-y-1 text-xs">Activité budgétaire impactée
                  <app-search-select [(ngModel)]="row.activiteImpacteeId" [name]="'ventAct' + $index" triggerClass="w-full rounded-lg border border-slate-300 bg-white p-2 text-xs">
                    <app-search-option [value]="null" label="-- Même activité que le dossier --" />
                    @for (act of activites(); track act.id) { <app-search-option [value]="act.id" [label]="act.codeActivite + ' — ' + act.libelle" /> }
                  </app-search-select>
                </label>
              </div>

              <div class="grid grid-cols-2 gap-2">
                <label class="block space-y-1 text-xs">Pourcentage (%) <span class="champ-obligatoire">*</span>
                  <input type="number" min="0.1" max="100" step="0.5" required [(ngModel)]="row.pourcentageApplique" (ngModelChange)="onPourcentageChange(row)" [name]="'ventPct' + $index" class="w-full rounded-lg border border-slate-300 bg-white p-2 text-xs font-mono">
                </label>
                <label class="block space-y-1 text-xs">Montant imputé (FCFA)
                  <input type="number" readonly [value]="row.montantImpute" class="w-full rounded-lg border border-slate-200 bg-slate-100 p-2 text-xs font-mono font-bold text-slate-800">
                </label>
              </div>
            </div>
          }
        </div>

        <div class="flex items-center justify-between rounded-xl border border-slate-200 bg-slate-50 p-3 text-xs">
          <span>Somme totale des pourcentages :</span>
          <strong class="font-mono text-sm" [ngClass]="sommePourcentagesVentilation() === 100 ? 'text-emerald-700' : 'text-amber-700'">
            {{ sommePourcentagesVentilation() }}% / 100%
          </strong>
        </div>

        <div class="flex items-center justify-end gap-2 pt-2">
          <button type="button" (click)="ventilationDaTarget = null" class="rounded-xl border border-slate-300 px-3 py-2 text-xs">Annuler</button>
          <button type="submit" [disabled]="ventilationSaving() || sommePourcentagesVentilation() !== 100" class="rounded-xl bg-blue-700 px-4 py-2 text-xs font-semibold text-white shadow-sm hover:bg-blue-800 disabled:opacity-50">
            {{ ventilationSaving() ? 'Enregistrement…' : 'Valider la ventilation' }}
          </button>
        </div>
      </form>
    </div>
  </div>
}

<!-- Modal Émission Bon de Commande (Conversion DA -> BC) -->
@if (bcDaTarget; as targetDa) {
  <div class="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-slate-900/60 p-4 backdrop-blur-sm" (click)="bcDaTarget = null">
    <div class="relative w-full max-w-lg rounded-2xl bg-white shadow-2xl overflow-hidden" (click)="$event.stopPropagation()">
      <div class="flex items-center justify-between border-b border-slate-200 px-6 py-4 bg-purple-50">
        <div>
          <h3 class="text-sm font-bold text-purple-950">Émettre un Bon de Commande (BC)</h3>
          <p class="text-xs text-purple-700">À partir du dossier DA : <strong class="font-mono">{{ targetDa.codeDossier }}</strong></p>
        </div>
        <button type="button" (click)="bcDaTarget = null" class="rounded-lg p-1 text-slate-400 hover:bg-slate-200">✕</button>
      </div>

      <form (ngSubmit)="submitCreerBc(targetDa.id)" class="p-6 space-y-4">
        <label class="block space-y-1 text-xs">Sélectionner le Fournisseur retenu <span class="champ-obligatoire" aria-hidden="true">*</span>
          <app-search-select required [(ngModel)]="bcFournisseurId" name="bcFournisseurId" triggerClass="w-full rounded-lg border border-slate-300 p-2 text-xs">
            @for (f of fournisseurs(); track f.id) {
              <app-search-option [value]="f.id" [label]="f.raisonSociale + ' (IFU: ' + f.ifu + ')'" />
            }
          </app-search-select>
        </label>

        <div class="grid grid-cols-2 gap-3">
          <label class="block space-y-1 text-xs">Type de Bon de Commande
            <select [(ngModel)]="bcType" name="bcType" class="w-full rounded-lg border border-slate-300 bg-white p-2 text-xs">
              <option value="CLASSIQUE">BC Classique</option>
              <option value="CONTRAT_CADRE">Sous contrat-cadre</option>
            </select>
          </label>

          <label class="block space-y-1 text-xs">Taux TVA (%)
            <input type="number" min="0" max="100" step="1" [(ngModel)]="bcTva" name="bcTva" class="w-full rounded-lg border border-slate-300 bg-white p-2 text-xs font-mono">
          </label>
        </div>

        <div class="rounded-xl border border-slate-200 bg-slate-50 p-3.5 space-y-1.5 text-xs">
          <div class="flex justify-between text-slate-600">
            <span>Montant HT du dossier :</span>
            <strong class="font-mono text-slate-900">{{ (targetDa.montantEstimeHt | number:'1.0-0') }} FCFA</strong>
          </div>
          <div class="flex justify-between text-slate-600">
            <span>TVA calculée ({{ bcTva }}%) :</span>
            <strong class="font-mono text-slate-900">{{ ((targetDa.montantEstimeHt * (bcTva / 100)) | number:'1.0-0') }} FCFA</strong>
          </div>
          <div class="flex justify-between border-t border-slate-200 pt-1.5 font-bold text-purple-900 text-sm">
            <span>Total TTC du Bon de Commande :</span>
            <span class="font-mono">{{ ((targetDa.montantEstimeHt * (1 + (bcTva / 100))) | number:'1.0-0') }} FCFA</span>
          </div>
        </div>

        <div class="flex items-center justify-end gap-2 pt-2">
          <button type="button" (click)="bcDaTarget = null" class="rounded-xl border border-slate-300 px-3 py-2 text-xs">Annuler</button>
          <button type="submit" [disabled]="bcSaving() || !bcFournisseurId" class="rounded-xl bg-purple-700 px-4 py-2 text-xs font-semibold text-white shadow-sm hover:bg-purple-800 disabled:opacity-50">
            {{ bcSaving() ? 'Génération du BC…' : 'Valider et Générer le BC' }}
          </button>
        </div>
      </form>
    </div>
  </div>
}
}
`,
})
export class DemandesAchatListComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly achatService = inject(AchatService);
  private readonly nomenclatureService = inject(NomenclatureService);
  private readonly stockService = inject(StockService);
  private readonly serviceApi = inject(ServiceOrganisationnelService);
  private readonly structureApi = inject(StructureService);
  private readonly profilApi = inject(ProfilService);
  private readonly notifications = inject(NotificationService);
  private readonly route = inject(ActivatedRoute);

  readonly statuts = STATUTS_DA;
  readonly statutKeys = Object.keys(STATUTS_DA) as StatutDemandeAchat[];
  readonly typesAchat = TYPES_ACHAT;

  readonly demandes = signal<DemandeAchat[]>([]);
  readonly activites = signal<ActiviteCatalogue[]>([]);
  readonly articles = signal<Article[]>([]);
  readonly services = signal<ServiceOrganisationnel[]>([]);
  readonly structures = signal<Structure[]>([]);
  readonly fournisseurs = signal<Fournisseur[]>([]);
  readonly validations = signal<HistoriqueValidation[]>([]);
  readonly ventilations = signal<VentilationImpact[]>([]);

  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly total = signal(0);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly validing = signal(false);
  readonly bcSaving = signal(false);
  readonly ventilationSaving = signal(false);
  readonly error = signal('');
  readonly formOpen = signal(false);
  readonly editingId = signal('');

  readonly statsEnAttente = computed(() =>
    this.demandes().filter(d => d.statut === 'SOUMIS' || d.statut === 'EN_COURS_VALIDATION').length
  );
  readonly statsApprouves = computed(() =>
    this.demandes().filter(d => d.statut === 'APPROUVE' || d.statut === 'COMMANDE').length
  );
  readonly statsMontantTotal = computed(() =>
    this.demandes().reduce((acc, d) => acc + (d.montantEstimeHt || 0), 0)
  );

  recherche = '';
  statutFilter = '';
  typeFilter = '';
  detailRecord: DemandeAchat | null = null;
  decisionCommentaire = '';
  draft: DemandeAchatRequest = this.emptyDraft();

  // Émission Bon de commande modal
  bcDaTarget: DemandeAchat | null = null;
  bcFournisseurId = '';
  bcType = 'CLASSIQUE';
  bcTva = 18;

  // Gestion Ventilation Analytique
  ventilationDaTarget: DemandeAchat | null = null;
  ventilationRows: VentilationImpactRequest[] = [];

  ngOnInit(): void {
    this.load();
    this.loadReferences();

    // Passerelle directe Module 3 Stock -> Module 4 Achat (si articleId passé en URL)
    this.route.queryParams.subscribe((params) => {
      if (params['articleId']) {
        this.openFormForArticle(params['articleId']);
      }
    });
  }

  loadReferences(): void {
    this.nomenclatureService.listerActivites().subscribe({ next: (res) => this.activites.set(res), error: () => {} });
    this.stockService.listerArticles({ page: 0, size: 200 }).subscribe({ next: (res) => this.articles.set(res.content), error: () => {} });
    this.serviceApi.lister(undefined, true).subscribe({ next: (res) => this.services.set(res), error: () => {} });
    this.structureApi.listerOuFiltrer().subscribe({ next: (res) => this.structures.set(res), error: () => {} });
    this.achatService.listerFournisseurs({ page: 0, size: 200, actif: true }).subscribe({
      next: (res) => this.fournisseurs.set(res.content),
      error: () => {},
    });
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.achatService.listerDemandesAchat({
      page: this.page(),
      size: 20,
      recherche: this.recherche.trim(),
      statut: this.statutFilter || undefined,
      typeAchat: this.typeFilter || undefined,
      sort: 'createdAt,desc',
    }).subscribe({
      next: (res) => {
        this.demandes.set(res.content);
        this.totalPages.set(res.totalPages);
        this.total.set(res.totalElements);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }

  search(): void { this.page.set(0); this.load(); }
  reset(): void { this.recherche = ''; this.statutFilter = ''; this.typeFilter = ''; this.search(); }
  goTo(p: number): void { this.page.set(p); this.load(); }

  openDetail(da: DemandeAchat): void {
    this.detailRecord = da;
    this.decisionCommentaire = '';
    this.validations.set([]);
    this.ventilations.set([]);
    this.achatService.obtenirHistoriqueValidations(da.id).subscribe({
      next: (res) => this.validations.set(res),
      error: () => {},
    });
    this.achatService.obtenirVentilations(da.id).subscribe({
      next: (res) => this.ventilations.set(res),
      error: () => {},
    });
  }

  closeDetail(): void {
    this.detailRecord = null;
    this.validations.set([]);
    this.ventilations.set([]);
    this.decisionCommentaire = '';
  }

  openVentilationModal(da: DemandeAchat): void {
    this.ventilationDaTarget = da;
    this.achatService.obtenirVentilations(da.id).subscribe({
      next: (res) => {
        if (res && res.length > 0) {
          this.ventilationRows = res.map((v) => ({
            entiteImpactee: v.entiteImpactee,
            activiteImpacteeId: v.activiteImpacteeId,
            montantImpute: v.montantImpute,
            pourcentageApplique: v.pourcentageApplique,
            statutBlocage: v.statutBlocage || false,
          }));
        } else {
          this.ventilationRows = [
            {
              entiteImpactee: da.entiteEmettrice,
              activiteImpacteeId: da.lignes[0]?.activiteId || null,
              montantImpute: da.montantEstimeHt,
              pourcentageApplique: 100,
              statutBlocage: false,
            },
          ];
        }
      },
      error: () => {
        this.ventilationRows = [
          {
            entiteImpactee: da.entiteEmettrice,
            activiteImpacteeId: da.lignes[0]?.activiteId || null,
            montantImpute: da.montantEstimeHt,
            pourcentageApplique: 100,
            statutBlocage: false,
          },
        ];
      },
    });
  }

  addVentilationRow(): void {
    const restant = Math.max(0, 100 - this.sommePourcentagesVentilation());
    const da = this.ventilationDaTarget;
    const montant = da ? Math.round((da.montantEstimeHt * restant) / 100) : 0;
    this.ventilationRows.push({
      entiteImpactee: this.structures()[0]?.code || 'UAB_VIE',
      activiteImpacteeId: this.activites()[0]?.id || null,
      montantImpute: montant,
      pourcentageApplique: restant,
      statutBlocage: false,
    });
  }

  removeVentilationRow(idx: number): void {
    this.ventilationRows.splice(idx, 1);
  }

  onPourcentageChange(row: VentilationImpactRequest): void {
    if (this.ventilationDaTarget) {
      row.montantImpute = Math.round((this.ventilationDaTarget.montantEstimeHt * (Number(row.pourcentageApplique) || 0)) / 100);
    }
  }

  sommePourcentagesVentilation(): number {
    return this.ventilationRows.reduce((sum, r) => sum + (Number(r.pourcentageApplique) || 0), 0);
  }

  saveVentilation(): void {
    if (!this.ventilationDaTarget) return;
    const sum = this.sommePourcentagesVentilation();
    if (Math.abs(sum - 100) > 0.5) {
      this.notifications.warning(`La somme des pourcentages doit être égale à 100% (actuel: ${sum}%).`);
      return;
    }
    this.ventilationSaving.set(true);
    const daId = this.ventilationDaTarget.id;
    this.achatService.enregistrerVentilations(daId, this.ventilationRows).subscribe({
      next: (res) => {
        this.ventilationSaving.set(false);
        this.ventilations.set(res);
        this.notifications.success('Ventilation analytique enregistrée avec succès.');
        this.ventilationDaTarget = null;
      },
      error: (e) => {
        this.ventilationSaving.set(false);
        this.notifications.error(errorMessage(e));
      },
    });
  }

  submitValidation(daId: string, decision: 'APPROUVE' | 'REJETE'): void {
    this.validing.set(true);
    const dto: DecisionValidationRequest = {
      decision,
      commentaire: this.decisionCommentaire.trim() || undefined,
    };
    this.achatService.validerDemandeAchat(daId, dto).subscribe({
      next: (updatedDa) => {
        this.validing.set(false);
        this.notifications.success(decision === 'APPROUVE' ? 'Demande d’achat approuvée avec succès.' : 'Demande d’achat rejetée.');
        this.detailRecord = updatedDa;
        this.load();
        this.achatService.obtenirHistoriqueValidations(daId).subscribe({
          next: (res) => this.validations.set(res),
        });
      },
      error: (e) => {
        this.validing.set(false);
        this.notifications.error(errorMessage(e));
      },
    });
  }

  openCreerBcModal(da: DemandeAchat): void {
    this.bcDaTarget = da;
    this.bcFournisseurId = this.fournisseurs()[0]?.id || '';
    this.bcType = 'CLASSIQUE';
    this.bcTva = 18;
  }

  submitCreerBc(daId: string): void {
    if (!this.bcFournisseurId) {
      this.notifications.warning('Veuillez sélectionner un fournisseur.');
      return;
    }
    this.bcSaving.set(true);
    this.achatService.creerBcDepuisDa(daId, {
      fournisseurId: this.bcFournisseurId,
      typeBc: this.bcType,
      tauxTva: this.bcTva,
    }).subscribe({
      next: (bc) => {
        this.bcSaving.set(false);
        this.bcDaTarget = null;
        if (this.detailRecord && this.detailRecord.id === daId) {
          this.closeDetail();
        }
        this.notifications.success(`Bon de commande ${bc.codeBc} émis avec succès.`);
        this.load();
      },
      error: (e) => {
        this.bcSaving.set(false);
        this.notifications.error(errorMessage(e));
      },
    });
  }

  openForm(): void {
    this.editingId.set('');
    this.draft = this.emptyDraft();
    this.profilApi.profil().subscribe({
      next: (p) => {
        if (p.structureCode) {
          this.draft.entiteEmettrice = p.structureCode;
          this.draft.entiteBeneficiaire = p.structureCode;
        }
        if (p.serviceLibelle) {
          const s = this.services().find((srv) => srv.libelle.trim().toLowerCase() === p.serviceLibelle!.trim().toLowerCase());
          if (s) this.draft.serviceDemandeurId = s.id;
        }
      },
      error: () => {},
    });
    this.formOpen.set(true);
  }

  openFormForArticle(articleId: string): void {
    this.openForm();
    this.draft.typeAchat = 'STOCKABLE';
    this.stockService.listerArticles({ id: articleId, page: 0, size: 1 }).subscribe({
      next: (res) => {
        const art = res.content.find((a) => a.id === articleId);
        if (art) {
          this.draft.objet = `Réapprovisionnement de stock : ${art.designation} (${art.codeArticle})`;
          this.draft.lignes = [{
            activiteId: this.activites()[0]?.id || '',
            articleId: art.id,
            description: `Réapprovisionnement ${art.designation}`,
            uniteMesure: art.uniteMesure,
            quantite: (art.seuilReapprovisionnement || 10) * 2,
            prixUnitaireEstime: art.prixUnitaireMoyenPondere || 1000,
          }];
        }
      },
      error: () => {},
    });
  }

  addLigne(): void {
    this.draft.lignes = [
      ...this.draft.lignes,
      {
        activiteId: this.activites()[0]?.id || '',
        articleId: null,
        description: '',
        uniteMesure: 'Unité',
        quantite: 1,
        prixUnitaireEstime: 0,
      },
    ];
  }

  removeLigne(idx: number): void {
    this.draft.lignes = this.draft.lignes.filter((_, i) => i !== idx);
  }

  onArticleChange(l: LigneDemandeAchatRequest, articleId: string | null): void {
    if (!articleId) return;
    const art = this.articles().find((a) => a.id === articleId);
    if (art) {
      if (!l.description) l.description = art.designation;
      if (!l.uniteMesure) l.uniteMesure = art.uniteMesure;
      if (!l.prixUnitaireEstime && art.prixUnitaireMoyenPondere) l.prixUnitaireEstime = art.prixUnitaireMoyenPondere;
    }
  }

  totalHtCalcul(): number {
    return this.draft.lignes.reduce((sum, l) => sum + ((l.quantite || 0) * (l.prixUnitaireEstime || 0)), 0);
  }

  save(): void {
    if (!this.draft.objet.trim()) {
      this.notifications.warning('L’objet de la demande est obligatoire.');
      return;
    }
    if (!this.draft.lignes.length) {
      this.notifications.warning('Veuillez ajouter au moins une ligne de besoin.');
      return;
    }

    this.saving.set(true);
    const op = this.editingId()
      ? this.achatService.modifierDemandeAchat(this.editingId(), this.draft)
      : this.achatService.creerDemandeAchat(this.draft);

    op.subscribe({
      next: () => {
        this.saving.set(false);
        this.formOpen.set(false);
        this.notifications.success(this.editingId() ? 'Demande d’achat modifiée.' : 'Demande d’achat soumise.');
        this.load();
      },
      error: (e) => {
        this.saving.set(false);
        this.notifications.error(errorMessage(e));
      },
    });
  }

  private emptyDraft(): DemandeAchatRequest {
    return {
      entiteEmettrice: 'UAB_VIE',
      entiteBeneficiaire: 'UAB_VIE',
      typeAchat: 'STOCKABLE',
      objet: '',
      justification: '',
      serviceDemandeurId: null,
      exerciceAnnee: new Date().getFullYear(),
      lignes: [
        {
          activiteId: '',
          articleId: null,
          description: '',
          uniteMesure: 'Unité',
          quantite: 1,
          prixUnitaireEstime: 0,
        },
      ],
    };
  }
}
