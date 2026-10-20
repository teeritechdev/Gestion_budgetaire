package com.budget.backend.security.security;

import com.budget.backend.security.entity.Permission;
import com.budget.backend.security.repository.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PermissionAutoInitializer implements CommandLineRunner {

    @Autowired
    private PermissionRepository permissionRepository;

    @Override
    public void run(String... args) throws Exception {
        // Catalogue exhaustif de toutes les actions de chaque table du Module 1 et du Module 2
        List<PermissionDefinition> permissionsAEnregistrer = List.of(
                // ==========================================
                // MODULE 1 : SÉCURITÉ & ORGANISATION
                // ==========================================

                // 1. Entités (entites)
                new PermissionDefinition("entite.lire", "Lister et consulter les entités", "SECURITE"),
                new PermissionDefinition("entite.creer", "Créer une nouvelle entité", "SECURITE"),
                new PermissionDefinition("entite.modifier", "Modifier une entité existante", "SECURITE"),
                new PermissionDefinition("entite.supprimer", "Supprimer une entité", "SECURITE"),
                new PermissionDefinition("entite.exporter", "Exporter les entités (CSV/PDF)", "SECURITE"),
                new PermissionDefinition("entite.importer", "Importer les entités en masse", "SECURITE"),

                // 2. Structures (structures)
                new PermissionDefinition("structure.lire", "Consulter l'organigramme et les structures", "SECURITE"),
                new PermissionDefinition("structure.creer", "Créer une structure", "SECURITE"),
                new PermissionDefinition("structure.modifier", "Modifier une structure", "SECURITE"),
                new PermissionDefinition("structure.supprimer", "Supprimer une structure", "SECURITE"),
                new PermissionDefinition("structure.exporter", "Exporter l'organigramme (CSV/PDF)", "SECURITE"),
                new PermissionDefinition("structure.importer", "Importer l'organigramme en masse", "SECURITE"),

                // 3. Rôles (roles)
                new PermissionDefinition("role.lire", "Consulter les rôles RBAC", "SECURITE"),
                new PermissionDefinition("role.creer", "Créer un rôle et lier des permissions", "SECURITE"),
                new PermissionDefinition("role.modifier", "Modifier un rôle et ses permissions", "SECURITE"),
                new PermissionDefinition("role.supprimer", "Supprimer un rôle", "SECURITE"),
                new PermissionDefinition("role.exporter", "Exporter les rôles (CSV/PDF)", "SECURITE"),
                new PermissionDefinition("role.importer", "Importer les rôles en masse", "SECURITE"),

                // 4. Permissions (permissions)
                new PermissionDefinition("permission.lire", "Consulter le catalogue des permissions", "SECURITE"),
                new PermissionDefinition("permission.creer", "Créer une permission", "SECURITE"),
                new PermissionDefinition("permission.modifier", "Modifier une permission", "SECURITE"),
                new PermissionDefinition("permission.supprimer", "Supprimer une permission", "SECURITE"),
                new PermissionDefinition("permission.exporter", "Exporter les permissions (CSV/PDF)", "SECURITE"),
                new PermissionDefinition("permission.importer", "Importer les permissions en masse", "SECURITE"),

                // 5. Utilisateurs (utilisateurs)
                new PermissionDefinition("utilisateur.lire", "Consulter les comptes utilisateurs", "SECURITE"),
                new PermissionDefinition("utilisateur.creer", "Créer un utilisateur", "SECURITE"),
                new PermissionDefinition("utilisateur.modifier", "Modifier un utilisateur", "SECURITE"),
                new PermissionDefinition("utilisateur.supprimer", "Supprimer un utilisateur", "SECURITE"),
                new PermissionDefinition("utilisateur.exporter", "Exporter la liste des utilisateurs (CSV/PDF)", "SECURITE"),
                new PermissionDefinition("utilisateur.importer", "Importer des utilisateurs en masse", "SECURITE"),

                // 6. Audit Logs (audit_logs)
                new PermissionDefinition("audit.lire", "Consulter le journal des audits inaltérables", "SECURITE"),
                new PermissionDefinition("audit.exporter", "Exporter les journaux d'audit (CSV/PDF)", "SECURITE"),


                // ==========================================
                // MODULE 2 : PYRAMIDE BUDGÉTAIRE & CASCADE
                // ==========================================

                // 7. Plans Stratégiques (plans_strategiques)
                new PermissionDefinition("plan.lire", "Consulter les plans stratégiques", "BUDGET_STRATEGIE"),
                new PermissionDefinition("plan.creer", "Créer un plan stratégique", "BUDGET_STRATEGIE"),
                new PermissionDefinition("plan.modifier", "Modifier un plan stratégique", "BUDGET_STRATEGIE"),
                new PermissionDefinition("plan.supprimer", "Supprimer un plan stratégique", "BUDGET_STRATEGIE"),
                new PermissionDefinition("plan.exporter", "Exporter les plans stratégiques (CSV/PDF)", "BUDGET_STRATEGIE"),

                // 8. Objectifs Stratégiques / OS (objectifs_strategiques)
                new PermissionDefinition("os.lire", "Consulter les objectifs stratégiques", "BUDGET_STRATEGIE"),
                new PermissionDefinition("os.creer", "Créer un objectif stratégique", "BUDGET_STRATEGIE"),
                new PermissionDefinition("os.modifier", "Modifier un objectif stratégique", "BUDGET_STRATEGIE"),
                new PermissionDefinition("os.supprimer", "Supprimer un objectif stratégique", "BUDGET_STRATEGIE"),
                new PermissionDefinition("os.exporter", "Exporter les objectifs stratégiques (CSV/PDF)", "BUDGET_STRATEGIE"),

                // 9. Objectifs Opérationnels / OP (objectifs_operationnels)
                new PermissionDefinition("op.lire", "Consulter les objectifs opérationnels", "BUDGET_STRATEGIE"),
                new PermissionDefinition("op.creer", "Créer un objectif opérationnel", "BUDGET_STRATEGIE"),
                new PermissionDefinition("op.modifier", "Modifier un objectif opérationnel", "BUDGET_STRATEGIE"),
                new PermissionDefinition("op.supprimer", "Supprimer un objectif opérationnel", "BUDGET_STRATEGIE"),
                new PermissionDefinition("op.exporter", "Exporter les objectifs opérationnels (CSV/PDF)", "BUDGET_STRATEGIE"),

                // 10. Catalogue des Activités (activites_catalogue)
                new PermissionDefinition("activite.lire", "Consulter le catalogue des activités", "BUDGET_STRATEGIE"),
                new PermissionDefinition("activite.creer", "Créer une activité type", "BUDGET_STRATEGIE"),
                new PermissionDefinition("activite.modifier", "Modifier une activité type", "BUDGET_STRATEGIE"),
                new PermissionDefinition("activite.supprimer", "Supprimer une activité type", "BUDGET_STRATEGIE"),
                new PermissionDefinition("activite.exporter", "Exporter le catalogue des activités (CSV/PDF)", "BUDGET_STRATEGIE"),
                new PermissionDefinition("activite.importer", "Importer le catalogue des activités en masse", "BUDGET_STRATEGIE"),

                // 11. Exercices Budgétaires / Campagnes (exercices_budgetaires)
                new PermissionDefinition("exercice.lire", "Consulter les exercices budgétaires", "BUDGET_CASCADE"),
                new PermissionDefinition("exercice.creer", "Créer ou ouvrir un exercice budgétaire", "BUDGET_CASCADE"),
                new PermissionDefinition("exercice.modifier", "Modifier un exercice budgétaire", "BUDGET_CASCADE"),
                new PermissionDefinition("exercice.supprimer", "Supprimer un exercice budgétaire", "BUDGET_CASCADE"),
                new PermissionDefinition("exercice.exporter", "Exporter les exercices budgétaires (CSV/PDF)", "BUDGET_CASCADE"),

                // 12. Allocations OS (budget_allocation_os)
                new PermissionDefinition("allocation_os.lire", "Consulter les allocations au niveau OS", "BUDGET_CASCADE"),
                new PermissionDefinition("allocation_os.creer", "Allouer un budget à un OS", "BUDGET_CASCADE"),
                new PermissionDefinition("allocation_os.modifier", "Modifier une allocation OS", "BUDGET_CASCADE"),
                new PermissionDefinition("allocation_os.exporter", "Exporter les allocations OS (CSV/PDF)", "BUDGET_CASCADE"),

                // 13. Allocations OP (budget_allocation_op)
                new PermissionDefinition("allocation_op.lire", "Consulter les allocations au niveau OP", "BUDGET_CASCADE"),
                new PermissionDefinition("allocation_op.creer", "Allouer un budget à un OP", "BUDGET_CASCADE"),
                new PermissionDefinition("allocation_op.modifier", "Modifier une allocation OP", "BUDGET_CASCADE"),
                new PermissionDefinition("allocation_op.exporter", "Exporter les allocations OP (CSV/PDF)", "BUDGET_CASCADE"),

                // 14. Allocations Activités (budget_allocation_activite)
                new PermissionDefinition("allocation_activite.lire", "Consulter les allocations au niveau activité", "BUDGET_CASCADE"),
                new PermissionDefinition("allocation_activite.creer", "Allouer un budget à une activité", "BUDGET_CASCADE"),
                new PermissionDefinition("allocation_activite.modifier", "Modifier une allocation d'activité", "BUDGET_CASCADE"),
                new PermissionDefinition("allocation_activite.exporter", "Exporter les allocations d'activités (CSV/PDF)", "BUDGET_CASCADE"),

                // 15. Exécution par Structure (budget_allocation_structure)
                new PermissionDefinition("execution.lire", "Consulter le suivi de l'exécution par structure", "BUDGET_EXECUTION"),
                new PermissionDefinition("execution.creer", "Attribuer un budget à une structure", "BUDGET_EXECUTION"),
                new PermissionDefinition("execution.modifier", "Mettre à jour l'exécution (engagements, liquidations)", "BUDGET_EXECUTION"),
                new PermissionDefinition("execution.exporter", "Exporter l'exécution par structure (CSV/PDF)", "BUDGET_EXECUTION"),

                // 16. Révisions & Virements Budgétaires (demandes_revision_budgetaire)
                new PermissionDefinition("revision.lire", "Consulter les demandes de révision et virements", "BUDGET_REVISION"),
                new PermissionDefinition("revision.creer", "Créer une demande de révision ou virement", "BUDGET_REVISION"),
                new PermissionDefinition("revision.valider", "Valider une demande de révision et appliquer le virement", "BUDGET_REVISION"),
                new PermissionDefinition("revision.exporter", "Exporter les demandes et lignes de révision (CSV/PDF)", "BUDGET_REVISION"),

                // 17. Journal des Mouvements Budgétaires (journal_mouvements_budgetaires)
                new PermissionDefinition("journal.lire", "Consulter le journal des mouvements budgétaires", "BUDGET_REVISION"),
                new PermissionDefinition("journal.exporter", "Exporter le journal des mouvements (CSV/PDF)", "BUDGET_REVISION"),

                // 18. Module Règles de Répartition CSP (repartition_csp_rules)
                new PermissionDefinition("repartition_csp.lire", "Consulter les règles de répartition CSP", "BUDGET_REVISION"),
                new PermissionDefinition("repartition_csp.creer", "Créer une règle de répartition CSP", "BUDGET_REVISION"),
                new PermissionDefinition("repartition_csp.modifier", "Modifier une règle de répartition CSP", "BUDGET_REVISION"),
                new PermissionDefinition("repartition_csp.supprimer", "Supprimer une règle de répartition CSP", "BUDGET_REVISION"),

                // 19. Module Clés Statistiques des Entités (entites_statistiques_cles)
                new PermissionDefinition("statistique_cle.lire", "Consulter les clés statistiques des entités", "BUDGET_REVISION"),
                new PermissionDefinition("statistique_cle.creer", "Créer une clé statistique", "BUDGET_REVISION"),
                new PermissionDefinition("statistique_cle.modifier", "Modifier une clé statistique", "BUDGET_REVISION"),
                new PermissionDefinition("statistique_cle.supprimer", "Supprimer une clé statistique", "BUDGET_REVISION"),
                new PermissionDefinition("statistique_cle.importer", "Importer les clés statistiques en masse", "BUDGET_REVISION")
        );

        int countAjoutees = 0;
        for (PermissionDefinition def : permissionsAEnregistrer) {
            if (permissionRepository.findByCode(def.code()).isEmpty()) {
                Permission p = Permission.builder()
                        .code(def.code())
                        .libelle(def.libelle())
                        .module(def.module())
                        .build();
                permissionRepository.save(p);
                countAjoutees++;
            }
        }

        if (countAjoutees > 0) {
            System.out.println("--> [AUTO-PERMISSION] " + countAjoutees + " nouvelles permissions enregistrées automatiquement en base de données !");
        } else {
            System.out.println("--> [AUTO-PERMISSION] Toutes les permissions sont déjà à jour.");
        }
    }

    private record PermissionDefinition(String code, String libelle, String module) {}
}