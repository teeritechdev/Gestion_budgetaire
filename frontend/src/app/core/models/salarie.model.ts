import { AffectationResume } from './affectation.model';

export type SexeSalarie = 'FEMME' | 'HOMME' | 'AUTRE' | 'NON_RENSEIGNE';

export interface Salarie {
  id: string;
  matricule: string;
  nom: string;
  prenom: string;
  sexe: SexeSalarie;
  telephone?: string | null;
  email?: string | null;
  poste?: string | null;
  dateEmbauche?: string | null;
  dateNaissance?: string | null;
  lieuNaissance?: string | null;
  /** Carte nationale d'identité burkinabè : B suivi de chiffres, ex. B12345678. */
  numeroCnib?: string | null;
  /** Affectation en cours la plus récente (gérée dans l'onglet Affectations). */
  affectation: AffectationResume | null;
  managerId?: string | null;
  actif: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface SalarieRequest {
  matricule: string;
  nom: string;
  prenom: string;
  sexe: SexeSalarie;
  telephone?: string | null;
  email?: string | null;
  poste?: string | null;
  dateEmbauche?: string | null;
  dateNaissance?: string | null;
  lieuNaissance?: string | null;
  numeroCnib?: string | null;
  managerId?: string | null;
  actif: boolean;
}
