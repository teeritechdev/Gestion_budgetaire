export interface EntiteStatistiqueCle {
  id?: string;
  anneeExercice: number;
  mois?: number | null;
  entiteCode: string;
  effectifAgents?: number | null;
  chiffreAffaires?: number | null;
  createdAt?: string;
  updatedAt?: string;
}

export type EntiteStatistiqueCleRequest = Omit<EntiteStatistiqueCle, 'id' | 'createdAt' | 'updatedAt'>;
