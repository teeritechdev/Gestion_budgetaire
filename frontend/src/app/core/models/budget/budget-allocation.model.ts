import { ExerciceBudgetaire } from './exercice-budgetaire.model';
import { ObjectifStrategique } from './objectif-strategique.model';
import { ObjectifOperationnel } from './objectif-operationnel.model';
import { ActiviteCatalogue } from './activite-catalogue.model';
import { Structure } from '../structure.model';

export interface BudgetAllocationOs {
  id?: string;
  exerciceBudgetaire?: ExerciceBudgetaire;
  objectifStrategique?: ObjectifStrategique;
  montantAlloue: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface AllocationOsRequestDto {
  exerciceId: string;
  osId: string;
  montantAlloue: number;
}

export interface BudgetAllocationOp {
  id?: string;
  budgetAllocationOs?: BudgetAllocationOs;
  objectifOperationnel?: ObjectifOperationnel;
  montantAlloue: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface AllocationOpRequestDto {
  allocationOsId: string;
  opId: string;
  montantAlloue: number;
}

export interface BudgetAllocationActivite {
  id?: string;
  budgetAllocationOp?: BudgetAllocationOp;
  activiteCatalogue?: ActiviteCatalogue;
  montantAlloue: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface AllocationActiviteRequestDto {
  allocationOpId: string;
  activiteCatalogueId: string;
  montantAlloue: number;
}

export interface BudgetAllocationStructure {
  id?: string;
  budgetAllocationActivite?: BudgetAllocationActivite;
  structure?: Structure;
  montantVote: number;
  montantModifie: number;
  montantAlloue: number;
  montantEngage: number;
  montantLiquide: number;
  montantDisponible: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface AllocationStructureRequestDto {
  allocationActiviteId: string;
  structureCode: string;
  montantVote: number;
}

export interface ExecutionStructureUpdateRequestDto {
  montantEngage: number;
  montantLiquide: number;
}
