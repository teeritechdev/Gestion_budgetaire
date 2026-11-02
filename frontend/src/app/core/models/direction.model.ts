export interface Direction {
  id: string;
  code: string;
  libelle: string;
  /** Null pour une direction CSP : elle sert toutes les structures du groupe. */
  structureCode: string | null;
  structureNom: string | null;
  responsableId?: string | null;
  actif: boolean;
  /** Direction jouant le rôle de centre de services partagés (CSP). */
  estCsp?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface DirectionRequest {
  code: string;
  libelle: string;
  structureCode: string;
  actif: boolean;
  estCsp: boolean;
}
