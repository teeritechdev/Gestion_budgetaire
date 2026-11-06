export interface LoginRequest {
  username: string;
  password: string;
}

export interface JwtResponse {
  token: string;
  type: string;
  id: string;
  username: string;
  email: string;
  nom: string;
  prenom: string;
  role: string;
  permissions: string[];
  /** Mot de passe temporaire (défini par un administrateur) : à changer avant toute autre action. */
  doitChangerMotDePasse?: boolean;
  avecPhoto?: boolean;
}

export interface CurrentUser {
  id: string;
  username: string;
  email: string;
  nom: string;
  prenom: string;
  role: string;
  permissions: string[];
  token: string;
  doitChangerMotDePasse?: boolean;
  avecPhoto?: boolean;
}
