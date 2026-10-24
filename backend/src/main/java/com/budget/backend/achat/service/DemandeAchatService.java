package com.budget.backend.achat.service;

import com.budget.backend.common.PageResponseDto;
import com.budget.backend.common.PageUtils;
import org.springframework.data.domain.Pageable;
import com.budget.backend.achat.AchatException;
import com.budget.backend.achat.api.BudgetReservationApi;
import com.budget.backend.achat.dto.*;
import com.budget.backend.achat.entity.*;
import com.budget.backend.achat.repository.DemandeAchatRepository;
import com.budget.backend.budget.entity.BudgetAllocationStructure;
import com.budget.backend.budget.repository.BudgetAllocationStructureRepository;
import com.budget.backend.security.entity.Entite;
import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.EntiteRepository;
import com.budget.backend.security.repository.StructureRepository;
import com.budget.backend.stock.entity.Article;
import com.budget.backend.stock.repository.ArticleRepository;
import com.budget.backend.stock.service.AuditHelper;
import com.budget.backend.stock.service.CurrentUserService;
import com.budget.backend.stock.service.NumeroDocumentService;
import com.budget.backend.workflow.WorkflowException;
import com.budget.backend.workflow.api.WorkflowEngineApi;
import com.budget.backend.workflow.api.WorkflowTransitionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Demande d'achat : création (avec engagement budgétaire), modification tant qu'aucune validation n'a eu lieu,
 * circuit de validation (WorkflowEngineApi) et libération des crédits en cas de rejet.
 * Statuts : SOUMIS -> EN_COURS_VALIDATION -> APPROUVE -> COMMANDE (voir BonCommandeService), ou REJETE.
 */
@Service
public class DemandeAchatService {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");
    private static final BigDecimal CENT = new BigDecimal("100");

    @Autowired
    private DemandeAchatRepository repository;
    @Autowired
    private EntiteRepository entiteRepository;
    @Autowired
    private StructureRepository structureRepository;
    @Autowired
    private BudgetAllocationStructureRepository allocationRepository;
    @Autowired
    private ArticleRepository articleRepository;
    @Autowired
    private CurrentUserService currentUserService;
    @Autowired
    private NumeroDocumentService numeroDocumentService;
    @Autowired
    private AuditHelper auditHelper;
    @Autowired
    private BudgetReservationApi budgetApi;
    @Autowired
    private WorkflowEngineApi workflowApi;

    // TVA du scénario du PDF (11 800 000 TTC pour 10 000 000 HT) ; modifiable sans recompiler.
    @Value("${achat.tva-taux:0.18}")
    private BigDecimal tvaTaux;

    // --- Consultation ---

