import { ServiceOrganisationnel } from './service-organisationnel.model';

export type SexeSalarie = 'FEMME' | 'HOMME' | 'AUTRE' | 'NON_RENSEIGNE';

export interface Salarie {
  id: string;
  matricule: string;
  nom: string;
  prenom: string;
  sexe: SexeSalarie;
  telephone?: string | null;
  email: string;
  poste?: string | null;
  dateEmbauche: string;
  service: ServiceOrganisationnel;
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
  email: string;
  poste?: string | null;
  dateEmbauche: string;
  serviceId: string;
  managerId?: string | null;
  actif: boolean;
}
