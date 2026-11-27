import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  PlanStrategique,
  PlanStrategiqueRequestDto,
} from '../models/budget/plan-strategique.model';
import {
  ExerciceBudgetaire,
  ExerciceBudgetaireRequestDto,
} from '../models/budget/exercice-budgetaire.model';
import {
  AllocationActiviteRequestDto,
  AllocationOpRequestDto,
  AllocationStructureRequestDto,
  BudgetAllocationActivite,
  BudgetAllocationOp,
  BudgetAllocationOs,
  BudgetAllocationStructure,
  ExecutionStructureUpdateRequestDto,
} from '../models/budget/budget-allocation.model';
import { DemandeRevisionBudgetaire } from '../models/budget/revision-budgetaire.model';
import {
  DemandeRevisionRequestDto,
  LigneRevisionBudgetaire,
  ValidationRevisionRequestDto,
} from '../models/budget/revision-budgetaire.model';
import {
  RepartitionCspRule,
  RepartitionCspRuleRequest,
} from '../models/budget/repartition-csp-rule.model';
import {
  EntiteStatistiqueCle,
  EntiteStatistiqueCleRequest,
} from '../models/budget/entite-statistique-cle.model';
import { JournalMouvementsBudgetaire } from '../models/budget/journal-mouvements.model';
import { DashboardSummary } from '../models/budget/dashboard-summary.model';

