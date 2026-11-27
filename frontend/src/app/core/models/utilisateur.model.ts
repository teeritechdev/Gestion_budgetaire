import { Role } from './role.model';
import { AffectationResume } from './affectation.model';

export interface Utilisateur {
  id?: string;
  username: string;
  salarieId?: string;
  matricule: string;
  nom: string;
  prenom: string;
  sexe: 'FEMME' | 'HOMME' | 'AUTRE' | 'NON_RENSEIGNE';
  telephone?: string | null;
  email: string;
  poste?: string | null;
  dateEmbauche: string;
  affectation: AffectationResume | null;
  managerId?: string | null;
  role: Role;
  actif: boolean;
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Compte de connexion. Il est rattaché à un salarié existant via salarieId ; les champs
 * du dossier salarié restent optionnels (utilisés uniquement par l'import CSV historique).
 */
export interface UtilisateurRequest {
  username: string;
  salarieId?: string;
  matricule?: string;
  nom?: string;
  prenom?: string;
  sexe?: Utilisateur['sexe'];
  telephone?: string | null;
  email?: string;
  poste?: string | null;
  dateEmbauche?: string;
  serviceId?: string;
  managerId?: string | null;
  roleId: string;
  actif: boolean;
}
