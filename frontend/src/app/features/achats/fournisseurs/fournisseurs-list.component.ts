import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { AchatService } from '../../../core/services/achat.service';
import { Fournisseur, FournisseurRequest } from '../../../core/models/achat/fournisseur.model';
import { PageHeaderComponent } from '../../../shared/components/page-header.component';
import { RowActionsComponent } from '../../../shared/components/row-actions.component';
import { FormPageComponent } from '../../../shared/components/form-page.component';
import { PaginationBarComponent } from '../../../shared/components/pagination-bar.component';
import { RecordDetailsDialogComponent } from '../../../shared/components/record-details-dialog.component';
import { DataActionsComponent } from '../../../shared/components/data-actions.component';
import { errorMessage } from '../../../core/utils/error-message';

@Component({
  selector: 'app-fournisseurs-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    PageHeaderComponent,
    RowActionsComponent,
    FormPageComponent,
    PaginationBarComponent,
    RecordDetailsDialogComponent,
    DataActionsComponent,
  ],
  template: `
@if (formOpen()) {
<app-form-page section="Fournisseurs" [title]="editingId() ? 'Modifier le fournisseur' : 'Nouveau fournisseur'" [mode]="editingId() ? 'Modification' : 'Création'" subtitle="Référentiel des prestataires, vendeurs et sous-traitants agréés de l'entreprise." (back)="formOpen.set(false)">
  <form (ngSubmit)="save()">
    <section class="space-y-4 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <h3 class="text-sm font-semibold text-slate-900">Identification légale</h3>
      <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <label class="block space-y-1 text-xs">Code fournisseur
          <input maxlength="50" [(ngModel)]="draft.codeFournisseur" name="codeFournisseur" placeholder="Laisser vide pour génération automatique" class="w-full rounded-lg border border-slate-300 p-2 uppercase font-mono">
        </label>
        <label class="block space-y-1 text-xs">Identifiant Fiscal Unique (IFU) <span class="champ-obligatoire" aria-hidden="true">*</span>
          <input required maxlength="50" [(ngModel)]="draft.ifu" name="ifu" placeholder="Ex. : 00012345A" class="w-full rounded-lg border border-slate-300 p-2 font-mono uppercase">
        </label>
        <label class="block space-y-1 text-xs sm:col-span-2">Raison sociale / Nom commercial <span class="champ-obligatoire" aria-hidden="true">*</span>
          <input required maxlength="200" [(ngModel)]="draft.raisonSociale" name="raisonSociale" placeholder="Ex. : CFAO MOTORS BURKINA FASO" class="w-full rounded-lg border border-slate-300 p-2">
        </label>
      </div>
    </section>

    <section class="mt-4 space-y-4 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <h3 class="text-sm font-semibold text-slate-900">Coordonnées de contact</h3>
      <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <label class="block space-y-1 text-xs">Téléphone
          <input maxlength="50" [(ngModel)]="draft.telephone" name="telephone" placeholder="Ex. : +226 25 30 00 00" class="w-full rounded-lg border border-slate-300 p-2">
        </label>
        <label class="block space-y-1 text-xs">Adresse e-mail
          <input type="email" maxlength="150" [(ngModel)]="draft.email" name="email" placeholder="contact@fournisseur.bf" class="w-full rounded-lg border border-slate-300 p-2">
        </label>
        <label class="block space-y-1 text-xs sm:col-span-2">Adresse physique / Siège social
          <textarea rows="2" [(ngModel)]="draft.adresse" name="adresse" placeholder="Avenue ou quartier, Ouagadougou" class="w-full rounded-lg border border-slate-300 p-2"></textarea>
        </label>
      </div>
      <label class="flex items-center gap-2 text-xs font-medium text-slate-700">
        <input type="checkbox" [(ngModel)]="draft.actif" name="actif" class="rounded border-slate-300 text-green-700"> Fournisseur actif (éligible pour les commandes et consultations)
      </label>
    </section>

    <div class="form-page-actions mt-4 flex items-center justify-end gap-2">
      <button type="button" (click)="formOpen.set(false)" class="rounded-lg border border-slate-300 px-3 py-2 text-xs">Annuler</button>
      <button [disabled]="saving()" class="rounded-lg bg-green-700 px-4 py-2 text-xs font-semibold text-white shadow-sm transition hover:bg-green-800 disabled:opacity-50">
        {{ saving() ? 'Enregistrement…' : 'Enregistrer' }}
      </button>
    </div>
  </form>
</app-form-page>
} @else {
<div class="mx-auto max-w-7xl space-y-6">
  <app-page-header icon="fournisseurs" module="Gestion des achats" title="Référentiel Fournisseurs" description="Répertoire officiel des tiers, prestataires agréés et vendeurs de biens et services.">
    <div class="flex flex-wrap items-center gap-2">
      <app-data-actions
        ressource="fournisseurs"
        exportPermission="fournisseur.lire"
        [ids]="exportIds()" />
      @if (auth.hasPermission('fournisseur.creer')) {
        <button type="button" (click)="openForm()" class="whitespace-nowrap rounded-xl bg-green-700 px-4 py-2 text-xs font-semibold text-white shadow-sm transition hover:bg-green-800">
          ＋ Nouveau fournisseur
        </button>
      }
    </div>
  </app-page-header>

  @if (error()) { <div role="alert" class="rounded-xl border border-rose-300 bg-rose-50 p-3 text-sm text-rose-700">{{ error() }}</div> }

  <!-- Cartes KPI (Design Djibril stats-grid avec liseré supérieur) -->
  <section class="stats-grid grid gap-3 sm:grid-cols-3">
    <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" (click)="actifFilter = ''; search()">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Fournisseurs déclarés</p>
      <p class="mt-2 text-2xl font-bold font-mono text-slate-900">{{ total() }}</p>
      <p class="mt-1 text-[11px] text-slate-500">Tiers et prestataires référencés</p>
    </article>

    <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" (click)="actifFilter = 'true'; search()">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Fournisseurs actifs</p>
      <p class="mt-2 text-2xl font-bold font-mono text-emerald-700">{{ statsActifs() }}</p>
      <p class="mt-1 text-[11px] text-slate-500">Agréés pour commandes et consultations</p>
    </article>

    <article class="cursor-pointer rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300" (click)="actifFilter = 'false'; search()">
      <p class="text-[10px] uppercase tracking-wider text-slate-500 font-semibold whitespace-nowrap">Inactifs / Suspendus</p>
      <p class="mt-2 text-2xl font-bold font-mono" [class.text-amber-500]="statsInactifs() > 0" [class.text-slate-900]="statsInactifs() === 0">{{ statsInactifs() }}</p>
      <p class="mt-1 text-[11px] text-slate-500">Non éligibles aux nouveaux achats</p>
    </article>
  </section>

  <div class="flex flex-wrap items-center gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
    <input type="text" [(ngModel)]="recherche" (keyup.enter)="search()" placeholder="Recherche par raison sociale, IFU ou code…" class="min-w-[280px] flex-1 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs" />
    <select [(ngModel)]="actifFilter" (change)="search()" class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs">
      <option value="">Tous les statuts</option>
      <option value="true">Actifs uniquement</option>
      <option value="false">Inactifs / Suspendus</option>
    </select>
    <button type="button" (click)="search()" class="rounded-xl bg-blue-700 px-3 py-2 text-xs font-medium text-white hover:bg-blue-800">Rechercher</button>
    <button type="button" (click)="reset()" class="rounded-xl bg-slate-100 px-3 py-2 text-xs text-slate-600 hover:bg-slate-200">Réinitialiser</button>
  </div>

  <section class="overflow-hidden rounded-2xl border border-slate-200 bg-white">
    <div class="overflow-x-auto">
      <table class="w-full min-w-[850px] text-left text-xs">
        <thead class="bg-slate-50 text-slate-600 whitespace-nowrap">
          <tr>
            <th class="p-3 whitespace-nowrap">Code</th>
            <th class="p-3 whitespace-nowrap">Raison Sociale</th>
            <th class="p-3 whitespace-nowrap">IFU</th>
            <th class="p-3 whitespace-nowrap">Téléphone</th>
            <th class="p-3 whitespace-nowrap">E-mail</th>
            <th class="p-3 whitespace-nowrap">Statut</th>
            <th class="p-3 text-right whitespace-nowrap">Actions</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-200">
          @for (f of fournisseurs(); track f.id) {
            <tr class="hover:bg-slate-50/60 transition">
              <td class="p-3 font-mono font-semibold text-blue-700 cursor-pointer hover:underline" (click)="detailRecord = f">{{ f.codeFournisseur }}</td>
              <td class="p-3 font-semibold text-slate-900">{{ f.raisonSociale }}</td>
              <td class="p-3 font-mono text-slate-700">{{ f.ifu }}</td>
              <td class="p-3">{{ f.telephone || '—' }}</td>
              <td class="p-3">{{ f.email || '—' }}</td>
              <td class="p-3">
                @if (f.actif) {
                  <span class="rounded-full bg-emerald-50 px-2 py-0.5 text-[11px] font-semibold text-emerald-700 border border-emerald-200">Actif</span>
                } @else {
                  <span class="rounded-full bg-slate-100 px-2 py-0.5 text-[11px] font-semibold text-slate-600 border border-slate-200">Inactif</span>
                }
              </td>
              <td class="p-3">
                <app-row-actions
                  [label]="f.raisonSociale"
                  [canEdit]="auth.hasPermission('fournisseur.modifier')"
                  (view)="detailRecord = f"
                  (edit)="edit(f)" />
              </td>
            </tr>
          } @empty {
            <tr><td colspan="7" class="p-6 text-center text-slate-500">{{ loading() ? 'Chargement des fournisseurs…' : 'Aucun fournisseur trouvé.' }}</td></tr>
          }
        </tbody>
      </table>
    </div>
    <app-pagination-bar [page]="page()" [totalPages]="totalPages()" [total]="total()" (pageChange)="goTo($event)" />
  </section>
</div>

@if (detailRecord) {
  <app-record-details-dialog title="Fournisseur" [record]="detailRecord" (close)="detailRecord = null" />
}
}
`,
})
export class FournisseursListComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly achatService = inject(AchatService);
  private readonly notifications = inject(NotificationService);

  readonly fournisseurs = signal<Fournisseur[]>([]);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly total = signal(0);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly formOpen = signal(false);
  readonly editingId = signal('');
  readonly exportIds = computed(() => this.fournisseurs().map((f) => f.id));
  readonly statsActifs = computed(() => this.fournisseurs().filter(f => f.actif).length);
  readonly statsInactifs = computed(() => this.fournisseurs().filter(f => !f.actif).length);

  recherche = '';
  actifFilter = '';
  detailRecord: object | null = null;
  draft: FournisseurRequest = this.emptyDraft();

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    const actifParam = this.actifFilter === 'true' ? true : this.actifFilter === 'false' ? false : undefined;
    this.achatService.listerFournisseurs({
      page: this.page(),
      size: 20,
      recherche: this.recherche.trim(),
      actif: actifParam,
      sort: 'raisonSociale,asc',
    }).subscribe({
      next: (res) => {
        this.fournisseurs.set(res.content);
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
  reset(): void { this.recherche = ''; this.actifFilter = ''; this.search(); }
  goTo(p: number): void { this.page.set(p); this.load(); }

  openForm(): void {
    this.editingId.set('');
    this.draft = this.emptyDraft();
    this.formOpen.set(true);
  }

  edit(f: Fournisseur): void {
    this.editingId.set(f.id);
    this.draft = {
      codeFournisseur: f.codeFournisseur,
      raisonSociale: f.raisonSociale,
      ifu: f.ifu,
      email: f.email || '',
      telephone: f.telephone || '',
      adresse: f.adresse || '',
      actif: f.actif,
    };
    this.formOpen.set(true);
  }

  save(): void {
    if (!this.draft.raisonSociale.trim()) {
      this.notifications.warning('La raison sociale est requise.');
      return;
    }
    if (!this.draft.ifu.trim()) {
      this.notifications.warning('Le numéro IFU est requis.');
      return;
    }

    this.saving.set(true);
    const op = this.editingId()
      ? this.achatService.modifierFournisseur(this.editingId(), this.draft)
      : this.achatService.creerFournisseur(this.draft);

    op.subscribe({
      next: () => {
        this.saving.set(false);
        this.formOpen.set(false);
        this.notifications.success(this.editingId() ? 'Fournisseur mis à jour.' : 'Fournisseur enregistré.');
        this.load();
      },
      error: (e) => {
        this.saving.set(false);
        this.notifications.error(errorMessage(e));
      },
    });
  }

  private emptyDraft(): FournisseurRequest {
    return {
      codeFournisseur: '',
      raisonSociale: '',
      ifu: '',
      email: '',
      telephone: '',
      adresse: '',
      actif: true,
    };
  }
}
