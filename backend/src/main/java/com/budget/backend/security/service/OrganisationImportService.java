package com.budget.backend.security.service;

import com.budget.backend.common.CsvReader;
import com.budget.backend.security.dto.DirectionRequestDto;
import com.budget.backend.security.dto.SalarieRequestDto;
import com.budget.backend.security.dto.SalarieResponseDto;
import com.budget.backend.security.dto.ServiceRequestDto;
import com.budget.backend.security.entity.Direction;
import com.budget.backend.security.entity.Salarie;
import com.budget.backend.security.entity.ServiceOrganisationnel;
import com.budget.backend.security.repository.DirectionRepository;
import com.budget.backend.security.repository.SalarieRepository;
import com.budget.backend.security.repository.ServiceOrganisationnelRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Imports CSV de l'organisation (directions, services, salariés), transactionnels et validés comme à l'écran. */
@Service
public class OrganisationImportService {

    private static final Map<String, String> SEXES = Map.ofEntries(
            Map.entry("homme", "HOMME"), Map.entry("h", "HOMME"), Map.entry("m", "HOMME"), Map.entry("masculin", "HOMME"),
            Map.entry("femme", "FEMME"), Map.entry("f", "FEMME"), Map.entry("feminin", "FEMME"),
            Map.entry("autre", "AUTRE"), Map.entry("non_renseigne", "NON_RENSEIGNE"), Map.entry("non renseigne", "NON_RENSEIGNE"));

    private final Validator validator;
    private final DirectionService directionService;
    private final ServiceOrganisationnelService serviceService;
    private final SalarieService salarieService;
    private final DirectionRepository directionRepository;
    private final ServiceOrganisationnelRepository serviceRepository;
    private final SalarieRepository salarieRepository;

    public OrganisationImportService(
            Validator validator,
            DirectionService directionService,
            ServiceOrganisationnelService serviceService,
            SalarieService salarieService,
            DirectionRepository directionRepository,
            ServiceOrganisationnelRepository serviceRepository,
            SalarieRepository salarieRepository) {
        this.validator = validator;
        this.directionService = directionService;
        this.serviceService = serviceService;
        this.salarieService = salarieService;
        this.directionRepository = directionRepository;
        this.serviceRepository = serviceRepository;
        this.salarieRepository = salarieRepository;
    }

    // --- Directions : code;libelle;code_structure;actif ---
    @Transactional
    public int importerDirections(MultipartFile fichier) {
        // code_structure : obligatoire pour une direction classique, vide pour une direction CSP.
        return CsvReader.lire(fichier, List.of("code", "libelle"), ligne -> {
            DirectionRequestDto dto = new DirectionRequestDto(
                    ligne.texte("code"), ligne.texte("libelle"),
                    ligne.texte("code_structure", "structure_code"), ligne.booleen(true, "actif"),
                    ligne.booleen(false, "est_csp", "csp"));
            valider(dto);
            directionService.creer(dto);
        });
    }

    // --- Services : code;libelle;code_direction;actif ---
    @Transactional
    public int importerServices(MultipartFile fichier) {
        return CsvReader.lire(fichier, List.of("code", "libelle", "code_direction|direction_code"), ligne -> {
            String codeDirection = ligne.obligatoire("code_direction", "direction_code");
            Direction direction = directionRepository.findByCode(codeDirection)
                    .orElseThrow(() -> new IllegalArgumentException("direction « " + codeDirection + " » introuvable"));
            ServiceRequestDto dto = new ServiceRequestDto(
                    ligne.texte("code"), ligne.texte("libelle"), direction.getId(), ligne.booleen(true, "actif"));
            valider(dto);
            serviceService.creer(dto);
        });
    }

    /**
     * Salariés : matricule;nom;prenom;sexe;telephone;email;poste;date_embauche;code_service;matricule_manager;actif.
     * Le manager peut être un salarié existant ou un salarié importé dans le même fichier (même plus bas).
     */
    @Transactional
    public int importerSalaries(MultipartFile fichier) {
        record Rattachement(UUID salarieId, SalarieRequestDto dto, String matriculeManager, int ligne) {}
        List<Rattachement> managersDiffere = new ArrayList<>();

        int lues = CsvReader.lire(fichier, List.of("matricule", "nom", "prenom", "email", "code_service|service"), ligne -> {
            String codeService = ligne.obligatoire("code_service", "service");
            ServiceOrganisationnel service = serviceRepository.findByCode(codeService)
                    .orElseThrow(() -> new IllegalArgumentException("service « " + codeService + " » introuvable"));
            String matriculeManager = ligne.texte("matricule_manager", "manager");
            UUID managerId = matriculeManager == null ? null
                    : salarieRepository.findByMatricule(matriculeManager).map(Salarie::getId).orElse(null);
            LocalDate dateEmbauche = ligne.date("date_embauche");
            SalarieRequestDto dto = new SalarieRequestDto(
                    upper(ligne.texte("matricule")), ligne.texte("nom"), ligne.texte("prenom"),
                    sexe(ligne.texte("sexe")), ligne.texte("telephone"), lower(ligne.texte("email")),
                    ligne.texte("poste"), dateEmbauche == null ? LocalDate.now() : dateEmbauche,
                    service.getId(), managerId, ligne.booleen(true, "actif"));
            valider(dto);
            SalarieResponseDto cree = salarieService.creer(dto);
            if (matriculeManager != null && managerId == null) {
                managersDiffere.add(new Rattachement(cree.id(), dto, matriculeManager, ligne.numero()));
            }
        });

        for (Rattachement r : managersDiffere) {
            UUID managerId = salarieRepository.findByMatricule(r.matriculeManager()).map(Salarie::getId)
                    .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.BAD_REQUEST,
                            "Ligne " + r.ligne() + " : manager de matricule « " + r.matriculeManager() + " » introuvable"));
            SalarieRequestDto d = r.dto();
            salarieService.modifier(r.salarieId(), new SalarieRequestDto(d.matricule(), d.nom(), d.prenom(), d.sexe(),
                    d.telephone(), d.email(), d.poste(), d.dateEmbauche(), d.serviceId(), managerId, d.actif()));
        }
        return lues;
    }

    private static String sexe(String valeur) {
        if (valeur == null) {
            return "NON_RENSEIGNE";
        }
        String cle = Normalizer.normalize(valeur.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        String sexe = SEXES.get(cle);
        if (sexe == null) {
            throw new IllegalArgumentException("sexe « " + valeur + " » invalide : HOMME, FEMME, AUTRE ou NON_RENSEIGNE");
        }
        return sexe;
    }

    private static String upper(String valeur) {
        return valeur == null ? null : valeur.toUpperCase(Locale.ROOT);
    }

    private static String lower(String valeur) {
        return valeur == null ? null : valeur.toLowerCase(Locale.ROOT);
    }

    private void valider(Object dto) {
        Set<ConstraintViolation<Object>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            ConstraintViolation<Object> v = violations.iterator().next();
            throw new IllegalArgumentException(v.getPropertyPath() + " : " + v.getMessage());
        }
    }
}
