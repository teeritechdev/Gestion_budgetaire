package com.budget.backend.stock.service;

import com.budget.backend.common.CsvReader;
import com.budget.backend.stock.entity.Article;
import com.budget.backend.stock.repository.ArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ArticleImportService {

    @Autowired
    private ArticleRepository articleRepository;
    @Autowired
    private CurrentUserService currentUserService;
    @Autowired
    private AuditHelper auditHelper;

    @Transactional
    public int importerArticles(MultipartFile fichier) {
        return CsvReader.lire(fichier, List.of("code_article|code", "designation", "unite_mesure|unite"), ligne -> {
            String codeArticle = ligne.obligatoire("code_article", "code").trim().toUpperCase();
            String designation = ligne.obligatoire("designation").trim();
            String uniteMesure = ligne.obligatoire("unite_mesure", "unite").trim();

            Integer seuil = ligne.entier("seuil_reapprovisionnement", "seuil");
            BigDecimal pump = ligne.decimal("prix_unitaire_moyen_pondere", "pump", "prix_unitaire");
            Integer stock = ligne.entier("quantite_stock", "stock", "quantite_initiale");
            boolean actif = ligne.booleen(true, "actif");
            String description = ligne.texte("description");

            Optional<Article> existant = articleRepository.findByCodeArticle(codeArticle);
            Article article;
            if (existant.isPresent()) {
                article = existant.get();
                article.setDesignation(designation);
                article.setUniteMesure(uniteMesure);
                if (seuil != null) {
                    article.setSeuilReapprovisionnement(seuil);
                }
                if (pump != null) {
                    article.setPrixUnitaireMoyenPondere(pump);
                }
                if (stock != null) {
                    article.setQuantiteStock(stock);
                }
                article.setActif(actif);
                if (description != null) {
                    article.setDescription(description);
                }
            } else {
                article = Article.builder()
                        .codeArticle(codeArticle)
                        .designation(designation)
                        .uniteMesure(uniteMesure)
                        .seuilReapprovisionnement(seuil != null ? seuil : 0)
                        .prixUnitaireMoyenPondere(pump != null ? pump : BigDecimal.ZERO)
                        .quantiteStock(stock != null ? stock : 0)
                        .actif(actif)
                        .description(description)
                        .build();
            }
            articleRepository.save(article);
        });
    }
}

