import { ObjectifOperationnel } from './objectif-operationnel.model';

export interface ActiviteCatalogue {
  id?: string;
  objectifOperationnel?: ObjectifOperationnel;
  codeActivite: string;
  libelle: string;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ActiviteCatalogueRequestDto {
  opId: string;
  codeActivite: string;
  libelle: string;
  description?: string;
}
