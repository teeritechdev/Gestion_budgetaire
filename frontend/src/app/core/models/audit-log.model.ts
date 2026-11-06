export interface AuditLog {
  id: string;
  utilisateurId?: string;
  typeAction: string;
  nomTable: string;
  enregistrementId: string;
  anciennesValeurs?: Record<string, any>;
  nouvellesValeurs?: Record<string, any>;
  adresseIp?: string;
  userAgent?: string;
  createdAt: string;
}

/** Vue détaillée renvoyée par GET /audit-logs/{id}/detail. */
export interface AuditLogDetail {
  id: string;
  createdAt: string;
  typeAction: string;
  nomTable: string;
  enregistrementId: string;
  utilisateurId?: string | null;
  auteur: string;
  auteurUsername?: string | null;
  auteurMatricule?: string | null;
  auteurRole?: string | null;
  adresseIp?: string | null;
  userAgent?: string | null;
  navigateur: string;
  requete: string;
  champs: { nom: string; ancienneValeur: string | null; nouvelleValeur: string | null; modifie: boolean }[];
  anciennesValeurs?: Record<string, unknown> | null;
  nouvellesValeurs?: Record<string, unknown> | null;
}
