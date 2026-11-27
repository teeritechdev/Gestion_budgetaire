export interface Article {
  id: string;
  codeArticle: string;
  designation: string;
  description?: string | null;
  uniteMesure: string;
  quantiteStock: number;
  seuilReapprovisionnement?: number | null;
  prixUnitaireMoyenPondere?: number | null;
  actif: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface ArticleRequest {
  codeArticle: string;
  designation: string;
  uniteMesure: string;
  description?: string | null;
  seuilReapprovisionnement?: number | null;
  prixUnitaireMoyenPondere?: number | null;
  actif: boolean;
}

export interface StockKpis {
  totalArticles: number;
  articlesEnAlerte: number;
  valeurTotaleStock: number;
  demandesEnAttenteValidation: number;
  demandesEnAttenteService: number;
}
