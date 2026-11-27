package com.budget.backend.security.service;

import com.budget.backend.common.CsvUtils;
import com.budget.backend.security.dto.UtilisateurRequestDto;
import com.budget.backend.security.dto.UtilisateurResponseDto;
import com.budget.backend.security.entity.Role;
import com.budget.backend.security.entity.Salarie;
import com.budget.backend.security.entity.ServiceOrganisationnel;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.RoleRepository;
import com.budget.backend.security.security.PrivilegeGuard;
import com.budget.backend.security.repository.SalarieRepository;
import com.budget.backend.security.repository.ServiceOrganisationnelRepository;
import com.budget.backend.security.repository.UtilisateurRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UtilisateurService {
    private final UtilisateurRepository utilisateurRepository;
    private final SalarieRepository salarieRepository;
    private final ServiceOrganisationnelRepository serviceRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final PrivilegeGuard privilegeGuard;
    private final AffectationService affectationService;

    public UtilisateurService(
            UtilisateurRepository utilisateurRepository,
            SalarieRepository salarieRepository,
            ServiceOrganisationnelRepository serviceRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            PrivilegeGuard privilegeGuard,
            AffectationService affectationService) {
        this.utilisateurRepository = utilisateurRepository;
        this.salarieRepository = salarieRepository;
        this.serviceRepository = serviceRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.privilegeGuard = privilegeGuard;
        this.affectationService = affectationService;
    }

    @Transactional(readOnly = true)
    public List<UtilisateurResponseDto> listerTousLesUtilisateurs() {
        return utilisateurRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<UtilisateurResponseDto> trouverParId(UUID id) {
        return utilisateurRepository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<UtilisateurResponseDto> filtrerUtilisateurs(UUID serviceId, UUID roleId, Boolean actif, String recherche) {
        String terme = recherche == null ? null : recherche.toLowerCase();
        return utilisateurRepository.findAll().stream()
                .filter(user -> serviceId == null || affectationService.estAffecteAuService(user.getSalarie().getId(), serviceId))
                .filter(user -> roleId == null || user.getRole().getId().equals(roleId))
                .filter(user -> actif == null || user.getActif().equals(actif))
                .filter(user -> terme == null
                        || user.getUsername().toLowerCase().contains(terme)
                        || user.getSalarie().getNom().toLowerCase().contains(terme)
                        || user.getSalarie().getPrenom().toLowerCase().contains(terme)
                        || (user.getSalarie().getEmail() != null && user.getSalarie().getEmail().toLowerCase().contains(terme))
                        || user.getSalarie().getMatricule().toLowerCase().contains(terme))
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public UtilisateurResponseDto creerUtilisateur(
            UtilisateurRequestDto request, String motDePasseClair, String utilisateurIdStr) {
        verifierUnicite(request, null, null);
        Role roleAttribue = trouverRole(request.roleId());
        privilegeGuard.verifierPeutGererRole(roleAttribue);
        Salarie savedSalarie;
        if (request.salarieId() == null) {
            validerProfilSalarie(request);
            savedSalarie = salarieRepository.save(creerSalarie(request));
            if (request.serviceId() != null) {
                affectationService.affecterAuService(savedSalarie, trouverService(request.serviceId()), savedSalarie.getDateEmbauche());
            }
        } else {
            savedSalarie = trouverSalarie(request.salarieId());
        }
        if (utilisateurRepository.existsBySalarie_Id(savedSalarie.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce salarié possède déjà un compte utilisateur.");
        }
        Utilisateur utilisateur = Utilisateur.builder()
                .username(request.username().trim())
                .motDePasseHash(passwordEncoder.encode(motDePasseClair))
                // Mot de passe choisi par l'administrateur : à changer à la première connexion.
                .doitChangerMotDePasse(true)
                .motDePasseModifieLe(java.time.Instant.now())
                .salarie(savedSalarie)
                .role(roleAttribue)
                .actif(request.actif())
                .build();
        Utilisateur saved = utilisateurRepository.save(utilisateur);
        audit(utilisateurIdStr, "INSERTION", saved, null,
                Map.of("username", saved.getUsername(), "salarieId", savedSalarie.getId().toString()));
        return toDto(saved);
    }

    @Transactional
    public UtilisateurResponseDto modifierUtilisateur(
            UUID id, UtilisateurRequestDto request, String nouveauMotDePasse, String utilisateurIdStr) {
        Utilisateur utilisateur = trouverUtilisateur(id);
        // On ne peut agir que sur un compte dont le rôle actuel ET le nouveau rôle sont à notre portée.
        privilegeGuard.verifierPeutGererRole(utilisateur.getRole());
        Role nouveauRole = trouverRole(request.roleId());
        privilegeGuard.verifierPeutGererRole(nouveauRole);
        if (!nouveauRole.getId().equals(utilisateur.getRole().getId())) {
            privilegeGuard.interdireSurSoiMeme(id, "Vous ne pouvez pas modifier votre propre rôle.");
        }
        if (!Boolean.TRUE.equals(request.actif())) {
            privilegeGuard.interdireSurSoiMeme(id, "Vous ne pouvez pas désactiver votre propre compte.");
        }
        verifierUnicite(request, id, utilisateur.getSalarie().getId());
        Salarie salarie = utilisateur.getSalarie();
        Map<String, Object> anciennesValeurs = Map.of(
                "username", utilisateur.getUsername(),
                "email", value(salarie.getEmail()),
                "matricule", salarie.getMatricule(),
                "actif", utilisateur.getActif());
        if (request.salarieId() != null && !request.salarieId().equals(salarie.getId())) {
            if (utilisateurRepository.existsBySalarie_Id(request.salarieId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Ce salarié possède déjà un compte utilisateur.");
            }
            salarie = trouverSalarie(request.salarieId());
            utilisateur.setSalarie(salarie);
        } else if (request.salarieId() == null && request.matricule() != null) {
            validerProfilSalarie(request);
            mettreAJourSalarie(salarie, request);
            if (request.serviceId() != null) {
                affectationService.affecterAuService(salarie, trouverService(request.serviceId()), salarie.getDateEmbauche());
            }
        }
        utilisateur.setUsername(request.username().trim());
        utilisateur.setRole(nouveauRole);
        utilisateur.setActif(request.actif());
        if (nouveauMotDePasse != null && !nouveauMotDePasse.isBlank()) {
            // Réinitialisé par un administrateur : temporaire, et les sessions ouvertes sont fermées.
            utilisateur.setMotDePasseHash(passwordEncoder.encode(nouveauMotDePasse));
            utilisateur.setDoitChangerMotDePasse(true);
            utilisateur.setMotDePasseModifieLe(java.time.Instant.now());
        }

        Utilisateur updated = utilisateurRepository.save(utilisateur);
        audit(utilisateurIdStr, "MODIFICATION", updated, anciennesValeurs,
                Map.of("username", updated.getUsername(), "actif", updated.getActif()));
        return toDto(updated);
    }

    @Transactional
    public void supprimerUtilisateur(UUID id, String utilisateurIdStr) {
        Utilisateur utilisateur = trouverUtilisateur(id);
        privilegeGuard.interdireSurSoiMeme(id, "Vous ne pouvez pas supprimer votre propre compte.");
        privilegeGuard.verifierPeutGererRole(utilisateur.getRole());
        Map<String, Object> anciennesValeurs = Map.of(
                "username", utilisateur.getUsername(),
                "salarieId", utilisateur.getSalarie().getId().toString());
        utilisateurRepository.delete(utilisateur);
        if (utilisateurIdStr != null) {
            auditService.logAction(UUID.fromString(utilisateurIdStr), "SUPPRESSION", "utilisateurs",
                    id.toString(), anciennesValeurs, null);
        }
    }

    @Transactional(readOnly = true)
    public String exporterEnCsv(UUID serviceId, UUID roleId, Boolean actif, String recherche) {
        return exporterEnCsv(filtrerUtilisateurs(serviceId, roleId, actif, recherche));
    }

    /** Exporte exactement la liste fournie (lignes affichées à l’écran, dans leur ordre). */
    public String exporterEnCsv(List<UtilisateurResponseDto> users) {
        StringBuilder csv = new StringBuilder(
                "ID;Username;Matricule;Nom;Prenom;Email;Poste;Date_Embauche;Affectation;Role;Actif;Date_Creation\n");
        for (UtilisateurResponseDto user : users) {
            csv.append(CsvUtils.ligne(';', user.id(), user.username(), user.matricule(), user.nom(), user.prenom(),
                    user.email(), value(user.poste()), user.dateEmbauche(), user.affectation() == null ? null : user.affectation().chemin(),
                    user.role().getCode(), user.actif(), user.createdAt()));
        }
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public byte[] exporterEnPdf(UUID serviceId, UUID roleId, Boolean actif, String recherche) {
        return exporterEnPdf(filtrerUtilisateurs(serviceId, roleId, actif, recherche));
    }

    /** Exporte exactement la liste fournie (lignes affichées à l’écran, dans leur ordre). */
    public byte[] exporterEnPdf(List<UtilisateurResponseDto> users) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Comptes utilisateurs et salariés", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            for (String column : List.of("Username", "Matricule", "Nom", "Prénom", "Email", "Affectation", "Rôle")) {
                table.addCell(column);
            }
            for (UtilisateurResponseDto user : users) {
                table.addCell(user.username());
                table.addCell(user.matricule());
                table.addCell(user.nom());
                table.addCell(user.prenom());
                table.addCell(value(user.email()));
                table.addCell(user.affectation() == null ? "" : user.affectation().cibleLibelle());
                table.addCell(user.role().getCode());
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Erreur lors de la génération du PDF des utilisateurs.", exception);
        }
    }

    @Transactional
    public int importerCsv(MultipartFile file, String utilisateurIdStr) {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                String[] data = line.split(";", -1);
                if (data.length < 8) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Chaque ligne CSV doit contenir 8 colonnes : matricule, nom, prénom, email, mot de passe, code service (peut être vide), code rôle et poste.");
                }
                String matricule = data[0].trim();
                String nom = data[1].trim();
                String prenom = data[2].trim();
                String email = data[3].trim();
                String password = data[4].trim();
                String serviceCode = data[5].trim();
                String roleCode = data[6].trim();
                String poste = data[7].trim();
                String username = data.length > 8 && !data[8].isBlank() ? data[8].trim() : email;
                if (username.isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Identifiant manquant pour " + matricule + " : renseignez l'e-mail ou le nom d'utilisateur (9e colonne).");
                }
                LocalDate dateEmbauche = data.length > 9 && !data[9].isBlank() ? LocalDate.parse(data[9].trim()) : null;
                String sexe = data.length > 10 ? sexeImporte(data[10]) : null;
                LocalDate dateNaissance = data.length > 11 && !data[11].isBlank() ? LocalDate.parse(data[11].trim()) : null;
                String lieuNaissance = data.length > 12 && !data[12].isBlank() ? data[12].trim() : null;
                String numeroCnib = data.length > 13 && !data[13].isBlank() ? data[13].trim() : null;
                if (sexe == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Sexe manquant ou invalide pour " + matricule + " (11e colonne) : HOMME ou FEMME (H / F).");
                }
                if (password.length() < 8) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Chaque mot de passe importé doit contenir au moins 8 caractères.");
                }
                // Service facultatif : s'il est indiqué, le salarié y est affecté.
                ServiceOrganisationnel service = serviceCode.isEmpty() ? null : serviceRepository.findByCode(serviceCode)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Service introuvable pour l'import : " + serviceCode));
                Role role = roleRepository.findByCode(roleCode)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Rôle introuvable pour l'import : " + roleCode));
                UtilisateurRequestDto request = new UtilisateurRequestDto(username, null, matricule, nom, prenom,
                        sexe, null, email, poste, dateEmbauche, dateNaissance, lieuNaissance, numeroCnib,
                        service == null ? null : service.getId(), null, role.getId(), true);
                creerUtilisateur(request, password, utilisateurIdStr);
                count++;
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Erreur lors de l'import CSV des utilisateurs.", exception);
        }
        return count;
    }

    private Salarie creerSalarie(UtilisateurRequestDto request) {
        Salarie salarie = new Salarie();
        mettreAJourSalarie(salarie, request);
        return salarie;
    }

    private void mettreAJourSalarie(Salarie salarie, UtilisateurRequestDto request) {
        salarie.setMatricule(request.matricule().trim());
        salarie.setNom(request.nom().trim());
        salarie.setPrenom(request.prenom().trim());
        salarie.setSexe(request.sexe());
        salarie.setTelephone(request.telephone());
        salarie.setEmail(request.email() == null || request.email().isBlank()
                ? null : request.email().trim().toLowerCase());
        salarie.setPoste(request.poste().trim());
        salarie.setDateEmbauche(request.dateEmbauche());
        salarie.setDateNaissance(request.dateNaissance());
        salarie.setLieuNaissance(request.lieuNaissance() == null || request.lieuNaissance().isBlank()
                ? null : request.lieuNaissance().trim());
        String cnib = normaliserCnib(request.numeroCnib());
        boolean cnibPris = cnib != null && (salarie.getId() == null
                ? salarieRepository.existsByNumeroCnib(cnib)
                : salarieRepository.existsByNumeroCnibAndIdNot(cnib, salarie.getId()));
        if (cnibPris) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un salarié avec ce numéro CNIB existe déjà.");
        }
        salarie.setNumeroCnib(cnib);
        if (request.managerId() != null && request.managerId().equals(salarie.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un salarié ne peut pas être son propre manager.");
        }
        salarie.setManager(request.managerId() == null ? null : trouverSalarie(request.managerId()));
        salarie.setActif(request.actif());
    }

    private void verifierUnicite(UtilisateurRequestDto request, UUID utilisateurId, UUID salarieId) {
        String username = request.username() == null ? "" : request.username().trim();
        if (utilisateurId == null ? utilisateurRepository.existsByUsername(username)
                : utilisateurRepository.existsByUsernameAndIdNot(username, utilisateurId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce nom d'utilisateur est déjà utilisé.");
        }
        if (request.salarieId() == null && request.matricule() != null) {
            boolean matriculeExiste = salarieId == null
                    ? salarieRepository.existsByMatricule(request.matricule().trim())
                    : salarieRepository.existsByMatriculeAndIdNot(request.matricule().trim(), salarieId);
            boolean emailExiste = request.email() != null && !request.email().isBlank() && (salarieId == null
                    ? salarieRepository.existsByEmail(request.email().trim().toLowerCase())
                    : salarieRepository.existsByEmailAndIdNot(request.email().trim().toLowerCase(), salarieId));
            if (matriculeExiste || emailExiste) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Un salarié avec ce matricule ou cette adresse e-mail existe déjà.");
            }
        }
        if (request.actif() == null || request.roleId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le rôle et le statut du compte sont obligatoires.");
        }
    }

    /** HOMME / FEMME, également H, F, M, masculin, féminin ; null si la valeur n'est pas reconnue. */
    private static String sexeImporte(String valeur) {
        String cle = java.text.Normalizer.normalize(valeur.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT);
        return switch (cle) {
            case "homme", "h", "m", "masculin" -> "HOMME";
            case "femme", "f", "feminin" -> "FEMME";
            default -> null;
        };
    }

    /** Majuscules, sans espaces ; null si non renseigné. */
    private static String normaliserCnib(String numeroCnib) {
        if (numeroCnib == null || numeroCnib.isBlank()) return null;
        return numeroCnib.replaceAll("\\s", "").toUpperCase(java.util.Locale.ROOT);
    }

    private void validerProfilSalarie(UtilisateurRequestDto request) {
        if (request.matricule() == null || request.matricule().isBlank()
                || request.nom() == null || request.nom().isBlank()
                || request.prenom() == null || request.prenom().isBlank()
                || request.sexe() == null
                || request.poste() == null || request.poste().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le dossier salarié doit préciser matricule, nom, prénom, sexe et poste.");
        }
        if (request.dateNaissance() != null && !request.dateNaissance().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La date de naissance doit être antérieure à aujourd'hui.");
        }
        if (request.dateNaissance() != null && request.dateEmbauche() != null
                && !request.dateNaissance().isBefore(request.dateEmbauche())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La date de naissance doit être antérieure à la date d'embauche.");
        }
        String cnib = normaliserCnib(request.numeroCnib());
        if (cnib != null && !cnib.matches("B\\d{5,15}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le numéro CNIB doit commencer par B suivi de chiffres (ex. B12345678).");
        }
        if (!java.util.Set.of("FEMME", "HOMME").contains(request.sexe())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sélectionnez une valeur valide pour le sexe.");
        }
        if (request.telephone() != null && request.telephone().length() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le numéro de téléphone ne peut pas dépasser 30 caractères.");
        }
    }

    private Utilisateur trouverUtilisateur(UUID id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
    }

    private Salarie trouverSalarie(UUID id) {
        return salarieRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salarié introuvable."));
    }

    private ServiceOrganisationnel trouverService(UUID id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service introuvable."));
    }

    private Role trouverRole(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rôle introuvable."));
    }

    private void audit(String utilisateurIdStr, String action, Utilisateur user,
                       Map<String, Object> anciennesValeurs, Map<String, Object> nouvellesValeurs) {
        if (utilisateurIdStr != null) {
            auditService.logAction(UUID.fromString(utilisateurIdStr), action, "utilisateurs",
                    user.getId().toString(), anciennesValeurs, nouvellesValeurs);
        }
    }

    private UtilisateurResponseDto toDto(Utilisateur user) {
        Salarie salarie = user.getSalarie();
        return new UtilisateurResponseDto(user.getId(), user.getUsername(), salarie.getId(),
                salarie.getMatricule(), salarie.getNom(), salarie.getPrenom(), salarie.getSexe(),
                salarie.getTelephone(), salarie.getEmail(), salarie.getPoste(), salarie.getDateEmbauche(),
                affectationService.resumeActuelle(salarie),
                salarie.getManager() == null ? null : salarie.getManager().getId(),
                user.getRole(), user.getActif(), user.getCreatedAt(), user.getUpdatedAt());
    }

    private String value(String text) {
        return text == null ? "" : text;
    }
}
