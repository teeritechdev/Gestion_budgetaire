package com.budget.backend.workflow;

import com.budget.backend.security.entity.Permission;
import com.budget.backend.security.repository.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Enregistre les permissions du moteur de workflow. Classe séparée de PermissionAutoInitializer pour ne pas
 * modifier le Module 1 ; @Order(1) : elles existent avant que DataInitializer ne les donne à ADMIN_SYSTEME.
 */
@Component
@Order(1)
public class WorkflowPermissionInitializer implements CommandLineRunner {

    @Autowired
    private PermissionRepository permissionRepository;

    @Override
    public void run(String... args) {
        List<Def> permissions = List.of(
                new Def("workflow.lire", "Consulter les définitions de workflow"),
                new Def("workflow.creer", "Créer une définition de workflow (étapes, seuils, permissions)")
        );
        int ajoutees = 0;
        for (Def def : permissions) {
            if (permissionRepository.findByCode(def.code()).isEmpty()) {
                permissionRepository.save(Permission.builder()
                        .code(def.code()).libelle(def.libelle()).module("WORKFLOW").build());
                ajoutees++;
            }
        }
        System.out.println("--> [WORKFLOW-PERMISSION] " + ajoutees + " nouvelle(s) permission(s) du moteur de workflow enregistrée(s).");
    }

    private record Def(String code, String libelle) {}
}
