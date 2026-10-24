package com.budget.backend.stock.service;

import com.budget.backend.common.PageResponseDto;
import com.budget.backend.common.PageUtils;
import org.springframework.data.domain.Pageable;
import com.budget.backend.stock.StockException;
import com.budget.backend.security.entity.Entite;
import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.EntiteRepository;
import com.budget.backend.security.repository.StructureRepository;
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
    private StructureRepository structureRepository;
    @Autowired
    private EntiteRepository entiteRepository;
    @Autowired
    private StockMovementService stockMovementService;
    @Autowired
    private NumeroDocumentService numeroDocumentService;
    @Autowired
    private CurrentUserService currentUserService;
    @Autowired
    private AuditHelper auditHelper;

    @Transactional(readOnly = true)
    public PageResponseDto<DemandeMagasinResponseDto> filtrer(String statut, UUID demandeurId, String entiteCode, Pageable pageable) {
        return PageResponseDto.from(repository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (statut != null && !statut.isEmpty()) {
                predicates.add(cb.equal(cb.upper(root.get("statut")), statut.toUpperCase()));
            }
            if (demandeurId != null) {
                predicates.add(cb.equal(root.get("demandeur").get("id"), demandeurId));
            }
            if (entiteCode != null && !entiteCode.isEmpty()) {
                predicates.add(cb.equal(root.get("entite").get("code"), entiteCode));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, PageUtils.limiter(pageable)), DemandeMagasinResponseDto::from);
    }

    @Transactional(readOnly = true)
    public Optional<DemandeMagasinResponseDto> trouverParId(UUID id) {
        return repository.findById(id).map(DemandeMagasinResponseDto::from);
    }

    @Transactional
    public DemandeMagasinResponseDto creer(DemandeMagasinRequestDto dto) {
        Utilisateur demandeur = currentUserService.utilisateurConnecte();
        Structure service = structureRepository.findById(dto.getServiceId())
                .orElseThrow(() -> new StockException("Structure (service) introuvable avec l'ID : " + dto.getServiceId()));
        Entite entite = entiteRepository.findById(dto.getEntiteCode())
                .orElseThrow(() -> new StockException("Entité introuvable avec le code : " + dto.getEntiteCode()));

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
                .entite(entite)
                .objet(dto.getObjet())
                .statut(DemandeMagasin.SOUMIS)
                .build();

        for (LigneDemandeMagasinRequestDto ligneDto : dto.getLignes()) {
            Article article = articleRepository.findById(ligneDto.getArticleId())
                    .orElseThrow(() -> new StockException("Article introuvable avec l'ID : " + ligneDto.getArticleId()));
            if (!Boolean.TRUE.equals(article.getActif())) {
                throw new StockException("L'article " + article.getCodeArticle() + " est inactif et ne peut pas être demandé.");
            }
            demande.getLignes().add(LigneDemandeMagasin.builder()
                    .demandeMagasin(demande)
                    .article(article)
                    .quantiteDemande(ligneDto.getQuantiteDemande())
                    .quantiteServie(0)
                    .build());
        }
        demande = repository.saveAndFlush(demande);
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

    /** Le valideur N+1 ne peut pas être le demandeur ; si le demandeur a un responsable, c'est lui qui valide. */
    private void verifierValideur(DemandeMagasin demande, Utilisateur valideur) {
        Utilisateur demandeur = demande.getDemandeur();
        if (estMemeUtilisateur(demandeur, valideur)) {
            throw new StockException("Un demandeur ne peut pas valider sa propre demande magasin.");
        }
        Utilisateur responsable = demandeur == null ? null : demandeur.getManager();
        if (responsable != null && !estMemeUtilisateur(responsable, valideur)) {
            throw new StockException("Seul le responsable hiérarchique du demandeur ("
                    + responsable.getPrenom() + " " + responsable.getNom() + ") peut valider cette demande.");
        }
    }

    private boolean estMemeUtilisateur(Utilisateur a, Utilisateur b) {
        return a != null && b != null && a.getId() != null && a.getId().equals(b.getId());
    }
}
