package com.budget.backend.abonnement.service;

import com.budget.backend.common.PageResponseDto;
import com.budget.backend.common.PageUtils;
import org.springframework.data.domain.Pageable;
import com.budget.backend.abonnement.AbonnementException;
import com.budget.backend.abonnement.dto.*;
import com.budget.backend.abonnement.entity.Abonnement;
import com.budget.backend.abonnement.entity.EcheanceAbonnement;
import com.budget.backend.abonnement.repository.AbonnementRepository;
import com.budget.backend.abonnement.repository.EcheanceAbonnementRepository;
import com.budget.backend.achat.entity.Fournisseur;
import com.budget.backend.achat.repository.FournisseurRepository;
import com.budget.backend.budget.entity.BudgetAllocationStructure;
import com.budget.backend.budget.repository.BudgetAllocationStructureRepository;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.UtilisateurRepository;
import com.budget.backend.stock.service.AuditHelper;
import com.budget.backend.stock.service.CurrentUserService;
import com.budget.backend.stock.service.NumeroDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrats à facturation périodique et leur échéancier.
 * Statuts : BROUILLON -> ACTIF <-> SUSPENDU -> RESILIE ; ACTIF -> EXPIRE (fin de contrat sans reconduction).
 */
@Service
public class AbonnementService {

