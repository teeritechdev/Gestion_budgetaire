package com.budget.backend.stock.service;

import com.budget.backend.common.PageResponseDto;
import com.budget.backend.common.PageUtils;
import org.springframework.data.domain.Pageable;
import com.budget.backend.stock.StockException;
import com.budget.backend.security.entity.Affectation;
import com.budget.backend.security.entity.Salarie;
import com.budget.backend.security.entity.ServiceOrganisationnel;
import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.AffectationRepository;
import com.budget.backend.security.repository.ServiceOrganisationnelRepository;
import com.budget.backend.security.service.AffectationService;
import com.budget.backend.stock.repository.StockUtilisateurRepository;
import com.budget.backend.stock.dto.*;
import com.budget.backend.stock.entity.Article;
import com.budget.backend.stock.entity.DemandeMagasin;
import com.budget.backend.stock.entity.LigneDemandeMagasin;
import com.budget.backend.stock.repository.ArticleRepository;
import com.budget.backend.stock.repository.DemandeMagasinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cycle : SOUMIS -> VALIDE_N1 -> (PARTIELLEMENT_SERVIE ->) SERVIE, ou SOUMIS -> REJETEE.
 * Une demande partiellement servie peut être servie à nouveau (les quantités se cumulent).
 */
@Service
public class DemandeMagasinService {

    @Autowired
    private DemandeMagasinRepository repository;
    @Autowired
    private ArticleRepository articleRepository;
    @Autowired
    private ServiceOrganisationnelRepository serviceRepository;
    @Autowired
    private StockUtilisateurRepository utilisateurRepository;
    @Autowired
    private StockMovementService stockMovementService;
    @Autowired
    private NumeroDocumentService numeroDocumentService;
    @Autowired
    private CurrentUserService currentUserService;

    @Autowired(required = false)
    private com.budget.backend.workflow.service.WorkflowService workflowService;
    @Autowired
    private AffectationRepository affectationRepository;
    @Autowired
    private AffectationService affectationService;
    @Autowired
    private AuditHelper auditHelper;

    @Transactional(readOnly = true)
    public PageResponseDto<DemandeMagasinResponseDto> filtrer(String statut, UUID demandeurId, String structureCode, Pageable pageable) {
        return PageResponseDto.from(repository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (statut != null && !statut.isEmpty()) {
                predicates.add(cb.equal(cb.upper(root.get("statut")), statut.toUpperCase()));
            }
            if (demandeurId != null) {
                predicates.add(cb.equal(root.get("demandeur").get("id"), demandeurId));
            }
            if (structureCode != null && !structureCode.isEmpty()) {
                predicates.add(cb.equal(root.get("structure").get("code"), structureCode));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, PageUtils.limiter(pageable)), DemandeMagasinResponseDto::from);
    }

    @Transactional(readOnly = true)
    public Optional<DemandeMagasinResponseDto> trouverParId(UUID id) {
        return repository.findById(id).map(DemandeMagasinResponseDto::from);
    }

    @Transactional(readOnly = true)
    public byte[] genererBonDeSortiePdf(UUID id) {
        DemandeMagasin demande = repository.findById(id)
                .orElseThrow(() -> new StockException("Demande magasin introuvable avec l'ID : " + id));
        return BonDeSortiePdfGenerator.generer(demande);
    }

