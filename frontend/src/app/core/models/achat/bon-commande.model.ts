import { Fournisseur } from './fournisseur.model';

export type StatutBonCommande = 'EMIS' | 'EN_COURS' | 'SOLDE' | 'ANNULE';

export interface LigneBonCommande {
  id: string;
  ligneDaId?: string | null;
  articleId?: string | null;
  articleCode?: string | null;
  articleDesignation?: string | null;
  description: string;
  quantiteCommandee: number;
  prixUnitaireHt: number;
  montantTotalHt: number;
  createdAt?: string;
}

export interface BonCommande {
  id: string;
  codeBc: string;
  typeBc: string;
  daId: string;
  daCodeDossier?: string;
  daObjet?: string;
  fournisseur: Fournisseur;
  montantTotalHt: number;
  montantTotalTtc: number;
  montantFactureTtc: number;
  soldeRestantTtc: number;
  statut: StatutBonCommande;
  dateEmission: string;
  lignes: LigneBonCommande[];
  createdAt: string;
  updatedAt?: string;
}

export interface CreerBcDepuisDaRequest {
  fournisseurId: string;
  typeBc?: string;
  tauxTva?: number;
}

