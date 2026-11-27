package com.budget.backend.workflow;

import com.budget.backend.security.entity.Permission;
import com.budget.backend.security.repository.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Enregistre les permissions du Module 8 (Moteur de Workflow Transversal & Circuits d'Approbation).
 * Classe à @Order(1) pour garantir qu'elles sont créées avant que DataInitializer ne les attribue au rôle ADMIN_SYSTEME.
 */
@Component
@Order(1)
public class WorkflowPermissionInitializer implements CommandLineRunner {

    @Autowired
    private PermissionRepository permissionRepository;

    @Override
    public void run(String... args) {
        List<Def> permissions = List.of(
                new Def("workflow.lire", "Consulter les modèles et instances de workflow"),
                new Def("workflow.creer", "Créer un nouveau modèle de circuit d'approbation"),
                new Def("workflow.modifier", "Configurer les étapes, seuils et transitions de workflow"),
                new Def("workflow.supprimer", "Supprimer un modèle ou une étape de workflow"),
                new Def("workflow.valider", "Approuver ou rejeter une étape de circuit d'approbation")
        );
        int ajoutees = 0;
        for (Def def : permissions) {
            if (permissionRepository.findByCode(def.code()).isEmpty()) {
                permissionRepository.save(Permission.builder()
                        .code(def.code())
                        .libelle(def.libelle())
                        .module("WORKFLOW")
                        .build());
                ajoutees++;
            }
        }
        if (ajoutees > 0) {
            System.out.println("--> [WORKFLOW-PERMISSION] " + ajoutees + " nouvelle(s) permission(s) du Module 8 enregistrée(s).");
        }
    }

    private record Def(String code, String libelle) {}
}