    private static final Set<String> PERIODICITES = Set.of(Abonnement.MENSUEL, Abonnement.TRIMESTRIEL,
            Abonnement.SEMESTRIEL, Abonnement.ANNUEL);

    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            Abonnement.BROUILLON, Set.of(Abonnement.ACTIF, Abonnement.RESILIE),
            Abonnement.ACTIF, Set.of(Abonnement.SUSPENDU, Abonnement.RESILIE),
            Abonnement.SUSPENDU, Set.of(Abonnement.ACTIF, Abonnement.RESILIE));

    @Autowired
    private AbonnementRepository repository;
    @Autowired
    private EcheanceAbonnementRepository echeanceRepository;
    @Autowired
    private FournisseurRepository fournisseurRepository;
    @Autowired
    private BudgetAllocationStructureRepository allocationRepository;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private CurrentUserService currentUserService;
    @Autowired
    private NumeroDocumentService numeroDocumentService;
    @Autowired
    private AuditHelper auditHelper;

    /** Met en majuscules et retire les accents : « RÉSILIÉ » et « resilie » donnent RESILIE. */
    public static String normaliser(String valeur) {
        if (valeur == null) {
            return null;
        }
        return Normalizer.normalize(valeur.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase();
    }

    // --- Consultation ---

    @Transactional(readOnly = true)
    public PageResponseDto<AbonnementResponseDto> filtrer(UUID fournisseurId, String statut, UUID responsableId, Pageable pageable) {
        String statutNormalise = normaliser(statut);
        return PageResponseDto.from(repository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (fournisseurId != null) {
                predicates.add(cb.equal(root.get("fournisseur").get("id"), fournisseurId));
            }
            if (statutNormalise != null && !statutNormalise.isEmpty()) {
                predicates.add(cb.equal(root.get("statut"), statutNormalise));
            }
            if (responsableId != null) {
                predicates.add(cb.equal(root.get("responsable").get("id"), responsableId));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, PageUtils.limiter(pageable)), AbonnementResponseDto::from);
    }

    @Transactional(readOnly = true)
    public Optional<AbonnementResponseDto> trouverParId(UUID id) {
        return repository.findById(id).map(AbonnementResponseDto::from);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<EcheanceResponseDto> echeances(UUID abonnementId, String statut, LocalDate dateDebut, LocalDate dateFin,
                                                          Pageable pageable) {
        if (!repository.existsById(abonnementId)) {
            throw new AbonnementException("Abonnement introuvable avec l'ID : " + abonnementId);
        }
        String statutNormalise = normaliser(statut);
        return PageResponseDto.from(echeanceRepository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("abonnement").get("id"), abonnementId));
            if (statutNormalise != null && !statutNormalise.isEmpty()) {
                predicates.add(cb.equal(root.get("statut"), statutNormalise));
            }
            if (dateDebut != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("dateEcheance"), dateDebut));
            }
            if (dateFin != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<LocalDate>get("dateEcheance"), dateFin));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, PageUtils.limiter(pageable)), EcheanceResponseDto::from);
    }

    // --- Création ---

    @Transactional
    public AbonnementResponseDto creer(AbonnementRequestDto dto) {
        verifierContrat(dto);
        Fournisseur fournisseur = fournisseurActif(dto.getFournisseurId());
        BudgetAllocationStructure activite = allocationRepository.findById(dto.getActiviteId())
                .orElseThrow(() -> new AbonnementException("Ligne budgétaire introuvable : " + dto.getActiviteId()));
        Utilisateur responsable = utilisateurRepository.findById(dto.getResponsableId())
                .orElseThrow(() -> new AbonnementException("Responsable introuvable : " + dto.getResponsableId()));

        String statut = dto.getStatut() == null || dto.getStatut().isBlank() ? Abonnement.ACTIF : normaliser(dto.getStatut());
        if (!Abonnement.ACTIF.equals(statut) && !Abonnement.BROUILLON.equals(statut)) {
            throw new AbonnementException("Un abonnement se crée au statut BROUILLON ou ACTIF (reçu : " + dto.getStatut() + ").");
        }
        String code = dto.getCodeAbonnement();
        if (code == null || code.isBlank()) {
            code = numeroDocumentService.prochainCodeAbonnement();
        } else if (repository.existsByCodeAbonnement(code)) {
            throw new AbonnementException("Un abonnement avec ce code existe déjà : " + code);
        }

        Abonnement ab = Abonnement.builder()
                .codeAbonnement(code)
                .libelle(dto.getLibelle())
                .fournisseur(fournisseur)
                .activite(activite)
                .periodicite(normaliser(dto.getPeriodicite()))
                .jourFacturation(dto.getJourFacturation())
                .montantEcheanceHt(dto.getMontantEcheanceHt())
                .montantEcheanceTtc(dto.getMontantEcheanceTtc())
                .dateDebut(dto.getDateDebut())
                .dateFin(dto.getDateFin())
                .taciteReconduction(Boolean.TRUE.equals(dto.getTaciteReconduction()))
                .preavisJours(dto.getPreavisJours())
                .statut(statut)
                .responsable(responsable)
                .build();
        ab = repository.saveAndFlush(ab);

        // « Dès la validation et l'activation du contrat, le moteur génère automatiquement l'échéancier »
        int nb = Abonnement.ACTIF.equals(statut) ? creerEcheancesManquantes(ab) : 0;
        auditHelper.tracer(currentUserService.utilisateurConnecte(), AuditHelper.INSERTION, "abonnements", ab.getId(), null,
                AuditHelper.valeurs("codeAbonnement", ab.getCodeAbonnement(), "libelle", ab.getLibelle(),
                        "statut", ab.getStatut(), "periodicite", ab.getPeriodicite(),
                        "montantEcheanceTtc", ab.getMontantEcheanceTtc(), "echeancesGenerees", nb));
        return AbonnementResponseDto.from(ab);
    }

    // --- Modification ---

    @Transactional
    public AbonnementResponseDto modifier(UUID id, AbonnementRequestDto dto) {
        Abonnement ab = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new AbonnementException("Abonnement introuvable avec l'ID : " + id));
        String statutActuel = ab.getStatut();
        if (Abonnement.RESILIE.equals(statutActuel) || Abonnement.EXPIRE.equals(statutActuel)) {
            throw new AbonnementException("Un abonnement " + statutActuel + " n'est plus modifiable.");
        }
        verifierContrat(dto);

        String nouveauStatut = dto.getStatut() == null || dto.getStatut().isBlank() ? statutActuel : normaliser(dto.getStatut());
        if (!nouveauStatut.equals(statutActuel) && !TRANSITIONS.getOrDefault(statutActuel, Set.of()).contains(nouveauStatut)) {
            throw new AbonnementException("Changement de statut interdit : " + statutActuel + " -> " + nouveauStatut + ".");
        }

        Map<String, Object> avant = AuditHelper.valeurs("libelle", ab.getLibelle(), "statut", ab.getStatut(),
                "periodicite", ab.getPeriodicite(), "jourFacturation", ab.getJourFacturation(),
                "montantEcheanceTtc", ab.getMontantEcheanceTtc(), "dateDebut", ab.getDateDebut(), "dateFin", ab.getDateFin());

        String periodicite = normaliser(dto.getPeriodicite());
        List<EcheanceAbonnement> echeances = echeanceRepository.findByAbonnementIdOrderByNumEcheance(ab.getId());

        // Changer le rythme ou le début recalcule tout le calendrier : impossible si une échéance est déjà traitée
        boolean structurel = !periodicite.equals(ab.getPeriodicite())
                || !dto.getJourFacturation().equals(ab.getJourFacturation())
                || !dto.getDateDebut().equals(ab.getDateDebut());
        if (structurel && !echeances.isEmpty()) {
            if (echeances.stream().anyMatch(e -> !EcheanceAbonnement.A_VENIR.equals(e.getStatut()))) {
                throw new AbonnementException("La périodicité, le jour de facturation et la date de début ne peuvent plus changer : "
                        + "des échéances ont déjà été traitées.");
            }
            echeanceRepository.deleteAll(echeances);
            echeanceRepository.flush(); // les suppressions doivent précéder les insertions (contraintes d'unicité)
            echeances = new ArrayList<>();
        }

        ab.setLibelle(dto.getLibelle());
        ab.setFournisseur(fournisseurActifOuInchange(ab, dto.getFournisseurId()));
        ab.setActivite(allocationRepository.findById(dto.getActiviteId())
                .orElseThrow(() -> new AbonnementException("Ligne budgétaire introuvable : " + dto.getActiviteId())));
        ab.setResponsable(utilisateurRepository.findById(dto.getResponsableId())
                .orElseThrow(() -> new AbonnementException("Responsable introuvable : " + dto.getResponsableId())));
        ab.setPeriodicite(periodicite);
        ab.setJourFacturation(dto.getJourFacturation());
        boolean montantChange = ab.getMontantEcheanceTtc().compareTo(dto.getMontantEcheanceTtc()) != 0;
        ab.setMontantEcheanceHt(dto.getMontantEcheanceHt());
        ab.setMontantEcheanceTtc(dto.getMontantEcheanceTtc());
        ab.setDateDebut(dto.getDateDebut());
        ab.setDateFin(dto.getDateFin());
        ab.setTaciteReconduction(Boolean.TRUE.equals(dto.getTaciteReconduction()));
        ab.setPreavisJours(dto.getPreavisJours());
        ab.setStatut(nouveauStatut);

        // Les échéances à venir suivent le nouveau montant ; celles au-delà de la nouvelle fin sont annulées
        for (EcheanceAbonnement e : echeances) {
            if (EcheanceAbonnement.A_VENIR.equals(e.getStatut())) {
                if (montantChange) {
                    e.setMontantAttenduTtc(ab.getMontantEcheanceTtc());
                }
                if (Abonnement.RESILIE.equals(nouveauStatut) || e.getDateEcheance().isAfter(ab.getDateFin())) {
                    e.setStatut(EcheanceAbonnement.ANNULE);
                }
            }
        }
        ab = repository.saveAndFlush(ab);
        if (Abonnement.ACTIF.equals(nouveauStatut)) {
            creerEcheancesManquantes(ab); // activation ou prolongation de la date de fin
        }
        auditHelper.tracer(currentUserService.utilisateurConnecte(), AuditHelper.MODIFICATION, "abonnements", ab.getId(), avant,
                AuditHelper.valeurs("libelle", ab.getLibelle(), "statut", ab.getStatut(), "periodicite", ab.getPeriodicite(),
                        "jourFacturation", ab.getJourFacturation(), "montantEcheanceTtc", ab.getMontantEcheanceTtc(),
                        "dateDebut", ab.getDateDebut(), "dateFin", ab.getDateFin()));
        return AbonnementResponseDto.from(ab);
    }

    // --- Echéancier ---

    /** Génère les échéances manquantes d'un contrat ACTIF (idempotent : ne recrée jamais une date existante). */
    @Transactional
    public List<EcheanceResponseDto> genererEcheances(UUID id) {
        Abonnement ab = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new AbonnementException("Abonnement introuvable avec l'ID : " + id));
        if (!Abonnement.ACTIF.equals(ab.getStatut())) {
            throw new AbonnementException("Seul un abonnement ACTIF a un échéancier (statut actuel : " + ab.getStatut() + ").");
        }
        int nb = creerEcheancesManquantes(ab);
        auditHelper.tracer(currentUserService.utilisateurConnecte(), AuditHelper.MODIFICATION, "abonnements", ab.getId(), null,
                AuditHelper.valeurs("action", "GENERATION_ECHEANCES", "echeancesGenerees", nb));
        return echeanceRepository.findByAbonnementIdOrderByNumEcheance(id).stream()
                .map(EcheanceResponseDto::from).collect(Collectors.toList());
    }

    /** Reconduction : prolonge d'un an la date de fin et génère les nouvelles échéances. */
    @Transactional
    public AbonnementResponseDto reconduire(UUID id) {
        Abonnement ab = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new AbonnementException("Abonnement introuvable avec l'ID : " + id));
        if (!Abonnement.ACTIF.equals(ab.getStatut())) {
            throw new AbonnementException("Seul un abonnement ACTIF peut être reconduit (statut actuel : " + ab.getStatut() + ").");
        }
        LocalDate ancienneFin = ab.getDateFin();
        ab.setDateFin(ancienneFin.plusYears(1));
        ab = repository.saveAndFlush(ab);
        int nb = creerEcheancesManquantes(ab);
        auditHelper.tracer(currentUserService.utilisateurConnecte(), AuditHelper.MODIFICATION, "abonnements", ab.getId(),
                AuditHelper.valeurs("dateFin", ancienneFin), AuditHelper.valeurs("dateFin", ab.getDateFin(),
                        "action", "RECONDUCTION", "echeancesGenerees", nb));
        return AbonnementResponseDto.from(ab);
    }

    /**
     * Traitement quotidien : un contrat ACTIF dont la date de fin est passée est reconduit s'il est à tacite
     * reconduction, sinon il passe à EXPIRE. Appelé par la tâche planifiée (pas d'utilisateur, donc pas d'audit).
     * @return nombre de contrats traités
     */
    @Transactional
    public int traiterFinsDeContrat(LocalDate aujourdhui) {
        int traites = 0;
        for (Abonnement candidat : repository.findByStatutAndDateFinBefore(Abonnement.ACTIF, aujourdhui)) {
            Abonnement ab = repository.findByIdForUpdate(candidat.getId()).orElse(null);
            if (ab == null || !Abonnement.ACTIF.equals(ab.getStatut()) || !ab.getDateFin().isBefore(aujourdhui)) {
                continue; // modifié entre-temps
            }
            if (Boolean.TRUE.equals(ab.getTaciteReconduction())) {
                while (ab.getDateFin().isBefore(aujourdhui)) {
                    ab.setDateFin(ab.getDateFin().plusYears(1));
                }
                repository.saveAndFlush(ab);
                creerEcheancesManquantes(ab);
            } else {
                ab.setStatut(Abonnement.EXPIRE);
                repository.saveAndFlush(ab);
            }
            traites++;
        }
        return traites;
    }

    // --- Alertes ---

    @Transactional(readOnly = true)
    public AlertesResponseDto alertes(int jours, LocalDate aujourdhui) {
        AlertesResponseDto dto = new AlertesResponseDto();
        dto.setEcheancesProches(echeanceRepository
                .findByStatutAndDateEcheanceBetweenOrderByDateEcheance(EcheanceAbonnement.A_VENIR, aujourdhui, aujourdhui.plusDays(jours))
                .stream().filter(e -> Abonnement.ACTIF.equals(e.getAbonnement().getStatut()))
                .map(EcheanceResponseDto::from).collect(Collectors.toList()));

        List<AlertesResponseDto.PreavisAlerte> preavis = new ArrayList<>();
        for (Abonnement ab : repository.findByStatut(Abonnement.ACTIF)) {
            LocalDate limite = ab.dateLimitePreavis();
            if (limite != null && !aujourdhui.isBefore(limite) && !aujourdhui.isAfter(ab.getDateFin())) {
                preavis.add(new AlertesResponseDto.PreavisAlerte(ab.getId(), ab.getCodeAbonnement(), ab.getLibelle(),
                        ab.getDateFin(), limite, ChronoUnit.DAYS.between(aujourdhui, ab.getDateFin()),
                        ab.getTaciteReconduction(), ab.getResponsable().getId()));
            }
        }
        dto.setPreavisAtteints(preavis);
        return dto;
    }

    // --- Règles internes ---

    /** Crée les échéances dont la date n'existe pas encore ; la numérotation continue après la dernière. */
    private int creerEcheancesManquantes(Abonnement ab) {
        List<EcheanceAbonnement> existantes = echeanceRepository.findByAbonnementIdOrderByNumEcheance(ab.getId());
        Set<LocalDate> datesExistantes = existantes.stream().map(EcheanceAbonnement::getDateEcheance).collect(Collectors.toSet());
        int prochainNumero = existantes.stream().mapToInt(EcheanceAbonnement::getNumEcheance).max().orElse(0) + 1;

        List<EcheanceAbonnement> nouvelles = new ArrayList<>();
        for (LocalDate date : CalendrierEcheances.generer(ab.getDateDebut(), ab.getDateFin(), ab.getPeriodicite(), ab.getJourFacturation())) {
            if (!datesExistantes.contains(date)) {
                nouvelles.add(EcheanceAbonnement.builder()
                        .abonnement(ab)
                        .numEcheance(prochainNumero++)
                        .dateEcheance(date)
                        .montantAttenduTtc(ab.getMontantEcheanceTtc())
                        .statut(EcheanceAbonnement.A_VENIR)
                        .build());
            }
        }
        echeanceRepository.saveAll(nouvelles);
        return nouvelles.size();
    }

    private void verifierContrat(AbonnementRequestDto dto) {
        if (!PERIODICITES.contains(normaliser(dto.getPeriodicite()))) {
            throw new AbonnementException("Périodicité invalide : '" + dto.getPeriodicite()
                    + "' (attendu : MENSUEL, TRIMESTRIEL, SEMESTRIEL ou ANNUEL).");
        }
        if (dto.getDateFin().isBefore(dto.getDateDebut())) {
            throw new AbonnementException("La date de fin doit être postérieure ou égale à la date de début.");
        }
        if (dto.getMontantEcheanceTtc().compareTo(dto.getMontantEcheanceHt()) < 0) {
            throw new AbonnementException("Le montant TTC ne peut pas être inférieur au montant HT.");
        }
    }

    private Fournisseur fournisseurActif(UUID fournisseurId) {
        Fournisseur f = fournisseurRepository.findById(fournisseurId)
                .orElseThrow(() -> new AbonnementException("Fournisseur introuvable : " + fournisseurId));
        if (!Boolean.TRUE.equals(f.getActif())) {
            throw new AbonnementException("Le fournisseur " + f.getCodeFournisseur() + " est suspendu.");
        }
        return f;
    }

    /** Garder le fournisseur actuel, même suspendu depuis, tant qu'on ne le change pas. */
    private Fournisseur fournisseurActifOuInchange(Abonnement ab, UUID fournisseurId) {
        return ab.getFournisseur().getId().equals(fournisseurId) ? ab.getFournisseur() : fournisseurActif(fournisseurId);
    }
}
