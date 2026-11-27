package com.budget.backend.security.service;

import com.budget.backend.common.CsvUtils;
import com.budget.backend.common.PdfUtils;
import com.budget.backend.security.dto.AffectationRequestDto;
import com.budget.backend.security.dto.AffectationResponseDto;
import com.budget.backend.security.dto.AffectationResumeDto;
import com.budget.backend.security.entity.Affectation;
import com.budget.backend.security.entity.Direction;
import com.budget.backend.security.entity.NiveauAffectation;
import com.budget.backend.security.entity.Salarie;
import com.budget.backend.security.entity.ServiceOrganisationnel;
import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.repository.AffectationRepository;
import com.budget.backend.security.repository.DirectionRepository;
import com.budget.backend.security.repository.SalarieRepository;
import com.budget.backend.security.repository.ServiceOrganisationnelRepository;
import com.budget.backend.security.repository.StructureRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class AffectationService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AffectationRepository affectationRepository;
    private final SalarieRepository salarieRepository;
    private final StructureRepository structureRepository;
    private final DirectionRepository directionRepository;
    private final ServiceOrganisationnelRepository serviceRepository;

    public AffectationService(
            AffectationRepository affectationRepository,
            SalarieRepository salarieRepository,
            StructureRepository structureRepository,
            DirectionRepository directionRepository,
            ServiceOrganisationnelRepository serviceRepository) {
        this.affectationRepository = affectationRepository;
        this.salarieRepository = salarieRepository;
        this.structureRepository = structureRepository;
        this.directionRepository = directionRepository;
        this.serviceRepository = serviceRepository;
    }

    /**
     * Affectations filtrées. {@code structureCode} retient toutes les affectations rattachées à la structure
     * (directement, ou via une de ses directions ou un de ses services).
     */
    @Transactional(readOnly = true)
    public List<AffectationResponseDto> lister(UUID salarieId, NiveauAffectation niveau, String structureCode,
                                               String statut, String recherche) {
        LocalDate aujourdHui = LocalDate.now();
        String terme = recherche == null || recherche.isBlank() ? null : recherche.trim().toLowerCase(Locale.ROOT);
        return affectationRepository.findAll().stream()
                .filter(a -> salarieId == null || a.getSalarie().getId().equals(salarieId))
                .filter(a -> niveau == null || a.getNiveau() == niveau)
                .filter(a -> structureCode == null || structureCode.isBlank() || (a.structureDeRattachement() != null
                        && a.structureDeRattachement().getCode().equalsIgnoreCase(structureCode)))
                .filter(a -> statut == null || statut.isBlank() || statut(a, aujourdHui).equals(statut))
                .map(a -> toDto(a, aujourdHui))
                .filter(dto -> terme == null || contient(terme, dto.salarieNom(), dto.salarieMatricule(),
                        dto.cibleCode(), dto.cibleLibelle(), dto.fonction()))
                .sorted(Comparator.comparing(AffectationResponseDto::salarieNom, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(AffectationResponseDto::dateDebut, Comparator.reverseOrder()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AffectationResponseDto obtenir(UUID id) {
        return toDto(trouver(id), LocalDate.now());
    }

    @Transactional
    public AffectationResponseDto creer(AffectationRequestDto request) {
        Affectation affectation = new Affectation();
        appliquer(affectation, request);
        return toDto(affectationRepository.save(affectation), LocalDate.now());
    }

    @Transactional
    public AffectationResponseDto modifier(UUID id, AffectationRequestDto request) {
        Affectation affectation = trouver(id);
        appliquer(affectation, request);
        return toDto(affectationRepository.save(affectation), LocalDate.now());
    }

    @Transactional
    public void supprimer(UUID id) {
        Affectation affectation = trouver(id);
        affectationRepository.delete(affectation);
    }

    /**
     * Affecte un salarié à un service (imports CSV), sauf s'il y est déjà affecté.
     */
    @Transactional
    public void affecterAuService(Salarie salarie, ServiceOrganisationnel service, LocalDate dateDebut) {
        boolean dejaAffecte = affectationRepository.findBySalarie_Id(salarie.getId()).stream()
                .anyMatch(a -> a.getNiveau() == NiveauAffectation.SERVICE && a.getService().getId().equals(service.getId()));
        if (dejaAffecte) return;
        Affectation affectation = Affectation.builder()
                .salarie(salarie)
                .niveau(NiveauAffectation.SERVICE)
                .service(service)
                .dateDebut(dateDebut == null ? LocalDate.now() : dateDebut)
                .build();
        affectationRepository.save(affectation);
    }

    /** Affectation en cours la plus récente du salarié ; null s'il n'en a aucune. */
    @Transactional(readOnly = true)
    public AffectationResumeDto resumeActuelle(Salarie salarie) {
        return affectationEnCours(salarie.getId()).map(AffectationService::resume).orElse(null);
    }

    /** Le salarié a-t-il une affectation en cours au service donné ? */
    @Transactional(readOnly = true)
    public boolean estAffecteAuService(UUID salarieId, UUID serviceId) {
        LocalDate aujourdHui = LocalDate.now();
        return affectationRepository.findBySalarie_Id(salarieId).stream()
                .anyMatch(a -> a.estEnCoursLe(aujourdHui) && a.getNiveau() == NiveauAffectation.SERVICE
                        && a.getService().getId().equals(serviceId));
    }

    public Optional<Affectation> affectationEnCours(UUID salarieId) {
        LocalDate aujourdHui = LocalDate.now();
        return affectationRepository.findBySalarie_Id(salarieId).stream()
                .filter(a -> a.estEnCoursLe(aujourdHui))
                .max(Comparator.comparing(Affectation::getDateDebut));
    }

    // --- Règles de gestion ---

    private void appliquer(Affectation affectation, AffectationRequestDto request) {
        Salarie salarie = salarieRepository.findById(request.salarieId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salarié introuvable."));
        if (request.dateFin() != null && request.dateFin().isBefore(request.dateDebut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La date de fin ne peut pas précéder la date de début.");
        }
        affectation.setSalarie(salarie);
        affectation.setNiveau(request.niveau());
        affectation.setStructure(null);
        affectation.setDirection(null);
        affectation.setService(null);
        String cible = request.cibleId().trim();
        switch (request.niveau()) {
            case STRUCTURE -> affectation.setStructure(structureRepository.findById(cible)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Structure introuvable.")));
            case DIRECTION -> affectation.setDirection(directionRepository.findById(uuid(cible))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Direction introuvable.")));
            case SERVICE -> affectation.setService(serviceRepository.findById(uuid(cible))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service introuvable.")));
        }
        affectation.setFonction(texte(request.fonction()));
        affectation.setDateDebut(request.dateDebut());
        affectation.setDateFin(request.dateFin());
        affectation.setCommentaire(texte(request.commentaire()));
        verifierChevauchement(affectation);
    }

    /** Un salarié ne peut pas être affecté deux fois à la même entité sur des périodes qui se chevauchent. */
    private void verifierChevauchement(Affectation affectation) {
        String cible = cleCible(affectation);
        boolean doublon = affectationRepository.findBySalarie_Id(affectation.getSalarie().getId()).stream()
                .filter(autre -> !autre.getId().equals(affectation.getId()))
                .filter(autre -> autre.getNiveau() == affectation.getNiveau() && cleCible(autre).equals(cible))
                .anyMatch(autre -> chevauche(autre, affectation));
        if (doublon) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce salarié est déjà affecté à cette entité sur une période qui se chevauche.");
        }
    }

    private static boolean chevauche(Affectation a, Affectation b) {
        LocalDate finA = a.getDateFin() == null ? LocalDate.MAX : a.getDateFin();
        LocalDate finB = b.getDateFin() == null ? LocalDate.MAX : b.getDateFin();
        return !a.getDateDebut().isAfter(finB) && !b.getDateDebut().isAfter(finA);
    }

    private static String cleCible(Affectation a) {
        return switch (a.getNiveau()) {
            case STRUCTURE -> a.getStructure().getCode();
            case DIRECTION -> a.getDirection().getId().toString();
            case SERVICE -> a.getService().getId().toString();
        };
    }

    // --- Conversions ---

    private AffectationResponseDto toDto(Affectation a, LocalDate aujourdHui) {
        Salarie salarie = a.getSalarie();
        Structure structure = a.structureDeRattachement();
        return new AffectationResponseDto(
                a.getId(),
                salarie.getId(),
                salarie.getMatricule(),
                salarie.getPrenom() + " " + salarie.getNom(),
                salarie.getPoste(),
                a.getNiveau(),
                switch (a.getNiveau()) {
                    case STRUCTURE -> a.getStructure().getCode();
                    case DIRECTION -> a.getDirection().getId().toString();
                    case SERVICE -> a.getService().getId().toString();
                },
                codeCible(a),
                libelleCible(a),
                chemin(a),
                structure == null ? null : structure.getCode(),
                structure == null ? null : structure.getNom(),
                a.getFonction(),
                a.getDateDebut(),
                a.getDateFin(),
                statut(a, aujourdHui),
                a.getCommentaire(),
                a.getCreatedAt(),
                a.getUpdatedAt());
    }

    private static AffectationResumeDto resume(Affectation a) {
        Structure structure = a.structureDeRattachement();
        Direction direction = a.directionDeRattachement();
        return new AffectationResumeDto(a.getNiveau(), codeCible(a), libelleCible(a), chemin(a),
                structure == null ? null : structure.getCode(),
                direction == null ? null : direction.getLibelle(),
                a.getNiveau() == NiveauAffectation.SERVICE ? a.getService().getLibelle() : null,
                a.getFonction());
    }

    private static String codeCible(Affectation a) {
        return switch (a.getNiveau()) {
            case STRUCTURE -> a.getStructure().getCode();
            case DIRECTION -> a.getDirection().getCode();
            case SERVICE -> a.getService().getCode();
        };
    }

    private static String libelleCible(Affectation a) {
        return switch (a.getNiveau()) {
            case STRUCTURE -> a.getStructure().getNom();
            case DIRECTION -> a.getDirection().getLibelle();
            case SERVICE -> a.getService().getLibelle();
        };
    }

    private static String chemin(Affectation a) {
        Structure structure = a.structureDeRattachement();
        Direction direction = a.directionDeRattachement();
        return String.join(" › ", Arrays.asList(
                        structure == null ? null : structure.getCode(),
                        direction == null ? null : direction.getCode(),
                        a.getNiveau() == NiveauAffectation.SERVICE ? a.getService().getCode() : null)
                .stream().filter(Objects::nonNull).toList());
    }

    private static String statut(Affectation a, LocalDate aujourdHui) {
        if (a.getDateDebut().isAfter(aujourdHui)) return "A_VENIR";
        if (a.getDateFin() != null && a.getDateFin().isBefore(aujourdHui)) return "TERMINEE";
        return "EN_COURS";
    }

    private static String libelleStatut(String statut) {
        return switch (statut) {
            case "A_VENIR" -> "À venir";
            case "TERMINEE" -> "Terminée";
            default -> "En cours";
        };
    }

    private Affectation trouver(UUID id) {
        return affectationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Affectation introuvable."));
    }

    private static UUID uuid(String valeur) {
        try {
            return UUID.fromString(valeur);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identifiant d'entité invalide.");
        }
    }

    private static String texte(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }

    private static boolean contient(String terme, String... valeurs) {
        return Arrays.stream(valeurs).anyMatch(v -> v != null && v.toLowerCase(Locale.ROOT).contains(terme));
    }

    private static String date(LocalDate date) {
        return date == null ? "" : date.format(DATE);
    }

    // --- Exports (liste fournie : lignes affichées à l'écran) ---

    public String exporterEnCsv(List<AffectationResponseDto> affectations) {
        StringBuilder csv = new StringBuilder(
                "Matricule;Salarie;Niveau;Code_Entite;Entite;Rattachement;Fonction;Date_Debut;Date_Fin;Statut\n");
        for (AffectationResponseDto a : affectations) {
            csv.append(CsvUtils.ligne(';', a.salarieMatricule(), a.salarieNom(), a.niveau().libelle(), a.cibleCode(),
                    a.cibleLibelle(), a.chemin(), a.fonction(),
                    a.dateDebut(), a.dateFin(), libelleStatut(a.statut())));
        }
        return csv.toString();
    }

    public byte[] exporterEnPdf(List<AffectationResponseDto> affectations) {
        return PdfUtils.tableau("Affectations des salariés",
                List.of("Salarié", "Niveau", "Entité", "Rattachement", "Fonction", "Période", "Statut"),
                affectations.stream().map(a -> Arrays.<Object>asList(
                        a.salarieNom() + " (" + a.salarieMatricule() + ")",
                        a.niveau().libelle(),
                        a.cibleLibelle() + " (" + a.cibleCode() + ")",
                        a.chemin(),
                        a.fonction() == null ? "" : a.fonction(),
                        date(a.dateDebut()) + (a.dateFin() == null ? " → …" : " → " + date(a.dateFin())),
                        libelleStatut(a.statut()))).toList());
    }
}