@Injectable({ providedIn: 'root' })
export class BudgetService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  getDashboardSummary(): Observable<DashboardSummary> {
    return this.http.get<DashboardSummary>(`${this.api}/dashboard/summary`);
  }

  listerPlans(): Observable<PlanStrategique[]> {
    return this.http.get<PlanStrategique[]>(`${this.api}/plans-strategiques`);
  }
  creerPlan(dto: PlanStrategiqueRequestDto): Observable<PlanStrategique> {
    return this.http.post<PlanStrategique>(`${this.api}/plans-strategiques`, dto);
  }
  modifierPlan(id: string, dto: PlanStrategiqueRequestDto): Observable<PlanStrategique> {
    return this.http.put<PlanStrategique>(`${this.api}/plans-strategiques/${id}`, dto);
  }
  supprimerPlan(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/plans-strategiques/${id}`);
  }

  listerExercices(): Observable<ExerciceBudgetaire[]> {
    return this.http.get<ExerciceBudgetaire[]>(`${this.api}/exercices-budgetaires`);
  }
  creerExercice(dto: ExerciceBudgetaireRequestDto): Observable<ExerciceBudgetaire> {
    return this.http.post<ExerciceBudgetaire>(`${this.api}/exercices-budgetaires`, dto);
  }
  modifierExercice(id: string, dto: ExerciceBudgetaireRequestDto): Observable<ExerciceBudgetaire> {
    return this.http.put<ExerciceBudgetaire>(`${this.api}/exercices-budgetaires/${id}`, dto);
  }
  supprimerExercice(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/exercices-budgetaires/${id}`);
  }

  listerAllocations(): Observable<BudgetAllocationStructure[]> {
    return this.http.get<BudgetAllocationStructure[]>(`${this.api}/allocations-structures`);
  }
  listerAllocationsCsp(): Observable<BudgetAllocationStructure[]> {
    return this.http.get<BudgetAllocationStructure[]>(`${this.api}/allocations-structures/csp`);
  }
  listerRevisions(): Observable<DemandeRevisionBudgetaire[]> {
    return this.http.get<DemandeRevisionBudgetaire[]>(`${this.api}/revisions-budgetaires`);
  }
  creerRevision(dto: DemandeRevisionRequestDto): Observable<DemandeRevisionBudgetaire> {
    return this.http.post<DemandeRevisionBudgetaire>(`${this.api}/revisions-budgetaires`, dto);
  }
  modifierRevision(id: string, dto: DemandeRevisionRequestDto): Observable<DemandeRevisionBudgetaire> {
    return this.http.put<DemandeRevisionBudgetaire>(`${this.api}/revisions-budgetaires/${id}`, dto);
  }
  soumettreRevision(id: string): Observable<DemandeRevisionBudgetaire> {
    return this.http.put<DemandeRevisionBudgetaire>(
      `${this.api}/revisions-budgetaires/${id}/soumettre`,
      {},
    );
  }
  supprimerRevision(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/revisions-budgetaires/${id}`);
  }
  validerRevision(
    id: string,
    dto: ValidationRevisionRequestDto,
  ): Observable<DemandeRevisionBudgetaire> {
    return this.http.put<DemandeRevisionBudgetaire>(
      `${this.api}/revisions-budgetaires/${id}/valider`,
      dto,
    );
  }
  listerLignesRevision(id: string): Observable<LigneRevisionBudgetaire[]> {
    return this.http.get<LigneRevisionBudgetaire[]>(
      `${this.api}/revisions-budgetaires/${id}/lignes`,
    );
  }
  listerMouvements(
    anneeExercice?: number,
    allocationStructureId?: string,
    typeMouvement?: string,
  ): Observable<JournalMouvementsBudgetaire[]> {
    let params = new HttpParams();
    if (anneeExercice) params = params.set('anneeExercice', anneeExercice);
    if (allocationStructureId) params = params.set('allocationStructureId', allocationStructureId);
    if (typeMouvement) params = params.set('typeMouvement', typeMouvement);
    return this.http.get<JournalMouvementsBudgetaire[]>(`${this.api}/journal-mouvements`, {
      params,
    });
  }
  listerReglesCsp(): Observable<RepartitionCspRule[]> {
    return this.http.get<RepartitionCspRule[]>(`${this.api}/repartition-csp`);
  }
  creerRegleCsp(dto: RepartitionCspRuleRequest): Observable<RepartitionCspRule> {
    return this.http.post<RepartitionCspRule>(`${this.api}/repartition-csp`, dto);
  }
  enregistrerRepartitionCsp(dtos: RepartitionCspRuleRequest[]): Observable<RepartitionCspRule[]> {
    return this.http.post<RepartitionCspRule[]>(`${this.api}/repartition-csp/batch`, dtos);
  }
  modifierRegleCsp(id: string, dto: RepartitionCspRuleRequest): Observable<RepartitionCspRule> {
    return this.http.put<RepartitionCspRule>(`${this.api}/repartition-csp/${id}`, dto);
  }
  supprimerRegleCsp(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/repartition-csp/${id}`);
  }
  listerStatistiquesCles(): Observable<EntiteStatistiqueCle[]> {
    return this.http.get<EntiteStatistiqueCle[]>(`${this.api}/statistiques-cles`);
  }
  creerStatistiqueCle(dto: EntiteStatistiqueCleRequest): Observable<EntiteStatistiqueCle> {
    return this.http.post<EntiteStatistiqueCle>(`${this.api}/statistiques-cles`, dto);
  }
  modifierStatistiqueCle(
    id: string,
    dto: EntiteStatistiqueCleRequest,
  ): Observable<EntiteStatistiqueCle> {
    return this.http.put<EntiteStatistiqueCle>(`${this.api}/statistiques-cles/${id}`, dto);
  }
  supprimerStatistiqueCle(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/statistiques-cles/${id}`);
  }
  listerAllocationsOs(): Observable<BudgetAllocationOs[]> {
    return this.http.get<BudgetAllocationOs[]>(`${this.api}/allocations-os`);
  }
  creerAllocationOs(dto: {
    exerciceId: string;
    osId: string;
    montantAlloue: number;
  }): Observable<BudgetAllocationOs> {
    return this.http.post<BudgetAllocationOs>(`${this.api}/allocations-os`, dto);
  }
  listerAllocationsOp(): Observable<BudgetAllocationOp[]> {
    return this.http.get<BudgetAllocationOp[]>(`${this.api}/allocations-op`);
  }
  creerAllocationOp(dto: AllocationOpRequestDto): Observable<BudgetAllocationOp> {
    return this.http.post<BudgetAllocationOp>(`${this.api}/allocations-op`, dto);
  }
  listerAllocationsActivites(): Observable<BudgetAllocationActivite[]> {
    return this.http.get<BudgetAllocationActivite[]>(`${this.api}/allocations-activites`);
  }
  creerAllocationActivite(dto: AllocationActiviteRequestDto): Observable<BudgetAllocationActivite> {
    return this.http.post<BudgetAllocationActivite>(`${this.api}/allocations-activites`, dto);
  }
  attribuerStructure(dto: AllocationStructureRequestDto): Observable<BudgetAllocationStructure> {
    return this.http.post<BudgetAllocationStructure>(`${this.api}/allocations-structures`, dto);
  }
  mettreAJourExecution(
    id: string,
    dto: ExecutionStructureUpdateRequestDto,
  ): Observable<BudgetAllocationStructure> {
    return this.http.put<BudgetAllocationStructure>(
      `${this.api}/allocations-structures/${id}/execution`,
      dto,
    );
  }
}
