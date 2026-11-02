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
}
