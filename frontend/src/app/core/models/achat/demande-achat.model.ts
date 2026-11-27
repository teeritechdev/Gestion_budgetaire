import { UtilisateurSummary } from '../stock/demande-magasin.model';

export type TypeAchat = 'STOCKABLE' | 'PRESTATION_SERVICE' | 'INVESTISSEMENT_IMMO' | 'TRAVAUX';
export type StatutDemandeAchat = 'BROUILLON' | 'SOUMIS' | 'EN_COURS_VALIDATION' | 'APPROUVE' | 'REJETE' | 'COMMANDE';

export interface LigneDemandeAchat {
  id: string;
  activiteId: string;
  activiteCode?: string;
  activiteLibelle?: string;
  articleId?: string | null;
  articleCode?: string | null;
  articleDesignation?: string | null;
  description: string;
  uniteMesure?: string;
  quantite: number;
  prixUnitaireEstime: number;
  montantTotalHt: number;
  createdAt?: string;
}

export interface LigneDemandeAchatRequest {
  activiteId: string;
  articleId?: string | null;
  description: string;
  uniteMesure?: string;
  quantite: number;
  prixUnitaireEstime: number;
}

export interface DemandeAchat {
  id: string;
  codeDossier: string;
  entiteEmettrice: string;
  entiteBeneficiaire: string;
  typeAchat: TypeAchat;
  objet: string;
  justification?: string | null;
  montantEstimeHt: number;
  montantEstimeTtc: number;
  statut: StatutDemandeAchat;
  etapeCourante?: string;
  demandeur: UtilisateurSummary;
  serviceDemandeurId?: string | null;
  serviceDemandeurCode?: string | null;
  serviceDemandeurLibelle?: string | null;
  exerciceAnnee: number;
  lignes: LigneDemandeAchat[];
  createdAt: string;
  updatedAt?: string;
}

export interface DemandeAchatRequest {
  codeDossier?: string;
  entiteEmettrice: string;
  entiteBeneficiaire: string;
  typeAchat: TypeAchat;
  objet: string;
  justification?: string;
  serviceDemandeurId?: string | null;
  exerciceAnnee: number;
  montantEstimeTtc?: number;
  lignes: LigneDemandeAchatRequest[];
}

export interface HistoriqueValidation {
  id: string;
  daId: string;
  etape: string;
  valideur: UtilisateurSummary;
  decision: 'APPROUVE' | 'REJETE' | string;
  commentaire?: string | null;
  dateDecision: string;
}

export interface DecisionValidationRequest {
  decision: 'APPROUVE' | 'REJETE' | string;
  commentaire?: string;
  etape?: string;
}

export interface VentilationImpact {
  id: string;
  daId: string;
  entiteImpactee: string;
  activiteImpacteeId?: string | null;
  activiteImpacteeCode?: string | null;
  activiteImpacteeLibelle?: string | null;
  montantImpute: number;
  pourcentageApplique: number;
  statutBlocage?: boolean;
  createdAt?: string;
}

export interface VentilationImpactRequest {
  entiteImpactee: string;
  activiteImpacteeId?: string | null;
  montantImpute: number;
  pourcentageApplique: number;
  statutBlocage?: boolean;
}



