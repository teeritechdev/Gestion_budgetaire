import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { FEATURE_ACCESS } from './core/guards/feature-access';
import { permissionGuard } from './core/guards/permission.guard';
import { motDePasseGuard } from './core/guards/mot-de-passe.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'mot-de-passe-oublie',
    loadComponent: () => import('./features/auth/mot-de-passe-oublie.component').then(m => m.MotDePasseOublieComponent)
  },
  {
    path: '',
    loadComponent: () => import('./features/layout/app-layout.component').then(m => m.AppLayoutComponent),
    canActivate: [authGuard],
    canActivateChild: [motDePasseGuard],
    children: [
      { path: 'profil', loadComponent: () => import('./features/profil/profil.component').then(m => m.ProfilComponent) },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', loadComponent: () => import('./features/budget/dashboard/budget-dashboard.component').then(m => m.BudgetDashboardComponent) },
      { path: 'budget/dashboard', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'budget/plans', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/budget/plans'] }, loadComponent: () => import('./features/budget/plans/plans-list.component').then(m => m.PlansListComponent) },
      { path: 'budget/nomenclature', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/budget/nomenclature'] }, loadComponent: () => import('./features/budget/nomenclature/nomenclature.component').then(m => m.NomenclatureComponent) },
      { path: 'budget/allocations', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/budget/allocations'] }, loadComponent: () => import('./features/budget/allocations/allocations.component').then(m => m.AllocationsComponent) },
      { path: 'budget/repartition-csp', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/budget/repartition-csp'] }, loadComponent: () => import('./features/budget/repartition-csp/repartition-csp.component').then(m => m.RepartitionCspComponent) },
      { path: 'budget/statistiques-cles', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/budget/statistiques-cles'] }, loadComponent: () => import('./features/budget/statistiques-cles/statistiques-cles.component').then(m => m.StatistiquesClesComponent) },
      { path: 'budget/revisions', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/budget/revisions'] }, loadComponent: () => import('./features/budget/revisions/revisions.component').then(m => m.RevisionsComponent) },
      { path: 'budget/journal-mouvements', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/budget/journal-mouvements'] }, loadComponent: () => import('./features/budget/journal-mouvements/journal-mouvements.component').then(m => m.JournalMouvementsComponent) },
      { path: 'budget/exercices', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/budget/exercices'] }, loadComponent: () => import('./features/budget/exercices/exercices-list.component').then(m => m.ExercicesListComponent) },
      { path: 'security', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'security/dashboard', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'security/salaries', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/security/salaries'] }, loadComponent: () => import('./features/security/salaries/salaries-list.component').then(m => m.SalariesListComponent) },
      { path: 'security/affectations', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/security/affectations'] }, loadComponent: () => import('./features/security/affectations/affectations-list.component').then(m => m.AffectationsListComponent) },
      { path: 'security/utilisateurs', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/security/utilisateurs'] }, loadComponent: () => import('./features/security/utilisateurs/utilisateurs-list.component').then(m => m.UtilisateursListComponent) },
      { path: 'security/roles-permissions', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/security/roles-permissions'] }, loadComponent: () => import('./features/security/roles-permissions/roles-permissions.component').then(m => m.RolesPermissionsComponent) },
      { path: 'security/structures', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/security/structures'] }, loadComponent: () => import('./features/security/entites/structures-list.component').then(m => m.StructuresListComponent) },
      { path: 'security/directions', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/security/directions'] }, loadComponent: () => import('./features/security/structures/directions-list.component').then(m => m.DirectionsListComponent) },
      { path: 'security/services', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/security/services'] }, loadComponent: () => import('./features/security/structures/services-list.component').then(m => m.ServicesListComponent) },
      { path: 'security/directions-services', pathMatch: 'full', redirectTo: 'security/directions' },
      { path: 'security/unites-organisationnelles', pathMatch: 'full', redirectTo: 'security/directions' },
      { path: 'security/entites', pathMatch: 'full', redirectTo: 'security/structures' },
      { path: 'security/audit-logs', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/security/audit-logs'] }, loadComponent: () => import('./features/security/audit-logs/audit-logs-list.component').then(m => m.AuditLogsListComponent) },
      { path: 'stock', pathMatch: 'full', redirectTo: 'stock/articles' },
      { path: 'stock/articles', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/stock/articles'] }, loadComponent: () => import('./features/stock/articles/articles-list.component').then(m => m.ArticlesListComponent) },
      { path: 'stock/demandes-magasin', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/stock/demandes-magasin'] }, loadComponent: () => import('./features/stock/demandes-magasin/demandes-magasin.component').then(m => m.DemandesMagasinComponent) },
      { path: 'stock/mouvements', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/stock/mouvements'] }, loadComponent: () => import('./features/stock/mouvements/mouvements-stock.component').then(m => m.MouvementsStockComponent) },
      { path: 'achats', pathMatch: 'full', redirectTo: 'achats/fournisseurs' },
      { path: 'achats/fournisseurs', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/achats/fournisseurs'] }, loadComponent: () => import('./features/achats/fournisseurs/fournisseurs-list.component').then(m => m.FournisseursListComponent) },
      { path: 'achats/demandes-achat', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/achats/demandes-achat'] }, loadComponent: () => import('./features/achats/demandes-achat/demandes-achat-list.component').then(m => m.DemandesAchatListComponent) },
      { path: 'achats/bons-commande', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/achats/bons-commande'] }, loadComponent: () => import('./features/achats/bons-commande/bons-commande-list.component').then(m => m.BonsCommandeListComponent) },
      { path: 'achats/abonnements', pathMatch: 'full', redirectTo: 'abonnements' },
      { path: 'abonnements', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/abonnements'] }, loadComponent: () => import('./features/abonnements/abonnements-list.component').then(m => m.AbonnementsListComponent) },
      { path: 'abonnements/echeancier', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/abonnements/echeancier'] }, loadComponent: () => import('./features/abonnements/echeancier-global.component').then(m => m.EcheancierGlobalComponent) },
      { path: 'workflows', pathMatch: 'full', redirectTo: 'workflows/definitions' },
      { path: 'workflows/definitions', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/workflows/definitions'] }, loadComponent: () => import('./features/workflows/workflow-definitions-list.component').then(m => m.WorkflowDefinitionsListComponent) },
      { path: 'workflows/instances', canActivate: [permissionGuard], data: { requiredPermissions: FEATURE_ACCESS['/workflows/instances'] }, loadComponent: () => import('./features/workflows/workflow-instances-list.component').then(m => m.WorkflowInstancesListComponent) }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
