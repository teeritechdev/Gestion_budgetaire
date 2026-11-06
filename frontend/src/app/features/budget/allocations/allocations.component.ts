import { PageHeaderComponent } from '../../../shared/components/page-header.component';
import { IMPORT_FORMATS } from '../../../core/models/import-formats';
import { ImportCsvPageComponent } from '../../../shared/components/import-csv-page.component';
import { DataActionsComponent } from '../../../shared/components/data-actions.component';
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SearchOptionDirective, SearchSelectComponent } from '../../../shared/components/search-select.component';
import { catchError, forkJoin, of } from 'rxjs';
import { BudgetService } from '../../../core/services/budget.service';
import { NomenclatureService } from '../../../core/services/nomenclature.service';
import { StructureService } from '../../../core/services/structure.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ExerciceBudgetaire } from '../../../core/models/budget/exercice-budgetaire.model';
import { BudgetAllocationOs, BudgetAllocationOp, BudgetAllocationActivite, BudgetAllocationStructure } from '../../../core/models/budget/budget-allocation.model';
import { ObjectifStrategique } from '../../../core/models/budget/objectif-strategique.model';
import { ObjectifOperationnel } from '../../../core/models/budget/objectif-operationnel.model';
import { ActiviteCatalogue } from '../../../core/models/budget/activite-catalogue.model';
import { Structure } from '../../../core/models/structure.model';

