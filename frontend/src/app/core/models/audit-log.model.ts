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
