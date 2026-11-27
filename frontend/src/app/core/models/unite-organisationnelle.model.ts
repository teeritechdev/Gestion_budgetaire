import { Structure } from './structure.model';

export type TypeStructure = 'DIRECTION' | 'DEPARTEMENT' | 'SERVICE' | 'AGENCE';

export interface UniteOrganisationnelle {
  id?: string;
  code: string;
  libelle: string;
  typeStructure: TypeStructure | string;
  structure: Structure;
  parent?: UniteOrganisationnelle | null;
  direction?: UniteOrganisationnelle | null;
  parentId?: string | null;
  directionId?: string | null;
  responsableId?: string | null;
  actif: boolean;
  createdAt?: string;
  updatedAt?: string;
}
