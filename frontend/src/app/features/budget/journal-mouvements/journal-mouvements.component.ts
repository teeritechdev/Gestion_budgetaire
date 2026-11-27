import { PageHeaderComponent } from '../../../shared/components/page-header.component';
import { DataActionsComponent } from '../../../shared/components/data-actions.component';
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BudgetService } from '../../../core/services/budget.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { JournalMouvementsBudgetaire } from '../../../core/models/budget/journal-mouvements.model';

@Component({
  selector: 'app-journal-mouvements', standalone: true, imports: [PageHeaderComponent, DataActionsComponent, CommonModule, DatePipe, FormsModule],
  template: `
    <div class="mx-auto max-w-7xl space-y-6">
      <app-page-header icon="journal" module="Répartition & contrôle" title="Journal des mouvements" description="L’historique fidèle de chaque mouvement budgétaire validé.">
<app-data-actions ressource="journal-mouvements" exportPermission="journal.exporter" [ids]="exportIds()" /><button (click)="refresh()" class="rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-700 hover:bg-slate-100">↻ Actualiser</button>
</app-page-header>
      @if (error()) {<p class="rounded-xl border border-amber-500/20 bg-amber-500/5 p-3 text-xs text-amber-200">{{ error() }}</p>}
      <section class="stats-grid grid gap-3 sm:grid-cols-3">
<article class="rounded-2xl border border-slate-200 bg-white p-4"><p class="text-[10px] uppercase tracking-wider text-slate-500">Mouvements affichés</p><p class="mt-2 text-2xl font-bold text-white">{{ filtered().length }}</p></article><article class="rounded-2xl border border-slate-200 bg-white p-4"><p class="text-[10px] uppercase tracking-wider text-slate-500">Variations positives</p><p class="mt-2 text-2xl font-bold text-blue-300">{{ positiveTotal() | number: '1.0-0' }} FCFA</p></article><article class="rounded-2xl border border-slate-200 bg-white p-4"><p class="text-[10px] uppercase tracking-wider text-slate-500">Variations négatives</p><p class="mt-2 text-2xl font-bold text-rose-300">{{ negativeTotal() | number: '1.0-0' }} FCFA</p></article></section>
      <section class="overflow-hidden rounded-2xl border border-slate-200 bg-white"><div class="grid gap-3 border-b border-slate-200 p-4 sm:grid-cols-[180px_220px_1fr_auto]"><select [(ngModel)]="yearFilter" class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-xs text-slate-700"><option value="">Toutes les années</option>@for (year of years(); track year) {<option [value]="year">{{ year }}</option>}</select><select [(ngModel)]="typeFilter" class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-xs text-slate-700"><option value="">Tous les mouvements</option>@for (type of types(); track type) {<option [value]="type">{{ typeLabel(type) }}</option>}</select><input [(ngModel)]="search" placeholder="Rechercher une structure ou référence…" class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-xs text-white outline-none placeholder:text-slate-600 focus:border-amber-500"><button (click)="refresh()" class="rounded-xl border border-slate-300 px-3 py-2 text-xs text-slate-700 hover:bg-slate-100">↻</button></div><div class="overflow-x-auto"><table class="w-full min-w-[900px] text-left text-xs"><thead class="bg-white text-[10px] uppercase tracking-wider text-slate-500"><tr><th class="px-5 py-3">Date / exercice</th><th class="px-4 py-3">Mouvement</th><th class="px-4 py-3">Structure / allocation</th><th class="px-4 py-3 text-right">Avant</th><th class="px-4 py-3 text-right">Variation</th><th class="px-4 py-3 text-right">Nouveau montant</th><th class="px-5 py-3">Référence</th></tr></thead><tbody class="divide-y divide-slate-200/70">@for (entry of filtered(); track entry.id) {<tr class="hover:bg-slate-50"><td class="px-5 py-3"><p class="text-slate-800">{{ entry.createdAt | date:'dd/MM/yyyy HH:mm' }}</p><p class="mt-1 text-[9px] text-slate-500">Exercice {{ entry.anneeExercice }}</p></td><td class="px-4 py-3"><span class="rounded-full px-2.5 py-1 text-[9px]" [class.bg-blue-500/10]="entry.variation >= 0" [class.text-blue-300]="entry.variation >= 0" [class.bg-rose-500/10]="entry.variation < 0" [class.text-rose-300]="entry.variation < 0">{{ typeLabel(entry.typeMouvement) }}</span></td><td class="px-4 py-3"><p class="font-medium text-white">{{ entry.allocationStructure?.structure?.libelle || 'Structure non renseignée' }}</p><p class="mt-1 text-[9px] text-slate-500">{{ entry.allocationStructure?.structure?.code || entry.allocationStructure?.id }}</p></td><td class="px-4 py-3 text-right text-slate-600">{{ entry.montantAnterieur | number: '1.0-0' }} FCFA</td><td class="px-4 py-3 text-right font-semibold" [class.text-blue-300]="entry.variation >= 0" [class.text-rose-300]="entry.variation < 0">{{ entry.variation > 0 ? '+' : '' }}{{ entry.variation | number: '1.0-0' }} FCFA</td><td class="px-4 py-3 text-right font-semibold text-white">{{ entry.nouveauMontant | number: '1.0-0' }} FCFA</td><td class="px-5 py-3 font-mono text-[10px] text-amber-200">{{ entry.referenceDocument || '—' }}</td></tr>}@empty {<tr><td colspan="7" class="p-12 text-center"><p class="text-sm font-medium text-white">Aucun mouvement budgétaire</p><p class="mt-1 text-xs text-slate-500">Les mouvements sont inscrits lorsqu’une révision budgétaire est validée.</p></td></tr>}</tbody></table></div><div class="border-t border-slate-200 px-5 py-3 text-[10px] text-slate-500">Journal consultatif · les mouvements sont créés par le backend lors de la validation des révisions</div></section>
    </div>`
})
export class JournalMouvementsComponent implements OnInit {
  /** Lignes affichées (filtres et tri appliqués) : ce sont elles qui sont exportées. */
  exportIds(): (string | undefined)[] {
    return this.filtered().map((item) => item.id);
  }
  private readonly budget = inject(BudgetService); private readonly auth = inject(AuthService); private readonly notifications = inject(NotificationService);
  readonly entries = signal<JournalMouvementsBudgetaire[]>([]); readonly error = signal('');
  yearFilter = ''; typeFilter = ''; search = '';
  ngOnInit(): void { this.refresh(); }
  years(): number[] { return [...new Set(this.entries().map(item => item.anneeExercice))].sort((a, b) => b - a); }
  types(): string[] { return [...new Set(this.entries().map(item => item.typeMouvement))].sort(); }
  filtered(): JournalMouvementsBudgetaire[] { const q = this.search.trim().toLocaleLowerCase('fr'); return [...this.entries()].filter(item => (!this.yearFilter || item.anneeExercice === Number(this.yearFilter)) && (!this.typeFilter || item.typeMouvement === this.typeFilter) && (!q || `${item.referenceDocument || ''} ${item.allocationStructure?.structure?.code || ''} ${item.allocationStructure?.structure?.libelle || ''}`.toLocaleLowerCase('fr').includes(q))).sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()); }
  positiveTotal(): number { return this.filtered().filter(item => item.variation > 0).reduce((sum, item) => sum + Number(item.variation), 0); }
  negativeTotal(): number { return this.filtered().filter(item => item.variation < 0).reduce((sum, item) => sum + Math.abs(Number(item.variation)), 0); }
  typeLabel(type: string): string { return ({ VIREMENT_INTERNE: 'Virement interne', EXTENSION: 'Extension' } as Record<string, string>)[type] || type.replaceAll('_', ' '); }
  refresh(): void { this.error.set(''); this.budget.listerMouvements().subscribe({ next: data => this.entries.set(data), error: () => this.error.set('Le journal est inaccessible. Vérifiez la permission journal.lire et la disponibilité du backend.') }); }
}
