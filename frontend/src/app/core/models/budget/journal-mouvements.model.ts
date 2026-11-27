import { BudgetAllocationStructure } from './budget-allocation.model';

export interface JournalMouvementsBudgetaire {
  id: string;
  allocationStructure?: BudgetAllocationStructure;
  anneeExercice: number;
  typeMouvement: string;
  montantAnterieur?: number;
  variation: number;
  nouveauMontant: number;
  referenceDocument?: string;
  auteurId: string;
  createdAt: string;
}
