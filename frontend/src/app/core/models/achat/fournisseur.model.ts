export interface Fournisseur {
  id: string;
  codeFournisseur: string;
  raisonSociale: string;
  ifu: string;
  email?: string | null;
  telephone?: string | null;
  adresse?: string | null;
  actif: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface FournisseurRequest {
  codeFournisseur?: string;
  raisonSociale: string;
  ifu: string;
  email?: string;
  telephone?: string;
  adresse?: string;
  actif?: boolean;
}

