export type RepartitionCspCleType = 'EFFECTIF' | 'CHIFFRE_AFFAIRES' | 'AUTRE';

export interface RepartitionCspRule {
  id?: string;
  activiteCspId: string;
  allocationStructureId?: string | null;
  entiteBeneficiaire: string;
  activiteBeneficiaireId: string;
  cleType?: RepartitionCspCleType | string;
  valeurCle?: number | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface RepartitionCspRuleRequest {
  activiteCspId: string;
  allocationStructureId: string;
  entiteBeneficiaire: string;
  activiteBeneficiaireId: string;
  cleType: RepartitionCspCleType;
  valeurCle: number | null;
}