@Component({
  selector: 'app-allocations', standalone: true, imports: [SearchSelectComponent, SearchOptionDirective, PageHeaderComponent, DataActionsComponent, ImportCsvPageComponent, CommonModule, FormsModule],
  template: `
@if (importOpen()) {
<app-import-csv-page section="Allocations budgétaires" [format]="importFormats.allocations" (back)="importOpen.set(false)" (imported)="refresh()" />
} @else {
    <div class="mx-auto max-w-[1450px] space-y-6">
      <app-page-header icon="allocations" module="Gestion budgétaire" title="Allocations budgétaires" description="Répartissez les crédits de l’enveloppe votée jusqu’aux structures, puis suivez leur exécution.">
<app-data-actions ressource="allocations" exportPermission="allocation_os.exporter" [ids]="exportIds()" [importPermissions]="['allocation.importer']" [importFormat]="importFormats.allocations" (importer)="importOpen.set(true)" /><button (click)="refresh()" [disabled]="loading()" class="rounded-xl border border-slate-300 bg-slate-100 px-4 py-2.5 text-xs font-medium text-slate-800 hover:bg-slate-100 disabled:opacity-50">{{ loading() ? 'Actualisation…' : '↻ Actualiser les données' }}</button>
</app-page-header>

      @if (partial()) {<div class="rounded-xl border border-amber-500/20 bg-amber-500/5 p-3 text-xs text-amber-200">Certaines données ne sont pas accessibles avec vos habilitations. Les étapes autorisées restent disponibles.</div>}
      @if (error()) {<div class="flex items-center justify-between rounded-xl border border-rose-500/20 bg-rose-500/5 p-4 text-xs text-rose-200"><span>{{ error() }}</span><button (click)="refresh()" class="underline">Réessayer</button></div>}

      <section class="grid gap-4 xl:grid-cols-[1.2fr_.8fr]">
        <div class="rounded-2xl border border-slate-200 bg-white p-5"><div class="flex items-start gap-3"><span class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-blue-500/10 text-sm font-bold text-blue-300">1</span><div class="flex-1"><h2 class="text-sm font-semibold text-white">Choisir un exercice budgétaire</h2><p class="mt-1 text-[10px] text-slate-500">Toutes les répartitions partiront de cette enveloppe annuelle.</p><app-search-select [(ngModel)]="exerciseId" (ngModelChange)="onExerciseChange($event)" placeholder="Sélectionner un exercice…" triggerClass="mt-4 w-full rounded-xl border border-slate-300 bg-slate-50 px-3 py-3 text-sm text-white outline-none focus:border-blue-500">@for (exercise of exercises(); track exercise.id) {<app-search-option [value]="exercise.id" [label]="exercise.annee + ' · ' + (exercise.planStrategique?.libelle || 'Plan budgétaire') + ' · ' + statusLabel(exercise.statut)" />}</app-search-select>@if (!exercises().length && !loading()) {<p class="mt-2 text-[10px] text-amber-300">Aucun exercice disponible. Créez d’abord un exercice budgétaire.</p>}</div></div></div>
        @if (selectedExercise()) {<div class="grid grid-cols-2 gap-3 rounded-2xl border border-slate-200 bg-white p-5"><div class="col-span-2 flex items-center justify-between"><div><p class="text-[10px] uppercase tracking-wider text-slate-500">Enveloppe de l’exercice {{ selectedExercise()?.annee }}</p><p class="mt-1 text-2xl font-bold text-white">{{ selectedExercise()?.montantTotalVote | number: '1.0-0' }} FCFA</p></div><span class="rounded-full bg-blue-500/10 px-2.5 py-1 text-[9px] text-blue-300">{{ statusLabel(selectedExercise()?.statut || '') }}</span></div><div class="rounded-xl bg-white p-3"><p class="text-[9px] uppercase text-slate-500">Réparti aux OS</p><p class="mt-1 text-sm font-semibold text-blue-200">{{ sumOs() | number: '1.0-0' }} FCFA</p></div><div class="rounded-xl bg-white p-3"><p class="text-[9px] uppercase text-slate-500">À répartir</p><p class="mt-1 text-sm font-semibold" [class.text-blue-300]="remainingExercise() > 0" [class.text-amber-300]="remainingExercise() <= 0">{{ remainingExercise() | number: '1.0-0' }} FCFA</p></div></div>}
      </section>

      @if (selectedExercise()) {
        <section class="flex flex-wrap items-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 py-3"><span class="mr-1 text-[10px] font-semibold uppercase tracking-wider text-slate-500">Parcours</span><span class="rounded-lg bg-blue-500/10 px-3 py-1.5 text-[10px] text-blue-200">01 · Exercice {{ selectedExercise()?.annee }}</span><span class="text-slate-700">→</span><span class="rounded-lg px-3 py-1.5 text-[10px]" [class.bg-blue-500/10]="true" [class.text-blue-200]="true">02 · OS {{ selectedOsAllocation() ? '✓' : '' }}</span><span class="text-slate-700">→</span><span class="rounded-lg px-3 py-1.5 text-[10px]" [class.bg-blue-500/10]="!!selectedOsAllocation()" [class.text-blue-200]="!!selectedOsAllocation()" [class.text-slate-600]="!selectedOsAllocation()">03 · OP {{ selectedOpAllocation() ? '✓' : '' }}</span><span class="text-slate-700">→</span><span class="rounded-lg px-3 py-1.5 text-[10px]" [class.bg-blue-500/10]="!!selectedOpAllocation()" [class.text-blue-200]="!!selectedOpAllocation()" [class.text-slate-600]="!selectedOpAllocation()">04 · Activité & structure</span></section>

        <section class="overflow-hidden rounded-2xl border border-slate-200 bg-white">
          <div class="flex flex-col justify-between gap-3 border-b border-slate-200 p-5 sm:flex-row sm:items-center"><div class="flex items-start gap-3"><span class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-blue-500/10 text-sm font-bold text-blue-300">2</span><div><h2 class="text-sm font-semibold text-white">Répartition par objectif stratégique</h2><p class="mt-1 text-[10px] text-slate-500">Choisissez un OS et fixez sa part de l’enveloppe annuelle.</p></div></div>@if (can('allocation_os.creer')) {<button (click)="showOsForm.set(!showOsForm())" [disabled]="remainingExercise() <= 0 || !availableOs().length" class="rounded-xl bg-green-700 px-3.5 py-2 text-[10px] font-semibold text-white hover:bg-green-600 disabled:cursor-not-allowed disabled:opacity-40">＋ Répartir vers un OS</button>}</div>
          @if (showOsForm()) {<form (ngSubmit)="createOsAllocation()" class="grid gap-3 border-b border-slate-200 bg-slate-50/40 p-4 md:grid-cols-[1fr_220px_auto]"><label class="space-y-1 text-[10px] text-slate-600">Objectif stratégique <span class="champ-obligatoire" aria-hidden="true">*</span><app-search-select required [(ngModel)]="osForm.osId" name="osId" placeholder="Sélectionner un OS…" triggerClass="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2.5 text-xs text-white">@for (os of availableOs(); track os.id) {<app-search-option [value]="os.id" [label]="os.codeOs + ' — ' + os.libelle" />}</app-search-select></label><label class="space-y-1 text-[10px] text-slate-600">Montant à répartir <span class="champ-obligatoire" aria-hidden="true">*</span><input required type="number" min="0.01" step="0.01" [max]="remainingExercise()" [(ngModel)]="osForm.amount" name="osAmount" class="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2.5 text-xs text-white"><span class="block text-[9px] text-slate-500">Maximum : {{ remainingExercise() | number: '1.0-0' }} FCFA</span></label><div class="flex items-end"><button [disabled]="saving() || !osForm.osId || !validAmount(osForm.amount, remainingExercise())" class="w-full rounded-lg bg-green-700 px-4 py-2.5 text-xs font-semibold text-white hover:bg-green-600 disabled:opacity-40">{{ saving() ? 'Enregistrement…' : 'Confirmer' }}</button></div></form>}
          <div class="grid gap-2 p-4 md:grid-cols-2 xl:grid-cols-3">@for (allocation of exerciseOsAllocations(); track allocation.id) {<button (click)="selectOsAllocation(allocation)" class="rounded-xl border p-3 text-left transition hover:border-blue-500/40" [class.border-blue-500/40]="selectedOsAllocationId() === allocation.id" [class.bg-blue-500/5]="selectedOsAllocationId() === allocation.id" [class.border-slate-200]="selectedOsAllocationId() !== allocation.id" [class.bg-slate-50/40]="selectedOsAllocationId() !== allocation.id"><span class="flex items-center gap-2"><span class="rounded-md bg-blue-500/10 px-2 py-1 font-mono text-[9px] text-blue-300">{{ allocation.objectifStrategique?.codeOs }}</span><span class="min-w-0 flex-1 truncate text-xs font-medium text-white">{{ allocation.objectifStrategique?.libelle }}</span><span class="text-slate-500">→</span></span><span class="mt-3 flex items-end justify-between"><span class="text-[9px] text-slate-500">{{ opsAllocatedFor(allocation.id).length }} OP ventilé(s)</span><span class="text-sm font-semibold text-blue-200">{{ allocation.montantAlloue | number: '1.0-0' }} FCFA</span></span></button>}@empty {<div class="col-span-full rounded-xl border border-dashed border-slate-200 p-6 text-center"><p class="text-xs text-slate-700">Aucune allocation OS pour cet exercice</p><p class="mt-1 text-[10px] text-slate-500">Commencez la cascade en répartissant une partie de l’enveloppe vers un OS.</p></div>}</div>
        </section>

        @if (selectedOsAllocation()) {<section class="overflow-hidden rounded-2xl border border-slate-200 bg-white"><div class="flex flex-col justify-between gap-3 border-b border-slate-200 p-5 sm:flex-row sm:items-center"><div class="flex items-start gap-3"><span class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-blue-500/10 text-sm font-bold text-blue-300">3</span><div><h2 class="text-sm font-semibold text-white">Répartir « {{ selectedOsAllocation()?.objectifStrategique?.libelle }} » vers les OP</h2><p class="mt-1 text-[10px] text-slate-500">Disponible sur cet OS : <strong class="text-blue-200">{{ remainingOs() | number: '1.0-0' }} FCFA</strong> sur {{ selectedOsAllocation()?.montantAlloue | number: '1.0-0' }} FCFA.</p></div></div>@if (can('allocation_op.creer')) {<button (click)="showOpForm.set(!showOpForm())" [disabled]="remainingOs() <= 0 || !availableOps().length" class="rounded-xl bg-green-700 px-3.5 py-2 text-[10px] font-semibold text-white hover:bg-green-600 disabled:opacity-40">＋ Répartir vers un OP</button>}</div>
            @if (showOpForm()) {<form (ngSubmit)="createOpAllocation()" class="grid gap-3 border-b border-slate-200 bg-slate-50/40 p-4 md:grid-cols-[1fr_220px_auto]"><label class="space-y-1 text-[10px] text-slate-600">Objectif opérationnel <span class="champ-obligatoire" aria-hidden="true">*</span><app-search-select required [(ngModel)]="opForm.opId" name="opId" placeholder="Sélectionner un OP de cet OS…" triggerClass="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2.5 text-xs text-white">@for (op of availableOps(); track op.id) {<app-search-option [value]="op.id" [label]="op.codeOp + ' — ' + op.libelle" />}</app-search-select></label><label class="space-y-1 text-[10px] text-slate-600">Montant à répartir <span class="champ-obligatoire" aria-hidden="true">*</span><input required type="number" min="0.01" step="0.01" [max]="remainingOs()" [(ngModel)]="opForm.amount" name="opAmount" class="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2.5 text-xs text-white"><span class="block text-[9px] text-slate-500">Maximum : {{ remainingOs() | number: '1.0-0' }} FCFA</span></label><div class="flex items-end"><button [disabled]="saving() || !opForm.opId || !validAmount(opForm.amount, remainingOs())" class="w-full rounded-lg bg-green-700 px-4 py-2.5 text-xs font-semibold text-white hover:bg-green-600 disabled:opacity-40">{{ saving() ? 'Enregistrement…' : 'Confirmer' }}</button></div></form>}
            <div class="grid gap-2 p-4 md:grid-cols-2 xl:grid-cols-3">@for (allocation of osOpAllocations(); track allocation.id) {<button (click)="selectOpAllocation(allocation)" class="rounded-xl border p-3 text-left transition hover:border-blue-500/40" [class.border-blue-500/40]="selectedOpAllocationId() === allocation.id" [class.bg-blue-500/5]="selectedOpAllocationId() === allocation.id" [class.border-slate-200]="selectedOpAllocationId() !== allocation.id" [class.bg-slate-50/40]="selectedOpAllocationId() !== allocation.id"><span class="flex items-center gap-2"><span class="rounded-md bg-blue-500/10 px-2 py-1 font-mono text-[9px] text-blue-300">{{ allocation.objectifOperationnel?.codeOp }}</span><span class="min-w-0 flex-1 truncate text-xs font-medium text-white">{{ allocation.objectifOperationnel?.libelle }}</span><span class="text-slate-500">→</span></span><span class="mt-3 flex items-end justify-between"><span class="text-[9px] text-slate-500">{{ activitiesAllocatedFor(allocation.id).length }} activité(s) ventilée(s)</span><span class="text-sm font-semibold text-blue-200">{{ allocation.montantAlloue | number: '1.0-0' }} FCFA</span></span></button>}@empty {<div class="col-span-full rounded-xl border border-dashed border-slate-200 p-6 text-center"><p class="text-xs text-slate-700">Aucun OP financé sur cet OS</p><p class="mt-1 text-[10px] text-slate-500">Choisissez un OP de cet OS pour poursuivre la répartition.</p></div>}</div>
          </section>}

        @if (selectedOpAllocation()) {<section class="overflow-hidden rounded-2xl border border-slate-200 bg-white"><div class="flex flex-col justify-between gap-3 border-b border-slate-200 p-5 sm:flex-row sm:items-center"><div class="flex items-start gap-3"><span class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-blue-500/10 text-sm font-bold text-blue-300">4</span><div><h2 class="text-sm font-semibold text-white">Répartir « {{ selectedOpAllocation()?.objectifOperationnel?.libelle }} » vers les activités</h2><p class="mt-1 text-[10px] text-slate-500">Disponible sur cet OP : <strong class="text-blue-200">{{ remainingOp() | number: '1.0-0' }} FCFA</strong> sur {{ selectedOpAllocation()?.montantAlloue | number: '1.0-0' }} FCFA.</p></div></div>@if (can('allocation_activite.creer')) {<button (click)="showActivityForm.set(!showActivityForm())" [disabled]="remainingOp() <= 0 || !availableActivities().length" class="rounded-xl bg-green-700 px-3.5 py-2 text-[10px] font-semibold text-white hover:bg-green-600 disabled:opacity-40">＋ Répartir vers une activité</button>}</div>
            @if (showActivityForm()) {<form (ngSubmit)="createActivityAllocation()" class="grid gap-3 border-b border-slate-200 bg-slate-50/40 p-4 md:grid-cols-[1fr_220px_auto]"><label class="space-y-1 text-[10px] text-slate-600">Activité catalogue de cet OP <span class="champ-obligatoire" aria-hidden="true">*</span><app-search-select required [(ngModel)]="activityForm.activityId" name="activityId" placeholder="Sélectionner une activité…" triggerClass="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2.5 text-xs text-white">@for (activity of availableActivities(); track activity.id) {<app-search-option [value]="activity.id" [label]="activity.codeActivite + ' — ' + activity.libelle" />}</app-search-select></label><label class="space-y-1 text-[10px] text-slate-600">Montant à répartir <span class="champ-obligatoire" aria-hidden="true">*</span><input required type="number" min="0.01" step="0.01" [max]="remainingOp()" [(ngModel)]="activityForm.amount" name="activityAmount" class="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2.5 text-xs text-white"><span class="block text-[9px] text-slate-500">Maximum : {{ remainingOp() | number: '1.0-0' }} FCFA</span></label><div class="flex items-end"><button [disabled]="saving() || !activityForm.activityId || !validAmount(activityForm.amount, remainingOp())" class="w-full rounded-lg bg-green-700 px-4 py-2.5 text-xs font-semibold text-white hover:bg-green-600 disabled:opacity-40">{{ saving() ? 'Enregistrement…' : 'Confirmer' }}</button></div></form>}
            <div class="grid gap-2 p-4 md:grid-cols-2 xl:grid-cols-3">@for (allocation of opActivityAllocations(); track allocation.id) {<button (click)="selectActivityAllocation(allocation)" class="rounded-xl border p-3 text-left transition hover:border-blue-500/40" [class.border-blue-500/40]="selectedActivityAllocationId() === allocation.id" [class.bg-blue-500/5]="selectedActivityAllocationId() === allocation.id" [class.border-slate-200]="selectedActivityAllocationId() !== allocation.id" [class.bg-slate-50/40]="selectedActivityAllocationId() !== allocation.id"><span class="flex items-center gap-2"><span class="rounded-md bg-blue-500/10 px-2 py-1 font-mono text-[9px] text-blue-300">{{ allocation.activiteCatalogue?.codeActivite }}</span><span class="min-w-0 flex-1 truncate text-xs font-medium text-white">{{ allocation.activiteCatalogue?.libelle }}</span><span class="text-slate-500">→</span></span><span class="mt-3 flex items-end justify-between"><span class="text-[9px] text-slate-500">{{ structureAllocationsFor(allocation.id).length }} structure(s)</span><span class="text-sm font-semibold text-blue-200">{{ allocation.montantAlloue | number: '1.0-0' }} FCFA</span></span></button>}@empty {<div class="col-span-full rounded-xl border border-dashed border-slate-200 p-6 text-center"><p class="text-xs text-slate-700">Aucune activité financée sur cet OP</p><p class="mt-1 text-[10px] text-slate-500">Sélectionnez une activité catalogue pour la doter en crédits.</p></div>}</div>
          </section>}

        @if (selectedActivityAllocation()) {<section class="overflow-hidden rounded-2xl border border-slate-200 bg-white"><div class="flex flex-col justify-between gap-3 border-b border-slate-200 p-5 sm:flex-row sm:items-center"><div class="flex items-start gap-3"><span class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-amber-500/10 text-sm font-bold text-amber-300">5</span><div><h2 class="text-sm font-semibold text-white">Attribuer « {{ selectedActivityAllocation()?.activiteCatalogue?.libelle }} » aux structures</h2><p class="mt-1 text-[10px] text-slate-500">Montant de l’activité : {{ selectedActivityAllocation()?.montantAlloue | number: '1.0-0' }} FCFA · Disponible pour attribution : <strong class="text-amber-200">{{ remainingActivity() | number: '1.0-0' }} FCFA</strong></p></div></div>@if (can('execution.creer')) {<button (click)="showStructureForm.set(!showStructureForm())" [disabled]="remainingActivity() <= 0 || !availableStructures().length" class="rounded-xl bg-amber-600 px-3.5 py-2 text-[10px] font-semibold text-white hover:bg-amber-500 disabled:opacity-40">＋ Attribuer à une structure</button>}</div>
            @if (showStructureForm()) {<form (ngSubmit)="createStructureAllocation()" class="grid gap-3 border-b border-slate-200 bg-slate-50/40 p-4 md:grid-cols-[1fr_220px_auto]"><label class="space-y-1 text-[10px] text-slate-600">Structure bénéficiaire <span class="champ-obligatoire" aria-hidden="true">*</span><app-search-select required [(ngModel)]="structureForm.structureCode" name="structureCode" placeholder="Sélectionner une structure…" triggerClass="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2.5 text-xs text-white">@for (structure of availableStructures(); track structure.code) {<app-search-option [value]="structure.code" [label]="structure.code + ' — ' + structure.nom" />}</app-search-select></label><label class="space-y-1 text-[10px] text-slate-600">Montant voté <span class="champ-obligatoire" aria-hidden="true">*</span><input required type="number" min="0.01" step="0.01" [max]="remainingActivity()" [(ngModel)]="structureForm.amount" name="structureAmount" class="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2.5 text-xs text-white"><span class="block text-[9px] text-slate-500">Maximum restant : {{ remainingActivity() | number: '1.0-0' }} FCFA</span></label><div class="flex items-end"><button [disabled]="saving() || !structureForm.structureCode || !validAmount(structureForm.amount, remainingActivity())" class="w-full rounded-lg bg-amber-600 px-4 py-2.5 text-xs font-semibold text-white hover:bg-amber-500 disabled:opacity-40">{{ saving() ? 'Enregistrement…' : 'Confirmer' }}</button></div></form>}
            <div class="divide-y divide-slate-200/70">@for (allocation of selectedStructureAllocations(); track allocation.id) {<div class="grid gap-4 p-4 lg:grid-cols-[1.2fr_1fr_auto] lg:items-center"><div><p class="text-xs font-semibold text-white">{{ allocation.structure?.nom }}</p><p class="mt-1 text-[10px] text-slate-500">{{ allocation.structure?.code }} · {{ allocation.structure?.sigle || 'Structure' }}</p></div><div><div class="flex justify-between text-[9px] text-slate-500"><span>Engagé {{ allocation.montantEngage | number: '1.0-0' }} FCFA</span><span>Liquidé {{ allocation.montantLiquide | number: '1.0-0' }} FCFA</span></div><div class="mt-2 h-1.5 overflow-hidden rounded-full bg-slate-100"><div class="h-full rounded-full bg-amber-400" [style.width.%]="executionRate(allocation)"></div></div><p class="mt-1 text-right text-[9px] text-slate-500">{{ allocation.montantDisponible | number: '1.0-0' }} FCFA disponible</p></div>@if (can('execution.modifier')) {<button (click)="editExecution(allocation)" class="rounded-lg border border-slate-300 px-3 py-2 text-[10px] text-slate-700 hover:border-amber-500/40 hover:text-amber-200">Mettre à jour l’exécution</button>}</div>
                @if (editingExecutionId() === allocation.id) {<form (ngSubmit)="saveExecution(allocation)" class="grid gap-3 border-t border-slate-200 bg-slate-50/40 p-4 md:grid-cols-[1fr_1fr_auto_auto]"><label class="space-y-1 text-[10px] text-slate-600">Montant engagé<input type="number" min="0" step="0.01" [max]="allocation.montantAlloue" [(ngModel)]="executionDraft.engage" name="engage" class="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2 text-xs text-white"></label><label class="space-y-1 text-[10px] text-slate-600">Montant liquidé<input type="number" min="0" step="0.01" [max]="allocation.montantAlloue" [(ngModel)]="executionDraft.liquide" name="liquide" class="w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2 text-xs text-white"></label><button [disabled]="saving() || !validExecution(allocation)" class="rounded-lg bg-amber-600 px-4 py-2 text-xs font-semibold text-white disabled:opacity-40">Enregistrer</button><button type="button" (click)="editingExecutionId.set('')" class="rounded-lg border border-slate-300 px-3 py-2 text-xs text-slate-600">Annuler</button></form>}
              }@empty {<div class="p-7 text-center text-xs text-slate-500">Aucune structure n’est encore attributaire de cette activité.</div>}</div>
          </section>}
      } @else {<div class="rounded-2xl border border-dashed border-slate-200 bg-white/50 p-10 text-center"><div class="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-blue-500/10 text-xl text-blue-300">1</div><h2 class="text-sm font-semibold text-white">Commencez par choisir un exercice</h2><p class="mt-1 text-xs text-slate-500">La répartition des crédits suivra ensuite les niveaux OS, OP, activité puis structure.</p></div>}
      <footer class="flex flex-wrap justify-between gap-2 border-t border-slate-200 pt-4 text-[10px] text-slate-600"><span>Exercice → OS → OP → Activité catalogue → Structure</span><span>Les plafonds sont calculés à partir des crédits restants du niveau parent.</span></footer>
    </div>
}
`
})
export class AllocationsComponent implements OnInit {
  readonly importOpen = signal(false);
  readonly importFormats = IMPORT_FORMATS;
  /** Lignes affichées (filtres et tri appliqués) : ce sont elles qui sont exportées. */
  exportIds(): (string | undefined)[] {
    return this.exerciseOsAllocations().flatMap((os) => [os.id, ...this.opAllocations().filter((op) => op.budgetAllocationOs?.id === os.id).flatMap((op) => [op.id, ...this.activityAllocations().filter((ac) => ac.budgetAllocationOp?.id === op.id).flatMap((ac) => [ac.id, ...this.structureAllocationsFor(ac.id).map((s) => s.id)])])]);
  }
  private readonly budget = inject(BudgetService);
  private readonly nomenclature = inject(NomenclatureService);
  private readonly structureService = inject(StructureService);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);

  readonly exercises = signal<ExerciceBudgetaire[]>([]);
  readonly osList = signal<ObjectifStrategique[]>([]);
  readonly opList = signal<ObjectifOperationnel[]>([]);
  readonly activityList = signal<ActiviteCatalogue[]>([]);
  readonly structures = signal<Structure[]>([]);
  readonly osAllocations = signal<BudgetAllocationOs[]>([]);
  readonly opAllocations = signal<BudgetAllocationOp[]>([]);
  readonly activityAllocations = signal<BudgetAllocationActivite[]>([]);
  readonly structureAllocations = signal<BudgetAllocationStructure[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly partial = signal(false);
  readonly error = signal('');
  readonly showOsForm = signal(false);
  readonly showOpForm = signal(false);
  readonly showActivityForm = signal(false);
  readonly showStructureForm = signal(false);
  readonly editingExecutionId = signal('');

  exerciseId = '';
  selectedOsAllocationId = signal('');
  selectedOpAllocationId = signal('');
  selectedActivityAllocationId = signal('');
  osForm = { osId: '', amount: 0 };
  opForm = { opId: '', amount: 0 };
  activityForm = { activityId: '', amount: 0 };
  structureForm = { structureCode: '', amount: 0 };
  executionDraft = { engage: 0, liquide: 0 };

  ngOnInit(): void { this.refresh(); }
  can(permission: string): boolean { return this.auth.hasPermission(permission); }
  selectedExercise(): ExerciceBudgetaire | undefined { return this.exercises().find(item => item.id === this.exerciseId); }
  selectedOsAllocation(): BudgetAllocationOs | undefined { return this.osAllocations().find(item => item.id === this.selectedOsAllocationId()); }
  selectedOpAllocation(): BudgetAllocationOp | undefined { return this.opAllocations().find(item => item.id === this.selectedOpAllocationId()); }
  selectedActivityAllocation(): BudgetAllocationActivite | undefined { return this.activityAllocations().find(item => item.id === this.selectedActivityAllocationId()); }
  exerciseOsAllocations(): BudgetAllocationOs[] { return this.osAllocations().filter(item => item.exerciceBudgetaire?.id === this.exerciseId); }
  osOpAllocations(): BudgetAllocationOp[] { return this.opAllocations().filter(item => item.budgetAllocationOs?.id === this.selectedOsAllocationId()); }
  opActivityAllocations(): BudgetAllocationActivite[] { return this.activityAllocations().filter(item => item.budgetAllocationOp?.id === this.selectedOpAllocationId()); }
  selectedStructureAllocations(): BudgetAllocationStructure[] { return this.structureAllocations().filter(item => item.budgetAllocationActivite?.id === this.selectedActivityAllocationId()); }
  opsAllocatedFor(osAllocationId?: string): BudgetAllocationOp[] { return this.opAllocations().filter(item => item.budgetAllocationOs?.id === osAllocationId); }
  activitiesAllocatedFor(opAllocationId?: string): BudgetAllocationActivite[] { return this.activityAllocations().filter(item => item.budgetAllocationOp?.id === opAllocationId); }
  structureAllocationsFor(activityAllocationId?: string): BudgetAllocationStructure[] { return this.structureAllocations().filter(item => item.budgetAllocationActivite?.id === activityAllocationId); }
  sumOs(): number { return this.exerciseOsAllocations().reduce((sum, item) => sum + Number(item.montantAlloue || 0), 0); }
  remainingExercise(): number { return Math.max(0, Number(this.selectedExercise()?.montantTotalVote || 0) - this.sumOs()); }
  sumOp(): number { return this.osOpAllocations().reduce((sum, item) => sum + Number(item.montantAlloue || 0), 0); }
  remainingOs(): number { return Math.max(0, Number(this.selectedOsAllocation()?.montantAlloue || 0) - this.sumOp()); }
  sumActivity(): number { return this.opActivityAllocations().reduce((sum, item) => sum + Number(item.montantAlloue || 0), 0); }
  remainingOp(): number { return Math.max(0, Number(this.selectedOpAllocation()?.montantAlloue || 0) - this.sumActivity()); }
  sumStructures(): number { return this.selectedStructureAllocations().reduce((sum, item) => sum + Number(item.montantVote || 0), 0); }
  remainingActivity(): number { return Math.max(0, Number(this.selectedActivityAllocation()?.montantAlloue || 0) - this.sumStructures()); }
  availableOs(): ObjectifStrategique[] { return this.osList().filter(item => !!item.id); }
  availableOps(): ObjectifOperationnel[] { const osId = this.selectedOsAllocation()?.objectifStrategique?.id; return this.opList().filter(item => item.objectifStrategique?.id === osId && !!item.id); }
  availableActivities(): ActiviteCatalogue[] { const opId = this.selectedOpAllocation()?.objectifOperationnel?.id; return this.activityList().filter(item => item.objectifOperationnel?.id === opId && !!item.id); }
  availableStructures(): Structure[] { return this.structures(); }
  validAmount(amount: number, max: number): boolean { return Number.isFinite(Number(amount)) && Number(amount) > 0 && Number(amount) <= max; }
  statusLabel(status: string): string { return ({ PREPARATION: 'Préparation', OUVERT: 'Ouvert', CLOTURE: 'Clôturé' } as Record<string, string>)[status] || status; }
  executionRate(allocation: BudgetAllocationStructure): number { const total = Number(allocation.montantAlloue || 0); return total ? Math.min(100, Math.round((Number(allocation.montantEngage || 0) + Number(allocation.montantLiquide || 0)) / total * 100)) : 0; }
  validExecution(allocation: BudgetAllocationStructure): boolean { const engage = Number(this.executionDraft.engage); const liquide = Number(this.executionDraft.liquide); return engage >= 0 && liquide >= 0 && engage + liquide <= Number(allocation.montantAlloue || 0); }

  refresh(): void {
    this.loading.set(true); this.error.set(''); this.partial.set(false);
    forkJoin({
      exercises: this.auth.hasPermission('exercice.lire')
        ? this.budget.listerExercices().pipe(catchError(() => this.unavailable<ExerciceBudgetaire[]>()))
        : of([] as ExerciceBudgetaire[]),
      os: this.auth.hasPermission('os.lire')
        ? this.nomenclature.listerOs().pipe(catchError(() => this.unavailable<ObjectifStrategique[]>()))
        : of([] as ObjectifStrategique[]),
      ops: this.auth.hasPermission('op.lire')
        ? this.nomenclature.listerOp().pipe(catchError(() => this.unavailable<ObjectifOperationnel[]>()))
        : of([] as ObjectifOperationnel[]),
      activities: this.auth.hasPermission('activite.lire')
        ? this.nomenclature.listerActivites().pipe(catchError(() => this.unavailable<ActiviteCatalogue[]>()))
        : of([] as ActiviteCatalogue[]),
      structures: this.auth.hasPermission('entite.lire')
        ? this.structureService.listerOuFiltrer().pipe(catchError(() => this.unavailable<Structure[]>()))
        : of([] as Structure[]),
      allocationsOs: this.auth.hasPermission('allocation_os.lire')
        ? this.budget.listerAllocationsOs().pipe(catchError(() => this.unavailable<BudgetAllocationOs[]>()))
        : of([] as BudgetAllocationOs[]),
      allocationsOp: this.auth.hasPermission('allocation_op.lire')
        ? this.budget.listerAllocationsOp().pipe(catchError(() => this.unavailable<BudgetAllocationOp[]>()))
        : of([] as BudgetAllocationOp[]),
      allocationsActivities: this.auth.hasPermission('allocation_activite.lire')
        ? this.budget.listerAllocationsActivites().pipe(catchError(() => this.unavailable<BudgetAllocationActivite[]>()))
        : of([] as BudgetAllocationActivite[]),
      allocationsStructures: this.auth.hasPermission('execution.lire')
        ? this.budget.listerAllocations().pipe(catchError(() => this.unavailable<BudgetAllocationStructure[]>()))
        : of([] as BudgetAllocationStructure[])
    }).subscribe(data => {
      this.exercises.set(data.exercises); this.osList.set(data.os); this.opList.set(data.ops); this.activityList.set(data.activities); this.structures.set(data.structures);
      this.osAllocations.set(data.allocationsOs); this.opAllocations.set(data.allocationsOp); this.activityAllocations.set(data.allocationsActivities); this.structureAllocations.set(data.allocationsStructures);
      if (!this.exerciseId && data.exercises.length) { const current = data.exercises.find(item => item.annee === new Date().getFullYear()); this.exerciseId = current?.id || data.exercises[0].id || ''; }
      if (this.selectedOsAllocationId() && !data.allocationsOs.some(item => item.id === this.selectedOsAllocationId())) this.selectedOsAllocationId.set('');
      if (this.selectedOpAllocationId() && !data.allocationsOp.some(item => item.id === this.selectedOpAllocationId())) this.selectedOpAllocationId.set('');
      if (this.selectedActivityAllocationId() && !data.allocationsActivities.some(item => item.id === this.selectedActivityAllocationId())) this.selectedActivityAllocationId.set('');
      this.loading.set(false);
    });
  }
  onExerciseChange(id: string): void { this.exerciseId = id; this.clearBelowExercise(); }
  selectOsAllocation(item: BudgetAllocationOs): void { this.selectedOsAllocationId.set(item.id || ''); this.selectedOpAllocationId.set(''); this.selectedActivityAllocationId.set(''); this.showOpForm.set(false); this.showActivityForm.set(false); this.showStructureForm.set(false); }
  selectOpAllocation(item: BudgetAllocationOp): void { this.selectedOpAllocationId.set(item.id || ''); this.selectedActivityAllocationId.set(''); this.showActivityForm.set(false); this.showStructureForm.set(false); }
  selectActivityAllocation(item: BudgetAllocationActivite): void { this.selectedActivityAllocationId.set(item.id || ''); this.showStructureForm.set(false); }

  createOsAllocation(): void {
    const exercise = this.selectedExercise(); if (!exercise?.id || !this.validAmount(this.osForm.amount, this.remainingExercise())) return;
    this.saving.set(true); this.budget.creerAllocationOs({ exerciceId: exercise.id, osId: this.osForm.osId, montantAlloue: Number(this.osForm.amount) }).subscribe({ next: result => { this.notifications.success('Crédit réparti vers l’objectif stratégique.'); this.saving.set(false); this.showOsForm.set(false); this.osForm = { osId: '', amount: 0 }; this.selectedOsAllocationId.set(result.id || ''); this.refresh(); }, error: () => this.failed('Répartition OS impossible. Vérifiez le montant disponible, l’exercice et vos droits.') });
  }
  createOpAllocation(): void {
    const parent = this.selectedOsAllocation(); if (!parent?.id || !this.validAmount(this.opForm.amount, this.remainingOs())) return;
    this.saving.set(true); this.budget.creerAllocationOp({ allocationOsId: parent.id, opId: this.opForm.opId, montantAlloue: Number(this.opForm.amount) }).subscribe({ next: result => { this.notifications.success('Crédit réparti vers l’objectif opérationnel.'); this.saving.set(false); this.showOpForm.set(false); this.opForm = { opId: '', amount: 0 }; this.selectedOpAllocationId.set(result.id || ''); this.refresh(); }, error: () => this.failed('Répartition OP impossible. Vérifiez que cet OP appartient bien à l’OS sélectionné et que le montant reste dans son enveloppe.') });
  }
  createActivityAllocation(): void {
    const parent = this.selectedOpAllocation(); if (!parent?.id || !this.validAmount(this.activityForm.amount, this.remainingOp())) return;
    this.saving.set(true); this.budget.creerAllocationActivite({ allocationOpId: parent.id, activiteCatalogueId: this.activityForm.activityId, montantAlloue: Number(this.activityForm.amount) }).subscribe({ next: result => { this.notifications.success('Crédit réparti vers l’activité catalogue.'); this.saving.set(false); this.showActivityForm.set(false); this.activityForm = { activityId: '', amount: 0 }; this.selectedActivityAllocationId.set(result.id || ''); this.refresh(); }, error: () => this.failed('Répartition de l’activité impossible. Vérifiez son rattachement à l’OP et le montant disponible.') });
  }
  createStructureAllocation(): void {
    const parent = this.selectedActivityAllocation(); if (!parent?.id || !this.validAmount(this.structureForm.amount, this.remainingActivity())) return;
    this.saving.set(true); this.budget.attribuerStructure({ allocationActiviteId: parent.id, structureCode: this.structureForm.structureCode, montantVote: Number(this.structureForm.amount) }).subscribe({ next: () => { this.notifications.success('Crédit attribué à la structure.'); this.saving.set(false); this.showStructureForm.set(false); this.structureForm = { structureCode: '', amount: 0 }; this.refresh(); }, error: () => this.failed('Attribution impossible. Vérifiez la structure et le montant restant sur cette activité.') });
  }
  editExecution(allocation: BudgetAllocationStructure): void { this.editingExecutionId.set(allocation.id || ''); this.executionDraft = { engage: Number(allocation.montantEngage || 0), liquide: Number(allocation.montantLiquide || 0) }; }
  saveExecution(allocation: BudgetAllocationStructure): void { if (!allocation.id || !this.validExecution(allocation)) return; this.saving.set(true); this.budget.mettreAJourExecution(allocation.id, { montantEngage: Number(this.executionDraft.engage), montantLiquide: Number(this.executionDraft.liquide) }).subscribe({ next: () => { this.notifications.success('Suivi d’exécution mis à jour.'); this.saving.set(false); this.editingExecutionId.set(''); this.refresh(); }, error: () => this.failed('Mise à jour impossible. Les montants engagés et liquidés ne peuvent pas dépasser le crédit attribué.') }); }

  private clearBelowExercise(): void { this.selectedOsAllocationId.set(''); this.selectedOpAllocationId.set(''); this.selectedActivityAllocationId.set(''); this.showOsForm.set(false); this.showOpForm.set(false); this.showActivityForm.set(false); this.showStructureForm.set(false); }
  private failed(message: string): void { this.saving.set(false); this.notifications.error(message); }
  private unavailable<T>() { this.partial.set(true); return of([] as T); }
}
