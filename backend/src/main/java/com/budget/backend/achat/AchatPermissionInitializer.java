package com.budget.backend.achat;

import com.budget.backend.security.entity.Permission;
import com.budget.backend.security.repository.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Enregistre les permissions du Module 4 (Achats). Classe à @Order(1) pour garantir
 * qu'elles sont créées avant que DataInitializer (Order 2) ne les attribue au rôle ADMIN_SYSTEME.
 */
@Component
@Order(1)
public class AchatPermissionInitializer implements CommandLineRunner {

    @Autowired
    private PermissionRepository permissionRepository;

    @Override
    public void run(String... args) {
        List<Def> permissions = List.of(
                new Def("fournisseur.lire", "Consulter le référentiel des fournisseurs"),
                new Def("fournisseur.creer", "Créer un fournisseur"),
                new Def("fournisseur.modifier", "Modifier un fournisseur"),
                new Def("fournisseur.exporter", "Exporter la liste des fournisseurs"),
                new Def("demande_achat.lire", "Consulter les demandes d'achat"),
                new Def("demande_achat.creer", "Créer ou soumettre une demande d'achat"),
                new Def("demande_achat.modifier", "Modifier une demande d'achat"),
                new Def("demande_achat.valider", "Valider ou rejeter une demande d'achat"),
                new Def("bon_commande.lire", "Consulter les bons de commande"),
                new Def("bon_commande.creer", "Émettre un bon de commande fournisseur")
        );
        int ajoutees = 0;
        for (Def def : permissions) {
            if (permissionRepository.findByCode(def.code()).isEmpty()) {
                permissionRepository.save(Permission.builder()
                        .code(def.code()).libelle(def.libelle()).module("ACHATS").build());
                ajoutees++;
            }
        }
        if (ajoutees > 0) {
            System.out.println("--> [ACHAT-PERMISSION] " + ajoutees + " nouvelle(s) permission(s) du Module 4 enregistrée(s).");
        }
    }

    private record Def(String code, String libelle) {}
}

