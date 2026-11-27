export interface WorkflowEtape {
  id?: string;
  workflowId?: string;
  numEtape: number;
  codeEtape: string;
  libelle: string;
  permissionRequise?: string;
  roleRequis?: string;
  montantMinimum?: number;
  montantMaximum?: number;
  etapeSuivanteSuccesId?: string;
  etapeSuivanteSuccesLibelle?: string;
  etapeSuivanteRejetId?: string;
  etapeSuivanteRejetLibelle?: string;
  estEtapeFinale?: boolean;
  estEtapeRejet?: boolean;
}

export interface WorkflowDefinition {
  id: string;
  code: string;
  libelle: string;
  description?: string;
  typeEntite: string;
  actif: boolean;
  etapes?: WorkflowEtape[];
  createdAt?: string;
  updatedAt?: string;
}

export interface WorkflowDefinitionRequest {
  code: string;
  libelle: string;
  description?: string;
  typeEntite: string;
  actif?: boolean;
}

export interface WorkflowInstance {
  id: string;
  workflowId: string;
  workflowLibelle: string;
  dossierId: string;
  typeEntite: string;
  referenceDossier: string;
  objet: string;
  montantTotal: number;
  demandeur?: {
    id: string;
    nom: string;
    prenom: string;
    email: string;
  };
  etapeCourante?: WorkflowEtape;
  statutActuel: 'EN_ATTENTE_VALIDATION' | 'APPROUVE' | 'REJETE' | 'ANNULE';
  createdAt: string;
  updatedAt?: string;
}

export interface WorkflowDecisionRequest {
  decision: 'VALIDER' | 'REJETER';
  commentaire?: string;
}

export interface WorkflowHistorique {
  id: string;
  daId?: string;
  instanceId?: string;
  etape: string;
  valideur: {
    id: string;
    nom: string;
    prenom: string;
    email: string;
  };
  decision: string;
  commentaire?: string;
  dateDecision: string;
}

