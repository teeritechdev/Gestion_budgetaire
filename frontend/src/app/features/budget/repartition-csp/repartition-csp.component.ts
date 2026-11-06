import { PageHeaderComponent } from '../../../shared/components/page-header.component';
import { IMPORT_FORMATS } from '../../../core/models/import-formats';
import { ImportCsvPageComponent } from '../../../shared/components/import-csv-page.component';
import { DataActionsComponent } from '../../../shared/components/data-actions.component';
import { FormPageComponent } from '../../../shared/components/form-page.component';
import { RecordDetailsDialogComponent } from '../../../shared/components/record-details-dialog.component';
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { SearchOptionDirective, SearchSelectComponent } from '../../../shared/components/search-select.component';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { BudgetService } from '../../../core/services/budget.service';
import { StructureService } from '../../../core/services/structure.service';
import { NomenclatureService } from '../../../core/services/nomenclature.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Structure } from '../../../core/models/structure.model';
import { ActiviteCatalogue } from '../../../core/models/budget/activite-catalogue.model';
import { EntiteStatistiqueCle } from '../../../core/models/budget/entite-statistique-cle.model';
import { BudgetAllocationStructure } from '../../../core/models/budget/budget-allocation.model';
import {
  RepartitionCspCleType,
  RepartitionCspRule,
  RepartitionCspRuleRequest,
} from '../../../core/models/budget/repartition-csp-rule.model';

interface RepartitionCspForm {
  allocationStructureId: string;
  activiteCspId: string;
  activiteBeneficiaireId: string;
  cleType: RepartitionCspCleType;
}

interface RepartitionCspDistributionRow {
  entity: Structure;
  percentage: number | null;
  statisticValue: number | null;
  statisticPeriod: string;
}

@Component({
  selector: 'app-repartition-csp',
  standalone: true,
  imports: [SearchSelectComponent, SearchOptionDirective, PageHeaderComponent, DataActionsComponent, ImportCsvPageComponent, FormPageComponent, RecordDetailsDialogComponent, CommonModule, FormsModule, DecimalPipe],
  templateUrl: './repartition-csp.component.html',
})
export class RepartitionCspComponent implements OnInit {
  readonly importOpen = signal(false);
  readonly importFormats = IMPORT_FORMATS;
  /** Lignes affichées (filtres et tri appliqués) : ce sont elles qui sont exportées. */
  exportIds(): (string | undefined)[] {
    return this.filtered().map((item) => item.id);
  }
  detailTitle = '';
  detailRecord: object | null = null;
  showDetails(title: string, record: object): void {
    this.detailTitle = title;
    this.detailRecord = record;
  }

