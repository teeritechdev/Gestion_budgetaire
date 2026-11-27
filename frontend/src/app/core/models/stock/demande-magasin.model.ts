import { Article } from './article.model';

export type StatutDemandeMagasin = 'SOUMIS' | 'VALIDE_N1' | 'PARTIELLEMENT_SERVIE' | 'SERVIE' | 'REJETEE';

export interface UtilisateurSummary {
  id: string;
  username?: string;
  matricule?: string;
  nom: string;
  prenom: string;
  email?: string;
  poste?: string;
}

export interface LigneDemandeMagasin {
  id: string;
  article: Article;
  quantiteDemande: number;
  quantiteServie: number;
}

export interface DemandeMagasin {
  id: string;
  numeroDemande: string;
  demandeur: UtilisateurSummary;
  serviceId: string;
  structureCode: string;
  objet: string;
  statut: StatutDemandeMagasin;
  valideurN1?: UtilisateurSummary | null;
  dateValidation?: string | null;
  magasinier?: UtilisateurSummary | null;
  dateServie?: string | null;
  lignes: LigneDemandeMagasin[];
  createdAt?: string;
}

export interface DemandeMagasinRequest {
  serviceId: string;
  structureCode?: string | null;
  objet: string;
  lignes: { articleId: string; quantiteDemande: number }[];
}

export interface ValidationDecision {
  decision: 'APPROUVE' | 'REJETE';
  commentaire?: string;
}

export interface ServirDemande {
  lignes: { ligneId: string; quantiteServie: number }[];
}
