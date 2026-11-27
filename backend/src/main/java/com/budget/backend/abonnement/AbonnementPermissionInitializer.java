package com.budget.backend.abonnement;

import com.budget.backend.security.entity.Permission;
import com.budget.backend.security.repository.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Enregistre les permissions du Module 6 (Abonnements & Contrats Récurrents).
 * Classe à @Order(1) pour garantir qu'elles sont créées avant que DataInitializer ne les attribue au rôle ADMIN_SYSTEME.
 */
@Component
@Order(1)
public class AbonnementPermissionInitializer implements CommandLineRunner {

    @Autowired
    private PermissionRepository permissionRepository;

    @Override
    public void run(String... args) {
        List<Def> permissions = List.of(
                new Def("abonnement.lire", "Consulter les contrats d'abonnement et échéanciers"),
                new Def("abonnement.creer", "Créer un contrat d'abonnement récurrent"),
                new Def("abonnement.modifier", "Modifier un abonnement ou générer des échéances"),
                new Def("abonnement.supprimer", "Supprimer ou résilier un contrat d'abonnement")
        );
        int ajoutees = 0;
        for (Def def : permissions) {
            if (permissionRepository.findByCode(def.code()).isEmpty()) {
                permissionRepository.save(Permission.builder()
                        .code(def.code()).libelle(def.libelle()).module("ABONNEMENTS").build());
                ajoutees++;
            }
        }
        if (ajoutees > 0) {
            System.out.println("--> [ABONNEMENT-PERMISSION] " + ajoutees + " nouvelle(s) permission(s) du Module 6 enregistrée(s).");
        }
    }

    private record Def(String code, String libelle) {}
}

