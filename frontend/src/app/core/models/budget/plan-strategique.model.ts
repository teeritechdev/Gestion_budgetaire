export interface PlanStrategique {
  id?: string;
  codePlan: string;
  libelle: string;
  dateDebut: string;
  dateFin: string;
  actif: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface PlanStrategiqueRequestDto {
  codePlan: string;
  libelle: string;
  dateDebut: string;
  dateFin: string;
  actif?: boolean;
}
