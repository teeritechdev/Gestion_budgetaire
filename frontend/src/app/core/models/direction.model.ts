export interface Direction {
  id: string;
  code: string;
  libelle: string;
  /** Structure de rattachement (une seule par direction). */
  structureCode: string;
  structureNom: string | null;
  responsableId?: string | null;
  actif: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface DirectionRequest {
  code: string;
  libelle: string;
  structureCode: string;
  actif: boolean;
}
