export interface Profil {
  id: string;
  username: string;
  nom: string | null;
  prenom: string | null;
  sexe: string | null;
  email: string | null;
  telephone: string | null;
  matricule: string | null;
  poste: string | null;
  dateEmbauche: string | null;
  serviceLibelle: string | null;
  directionLibelle: string | null;
  structureCode: string | null;
  roleCode: string | null;
  roleLibelle: string | null;
  compteCreeLe: string | null;
  motDePasseModifieLe: string | null;
  doitChangerMotDePasse: boolean;
  avecPhoto: boolean;
  photoModifieeLe: string | null;
}
