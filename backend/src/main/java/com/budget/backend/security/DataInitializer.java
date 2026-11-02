package com.budget.backend.security;

import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.entity.Direction;
import com.budget.backend.security.entity.ServiceOrganisationnel;
import com.budget.backend.security.entity.Permission;
import com.budget.backend.security.entity.Role;
import com.budget.backend.security.entity.Salarie;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.StructureRepository;
import com.budget.backend.security.repository.DirectionRepository;
import com.budget.backend.security.repository.ServiceOrganisationnelRepository;
import com.budget.backend.security.repository.PermissionRepository;
import com.budget.backend.security.repository.RoleRepository;
import com.budget.backend.security.repository.SalarieRepository;
import com.budget.backend.security.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.time.LocalDate;

@Component
@Order(2) // S'exécute après le PermissionAutoInitializer (Order 1 par défaut)
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private StructureRepository entiteRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DirectionRepository directionRepository;

    @Autowired
    private ServiceOrganisationnelRepository serviceRepository;

    @Autowired
    private SalarieRepository salarieRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Mot de passe du compte admin créé au premier démarrage (surchargeable par la variable APP_ADMIN_INITIAL_PASSWORD).
    @Value("${app.admin.initial-password:Password123!}")
    private String adminInitialPassword;

    @Override
    public void run(String... args) throws Exception {
        // 1. Créer l'entité si elle n'existe pas
        Structure entite = entiteRepository.findById("UAB_VIE").orElseGet(() -> {
            Structure e = Structure.builder()
                    .code("UAB_VIE")
                    .nom("UAB Vie Assurances")
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

        Direction direction = directionRepository.findByCode("DIR-IT")
                .orElseGet(() -> directionRepository.save(Direction.builder()
                        .code("DIR-IT")
                        .libelle("Direction Informatique")
                        .structure(entite)
                        .actif(true)
                        .build()));
        ServiceOrganisationnel service = serviceRepository.findByCode("SVC-IT").orElseGet(() ->
                serviceRepository.save(ServiceOrganisationnel.builder()
                    .code("SVC-IT")
                    .libelle("Service Informatique")
                    .direction(direction)
                    .actif(true)
                    .build()));

        // 5. Créer l'utilisateur Admin uniquement s'il n'existe pas encore.
        //    Un compte existant n'est plus modifié : son rôle reste celui choisi par les administrateurs.
        if (utilisateurRepository.findByUsername("admin@uab.bf").isPresent()) {
            return;
        }
        Salarie adminSalarie = salarieRepository.findByEmail("admin@uab.bf").orElseGet(() ->
                salarieRepository.save(Salarie.builder()
                .matricule("EMP-001")
                .nom("Admin")
                .prenom("Super")
                .sexe("NON_RENSEIGNE")
                .email("admin@uab.bf")
                .dateEmbauche(LocalDate.now())
                .service(service)
                .actif(true)
                .build()));
        Utilisateur newAdmin = Utilisateur.builder()
                .username("admin@uab.bf")
                .motDePasseHash(passwordEncoder.encode(adminInitialPassword))
                .salarie(adminSalarie)
                .role(roleAdmin)
                .actif(true)
                .build();
        utilisateurRepository.save(newAdmin);
        System.out.println("--> [SECURITY SEED] Utilisateur admin@uab.bf créé. Changez son mot de passe initial dès la première connexion.");
    }
}