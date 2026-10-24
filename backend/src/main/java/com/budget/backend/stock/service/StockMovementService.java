package com.budget.backend.stock.service;

import com.budget.backend.common.PageResponseDto;
import com.budget.backend.common.PageUtils;
import org.springframework.data.domain.Pageable;
import com.budget.backend.stock.StockException;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.stock.dto.MouvementStockResponseDto;
import com.budget.backend.stock.entity.Article;
import com.budget.backend.stock.entity.MouvementStock;
import com.budget.backend.stock.repository.ArticleRepository;
import com.budget.backend.stock.repository.MouvementStockRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Seul point d'écriture du stock : toute variation de quantite_stock passe par ici
 * et laisse une trace dans mouvements_stock.
 * entreeStock() est destiné au Module 5 (validation d'un PV de réception conforme).
 */
@Service
public class StockMovementService {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private MouvementStockRepository mouvementRepository;

    @PersistenceContext
    private EntityManager em;

    /** Entrée en stock + recalcul du PUMP si un prix unitaire est fourni. */
    @Transactional
    public MouvementStock entreeStock(UUID articleId, int quantite, BigDecimal prixUnitaire,
                                      String referenceDocument, Utilisateur auteur) {
        if (quantite <= 0) {
            throw new StockException("La quantité d'une entrée doit être strictement positive.");
        }
        Article article = verrouiller(articleId);
        int ancienStock = article.getQuantiteStock();
        int nouveauStock = ancienStock + quantite;

        if (prixUnitaire != null) {
            if (prixUnitaire.signum() < 0) {
                throw new StockException("Le prix unitaire ne peut pas être négatif.");
            }
            // PUMP = (stock * PUMP + qté * prix) / (stock + qté)
            BigDecimal valeurAncienne = article.getPrixUnitaireMoyenPondere().multiply(BigDecimal.valueOf(ancienStock));
            BigDecimal valeurEntree = prixUnitaire.multiply(BigDecimal.valueOf(quantite));
            article.setPrixUnitaireMoyenPondere(
                    valeurAncienne.add(valeurEntree).divide(BigDecimal.valueOf(nouveauStock), 2, RoundingMode.HALF_UP));
        }
        return appliquer(article, MouvementStock.ENTREE, quantite, ancienStock, nouveauStock, referenceDocument, auteur);
    }

    /** Sortie de stock : refuse tout stock négatif. */
    @Transactional
    public MouvementStock sortieStock(UUID articleId, int quantite, String referenceDocument, Utilisateur auteur) {
        if (quantite <= 0) {
            throw new StockException("La quantité d'une sortie doit être strictement positive.");
        }
        Article article = verrouiller(articleId);
        int ancienStock = article.getQuantiteStock();
        if (quantite > ancienStock) {
            throw new StockException("Stock insuffisant pour l'article " + article.getCodeArticle()
                    + " : disponible " + ancienStock + ", demandé " + quantite);
        }
        return appliquer(article, MouvementStock.SORTIE, quantite, ancienStock, ancienStock - quantite, referenceDocument, auteur);
    }

    /** Ajustement d'inventaire : fixe le stock au niveau physique compté. */
    @Transactional
    public MouvementStock ajusterStock(UUID articleId, int stockCompte, String referenceDocument, Utilisateur auteur) {
        if (stockCompte < 0) {
            throw new StockException("Le stock compté ne peut pas être négatif.");
        }
        Article article = verrouiller(articleId);
        int ancienStock = article.getQuantiteStock();
        // quantite = écart absolu ; le sens se lit avec stock_anterieur / nouveau_stock
        return appliquer(article, MouvementStock.AJUSTEMENT, Math.abs(stockCompte - ancienStock),
                ancienStock, stockCompte, referenceDocument, auteur);
    }

    /**
     * Verrouille l'article puis en relit l'état : si l'article était déjà chargé dans cette transaction,
     * la requête verrouillée renverrait l'instance existante, éventuellement périmée.
     */
    private Article verrouiller(UUID articleId) {
        Article article = articleRepository.findByIdForUpdate(articleId)
                .orElseThrow(() -> new StockException("Article introuvable avec l'ID : " + articleId));
        em.refresh(article);
        return article;
    }

    private MouvementStock appliquer(Article article, String type, int quantite, int ancienStock, int nouveauStock,
                                     String referenceDocument, Utilisateur auteur) {
        if (referenceDocument == null || referenceDocument.isBlank()) {
            throw new StockException("La référence du document justificatif est obligatoire.");
        }
        article.setQuantiteStock(nouveauStock);
        articleRepository.save(article);
        return mouvementRepository.saveAndFlush(MouvementStock.builder()
                .article(article)
                .typeMouvement(type)
                .quantite(quantite)
                .stockAnterieur(ancienStock)
                .nouveauStock(nouveauStock)
                .referenceDocument(referenceDocument)
                .auteur(auteur)
                .build());
    }

    // --- Consultation ---

    @Transactional(readOnly = true)
    public PageResponseDto<MouvementStockResponseDto> filtrer(UUID articleId, String typeMouvement, LocalDate dateDebut, LocalDate dateFin, Pageable pageable) {
        return PageResponseDto.from(mouvementRepository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (articleId != null) {
                predicates.add(cb.equal(root.get("article").get("id"), articleId));
            }
            if (typeMouvement != null && !typeMouvement.isEmpty()) {
                predicates.add(cb.equal(cb.upper(root.get("typeMouvement")), typeMouvement.toUpperCase()));
            }
            if (dateDebut != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("createdAt"), dateDebut.atStartOfDay()));
            }
            if (dateFin != null) {
                predicates.add(cb.lessThan(root.<LocalDateTime>get("createdAt"), dateFin.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, PageUtils.limiter(pageable)), MouvementStockResponseDto::from);
    }
}
