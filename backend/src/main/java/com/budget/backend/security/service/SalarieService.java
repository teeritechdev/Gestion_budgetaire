package com.budget.backend.security.service;

import java.util.Map;
import com.budget.backend.common.PdfUtils;
import com.budget.backend.common.CsvUtils;
import com.budget.backend.security.dto.SalarieRequestDto;
import com.budget.backend.security.dto.SalarieResponseDto;
import com.budget.backend.security.entity.Salarie;
import com.budget.backend.security.repository.AffectationRepository;
import com.budget.backend.security.repository.SalarieRepository;
import com.budget.backend.security.repository.UtilisateurRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class SalarieService {
    private final SalarieRepository salarieRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AffectationRepository affectationRepository;
    private final AffectationService affectationService;

    public SalarieService(
            SalarieRepository salarieRepository,
            UtilisateurRepository utilisateurRepository,
            AffectationRepository affectationRepository,
            AffectationService affectationService) {
        this.salarieRepository = salarieRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.affectationRepository = affectationRepository;
        this.affectationService = affectationService;
    }

    @Transactional(readOnly = true)
    public List<SalarieResponseDto> lister(UUID serviceId, Boolean actif, String recherche) {
        String terme = recherche == null ? null : recherche.toLowerCase();
        return salarieRepository.findAll().stream()
                .filter(salarie -> serviceId == null || affectationService.estAffecteAuService(salarie.getId(), serviceId))
                .filter(salarie -> actif == null || salarie.getActif().equals(actif))
                .filter(salarie -> terme == null
                        || salarie.getMatricule().toLowerCase().contains(terme)
                        || salarie.getNom().toLowerCase().contains(terme)
                        || salarie.getPrenom().toLowerCase().contains(terme)
                        || (salarie.getEmail() != null && salarie.getEmail().toLowerCase().contains(terme)))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SalarieResponseDto obtenir(UUID id) {
        return toDto(trouverSalarie(id));
    }

    @Transactional
    public SalarieResponseDto creer(SalarieRequestDto request) {
        verifierUnicite(request, null);
        Salarie salarie = new Salarie();
        appliquer(salarie, request);
        return toDto(salarieRepository.save(salarie));
    }

    @Transactional
    public SalarieResponseDto modifier(UUID id, SalarieRequestDto request) {
        Salarie salarie = trouverSalarie(id);
        verifierUnicite(request, id);
        appliquer(salarie, request);
        return toDto(salarieRepository.save(salarie));
    }

    @Transactional
    public void supprimer(UUID id) {
        Salarie salarie = trouverSalarie(id);
        if (utilisateurRepository.existsBySalarie_Id(id) || salarieRepository.existsByManager_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce salarié ne peut pas être supprimé tant qu'un compte utilisateur ou un collaborateur y est rattaché.");
        }
        // Les affectations appartiennent au dossier du salarié : elles sont supprimées avec lui.
        affectationRepository.deleteBySalarie_Id(id);
        salarieRepository.delete(salarie);
    }

    private void verifierUnicite(SalarieRequestDto request, UUID id) {
        boolean matriculeExiste = id == null
                ? salarieRepository.existsByMatricule(request.matricule().trim())
                : salarieRepository.existsByMatriculeAndIdNot(request.matricule().trim(), id);
        String email = normaliserEmail(request.email());
        boolean emailExiste = email != null && (id == null
                ? salarieRepository.existsByEmail(email)
                : salarieRepository.existsByEmailAndIdNot(email, id));
        if (matriculeExiste || emailExiste) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un salarié avec ce matricule ou cette adresse e-mail existe déjà.");
        }
        String cnib = normaliserCnib(request.numeroCnib());
        if (cnib != null && (id == null
                ? salarieRepository.existsByNumeroCnib(cnib)
                : salarieRepository.existsByNumeroCnibAndIdNot(cnib, id))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un salarié avec ce numéro CNIB existe déjà.");
        }
    }

    /** Minuscules, sans espaces autour ; null si non renseigné. */
    private static String normaliserEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String texte(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }

    /** Majuscules, sans espaces ; null si non renseigné. */
    private static String normaliserCnib(String numeroCnib) {
        if (numeroCnib == null || numeroCnib.isBlank()) return null;
        return numeroCnib.replaceAll("\\s", "").toUpperCase(java.util.Locale.ROOT);
    }

    private void appliquer(Salarie salarie, SalarieRequestDto request) {
        if (request.managerId() != null && request.managerId().equals(salarie.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un salarié ne peut pas être son propre manager.");
        }
        salarie.setMatricule(request.matricule().trim());
        salarie.setNom(request.nom().trim());
        salarie.setPrenom(request.prenom().trim());
        salarie.setSexe(request.sexe());
        salarie.setTelephone(request.telephone());
        salarie.setEmail(normaliserEmail(request.email()));
        salarie.setPoste(request.poste().trim());
        salarie.setDateEmbauche(request.dateEmbauche());
        if (request.dateNaissance() != null && request.dateEmbauche() != null
                && !request.dateNaissance().isBefore(request.dateEmbauche())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La date de naissance doit être antérieure à la date d'embauche.");
        }
        salarie.setDateNaissance(request.dateNaissance());
        salarie.setLieuNaissance(texte(request.lieuNaissance()));
        salarie.setNumeroCnib(normaliserCnib(request.numeroCnib()));
        salarie.setManager(request.managerId() == null ? null : trouverSalarie(request.managerId()));
        salarie.setActif(request.actif());
    }

    private Salarie trouverSalarie(UUID id) {
        return salarieRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salarié introuvable."));
    }

    private SalarieResponseDto toDto(Salarie salarie) {
        return new SalarieResponseDto(salarie.getId(), salarie.getMatricule(), salarie.getNom(),
                salarie.getPrenom(), salarie.getSexe(), salarie.getTelephone(), salarie.getEmail(),
                salarie.getPoste(), salarie.getDateEmbauche(), salarie.getDateNaissance(), salarie.getLieuNaissance(),
                salarie.getNumeroCnib(), affectationService.resumeActuelle(salarie),
                salarie.getManager() == null ? null : salarie.getManager().getId(),
                salarie.getActif(), salarie.getCreatedAt(), salarie.getUpdatedAt());
    }

    // --- Exports (liste fournie : lignes affichées à l'écran) ---
    public String exporterEnCsv(List<SalarieResponseDto> salaries) {
        Map<UUID, String> matricules = matriculesParId();
        StringBuilder csv = new StringBuilder(
                "Matricule;Nom;Prenom;Sexe;Telephone;Email;Poste;Date_Embauche;Affectation;Matricule_Manager;Actif;"
                        + "Date_Naissance;Lieu_Naissance;Numero_CNIB\n");
        for (SalarieResponseDto s : salaries) {
            csv.append(CsvUtils.ligne(';', s.matricule(), s.nom(), s.prenom(), s.sexe(), s.telephone(), s.email(),
                    s.poste(), s.dateEmbauche(), s.affectation() == null ? null : s.affectation().chemin(),
                    s.managerId() == null ? null : matricules.get(s.managerId()),
                    Boolean.TRUE.equals(s.actif()) ? "oui" : "non",
                    s.dateNaissance(), s.lieuNaissance(), s.numeroCnib()));
        }
        return csv.toString();
    }

    public byte[] exporterEnPdf(List<SalarieResponseDto> salaries) {
        Map<UUID, String> matricules = matriculesParId();
        return PdfUtils.tableau("Liste des salariés",
                List.of("Matricule", "Nom et prénom", "N° CNIB", "Naissance", "E-mail", "Poste", "Affectation", "Embauche", "Manager", "Statut"),
                salaries.stream().map(s -> java.util.Arrays.<Object>asList(s.matricule(), s.nom() + " " + s.prenom(),
                        s.numeroCnib(), naissance(s),
                        s.email(), s.poste(), s.affectation() == null ? "" : s.affectation().cibleLibelle(), s.dateEmbauche(),
                        s.managerId() == null ? "" : matricules.getOrDefault(s.managerId(), ""),
                        Boolean.TRUE.equals(s.actif()) ? "Actif" : "Inactif")).toList());
    }

    /** « 12/03/1990 à Ouagadougou » (parties connues uniquement). */
    private static String naissance(SalarieResponseDto s) {
        String date = s.dateNaissance() == null ? ""
                : s.dateNaissance().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String lieu = s.lieuNaissance() == null ? "" : s.lieuNaissance();
        return date.isEmpty() || lieu.isEmpty() ? date + lieu : date + " à " + lieu;
    }

    private Map<UUID, String> matriculesParId() {
        Map<UUID, String> matricules = new java.util.HashMap<>();
        salarieRepository.findAll().forEach(salarie -> matricules.put(salarie.getId(), salarie.getMatricule()));
        return matricules;
    }
}
