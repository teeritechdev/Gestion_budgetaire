package com.budget.backend.stock;

import com.budget.backend.security.entity.Permission;
import com.budget.backend.security.repository.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Enregistre les permissions du Module 3 (Stocks). Classe séparée de PermissionAutoInitializer
 * pour ne pas modifier le code du Module 1 ; @Order(1) garantit qu'elles existent avant que
 * DataInitializer (Order 2) ne les attribue au rôle ADMIN_SYSTEME.
 */
@Component
@Order(1)
public class StockPermissionInitializer implements CommandLineRunner {

    @Autowired
    private PermissionRepository permissionRepository;

    @Override
    public void run(String... args) {
        List<Def> permissions = List.of(
                new Def("article.lire", "Consulter le catalogue des articles"),
                new Def("article.creer", "Créer un article"),
                new Def("article.modifier", "Modifier un article"),
                new Def("demande_magasin.lire", "Consulter les demandes magasin"),
                new Def("demande_magasin.creer", "Créer une demande magasin (bon de sortie)"),
                new Def("demande_magasin.valider", "Valider ou rejeter une demande magasin (N+1)"),
                new Def("demande_magasin.servir", "Servir une demande magasin (magasinier)"),
                new Def("mouvement_stock.lire", "Consulter l'historique des mouvements de stock"),
                new Def("mouvement_stock.creer", "Saisir un mouvement de stock ou un ajustement d'inventaire")
        );
        int ajoutees = 0;
        for (Def def : permissions) {
            if (permissionRepository.findByCode(def.code()).isEmpty()) {
                permissionRepository.save(Permission.builder()
                        .code(def.code()).libelle(def.libelle()).module("STOCKS").build());
                ajoutees++;
            }
        }
        System.out.println("--> [STOCK-PERMISSION] " + ajoutees + " nouvelle(s) permission(s) du Module 3 enregistrée(s).");
    }

    private record Def(String code, String libelle) {}
}
