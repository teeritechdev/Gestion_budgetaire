export interface ServiceOrganisationnel {
  id: string;
  code: string;
  libelle: string;
  directionId: string;
  directionCode: string;
  directionLibelle: string;
  structureCode: string;
  responsableId?: string | null;
  actif: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface ServiceOrganisationnelRequest {
  code: string;
  libelle: string;
  directionId: string;
  actif: boolean;
}
