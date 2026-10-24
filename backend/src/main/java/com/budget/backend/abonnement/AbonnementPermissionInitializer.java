package com.budget.backend.abonnement;

import com.budget.backend.security.entity.Permission;
import com.budget.backend.security.repository.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Enregistre les permissions du Module 6 (Abonnements). Classe séparée de PermissionAutoInitializer pour ne pas
 * modifier le Module 1 ; @Order(1) : elles existent avant que DataInitializer ne les donne à ADMIN_SYSTEME.
 */
@Component
@Order(1)
public class AbonnementPermissionInitializer implements CommandLineRunner {

    @Autowired
    private PermissionRepository permissionRepository;

    @Override
    public void run(String... args) {
        List<Def> permissions = List.of(
                new Def("abonnement.lire", "Consulter les abonnements, leurs échéances et les alertes"),
                new Def("abonnement.creer", "Créer un abonnement"),
                new Def("abonnement.modifier", "Modifier, suspendre, résilier ou reconduire un abonnement"),
                new Def("abonnement.generer_echeances", "Générer l'échéancier d'un abonnement"),
                new Def("echeance.modifier", "Suivre une échéance (BC, facture, paiement, annulation)")
        );
        int ajoutees = 0;
        for (Def def : permissions) {
            if (permissionRepository.findByCode(def.code()).isEmpty()) {
                permissionRepository.save(Permission.builder()
                        .code(def.code()).libelle(def.libelle()).module("ACHATS").build());
                ajoutees++;
            }
        }
        System.out.println("--> [ABONNEMENT-PERMISSION] " + ajoutees + " nouvelle(s) permission(s) du Module 6 enregistrée(s).");
    }

    private record Def(String code, String libelle) {}
}
