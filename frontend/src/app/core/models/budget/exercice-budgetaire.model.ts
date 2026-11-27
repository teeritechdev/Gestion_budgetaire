import { PlanStrategique } from './plan-strategique.model';

export type StatutExercice = 'PREPARATION' | 'OUVERT' | 'CLOTURE';

export interface ExerciceBudgetaire {
  id?: string;
  annee: number;
  planStrategique?: PlanStrategique;
  statut: StatutExercice | string;
  montantTotalVote: number;
  dateOuverture?: string;
  dateFermeture?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ExerciceBudgetaireRequestDto {
  annee: number;
  planId: string;
  statut: string;
  montantTotalVote: number;
  dateOuverture?: string;
  dateFermeture?: string;
}
