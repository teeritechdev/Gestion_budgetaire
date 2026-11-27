export interface ObjectifStrategique {
  id?: string;
  codeOs: string;
  libelle: string;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ObjectifStrategiqueRequestDto {
  codeOs: string;
  libelle: string;
  description?: string;
}
