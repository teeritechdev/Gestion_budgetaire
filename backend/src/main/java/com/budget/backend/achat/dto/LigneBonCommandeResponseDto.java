package com.budget.backend.achat.dto;

import com.budget.backend.achat.entity.LigneBonCommande;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneBonCommandeResponseDto {
    private UUID id;
    private UUID ligneDaId;
    private UUID articleId;
    private String articleCode;
    private String articleDesignation;
    private String description;
    private Integer quantiteCommandee;
    private BigDecimal prixUnitaireHt;
    private BigDecimal montantTotalHt;

    public static LigneBonCommandeResponseDto from(LigneBonCommande l) {
        if (l == null) return null;
        return LigneBonCommandeResponseDto.builder()
                .id(l.getId())
                .ligneDaId(l.getLigneDa() != null ? l.getLigneDa().getId() : null)
                .articleId(l.getArticle() != null ? l.getArticle().getId() : null)
                .articleCode(l.getArticle() != null ? l.getArticle().getCodeArticle() : null)
                .articleDesignation(l.getArticle() != null ? l.getArticle().getDesignation() : null)
                .description(l.getDescription())
                .quantiteCommandee(l.getQuantiteCommandee())
                .prixUnitaireHt(l.getPrixUnitaireHt())
                .montantTotalHt(l.getMontantTotalHt())
                .build();
    }
}