    @Transactional
    public DemandeMagasinResponseDto creer(DemandeMagasinRequestDto dto) {
        Utilisateur demandeur = currentUserService.utilisateurConnecte();
        ServiceOrganisationnel service = null;
        if (dto.getServiceId() != null) {
            service = serviceRepository.findById(dto.getServiceId())
                    .orElseThrow(() -> new StockException("Service introuvable avec l'ID : " + dto.getServiceId()));
        } else if (demandeur.getSalarie() != null) {
            // Auto-détection intelligente du service via l'affectation active du salarié demandeur
            Affectation aff = affectationService.affectationEnCours(demandeur.getSalarie().getId()).orElse(null);
            if (aff != null && aff.getService() != null) {
                service = aff.getService();
            }
        }
        if (service == null) {
            throw new StockException("Le service demandeur est obligatoire et n'a pas pu être résolu depuis vos affectations.");
        }
        Structure structure = service.getDirection() == null ? null : service.getDirection().getStructure();
        if (structure == null) {
            throw new StockException("Le service " + service.getCode() + " n'est rattaché à aucune structure.");
        }
        if (dto.getStructureCode() != null && !dto.getStructureCode().isBlank()
                && !structure.getCode().equalsIgnoreCase(dto.getStructureCode().trim())) {
            throw new StockException("Le service " + service.getCode() + " appartient à la structure "
                    + structure.getCode() + ", pas à " + dto.getStructureCode() + ".");
        }

        String numero = dto.getNumeroDemande();
        if (numero == null || numero.isBlank()) {
            numero = numeroDocumentService.prochainNumeroBonSortie();
        } else if (repository.existsByNumeroDemande(numero)) {
            throw new StockException("Une demande avec ce numéro existe déjà : " + numero);
        }

        DemandeMagasin demande = DemandeMagasin.builder()
                .numeroDemande(numero)
                .demandeur(demandeur)
                .service(service)
                .structure(structure)
                .objet(dto.getObjet())
                .statut(DemandeMagasin.SOUMIS)
                .build();

        for (LigneDemandeMagasinRequestDto ligneDto : dto.getLignes()) {
            Article article = articleRepository.findById(ligneDto.getArticleId())
                    .orElseThrow(() -> new StockException("Article introuvable avec l'ID : " + ligneDto.getArticleId()));
            if (!Boolean.TRUE.equals(article.getActif())) {
                throw new StockException("L'article " + article.getCodeArticle() + " est inactif et ne peut pas être demandé.");
            }
            int stockDisponible = article.getQuantiteStock() != null ? article.getQuantiteStock() : 0;
            if (stockDisponible <= 0) {
                throw new StockException("L'article " + article.getCodeArticle() + " (" + article.getDesignation()
                        + ") est en rupture de stock (stock disponible : 0). Impossible de créer la demande.");
            }
            if (ligneDto.getQuantiteDemande() == null || ligneDto.getQuantiteDemande() <= 0) {
                throw new StockException("La quantité demandée pour l'article " + article.getCodeArticle()
                        + " doit être supérieure à zéro.");
            }
            if (ligneDto.getQuantiteDemande() > stockDisponible) {
                throw new StockException("La quantité demandée (" + ligneDto.getQuantiteDemande()
                        + ") pour l'article " + article.getCodeArticle() + " (" + article.getDesignation()
                        + ") dépasse le stock disponible (" + stockDisponible + " " + article.getUniteMesure() + ").");
            }
            demande.getLignes().add(LigneDemandeMagasin.builder()
                    .demandeMagasin(demande)
                    .article(article)
                    .quantiteDemande(ligneDto.getQuantiteDemande())
                    .quantiteServie(0)
                    .build());
        }
        demande = repository.saveAndFlush(demande);

        // Déclenchement automatique du circuit de workflow transversal si configuré
        if (workflowService != null) {
            try {
                workflowService.demarrerWorkflowSiActif(
                        "DEMANDE_STOCK",
                        demande.getId(),
                        demande.getNumeroDemande(),
                        demande.getObjet(),
                        BigDecimal.ZERO,
                        demande.getDemandeur()
                );
            } catch (Exception e) {
                System.err.println("Workflow non déclenché pour Demande Magasin " + demande.getNumeroDemande() + ": " + e.getMessage());
            }
        }
        auditHelper.tracer(demandeur, AuditHelper.INSERTION, "demandes_magasin", demande.getId(), null,
                AuditHelper.valeurs("numeroDemande", demande.getNumeroDemande(), "statut", demande.getStatut(),
                        "nbLignes", demande.getLignes().size()));
        return DemandeMagasinResponseDto.from(demande);
    }

