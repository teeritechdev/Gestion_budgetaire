import { ObjectifStrategique } from './objectif-strategique.model';

export interface ObjectifOperationnel {
  id?: string;
  objectifStrategique?: ObjectifStrategique;
  codeOp: string;
  libelle: string;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ObjectifOperationnelRequestDto {
  osId: string;
  codeOp: string;
  libelle: string;
  description?: string;
}
