package com.budget.backend.security;

import com.budget.backend.security.entity.Entite;
import com.budget.backend.security.entity.Permission;
import com.budget.backend.security.entity.Role;
import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.EntiteRepository;
import com.budget.backend.security.repository.PermissionRepository;
import com.budget.backend.security.repository.RoleRepository;
import com.budget.backend.security.repository.StructureRepository;
import com.budget.backend.security.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

@Component
@Order(2) // S'exécute après le PermissionAutoInitializer (Order 1 par défaut)
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private EntiteRepository entiteRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StructureRepository structureRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 1. Créer l'entité si elle n'existe pas
        Entite entite = entiteRepository.findById("UAB_VIE").orElseGet(() -> {
            Entite e = Entite.builder()
                    .code("UAB_VIE")
                    .nom("UAB Vie Assurances")
                    .estCsP(false)
                    .build();
            return entiteRepository.save(e);
        });

        // 2. Récupérer toutes les permissions (Modules 1 & 2)
        List<Permission> toutesLesPermissions = permissionRepository.findAll();

        // 3. Créer ou mettre à jour le rôle Admin avec l'intégralité des permissions
        Role roleAdmin = roleRepository.findByCode("ADMIN_SYSTEME").map(r -> {
            // Met à jour les permissions si le rôle existe déjà
            r.setPermissions(new HashSet<>(toutesLesPermissions));
            return roleRepository.save(r);
        }).orElseGet(() -> {
            Role r = Role.builder()
                    .code("ADMIN_SYSTEME")
                    .libelle("Administrateur Système")
                    .description("Accès total et global à tous les modules")
                    .permissions(new HashSet<>(toutesLesPermissions))
                    .build();
            return roleRepository.save(r);
        });

        System.out.println("--> [SECURITY SEED] Rôle ADMIN_SYSTEME configuré avec " + toutesLesPermissions.size() + " permissions.");

        // 4. Créer la structure si elle n'existe pas
        Structure structure = structureRepository.findByCode("DIR-IT").orElseGet(() -> {
            Structure s = Structure.builder()
                    .code("DIR-IT")
                    .libelle("Direction Informatique")
                    .typeStructure("DIRECTION")
                    .entite(entite)
                    .actif(true)
                    .build();
            return structureRepository.save(s);
        });

        // 5. Créer l'utilisateur Admin s'il n'existe pas
        if (!utilisateurRepository.existsByEmail("admin@uab.bf")) {
            Utilisateur admin = Utilisateur.builder()
                    .matricule("EMP-001")
                    .nom("Admin")
                    .prenom("Super")
                    .email("admin@uab.bf")
                    .motDePasseHash(passwordEncoder.encode("Password123!"))
                    .structure(structure)
                    .role(roleAdmin)
                    .actif(true)
                    .build();
            utilisateurRepository.save(admin);
            System.out.println("--> [SECURITY SEED] Utilisateur admin@uab.bf créé avec succès ! (Mot de passe: Password123!)");
        }
    }
}