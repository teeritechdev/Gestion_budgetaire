package com.budget.backend.achat;

import com.budget.backend.security.entity.Permission;
import com.budget.backend.security.repository.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Enregistre les permissions du Module 4 (Achats). Classe séparée de PermissionAutoInitializer pour ne pas
 * modifier le Module 1 ; @Order(1) : elles existent avant que DataInitializer ne les donne à ADMIN_SYSTEME.
 */
@Component
@Order(1)
public class AchatPermissionInitializer implements CommandLineRunner {

    @Autowired
    private PermissionRepository permissionRepository;

    @Override
    public void run(String... args) {
        List<Def> permissions = List.of(
                new Def("fournisseur.lire", "Consulter les fournisseurs"),
                new Def("fournisseur.creer", "Créer un fournisseur"),
                new Def("fournisseur.modifier", "Modifier ou suspendre un fournisseur"),
                new Def("demande_achat.lire", "Consulter les demandes d'achat"),
                new Def("demande_achat.creer", "Créer une demande d'achat"),
                new Def("demande_achat.modifier", "Modifier sa demande d'achat avant validation"),
                new Def("demande_achat.viser", "Valider une demande d'achat (étape N+1)"),
                new Def("demande_achat.controler_budget", "Contrôle budgétaire d'une demande d'achat"),
                new Def("demande_achat.approuver_dg", "Approuver une demande d'achat (Direction Générale)"),
                new Def("bon_commande.lire", "Consulter les bons de commande et leur PDF"),
                new Def("bon_commande.creer", "Créer un bon de commande"),
                new Def("bon_commande.emettre", "Envoyer un bon de commande au fournisseur")
        );
        int ajoutees = 0;
        for (Def def : permissions) {
            if (permissionRepository.findByCode(def.code()).isEmpty()) {
                permissionRepository.save(Permission.builder()
                        .code(def.code()).libelle(def.libelle()).module("ACHATS").build());
                ajoutees++;
            }
        }
        System.out.println("--> [ACHAT-PERMISSION] " + ajoutees + " nouvelle(s) permission(s) du Module 4 enregistrée(s).");
    }

    private record Def(String code, String libelle) {}
}