    @Transactional(readOnly = true)
    public PageResponseDto<DemandeAchatResponseDto> filtrer(String codeDossier, String statut, UUID demandeurId, Integer exerciceAnnee, Pageable pageable) {
        return PageResponseDto.from(repository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (codeDossier != null && !codeDossier.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("codeDossier")), "%" + codeDossier.toLowerCase() + "%"));
            }
            if (statut != null && !statut.isEmpty()) {
                predicates.add(cb.equal(cb.upper(root.get("statut")), statut.toUpperCase()));
            }
            if (demandeurId != null) {
                predicates.add(cb.equal(root.get("demandeur").get("id"), demandeurId));
            }
            if (exerciceAnnee != null) {
                predicates.add(cb.equal(root.get("exerciceAnnee"), exerciceAnnee));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, PageUtils.limiter(pageable)), DemandeAchatResponseDto::from);
    }

    @Transactional(readOnly = true)
    public Optional<DemandeAchatResponseDto> trouverParId(UUID id) {
        return repository.findById(id).map(DemandeAchatResponseDto::from);
    }

    // --- Création ---

    @Transactional
    public DemandeAchatResponseDto creer(DemandeAchatRequestDto dto) {
        Utilisateur demandeur = currentUserService.utilisateurConnecte();
        String code = dto.getCodeDossier();
        if (code == null || code.isBlank()) {
            code = numeroDocumentService.prochainCodeDemandeAchat();
        } else if (repository.existsByCodeDossier(code)) {
            throw new AchatException("Une demande d'achat avec ce code existe déjà : " + code);
        }

        DemandeAchat da = DemandeAchat.builder()
                .codeDossier(code)
                .demandeur(demandeur)
                .statut(DemandeAchat.SOUMIS)
                .build();
        remplir(da, dto);

        // L'identifiant est nécessaire pour référencer le dossier dans le journal et le workflow
        da = repository.saveAndFlush(da);
        engagerCredits(da, demandeur);

        WorkflowEngineApi.EtatWorkflow etat = workflowApi.demarrer(WorkflowEngineApi.WORKFLOW_DA, da.getId(), da.getMontantEstimeHt());
        da.setEtapeCourante(etat.etapeCourante());
        da = repository.saveAndFlush(da);
        auditHelper.tracer(demandeur, AuditHelper.INSERTION, "demandes_achat", da.getId(), null,
                AuditHelper.valeurs("codeDossier", da.getCodeDossier(), "montantEstimeHt", da.getMontantEstimeHt(),
                        "statut", da.getStatut(), "exerciceAnnee", da.getExerciceAnnee()));
        return DemandeAchatResponseDto.from(da);
    }

    // --- Modification (uniquement tant que personne n'a validé) ---

    @Transactional
    public DemandeAchatResponseDto modifier(UUID id, DemandeAchatRequestDto dto) {
        DemandeAchat da = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new AchatException("Demande d'achat introuvable avec l'ID : " + id));
        Utilisateur utilisateur = currentUserService.utilisateurConnecte();

        if (!da.getDemandeur().getId().equals(utilisateur.getId())) {
            throw new AchatException("Seul le demandeur peut modifier sa demande d'achat.");
        }
        if (!DemandeAchat.SOUMIS.equals(da.getStatut()) || !da.getHistorique().isEmpty()) {
            throw new AchatException("La demande ne peut plus être modifiée : elle est déjà en cours de validation "
                    + "ou terminée (statut " + da.getStatut() + ").");
        }

        java.util.Map<String, Object> avant = AuditHelper.valeurs("objet", da.getObjet(),
                "montantEstimeHt", da.getMontantEstimeHt(), "nbLignes", da.getLignes().size());

        // On libère l'ancien engagement avant d'en prendre un nouveau sur les lignes modifiées
        libererCredits(da, utilisateur);
        da.getLignes().clear();
        da.getVentilations().clear();
        repository.flush();

        remplir(da, dto);
        da = repository.saveAndFlush(da);
        engagerCredits(da, utilisateur);
        auditHelper.tracer(utilisateur, AuditHelper.MODIFICATION, "demandes_achat", da.getId(), avant,
                AuditHelper.valeurs("objet", da.getObjet(), "montantEstimeHt", da.getMontantEstimeHt(),
                        "nbLignes", da.getLignes().size()));
        return DemandeAchatResponseDto.from(da);
    }

    // --- Circuit de validation ---

    /** Une étape du circuit, décidée depuis le module Achats (POST /demandes-achat/{id}/approuver). */
    @Transactional
    public DemandeAchatResponseDto approuver(UUID id, WorkflowExecutionActionDto dto) {
        DemandeAchat da = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new AchatException("Demande d'achat introuvable avec l'ID : " + id));
        verifierTraitable(da);
        String action = dto.getAction() == null ? "" : dto.getAction().trim().toUpperCase();
        boolean approuve;
        switch (action) {
            case "APPROVE" -> approuve = true;
            case "REJECT" -> approuve = false;
            default -> throw new AchatException("Action invalide : '" + dto.getAction() + "' (attendu : APPROVE ou REJECT).");
        }
        Utilisateur valideur = currentUserService.utilisateurConnecte();
        verifierNonDemandeur(da, valideur);

        String etapeAvant = da.getEtapeCourante();
        WorkflowEngineApi.EtatWorkflow etat = workflowApi.prochaineEtape(WorkflowEngineApi.WORKFLOW_DA, da.getId(),
                da.getMontantEstimeHt(), approuve, valideur);
        return DemandeAchatResponseDto.from(appliquerDecision(da, etapeAvant, approuve, etat, valideur, dto.getCommentaire()));
    }

    /**
     * Décision prise directement sur le moteur (POST /workflows/instances/execute) : on applique ici les mêmes
     * effets que pour approuver(). L'écouteur s'exécute dans la transaction du moteur : toute exception annule
     * aussi la transition du workflow.
     */
    @EventListener
    public void surDecisionWorkflow(WorkflowTransitionEvent event) {
        if (!WorkflowEngineApi.WORKFLOW_DA.equals(event.workflowCode())) {
            return;
        }
        try {
            DemandeAchat da = repository.findByIdForUpdate(event.dossierId())
                    .orElseThrow(() -> new AchatException("Demande d'achat introuvable avec l'ID : " + event.dossierId()));
            verifierTraitable(da);
            verifierNonDemandeur(da, event.utilisateur());
            appliquerDecision(da, event.etapeAvant(), event.approuve(), event.etatApres(), event.utilisateur(), event.commentaire());
        } catch (AchatException e) {
            // La requête est servie par le contrôleur du moteur : l'erreur doit être de son type pour sortir en 400 lisible
            throw new WorkflowException(e.getMessage());
        }
    }

    private void verifierTraitable(DemandeAchat da) {
        if (!DemandeAchat.SOUMIS.equals(da.getStatut()) && !DemandeAchat.EN_COURS_VALIDATION.equals(da.getStatut())) {
            throw new AchatException("Seule une demande SOUMIS ou EN_COURS_VALIDATION peut être traitée (statut actuel : "
                    + da.getStatut() + ").");
        }
    }

    private void verifierNonDemandeur(DemandeAchat da, Utilisateur valideur) {
        if (da.getDemandeur().getId().equals(valideur.getId())) {
            throw new AchatException("Un demandeur ne peut pas valider sa propre demande d'achat.");
        }
    }

    /**
     * Reporte sur la DA l'état renvoyé par le moteur : historique, étape, statut, libération des crédits.
     * REJETE avec une étape en attente = circuit d'exception (annulation à traiter) : la DA reste en cours et
     * ses crédits restent engagés jusqu'à la fin du circuit (REJETE sans étape, ou ANNULE).
     */
    private DemandeAchat appliquerDecision(DemandeAchat da, String etapeAvant, boolean approuve,
                                           WorkflowEngineApi.EtatWorkflow etat, Utilisateur valideur, String commentaire) {
        String statutAvant = da.getStatut();
        da.getHistorique().add(HistoriqueValidation.builder()
                .demandeAchat(da)
                .etape(etapeAvant)
                .valideur(valideur)
                .decision(approuve ? HistoriqueValidation.VALIDE : HistoriqueValidation.REJETE)
                .commentaire(commentaire)
                .build());

        da.setEtapeCourante(etat.etapeCourante());
        boolean echecDefinitif = WorkflowEngineApi.STATUT_ANNULE.equals(etat.statut())
                || (WorkflowEngineApi.STATUT_REJETE.equals(etat.statut()) && etat.termine());
        if (WorkflowEngineApi.STATUT_APPROUVE.equals(etat.statut())) {
            da.setStatut(DemandeAchat.APPROUVE);
            // Fin du circuit de validation : la DA passe à l'acheteur (scénario du PDF, étape TRAITEMENT_CSP)
            da.setEtapeCourante(DemandeAchat.ETAPE_TRAITEMENT_CSP);
        } else if (echecDefinitif) {
            da.setStatut(DemandeAchat.REJETE);
            libererCredits(da, valideur);
        } else {
            da.setStatut(DemandeAchat.EN_COURS_VALIDATION);
        }
        da = repository.saveAndFlush(da);
        auditHelper.tracer(valideur, AuditHelper.VALIDATION, "demandes_achat", da.getId(),
                AuditHelper.valeurs("statut", statutAvant, "etapeCourante", etapeAvant),
                AuditHelper.valeurs("statut", da.getStatut(), "etapeCourante", da.getEtapeCourante(),
                        "decision", approuve ? HistoriqueValidation.VALIDE : HistoriqueValidation.REJETE,
                        "commentaire", commentaire));
        return da;
    }

    // --- Engagement budgétaire ---

    /**
     * Montant à engager par ligne budgétaire : selon la ventilation si elle existe, sinon selon les lignes de la DA.
     * Trié par identifiant pour que les verrous soient toujours pris dans le même ordre (pas d'interblocage).
     */
    public Map<UUID, BigDecimal> repartitionEngagement(DemandeAchat da) {
        Map<UUID, BigDecimal> repartition = new TreeMap<>();
        if (!da.getVentilations().isEmpty()) {
            for (DossierVentilationImpact v : da.getVentilations()) {
                repartition.merge(v.getActiviteImpactee().getId(), v.getMontantImpute(), BigDecimal::add);
            }
        } else {
            for (LigneDemandeAchat l : da.getLignes()) {
                repartition.merge(l.getActivite().getId(), l.calculerMontantHt(), BigDecimal::add);
            }
        }
        return repartition;
    }

    private void engagerCredits(DemandeAchat da, Utilisateur auteur) {
        repartitionEngagement(da).forEach((allocationId, montant) -> budgetApi.engager(allocationId, montant,
                BudgetReservationApi.ENGAGEMENT_DA, da.getCodeDossier(), da.getExerciceAnnee(), auteur.getId()));
    }

    private void libererCredits(DemandeAchat da, Utilisateur auteur) {
        repartitionEngagement(da).forEach((allocationId, montant) -> budgetApi.liberer(allocationId, montant,
                BudgetReservationApi.ENGAGEMENT_DA, da.getCodeDossier(), da.getExerciceAnnee(), auteur.getId()));
    }

    // --- Construction d'une DA à partir du DTO ---

    private void remplir(DemandeAchat da, DemandeAchatRequestDto dto) {
        Entite emettrice = entiteRepository.findById(dto.getEntiteEmettrice())
                .orElseThrow(() -> new AchatException("Entité émettrice introuvable : " + dto.getEntiteEmettrice()));
        Entite beneficiaire = entiteRepository.findById(dto.getEntiteBeneficiaire())
                .orElseThrow(() -> new AchatException("Entité bénéficiaire introuvable : " + dto.getEntiteBeneficiaire()));
        Structure direction = structureRepository.findById(dto.getDirectionDemandeurId())
                .orElseThrow(() -> new AchatException("Direction demandeuse introuvable : " + dto.getDirectionDemandeurId()));

        da.setEntiteEmettrice(emettrice);
        da.setEntiteBeneficiaire(beneficiaire);
        da.setTypeAchat(dto.getTypeAchat());
        da.setObjet(dto.getObjet());
        da.setJustification(dto.getJustification());
        da.setDirectionDemandeur(direction);
        da.setExerciceAnnee(dto.getExerciceAnnee());

        BigDecimal totalHt = BigDecimal.ZERO;
        for (LigneDARequestDto ligneDto : dto.getLignes()) {
            BudgetAllocationStructure activite = allocationRepository.findById(ligneDto.getActiviteId())
                    .orElseThrow(() -> new AchatException("Ligne budgétaire introuvable : " + ligneDto.getActiviteId()));
            verifierExercice(activite, da.getExerciceAnnee());
            verifierPropriete(activite, direction);
            Article article = null;
            if (ligneDto.getArticleId() != null) {
                article = articleRepository.findById(ligneDto.getArticleId())
                        .orElseThrow(() -> new AchatException("Article introuvable : " + ligneDto.getArticleId()));
                if (!Boolean.TRUE.equals(article.getActif())) {
                    throw new AchatException("L'article " + article.getCodeArticle() + " est inactif.");
                }
            }
            LigneDemandeAchat ligne = LigneDemandeAchat.builder()
                    .demandeAchat(da)
                    .activite(activite)
                    .article(article)
                    .description(ligneDto.getDescription())
                    .uniteMesure(ligneDto.getUniteMesure())
                    .quantite(ligneDto.getQuantite())
                    .prixUnitaireEstime(ligneDto.getPrixUnitaireEstime())
                    .build();
            da.getLignes().add(ligne);
            totalHt = totalHt.add(ligne.calculerMontantHt());
        }
        da.setMontantEstimeHt(totalHt);
        da.setMontantEstimeTtc(totalHt.multiply(BigDecimal.ONE.add(tvaTaux)).setScale(2, RoundingMode.HALF_UP));

        construireVentilations(da, dto.getVentilations(), totalHt);
    }

    // --- Contrôles de cohérence budgétaire ---

    /** La ligne budgétaire doit appartenir à l'exercice de la demande (ex. une DA 2026 n'engage pas un budget 2025). */
    private void verifierExercice(BudgetAllocationStructure ligne, Integer exerciceDa) {
        Integer anneeLigne = ligne.getBudgetAllocationActivite().getBudgetAllocationOp()
                .getBudgetAllocationOs().getExerciceBudgetaire().getAnnee();
        if (!exerciceDa.equals(anneeLigne)) {
            throw new AchatException("La ligne budgétaire " + ligne.getId() + " appartient à l'exercice " + anneeLigne
                    + " alors que la demande porte sur l'exercice " + exerciceDa + ".");
        }
    }

    /** La ligne doit appartenir à la direction demandeuse, ou à l'une de ses structures (service, département...). */
    private void verifierPropriete(BudgetAllocationStructure ligne, Structure direction) {
        Structure courante = ligne.getStructure();
        while (courante != null) {
            if (direction.getId().equals(courante.getId())
                    || (courante.getDirection() != null && direction.getId().equals(courante.getDirection().getId()))) {
                return;
            }
            courante = courante.getParent();
        }
        throw new AchatException("La ligne budgétaire " + ligne.getId() + " n'appartient pas à la direction demandeuse "
                + direction.getCode() + " : une demande ne peut engager que le budget de sa propre direction.");
    }

    /** Une imputation vers une entité ne peut viser qu'une ligne budgétaire de cette entité. */
    private void verifierEntite(BudgetAllocationStructure ligne, Entite entiteImpactee) {
        Entite entiteLigne = ligne.getStructure() != null ? ligne.getStructure().getEntite() : null;
        if (entiteLigne == null || !entiteLigne.getCode().equals(entiteImpactee.getCode())) {
            throw new AchatException("La ligne budgétaire " + ligne.getId() + " n'appartient pas à l'entité "
                    + entiteImpactee.getCode() + ".");
        }
    }

    /** Les pourcentages doivent totaliser 100 % et les montants imputés le montant HT de la DA. */
    private void construireVentilations(DemandeAchat da, List<DossierVentilationRequestDto> ventilationsDto, BigDecimal totalHt) {
        if (ventilationsDto == null || ventilationsDto.isEmpty()) {
            return;
        }
        BigDecimal totalPourcentage = BigDecimal.ZERO;
        BigDecimal totalImpute = BigDecimal.ZERO;
        for (DossierVentilationRequestDto v : ventilationsDto) {
            Entite entite = entiteRepository.findById(v.getEntiteImpactee())
                    .orElseThrow(() -> new AchatException("Entité impactée introuvable : " + v.getEntiteImpactee()));
            BudgetAllocationStructure activite = allocationRepository.findById(v.getActiviteImpacteeId())
                    .orElseThrow(() -> new AchatException("Ligne budgétaire impactée introuvable : " + v.getActiviteImpacteeId()));
            verifierExercice(activite, da.getExerciceAnnee());
            verifierEntite(activite, entite);
            da.getVentilations().add(DossierVentilationImpact.builder()
                    .demandeAchat(da)
                    .entiteImpactee(entite)
                    .activiteImpactee(activite)
                    .montantImpute(v.getMontantImpute())
                    .pourcentageApplique(v.getPourcentageApplique())
                    .statutBlocage(false)
                    .build());
            totalPourcentage = totalPourcentage.add(v.getPourcentageApplique());
            totalImpute = totalImpute.add(v.getMontantImpute());
        }
        if (totalPourcentage.subtract(CENT).abs().compareTo(TOLERANCE) > 0) {
            throw new AchatException("La ventilation doit totaliser 100 % (actuellement " + totalPourcentage + " %).");
        }
        if (totalImpute.subtract(totalHt).abs().compareTo(TOLERANCE) > 0) {
            throw new AchatException("Les montants imputés (" + totalImpute + ") doivent totaliser le montant HT de la demande ("
                    + totalHt + ").");
        }
    }
}
