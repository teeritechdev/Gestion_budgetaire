package com.budget.backend.achat.dto;

import com.budget.backend.achat.entity.LigneBonCommande;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class LigneBCResponseDto {
    private UUID id;
    private UUID bcId;
    private UUID ligneDaId;
    private UUID articleId;
    private String description;
    private BigDecimal quantiteCommandee;
    private BigDecimal prixUnitaireHt;
    private BigDecimal montantTotalHt;
    private LocalDateTime createdAt;

    public static LigneBCResponseDto from(LigneBonCommande l) {
        LigneBCResponseDto dto = new LigneBCResponseDto();
        dto.setId(l.getId());
        dto.setBcId(l.getBonCommande().getId());
        dto.setLigneDaId(l.getLigneDa().getId());
        dto.setArticleId(l.getArticle() != null ? l.getArticle().getId() : null);
        dto.setDescription(l.getDescription());
        dto.setQuantiteCommandee(l.getQuantiteCommandee());
        dto.setPrixUnitaireHt(l.getPrixUnitaireHt());
        dto.setMontantTotalHt(l.getMontantTotalHt());
        dto.setCreatedAt(l.getCreatedAt());
        return dto;
    }
}
