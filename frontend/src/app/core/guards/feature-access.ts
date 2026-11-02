export const FEATURE_ACCESS = {
  '/budget/plans': ['plan.lire'],
  '/budget/nomenclature': ['os.lire', 'op.lire', 'activite.lire'],
  '/budget/allocations': [
    'allocation_os.lire',
    'allocation_op.lire',
    'allocation_activite.lire',
    'execution.lire',
  ],
  '/budget/repartition-csp': ['repartition_csp.lire'],
  '/budget/statistiques-cles': ['statistique_cle.lire'],
  '/budget/revisions': ['revision.lire'],
  '/budget/journal-mouvements': ['journal.lire'],
  '/budget/exercices': ['exercice.lire'],
  '/security/salaries': ['salarie.lire'],
  '/security/utilisateurs': ['utilisateur.lire'],
  '/security/roles-permissions': ['role.lire', 'permission.lire'],
  '/security/structures': ['entite.lire'],
  '/security/directions': ['direction.lire'],
  '/security/services': ['service.lire'],
  '/security/audit-logs': ['audit.lire'],
} as const satisfies Record<string, readonly string[]>;

export type FeaturePath = keyof typeof FEATURE_ACCESS;
