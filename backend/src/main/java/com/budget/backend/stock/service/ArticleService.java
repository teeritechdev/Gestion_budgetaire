package com.budget.backend.stock.service;

import com.budget.backend.common.PageResponseDto;
import com.budget.backend.common.PageUtils;
import org.springframework.data.domain.Pageable;
import com.budget.backend.stock.StockException;
import com.budget.backend.stock.dto.ArticleRequestDto;
import com.budget.backend.stock.dto.ArticleResponseDto;
import com.budget.backend.stock.entity.Article;
import com.budget.backend.stock.repository.ArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    @Autowired
    private ArticleRepository repository;
    @Autowired
    private AuditHelper auditHelper;
    @Autowired
    private CurrentUserService currentUserService;

    public Optional<ArticleResponseDto> trouverParId(UUID id) {
        return repository.findById(id).map(ArticleResponseDto::from);
    }

    @Transactional
    public ArticleResponseDto creer(ArticleRequestDto dto) {
        if (repository.existsByCodeArticle(dto.getCodeArticle())) {
            throw new StockException("Un article avec ce code existe déjà : " + dto.getCodeArticle());
        }
        if (dto.getUniteMesure() == null || dto.getUniteMesure().isBlank()) {
            throw new StockException("L'unité de mesure est obligatoire.");
        }
        // quantiteStock démarre à 0 : il ne varie que par les mouvements de stock
        Article article = Article.builder()
                .codeArticle(dto.getCodeArticle())
                .designation(dto.getDesignation())
                .description(dto.getDescription())
                .uniteMesure(dto.getUniteMesure())
                .seuilReapprovisionnement(dto.getSeuilReapprovisionnement() != null ? dto.getSeuilReapprovisionnement() : 0)
                .prixUnitaireMoyenPondere(dto.getPrixUnitaireMoyenPondere() != null ? dto.getPrixUnitaireMoyenPondere() : BigDecimal.ZERO)
                .actif(dto.getActif() != null ? dto.getActif() : true)
                .build();
        article = repository.saveAndFlush(article);
        auditHelper.tracer(currentUserService.utilisateurConnecte(), AuditHelper.INSERTION, "articles", article.getId(), null,
                AuditHelper.valeurs("codeArticle", article.getCodeArticle(), "designation", article.getDesignation(),
                        "seuilReapprovisionnement", article.getSeuilReapprovisionnement(), "actif", article.getActif()));
        return ArticleResponseDto.from(article);
    }

    @Transactional
    public ArticleResponseDto modifier(UUID id, ArticleRequestDto dto) {
        Article article = repository.findById(id)
                .orElseThrow(() -> new StockException("Article introuvable avec l'ID : " + id));

        if (!article.getCodeArticle().equals(dto.getCodeArticle()) && repository.existsByCodeArticle(dto.getCodeArticle())) {
            throw new StockException("Un article avec ce code existe déjà : " + dto.getCodeArticle());
        }
        java.util.Map<String, Object> avant = AuditHelper.valeurs("codeArticle", article.getCodeArticle(),
                "designation", article.getDesignation(), "seuilReapprovisionnement", article.getSeuilReapprovisionnement(),
                "prixUnitaireMoyenPondere", article.getPrixUnitaireMoyenPondere(), "actif", article.getActif());
        article.setCodeArticle(dto.getCodeArticle());
        article.setDesignation(dto.getDesignation());
        article.setDescription(dto.getDescription());
        if (dto.getUniteMesure() != null && !dto.getUniteMesure().isBlank()) {
            article.setUniteMesure(dto.getUniteMesure());
        }
        if (dto.getSeuilReapprovisionnement() != null) {
            article.setSeuilReapprovisionnement(dto.getSeuilReapprovisionnement());
        }
        if (dto.getPrixUnitaireMoyenPondere() != null) {
            article.setPrixUnitaireMoyenPondere(dto.getPrixUnitaireMoyenPondere());
        }
        if (dto.getActif() != null) {
            article.setActif(dto.getActif());
        }
        article = repository.saveAndFlush(article);
        auditHelper.tracer(currentUserService.utilisateurConnecte(), AuditHelper.MODIFICATION, "articles", article.getId(), avant,
                AuditHelper.valeurs("codeArticle", article.getCodeArticle(), "designation", article.getDesignation(),
                        "seuilReapprovisionnement", article.getSeuilReapprovisionnement(),
                        "prixUnitaireMoyenPondere", article.getPrixUnitaireMoyenPondere(), "actif", article.getActif()));
        return ArticleResponseDto.from(article);
    }

    // Filtrage multicritères dynamique (sousSeuil : stock <= seuil de réapprovisionnement)
    public PageResponseDto<ArticleResponseDto> filtrer(String codeArticle, String designation, Boolean sousSeuil, Pageable pageable) {
        return PageResponseDto.from(repository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (codeArticle != null && !codeArticle.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("codeArticle")), "%" + codeArticle.toLowerCase() + "%"));
            }
            if (designation != null && !designation.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("designation")), "%" + designation.toLowerCase() + "%"));
            }
            if (Boolean.TRUE.equals(sousSeuil)) {
                predicates.add(cb.le(root.<Integer>get("quantiteStock"), root.<Integer>get("seuilReapprovisionnement")));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, PageUtils.limiter(pageable)), ArticleResponseDto::from);
    }
}