    /** Validation (APPROUVE) ou rejet (REJETE) par le responsable hiérarchique. */
    @Transactional
    public DemandeMagasinResponseDto validerN1(UUID id, ValidationDecisionDto dto) {
        DemandeMagasin demande = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new StockException("Demande magasin introuvable avec l'ID : " + id));
        if (!DemandeMagasin.SOUMIS.equals(demande.getStatut())) {
            throw new StockException("Seule une demande au statut SOUMIS peut être validée ou rejetée (statut actuel : "
                    + demande.getStatut() + ").");
        }
        Utilisateur valideur = currentUserService.utilisateurConnecte();
        verifierValideur(demande, valideur);
        String decision = dto.getDecision() == null ? "" : dto.getDecision().trim().toUpperCase();
        switch (decision) {
            case "APPROUVE" -> demande.setStatut(DemandeMagasin.VALIDE_N1);
            case "REJETE" -> demande.setStatut(DemandeMagasin.REJETEE);
            default -> throw new StockException("Décision invalide : '" + dto.getDecision() + "' (attendu : APPROUVE ou REJETE).");
        }
        demande.setValideurN1(valideur);
        demande.setDateValidation(LocalDateTime.now());
        demande = repository.saveAndFlush(demande);
        auditHelper.tracer(valideur, AuditHelper.VALIDATION, "demandes_magasin", demande.getId(),
                AuditHelper.valeurs("statut", DemandeMagasin.SOUMIS),
                AuditHelper.valeurs("statut", demande.getStatut(), "decision", decision, "commentaire", dto.getCommentaire()));
        return DemandeMagasinResponseDto.from(demande);
    }

    /** Remise du matériel par le magasinier ; peut être appelée plusieurs fois tant qu'il reste un reliquat. */
    @Transactional
    public DemandeMagasinResponseDto servir(UUID id, DemandeMagasinServieDto dto) {
        DemandeMagasin demande = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new StockException("Demande magasin introuvable avec l'ID : " + id));
        if (!DemandeMagasin.VALIDE_N1.equals(demande.getStatut())
                && !DemandeMagasin.PARTIELLEMENT_SERVIE.equals(demande.getStatut())) {
            throw new StockException("La demande doit être validée (VALIDE_N1) ou partiellement servie pour être servie "
                    + "(statut actuel : " + demande.getStatut() + ").");
        }
        Utilisateur magasinier = currentUserService.utilisateurConnecte();
        if (estMemeUtilisateur(demande.getDemandeur(), magasinier)) {
            throw new StockException("Un demandeur ne peut pas servir sa propre demande magasin.");
        }

        int totalServi = 0;
        for (DemandeMagasinServieDto.LigneServie ls : dto.getLignes()) {
            LigneDemandeMagasin ligne = demande.getLignes().stream()
                    .filter(l -> l.getId().equals(ls.getLigneId()))
                    .findFirst()
                    .orElseThrow(() -> new StockException("La ligne " + ls.getLigneId() + " n'appartient pas à cette demande."));

            int reste = ligne.getQuantiteDemande() - ligne.getQuantiteServie();
            if (ls.getQuantiteServie() > reste) {
                throw new StockException("Quantité servie (" + ls.getQuantiteServie() + ") supérieure au reste à servir ("
                        + reste + ") pour l'article " + ligne.getArticle().getCodeArticle() + ".");
            }
            if (ls.getQuantiteServie() > 0) {
                stockMovementService.sortieStock(ligne.getArticle().getId(), ls.getQuantiteServie(),
                        demande.getNumeroDemande(), magasinier);
                ligne.setQuantiteServie(ligne.getQuantiteServie() + ls.getQuantiteServie());
                totalServi += ls.getQuantiteServie();
            }
        }
        if (totalServi == 0) {
            throw new StockException("Aucune quantité servie : renseignez au moins une quantité positive.");
        }

        boolean toutServi = demande.getLignes().stream()
                .allMatch(l -> l.getQuantiteServie() >= l.getQuantiteDemande());
        String statutAvant = demande.getStatut();
        demande.setStatut(toutServi ? DemandeMagasin.SERVIE : DemandeMagasin.PARTIELLEMENT_SERVIE);
        demande.setMagasinier(magasinier);
        demande.setDateServie(LocalDateTime.now());
        demande = repository.saveAndFlush(demande);
        auditHelper.tracer(magasinier, AuditHelper.MODIFICATION, "demandes_magasin", demande.getId(),
                AuditHelper.valeurs("statut", statutAvant),
                AuditHelper.valeurs("statut", demande.getStatut(), "quantiteServieCeJour", totalServi));
        return DemandeMagasinResponseDto.from(demande);
    }

    /**
     * Le valideur N+1 ne peut pas être le demandeur lui-même.
     * Sont habilités à valider la demande :
     * 1. L'administrateur système (rôle ADMIN_SYSTEME).
     * 2. Le manager hiérarchique direct du salarié demandeur.
     * 3. Le chef de service ou directeur de rattachement du service demandeur
     *    (identifié via les affectations actives avec fonction chef/responsable/directeur).
     */
    private void verifierValideur(DemandeMagasin demande, Utilisateur valideur) {
        Utilisateur demandeur = demande.getDemandeur();
        if (estMemeUtilisateur(demandeur, valideur)) {
            throw new StockException("Un demandeur ne peut pas valider sa propre demande magasin.");
        }

        // 1. Administrateur système
        if (valideur.getRole() != null && "ADMIN_SYSTEME".equalsIgnoreCase(valideur.getRole().getCode())) {
            return;
        }

        // 2. Manager hiérarchique direct du demandeur
        Salarie manager = demandeur == null || demandeur.getSalarie() == null ? null : demandeur.getSalarie().getManager();
        if (manager != null) {
            Utilisateur responsable = utilisateurRepository.findBySalarie_Id(manager.getId()).orElse(null);
            if (estMemeUtilisateur(responsable, valideur)) {
                return;
            }
        }

        // 3. Chef de service ou directeur via affectation active
        if (valideur.getSalarie() != null && demande.getService() != null) {
            LocalDate aujourdHui = LocalDate.now();
            UUID salarieValideurId = valideur.getSalarie().getId();
            UUID serviceId = demande.getService().getId();
            UUID directionId = demande.getService().getDirection() != null ? demande.getService().getDirection().getId() : null;

            List<Affectation> affectationsValideur = affectationRepository.findBySalarie_Id(salarieValideurId);
            boolean estResponsableHierarchique = affectationsValideur.stream()
                    .filter(a -> a.estEnCoursLe(aujourdHui))
                    .anyMatch(a -> {
                        boolean matchEntite = (a.getService() != null && a.getService().getId().equals(serviceId))
                                || (directionId != null && a.getDirection() != null && a.getDirection().getId().equals(directionId));
                        if (!matchEntite) return false;
                        String f = a.getFonction() != null ? a.getFonction().toLowerCase() : "";
                        return f.contains("chef") || f.contains("direct") || f.contains("responsable")
                                || f.contains("manager") || f.contains("supervis");
                    });

            if (estResponsableHierarchique) {
                return;
            }
        }

        if (manager != null) {
            throw new StockException("Seul le responsable hiérarchique ("
                    + manager.getPrenom() + " " + manager.getNom()
                    + ") ou le chef de service / directeur peut valider cette demande.");
        } else {
            throw new StockException("Seul le chef de service, le directeur ou un administrateur peut valider cette demande.");
        }
    }

    private boolean estMemeUtilisateur(Utilisateur a, Utilisateur b) {
        return a != null && b != null && a.getId() != null && a.getId().equals(b.getId());
    }
}
