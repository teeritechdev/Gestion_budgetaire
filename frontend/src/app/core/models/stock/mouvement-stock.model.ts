import { UtilisateurSummary } from './demande-magasin.model';

export type TypeMouvement = 'ENTREE' | 'SORTIE' | 'AJUSTEMENT';

export interface MouvementStock {
  id: string;
  articleId: string;
  typeMouvement: TypeMouvement;
  quantite: number;
  stockAnterieur: number;
  nouveauStock: number;
  referenceDocument: string;
  auteur?: UtilisateurSummary | null;
  createdAt: string;
}

export interface MouvementStockRequest {
  articleId: string;
  typeMouvement: TypeMouvement;
  quantite: number;
  referenceDocument: string;
  prixUnitaire?: number | null;
}