  private readonly budget = inject(BudgetService);
  private readonly entityService = inject(StructureService);
  private readonly nomenclature = inject(NomenclatureService);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  readonly rules = signal<RepartitionCspRule[]>([]);
  readonly entities = signal<Structure[]>([]);
  readonly activities = signal<ActiviteCatalogue[]>([]);
  readonly cspAllocations = signal<BudgetAllocationStructure[]>([]);
  readonly statistics = signal<EntiteStatistiqueCle[]>([]);
  readonly modal = signal(false);
  readonly editing = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly entityLookupError = signal('');
  readonly activityLookupError = signal('');
  readonly allocationLookupError = signal('');
  readonly statisticsLookupError = signal('');
  readonly saveError = signal('');
  readonly distributionTotal = signal(0);
  private statisticsLoadError = '';
  manualPercentages = new Map<string, number | null>();
  search = '';
  keyFilter = '';
  form: RepartitionCspForm = this.emptyForm();
  ngOnInit(): void {
    this.refresh();
  }
  can(permission: string): boolean {
    return this.auth.hasPermission(permission);
  }
  keyTypes(): string[] {
    return [
      ...new Set(
        this.rules()
          .map((rule) => rule.cleType)
          .filter((key): key is string => !!key),
      ),
    ].sort();
  }
  beneficiaryCount(): number {
    return new Set(this.rules().map((rule) => rule.entiteBeneficiaire)).size;
  }
  displayedPercentage(rule: RepartitionCspRule): number | null {
    return rule.valeurCle == null ? null : rule.valeurCle * 100;
  }
  keyTypeCount(): number {
    return this.keyTypes().length;
  }
  filtered(): RepartitionCspRule[] {
    const q = this.search.trim().toLocaleLowerCase('fr');
    return this.rules().filter(
      (rule) =>
        (!this.keyFilter || rule.cleType === this.keyFilter) &&
        (!q ||
          `${rule.activiteCspId} ${rule.allocationStructureId || ''} ${rule.activiteBeneficiaireId} ${rule.entiteBeneficiaire} ${this.allocationStructureLabel(rule.allocationStructureId, rule.activiteCspId)} ${this.activityLabel(rule.activiteBeneficiaireId)} ${this.entityLabel(rule.entiteBeneficiaire)} ${rule.cleType || ''}`
            .toLocaleLowerCase('fr')
            .includes(q)),
    );
  }
  activityLabel(id: string): string {
    const allocationActivity = this.cspAllocationStructures()
      .map((allocation) => allocation.budgetAllocationActivite)
      .find(
        (activity) =>
          activity?.id === id || activity?.activiteCatalogue?.id === id,
      );
    const item =
      allocationActivity?.activiteCatalogue ??
      this.activities().find((activity) => activity.id === id);
    return item ? `${item.codeActivite} · ${item.libelle}` : 'Activité non référencée';
  }
  allocationStructureLabel(id: string | null | undefined, activityId: string): string {
    const allocation = this.cspAllocations().find((item) => item.id === id);
    if (!allocation) return this.activityLabel(activityId);
    const activity = allocation.budgetAllocationActivite?.activiteCatalogue;
    const structure = allocation.structure;
    return `${activity?.codeActivite ?? activityId} — ${activity?.libelle ?? ''} · ${structure?.code ?? ''} — ${structure?.nom ?? ''}`;
  }
  entityLabel(code: string): string {
    const item = this.entities().find((entity) => entity.code === code);
    return item ? `${item.code} · ${item.nom}` : code;
  }
  /** Provisoire : en attendant les règles de partage, toute allocation de structure peut être répartie. */
  allocationSourceIsCsp(allocation: BudgetAllocationStructure): boolean {
    return !!allocation.structure;
  }
  cspAllocationStructures(): BudgetAllocationStructure[] {
    return this.cspAllocations().filter((allocation) => this.allocationSourceIsCsp(allocation));
  }
  /** Toutes les structures, sauf celle qui porte l'allocation répartie. */
  entitiesBeneficiaires(): Structure[] {
    const source = this.cspAllocations().find((item) => item.id === this.form.allocationStructureId)?.structure?.code;
    return this.entities().filter((entity) => entity.code !== source);
  }
  entityCountSummary(): string {
    const total = this.entities().length;
    const beneficiaries = this.entitiesBeneficiaires().length;
    return `${beneficiaries} bénéficiaire(s) parmi ${total} entité(s) (structure source exclue)`;
  }
  refresh(): void {
    this.error.set('');
    this.entityLookupError.set('');
    this.activityLookupError.set('');
    this.allocationLookupError.set('');
    this.statisticsLookupError.set('');
    this.statisticsLoadError = '';
    this.statistics.set([]);
    forkJoin({
      rules: this.budget.listerReglesCsp().pipe(
        catchError((error: HttpErrorResponse) => {
          this.error.set(this.describeApiError('charger les règles CSP', error));
          return of([] as RepartitionCspRule[]);
        }),
      ),
      entities: (this.auth.hasPermission('entite.lire')
        ? this.entityService.listerOuFiltrer()
        : of([] as Structure[])).pipe(
        catchError((error: HttpErrorResponse) => {
          this.entityLookupError.set(this.describeApiError('charger les entités bénéficiaires', error));
          return of([] as Structure[]);
        }),
      ),
      activities: (this.auth.hasPermission('activite.lire')
        ? this.nomenclature.listerActivites()
        : of([] as ActiviteCatalogue[])).pipe(
        catchError((error: HttpErrorResponse) => {
          this.activityLookupError.set(this.describeApiError('charger le catalogue des activités', error));
          return of([] as ActiviteCatalogue[]);
        }),
      ),
      allocations: this.budget.listerAllocationsCsp().pipe(
        catchError((error: HttpErrorResponse) => {
          this.allocationLookupError.set(this.describeApiError('charger les allocations de structure CSP', error));
          return of([] as BudgetAllocationStructure[]);
        }),
      ),
      statistics: (this.auth.hasPermission('statistique_cle.lire')
        ? this.budget.listerStatistiquesCles()
        : of([] as EntiteStatistiqueCle[])).pipe(
        catchError((error: HttpErrorResponse) => {
          this.statisticsLoadError = this.describeApiError(
            'charger les statistiques mensuelles des entités',
            error,
          );
          this.statisticsLookupError.set(this.statisticsLoadError);
          return of([] as EntiteStatistiqueCle[]);
        }),
      ),
    }).subscribe((data) => {
      this.rules.set(data.rules);
      this.entities.set(data.entities);
      this.activities.set(data.activities);
      this.cspAllocations.set(data.allocations);
      this.statistics.set(data.statistics);
      if (this.modal()) this.updateDistributionTotal();
    });
  }
  openCreate(): void {
    this.editing.set(false);
    this.form = this.emptyForm();
    this.manualPercentages = new Map();
    this.statisticsLookupError.set('');
    this.saveError.set('');
    this.updateDistributionTotal();
    this.modal.set(true);
    if (this.allocationLookupError()) {
      this.saveError.set(this.allocationLookupError());
    } else if (!this.cspAllocationStructures().length) {
      this.saveError.set('Aucune allocation de structure rattachée à une entité CSP n’est disponible.');
    } else if (this.entityLookupError()) {
      this.saveError.set(this.entityLookupError());
    } else if (!this.entitiesBeneficiaires().length) {
      this.saveError.set('Aucune entité bénéficiaire n’est disponible.');
    }
  }
  openEdit(rule: RepartitionCspRule): void {
    if (!this.entitiesBeneficiaires().length || !this.cspAllocationStructures().length) {
      this.notifications.warning(
        'Impossible de modifier la règle sans charger le référentiel des entités et activités.',
      );
      return;
    }
    this.editing.set(true);
    const selectedAllocationId =
      rule.allocationStructureId ??
      this.cspAllocationStructures().find(
        (allocation) =>
          allocation.budgetAllocationActivite?.id === rule.activiteCspId ||
          allocation.budgetAllocationActivite?.activiteCatalogue?.id === rule.activiteCspId,
      )?.id ??
      '';
    const activityRules = this.rules().filter((item) =>
      selectedAllocationId
        ? item.allocationStructureId === selectedAllocationId
        : item.activiteCspId === rule.activiteCspId &&
          item.activiteBeneficiaireId === rule.activiteBeneficiaireId,
    );
    this.form = {
      allocationStructureId: selectedAllocationId,
      activiteCspId: rule.activiteCspId,
      activiteBeneficiaireId: rule.activiteBeneficiaireId,
      cleType: this.isCleType(rule.cleType) ? rule.cleType : 'AUTRE',
    };
    this.manualPercentages = new Map(
      activityRules.map((item) => [
        item.entiteBeneficiaire,
        item.valeurCle == null ? null : item.valeurCle * 100,
      ]),
    );
    this.statisticsLookupError.set('');
    this.saveError.set('');
    this.updateDistributionTotal();
    this.modal.set(true);
  }
  onSelectionChange(): void {
    const selected = this.cspAllocationStructures().find(
      (allocation) => allocation.id === this.form.allocationStructureId,
    );
    const activityId = selected?.budgetAllocationActivite?.activiteCatalogue?.id ?? '';
    this.form.activiteCspId = activityId;
    this.form.activiteBeneficiaireId = activityId;
    this.saveError.set('');
    this.updateDistributionTotal();
  }
  onKeyChange(): void {
    this.saveError.set('');
    this.statisticsLookupError.set('');
    this.updateDistributionTotal();
  }
  private isCleType(value: string | undefined): value is RepartitionCspCleType {
    return value === 'EFFECTIF' || value === 'CHIFFRE_AFFAIRES' || value === 'AUTRE';
  }
  distributionRows(): RepartitionCspDistributionRow[] {
    if (this.form.cleType === 'AUTRE') {
      return this.entitiesBeneficiaires().map((entity) => ({
        entity,
        percentage: this.manualPercentages.get(entity.code) ?? null,
        statisticValue: null,
        statisticPeriod: '',
      }));
    }

    const period = this.latestMonthlyPeriod();
    const rows = this.entitiesBeneficiaires().map((entity) => {
      const statistic = period
        ? this.statistics().find(
            (item) =>
              item.entiteCode === entity.code &&
              item.anneeExercice === period.year &&
              item.mois === period.month,
          )
        : undefined;
      const statisticValue =
        this.form.cleType === 'EFFECTIF' ? statistic?.effectifAgents : statistic?.chiffreAffaires;
      return {
        entity,
        percentage: null,
        statisticValue: statisticValue ?? null,
        statisticPeriod: statistic ? `${statistic.mois}/${statistic.anneeExercice}` : '',
      };
    });
    const values = rows.map((row) => row.statisticValue);
    if (
      rows.length === 0 ||
      values.some((value) => value == null || !Number.isFinite(value) || value < 0)
    ) {
      return rows;
    }
    const total = values.reduce<number>((sum, value) => sum + (value ?? 0), 0);
    if (!Number.isFinite(total) || total <= 0) return rows;

    let allocated = 0;
    let lastNonZeroIndex = -1;
    for (let index = values.length - 1; index >= 0; index--) {
      if (values[index] != null && values[index]! > 0) {
        lastNonZeroIndex = index;
        break;
      }
    }
    return rows.map((row, index) => {
      if (row.statisticValue === 0) return { ...row, percentage: 0 };
      const percentage =
        index === lastNonZeroIndex
          ? Number((100 - allocated).toFixed(2))
          : Number(((row.statisticValue! / total) * 100).toFixed(2));
      allocated += percentage;
      return { ...row, percentage };
    });
  }
  private latestMonthlyPeriod(): { year: number; month: number } | undefined {
    const beneficiaryCodes = new Set(this.entitiesBeneficiaires().map((entity) => entity.code));
    const latest = this.statistics()
      .filter(
        (item) =>
          beneficiaryCodes.has(item.entiteCode) &&
          item.mois != null &&
          Number.isFinite(item.anneeExercice) &&
          Number.isFinite(item.mois),
      )
      .sort(
        (left, right) =>
          right.anneeExercice - left.anneeExercice || (right.mois ?? 0) - (left.mois ?? 0),
      )[0];
    return latest?.mois == null
      ? undefined
      : { year: latest.anneeExercice, month: latest.mois };
  }
  private updateDistributionTotal(): void {
    const rows = this.distributionRows();
    this.distributionTotal.set(rows.reduce((total, row) => total + (row.percentage ?? 0), 0));
    if (this.form.cleType === 'AUTRE') {
      this.statisticsLookupError.set('');
      return;
    }
    if (this.statisticsLoadError) {
      this.statisticsLookupError.set(this.statisticsLoadError);
      return;
    }
    const missingValue = rows.some((row) => row.statisticValue == null);
    const total = rows.reduce<number>((sum, row) => sum + (row.statisticValue ?? 0), 0);
    this.statisticsLookupError.set(
      missingValue
        ? 'Une statistique mensuelle récente manque pour au moins une entité.'
        : total <= 0
          ? 'Le total des statistiques doit être supérieur à zéro pour calculer les pourcentages.'
          : '',
    );
  }
  manualPercentage(entiteCode: string): number | null {
    return this.manualPercentages.get(entiteCode) ?? null;
  }
  isDistributionTotalValid(): boolean {
    return Math.abs(this.distributionTotal() - 100) < 0.01;
  }
  setManualPercentage(entiteCode: string, value: string): void {
    const percentage = value === '' ? null : Number(value);
    this.manualPercentages.set(
      entiteCode,
      percentage != null && Number.isFinite(percentage) ? percentage : null,
    );
    this.saveError.set('');
    this.updateDistributionTotal();
  }
  canSaveDistribution(): boolean {
    const rows = this.distributionRows();
    return (
      !this.saving() &&
      !!this.form.allocationStructureId &&
      !!this.form.activiteCspId &&
      rows.length > 0 &&
      rows.every(
        (row) =>
          row.percentage != null &&
          Number.isFinite(row.percentage) &&
          row.percentage >= 0 &&
          row.percentage <= 100,
      ) &&
      Math.abs(this.distributionTotal() - 100) < 0.01 &&
      (this.form.cleType === 'AUTRE' || !this.statisticsLookupError())
    );
  }
  close(): void {
    if (!this.saving()) this.modal.set(false);
  }
  save(): void {
    this.saveError.set('');
    const selectedAllocation = this.cspAllocationStructures().find(
      (allocation) => allocation.id === this.form.allocationStructureId,
    );
    const selectedActivityId = selectedAllocation?.budgetAllocationActivite?.id;
    if (!selectedAllocation || !selectedActivityId) {
      const message = 'Choisissez une allocation de structure rattachée à une entité CSP.';
      this.saveError.set(message);
      this.notifications.warning(message);
      return;
    }
    this.form.activiteCspId = selectedActivityId;
    this.form.activiteBeneficiaireId = selectedActivityId;
    if (!this.isCleType(this.form.cleType) || !this.entitiesBeneficiaires().length) {
      const message = 'Choisissez un mode et vérifiez que les entités sont disponibles.';
      this.saveError.set(message);
      this.notifications.warning(message);
      return;
    }
    if (!this.canSaveDistribution()) {
      const message =
        this.form.cleType === 'AUTRE'
          ? 'Saisissez un pourcentage pour chaque entité. Le total doit être égal à 100 %.'
          : this.statisticsLookupError() || 'Le total des pourcentages doit être égal à 100 %.';
      this.saveError.set(message);
      this.notifications.warning(message);
      return;
    }
    const requests: RepartitionCspRuleRequest[] = this.distributionRows().map((row) => ({
      activiteCspId: this.form.activiteCspId,
      allocationStructureId: this.form.allocationStructureId,
      entiteBeneficiaire: row.entity.code,
      activiteBeneficiaireId: this.form.activiteBeneficiaireId,
      cleType: this.form.cleType,
      valeurCle: row.percentage! / 100,
    }));
    this.saving.set(true);
    this.budget.enregistrerRepartitionCsp(requests).subscribe({
      next: () => {
        this.saving.set(false);
        this.modal.set(false);
        this.notifications.success(
          this.editing()
            ? 'Répartition CSP mise à jour.'
            : 'Répartition CSP enregistrée pour toutes les entités.',
        );
        this.refresh();
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        const message = this.describeApiError('enregistrer la répartition CSP', error);
        this.saveError.set(message);
        this.notifications.error(message);
      },
    });
  }
  private describeApiError(operation: string, error: HttpErrorResponse, detailOverride?: string): string {
    if (error.status === 0) {
      return `Le serveur est injoignable pendant l’action « ${operation} ». Vérifiez la connexion au backend puis réessayez.`;
    }
    if (error.status === 401) return `Votre session a expiré. Reconnectez-vous pour ${operation}.`;
    if (error.status === 403) return `Accès refusé : vous n’avez pas les droits nécessaires pour ${operation}.`;
    const detail = detailOverride ?? this.extractApiErrorDetail(error.error);
    return `${operation} a échoué (HTTP ${error.status}). ${
      detail || error.statusText || 'Consultez les journaux du serveur.'
    }`;
  }
  private extractApiErrorDetail(body: unknown): string {
    if (body instanceof Blob) return '';
    if (typeof body === 'string') {
      const text = body.trim();
      if (!text) return '';
      try {
        return this.extractApiErrorDetail(JSON.parse(text));
      } catch {
        return text;
      }
    }
    if (!body || typeof body !== 'object') return '';
    const payload = body as Record<string, unknown>;
    const errors = payload['errors'];
    const fieldErrors =
      errors && typeof errors === 'object'
        ? Object.entries(errors as Record<string, unknown>)
            .map(([field, message]) => `${field} : ${String(message)}`)
            .join('; ')
        : '';
    for (const key of ['detail', 'message', 'title', 'error']) {
      const value = payload[key];
      if (typeof value === 'string' && value.trim()) {
        return fieldErrors ? `${value.trim()} ${fieldErrors}` : value.trim();
      }
      if (value && typeof value === 'object') {
        const nested = this.extractApiErrorDetail(value);
        if (nested) return nested;
      }
    }
    return fieldErrors;
  }
  remove(rule: RepartitionCspRule): void {
    if (!rule.id || !confirm('Supprimer cette règle de répartition CSP ?')) return;
    this.budget.supprimerRegleCsp(rule.id).subscribe({
      next: () => {
        this.notifications.success('Règle supprimée.');
        this.refresh();
      },
      error: (error: HttpErrorResponse) =>
        this.notifications.error(this.describeApiError('supprimer la règle CSP', error)),
    });
  }
  private download(blob: Blob, name: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = name;
    a.click();
    URL.revokeObjectURL(url);
  }
  private emptyForm(): RepartitionCspForm {
    return {
      allocationStructureId: '',
      activiteCspId: '',
      activiteBeneficiaireId: '',
      cleType: 'EFFECTIF',
    };
  }
}
