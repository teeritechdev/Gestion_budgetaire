import { BudgetAllocationStructure } from './budget-allocation.model';

export type StatutRevision = 'BROUILLON' | 'EN_ATTENTE' | 'VALIDE' | 'REJETE';

export interface LigneRevisionBudgetaire {
  id?: string;
  typeMouvement: 'VIREMENT_INTERNE' | 'EXTENSION' | string;
  montant: number;
  allocationSource?: BudgetAllocationStructure;
  allocationDestination?: BudgetAllocationStructure;
  createdAt?: string;
}

export interface DemandeRevisionBudgetaire {
  id?: string;
  numeroDemande: string;
  anneeExercice: number;
  demandeurId: string;
  motif?: string;
  statut: StatutRevision | string;
  valideurId?: string;
  dateValidation?: string;
  commentaireValidation?: string;
  lignes?: LigneRevisionBudgetaire[];
  createdAt?: string;
  updatedAt?: string;
}

export interface LigneRevisionRequestDto {
  typeMouvement: string;
  montant: number;
  allocationSourceId?: string;
  allocationDestinationId?: string;
}

export interface DemandeRevisionRequestDto {
  numeroDemande: string;
  anneeExercice: number;
  demandeurId: string;
  motif?: string;
  lignes: LigneRevisionRequestDto[];
}

// Le valideur est déduit du token par le backend : il n'est pas transmis.
export interface ValidationRevisionRequestDto {
  statut: 'VALIDE' | 'REJETE';
  commentaire?: string;
}
