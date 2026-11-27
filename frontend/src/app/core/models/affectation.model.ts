/** Niveau de l'organisation auquel un salarié est affecté. */
export type NiveauAffectation = 'STRUCTURE' | 'DIRECTION' | 'SERVICE';

export type StatutAffectation = 'EN_COURS' | 'A_VENIR' | 'TERMINEE';

export const NIVEAUX_AFFECTATION: { valeur: NiveauAffectation; libelle: string; aide: string }[] = [
  { valeur: 'STRUCTURE', libelle: 'Structure', aide: 'Le salarié travaille pour l’ensemble de la structure.' },
  { valeur: 'DIRECTION', libelle: 'Direction', aide: 'Le salarié est rattaché à une direction, sans service précis.' },
  { valeur: 'SERVICE', libelle: 'Service', aide: 'Le salarié travaille dans un service d’une direction.' },
];

export const STATUTS_AFFECTATION: Record<StatutAffectation, string> = {
  EN_COURS: 'En cours',
  A_VENIR: 'À venir',
  TERMINEE: 'Terminée',
};

export interface Affectation {
  id: string;
  salarieId: string;
  salarieMatricule: string;
  salarieNom: string;
  salariePoste: string | null;
  niveau: NiveauAffectation;
  /** Code de la structure, ou identifiant de la direction ou du service. */
  cibleId: string;
  cibleCode: string;
  cibleLibelle: string;
  /** Ex. « UAB_VIE › DIR-FIN › SVC-COMPTA ». */
  chemin: string;
  structureCode: string | null;
  structureNom: string | null;
  fonction: string | null;
  dateDebut: string;
  dateFin: string | null;
  statut: StatutAffectation;
  commentaire: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface AffectationRequest {
  salarieId: string;
  niveau: NiveauAffectation;
  cibleId: string;
  fonction: string | null;
  dateDebut: string;
  dateFin: string | null;
  commentaire: string | null;
}

/** Affectation en cours la plus récente, telle qu'affichée dans le dossier d'un salarié ou d'un compte. */
export interface AffectationResume {
  niveau: NiveauAffectation;
  cibleCode: string;
  cibleLibelle: string;
  chemin: string;
  structureCode: string | null;
  directionLibelle: string | null;
  serviceLibelle: string | null;
  fonction: string | null;
}
