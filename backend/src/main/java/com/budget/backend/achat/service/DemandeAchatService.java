package com.budget.backend.achat.service;

import com.budget.backend.achat.AchatException;
import com.budget.backend.achat.dto.DecisionValidationDto;
import com.budget.backend.achat.dto.DemandeAchatRequestDto;
import com.budget.backend.achat.dto.DemandeAchatResponseDto;
import com.budget.backend.achat.dto.HistoriqueValidationResponseDto;
import com.budget.backend.achat.dto.LigneDemandeAchatRequestDto;
import com.budget.backend.achat.dto.VentilationImpactRequestDto;
import com.budget.backend.achat.dto.VentilationImpactResponseDto;
import com.budget.backend.achat.entity.DemandeAchat;
import com.budget.backend.achat.entity.DossierVentilationImpact;
import com.budget.backend.achat.entity.HistoriqueValidation;
import com.budget.backend.achat.entity.LigneDemandeAchat;
import com.budget.backend.achat.repository.DemandeAchatRepository;
import com.budget.backend.achat.repository.DossierVentilationImpactRepository;
import com.budget.backend.achat.repository.HistoriqueValidationRepository;
import com.budget.backend.budget.entity.ActiviteCatalogue;
import com.budget.backend.budget.repository.ActiviteCatalogueRepository;
import com.budget.backend.common.PageResponseDto;
import com.budget.backend.common.PageUtils;
import com.budget.backend.security.entity.Affectation;
import com.budget.backend.security.entity.ServiceOrganisationnel;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.ServiceOrganisationnelRepository;
import com.budget.backend.security.service.AffectationService;
import com.budget.backend.stock.entity.Article;
import com.budget.backend.stock.repository.ArticleRepository;
import com.budget.backend.stock.service.CurrentUserService;
import com.budget.backend.stock.service.NumeroDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DemandeAchatService {

    @Autowired
    private DemandeAchatRepository repository;

    @Autowired
    private HistoriqueValidationRepository validationRepository;

    @Autowired
    private DossierVentilationImpactRepository ventilationRepository;

    @Autowired
    private ActiviteCatalogueRepository activiteRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ServiceOrganisationnelRepository serviceRepository;

    @Autowired
    private NumeroDocumentService numeroDocumentService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired(required = false)
    private com.budget.backend.workflow.service.WorkflowService workflowService;

    @Autowired
    private AffectationService affectationService;

    @Transactional(readOnly = true)
    public PageResponseDto<DemandeAchatResponseDto> filtrer(
            String statut, UUID demandeurId, String typeAchat, Integer annee, String recherche, Pageable pageable) {
        return PageResponseDto.from(repository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (statut != null && !statut.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("statut")), statut.trim().toUpperCase()));
            }
            if (demandeurId != null) {
                predicates.add(cb.equal(root.get("demandeur").get("id"), demandeurId));
            }
            if (typeAchat != null && !typeAchat.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("typeAchat")), typeAchat.trim().toUpperCase()));
            }
            if (annee != null) {
                predicates.add(cb.equal(root.get("exerciceAnnee"), annee));
            }
            if (recherche != null && !recherche.isBlank()) {
                String term = "%" + recherche.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("codeDossier")), term),
                        cb.like(cb.lower(root.get("objet")), term)
                ));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, PageUtils.limiter(pageable)), DemandeAchatResponseDto::from);
    }

    @Transactional(readOnly = true)
    public Optional<DemandeAchatResponseDto> trouverParId(UUID id) {
        return repository.findById(id).map(DemandeAchatResponseDto::from);
    }

    @Transactional
    public DemandeAchatResponseDto creer(DemandeAchatRequestDto dto) {
        Utilisateur demandeur = currentUserService.utilisateurConnecte();

        String code = dto.getCodeDossier();
        if (code == null || code.isBlank()) {
            code = numeroDocumentService.prochainCodeDemandeAchat();
        } else if (repository.existsByCodeDossier(code.trim())) {
            throw new AchatException("Une demande d'achat avec le code " + code + " existe déjà.");
        }

        ServiceOrganisationnel service = null;
        if (dto.getServiceDemandeurId() != null) {
            service = serviceRepository.findById(dto.getServiceDemandeurId())
                    .orElseThrow(() -> new AchatException("Service demandeur introuvable avec l'ID : " + dto.getServiceDemandeurId()));
        } else if (demandeur.getSalarie() != null) {
            Affectation aff = affectationService.affectationEnCours(demandeur.getSalarie().getId()).orElse(null);
            if (aff != null && aff.getService() != null) {
                service = aff.getService();
            }
        }

        DemandeAchat da = DemandeAchat.builder()
                .codeDossier(code)
                .entiteEmettrice(dto.getEntiteEmettrice().trim().toUpperCase())
                .entiteBeneficiaire(dto.getEntiteBeneficiaire().trim().toUpperCase())
                .typeAchat(dto.getTypeAchat().trim().toUpperCase())
                .objet(dto.getObjet().trim())
                .justification(dto.getJustification())
                .demandeur(demandeur)
                .serviceDemandeur(service)
                .exerciceAnnee(dto.getExerciceAnnee())
                .statut(DemandeAchat.SOUMIS)
                .etapeCourante("VALIDATION_N1")
                .lignes(new ArrayList<>())
                .build();

        for (LigneDemandeAchatRequestDto ligneDto : dto.getLignes()) {
            ActiviteCatalogue activite = activiteRepository.findById(ligneDto.getActiviteId())
                    .orElseThrow(() -> new AchatException("Activité budgétaire introuvable : " + ligneDto.getActiviteId()));

            Article article = null;
            if (ligneDto.getArticleId() != null) {
                article = articleRepository.findById(ligneDto.getArticleId())
                        .orElseThrow(() -> new AchatException("Article catalogue introuvable : " + ligneDto.getArticleId()));
            }

            LigneDemandeAchat ligne = LigneDemandeAchat.builder()
                    .demandeAchat(da)
                    .activite(activite)
                    .article(article)
                    .description(ligneDto.getDescription().trim())
                    .uniteMesure(ligneDto.getUniteMesure() != null ? ligneDto.getUniteMesure().trim() : (article != null ? article.getUniteMesure() : "Unité"))
                    .quantite(ligneDto.getQuantite())
                    .prixUnitaireEstime(ligneDto.getPrixUnitaireEstime())
                    .build();

            ligne.calculerMontantTotal();
            da.getLignes().add(ligne);
        }

        da.recalculerMontants();
        if (dto.getMontantEstimeTtc() != null && dto.getMontantEstimeTtc().compareTo(BigDecimal.ZERO) > 0) {
            da.setMontantEstimeTtc(dto.getMontantEstimeTtc());
        }

        da = repository.save(da);

        // Déclenchement automatique du circuit de workflow transversal s'il existe
        if (workflowService != null) {
            try {
                var instanceOpt = workflowService.demarrerWorkflowSiActif(
                        "DEMANDE_ACHAT",
                        da.getId(),
                        da.getCodeDossier(),
                        da.getObjet(),
                        da.getMontantEstimeTtc(),
                        da.getDemandeur()
                );
                if (instanceOpt.isPresent()) {
                    var inst = instanceOpt.get();
                    if (inst.getEtapeCourante() != null) {
                        da.setEtapeCourante(inst.getEtapeCourante().getCodeEtape());
                    }
                    da.setStatut(DemandeAchat.EN_COURS_VALIDATION);
                    da = repository.save(da);
                }
            } catch (Exception e) {
                System.err.println("Workflow non déclenché pour DA " + da.getCodeDossier() + ": " + e.getMessage());
            }
        }

        return DemandeAchatResponseDto.from(da);
    }

    @Transactional
    public DemandeAchatResponseDto modifier(UUID id, DemandeAchatRequestDto dto) {
        DemandeAchat da = repository.findById(id)
                .orElseThrow(() -> new AchatException("Demande d'achat introuvable avec l'ID : " + id));

        if (!DemandeAchat.BROUILLON.equals(da.getStatut()) && !DemandeAchat.SOUMIS.equals(da.getStatut())) {
            throw new AchatException("Seule une demande au statut BROUILLON ou SOUMIS peut être modifiée.");
        }

        da.setEntiteEmettrice(dto.getEntiteEmettrice().trim().toUpperCase());
        da.setEntiteBeneficiaire(dto.getEntiteBeneficiaire().trim().toUpperCase());
        da.setTypeAchat(dto.getTypeAchat().trim().toUpperCase());
        da.setObjet(dto.getObjet().trim());
        da.setJustification(dto.getJustification());
        da.setExerciceAnnee(dto.getExerciceAnnee());

        if (dto.getServiceDemandeurId() != null) {
            ServiceOrganisationnel service = serviceRepository.findById(dto.getServiceDemandeurId())
                    .orElseThrow(() -> new AchatException("Service demandeur introuvable : " + dto.getServiceDemandeurId()));
            da.setServiceDemandeur(service);
        }

        da.getLignes().clear();
        for (LigneDemandeAchatRequestDto ligneDto : dto.getLignes()) {
            ActiviteCatalogue activite = activiteRepository.findById(ligneDto.getActiviteId())
                    .orElseThrow(() -> new AchatException("Activité budgétaire introuvable : " + ligneDto.getActiviteId()));

            Article article = null;
            if (ligneDto.getArticleId() != null) {
                article = articleRepository.findById(ligneDto.getArticleId())
                        .orElseThrow(() -> new AchatException("Article introuvable : " + ligneDto.getArticleId()));
            }

            LigneDemandeAchat ligne = LigneDemandeAchat.builder()
                    .demandeAchat(da)
                    .activite(activite)
                    .article(article)
                    .description(ligneDto.getDescription().trim())
                    .uniteMesure(ligneDto.getUniteMesure() != null ? ligneDto.getUniteMesure().trim() : (article != null ? article.getUniteMesure() : "Unité"))
                    .quantite(ligneDto.getQuantite())
                    .prixUnitaireEstime(ligneDto.getPrixUnitaireEstime())
                    .build();

            ligne.calculerMontantTotal();
            da.getLignes().add(ligne);
        }

        da.recalculerMontants();
        if (dto.getMontantEstimeTtc() != null && dto.getMontantEstimeTtc().compareTo(BigDecimal.ZERO) > 0) {
            da.setMontantEstimeTtc(dto.getMontantEstimeTtc());
        }

        da = repository.save(da);

        // Déclenchement automatique du circuit de workflow transversal s'il existe
        if (workflowService != null) {
            try {
                var instanceOpt = workflowService.demarrerWorkflowSiActif(
                        "DEMANDE_ACHAT",
                        da.getId(),
                        da.getCodeDossier(),
                        da.getObjet(),
                        da.getMontantEstimeTtc(),
                        da.getDemandeur()
                );
                if (instanceOpt.isPresent()) {
                    var inst = instanceOpt.get();
                    if (inst.getEtapeCourante() != null) {
                        da.setEtapeCourante(inst.getEtapeCourante().getCodeEtape());
                    }
                    da.setStatut(DemandeAchat.EN_COURS_VALIDATION);
                    da = repository.save(da);
                }
            } catch (Exception e) {
                System.err.println("Workflow non déclenché pour DA " + da.getCodeDossier() + ": " + e.getMessage());
            }
        }

        return DemandeAchatResponseDto.from(da);
    }

    @Transactional
    public DemandeAchatResponseDto valider(UUID id, DecisionValidationDto dto) {
        DemandeAchat da = repository.findById(id)
                .orElseThrow(() -> new AchatException("Demande d'achat introuvable avec l'ID : " + id));

        if (DemandeAchat.COMMANDE.equals(da.getStatut())) {
            throw new AchatException("Une demande d'achat déjà commandée ne peut plus être modifiée.");
        }

        Utilisateur valideur = currentUserService.utilisateurConnecte();

        String decision = dto.getDecision() != null ? dto.getDecision().trim().toUpperCase() : "";
        if (!"APPROUVE".equals(decision) && !"REJETE".equals(decision)) {
            throw new AchatException("Décision invalide : " + decision + ". Doit être APPROUVE ou REJETE.");
        }

        String etape = dto.getEtape() != null && !dto.getEtape().isBlank() ? dto.getEtape().trim() : da.getEtapeCourante();
        if (etape == null || etape.isBlank()) {
            etape = "VALIDATION_N1";
        }

        HistoriqueValidation h = HistoriqueValidation.builder()
                .demandeAchat(da)
                .etape(etape)
                .valideur(valideur)
                .decision(decision)
                .commentaire(dto.getCommentaire() != null ? dto.getCommentaire().trim() : null)
                .build();
        validationRepository.save(h);

        if ("APPROUVE".equals(decision)) {
            da.setStatut(DemandeAchat.APPROUVE);
            da.setEtapeCourante("APPROUVE");
        } else {
            da.setStatut(DemandeAchat.REJETE);
            da.setEtapeCourante("REJETE");
        }

        da = repository.save(da);
        return DemandeAchatResponseDto.from(da);
    }

    @Transactional(readOnly = true)
    public List<HistoriqueValidationResponseDto> obtenirHistoriqueValidations(UUID daId) {
        return validationRepository.findByDemandeAchat_IdOrderByDateDecisionAsc(daId).stream()
                .map(HistoriqueValidationResponseDto::from)
                .toList();
    }

    @Transactional
    public List<VentilationImpactResponseDto> enregistrerVentilations(UUID daId, List<VentilationImpactRequestDto> dtos) {
        DemandeAchat da = repository.findById(daId)
                .orElseThrow(() -> new AchatException("Demande d'achat introuvable avec l'ID : " + daId));

        if (DemandeAchat.COMMANDE.equals(da.getStatut())) {
            throw new AchatException("Impossible de modifier la ventilation d'une demande déjà commandée.");
        }

        if (dtos == null || dtos.isEmpty()) {
            ventilationRepository.deleteByDemandeAchat_Id(daId);
            return List.of();
        }

        BigDecimal sommePourcentages = dtos.stream()
                .map(VentilationImpactRequestDto::getPourcentageApplique)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sommePourcentages.subtract(BigDecimal.valueOf(100)).abs().compareTo(new BigDecimal("0.5")) > 0) {
            throw new AchatException("La somme des pourcentages de ventilation doit être égale à 100% (actuel: " + sommePourcentages + "%).");
        }

        ventilationRepository.deleteByDemandeAchat_Id(daId);

        List<DossierVentilationImpact> entites = new ArrayList<>();
        for (VentilationImpactRequestDto dto : dtos) {
            ActiviteCatalogue act = null;
            if (dto.getActiviteImpacteeId() != null) {
                act = activiteRepository.findById(dto.getActiviteImpacteeId())
                        .orElseThrow(() -> new AchatException("Activité budgétaire impactée introuvable : " + dto.getActiviteImpacteeId()));
            }

            DossierVentilationImpact v = DossierVentilationImpact.builder()
                    .demandeAchat(da)
                    .entiteImpactee(dto.getEntiteImpactee().trim().toUpperCase())
                    .activiteImpactee(act)
                    .montantImpute(dto.getMontantImpute())
                    .pourcentageApplique(dto.getPourcentageApplique())
                    .statutBlocage(dto.getStatutBlocage() != null ? dto.getStatutBlocage() : false)
                    .build();
            entites.add(v);
        }

        List<DossierVentilationImpact> saved = ventilationRepository.saveAll(entites);
        return saved.stream().map(VentilationImpactResponseDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<VentilationImpactResponseDto> obtenirVentilations(UUID daId) {
        return ventilationRepository.findByDemandeAchat_IdOrderByCreatedAtAsc(daId).stream()
                .map(VentilationImpactResponseDto::from)
                .toList();
    }
}

