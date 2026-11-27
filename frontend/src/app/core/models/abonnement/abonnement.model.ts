import { Fournisseur } from '../achat/fournisseur.model';

export type PeriodiciteAbonnement = 'MENSUEL' | 'TRIMESTRIEL' | 'SEMESTRIEL' | 'ANNUEL';

export type StatutAbonnement = 'ACTIF' | 'SUSPENDU' | 'RESILIE' | 'EXPIRE';

export type StatutEcheance = 'A_VENIR' | 'BON_COMMANDE_GENERE' | 'FACTURE' | 'ANNULE';

export interface EcheanceAbonnement {
  id: string;
  abonnementId: string;
  numEcheance: number;
  dateEcheance: string;
  montantAttenduTtc: number;
  montantFactureTtc?: number | null;
  statut: StatutEcheance | string;
  bcId?: string | null;
  bcCode?: string | null;
  referenceFacture?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface Abonnement {
  id: string;
  codeAbonnement: string;
  libelle: string;
  fournisseur: Fournisseur;
  activiteId?: string | null;
  activiteCode?: string | null;
  activiteLibelle?: string | null;
  periodicite: PeriodiciteAbonnement;
  jourFacturation: number;
  montantEcheanceHt: number;
  montantEcheanceTtc: number;
  dateDebut: string;
  dateFin?: string | null;
  taciteReconduction: boolean;
  preavisJours: number;
  statut: StatutAbonnement;
  nombreEcheances: number;
  echeances?: EcheanceAbonnement[];
  createdAt?: string;
}

export interface AbonnementRequest {
  codeAbonnement?: string;
  libelle: string;
  fournisseurId: string;
  activiteId?: string | null;
  periodicite: PeriodiciteAbonnement;
  jourFacturation?: number;
  montantEcheanceHt: number;
  montantEcheanceTtc?: number;
  dateDebut: string;
  dateFin?: string | null;
  taciteReconduction?: boolean;
  preavisJours?: number;
  statut?: StatutAbonnement;
}

export interface RapprochementFactureRequest {
  referenceFacture: string;
  montantFactureTtc: number;
  dateFacture?: string | null;
  commentaire?: string | null;
}
