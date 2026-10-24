package com.budget.backend.achat.dto;

import com.budget.backend.achat.entity.LigneDemandeAchat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class LigneDAResponseDto {
    private UUID id;
    private UUID activiteId;
    private UUID articleId;
    private String description;
    private String uniteMesure;
    private BigDecimal quantite;
    private BigDecimal prixUnitaireEstime;
    private BigDecimal montantTotalHt;
    private LocalDateTime createdAt;

    public static LigneDAResponseDto from(LigneDemandeAchat l) {
        LigneDAResponseDto dto = new LigneDAResponseDto();
        dto.setId(l.getId());
        dto.setActiviteId(l.getActivite().getId());
        dto.setArticleId(l.getArticle() != null ? l.getArticle().getId() : null);
        dto.setDescription(l.getDescription());
        dto.setUniteMesure(l.getUniteMesure());
        dto.setQuantite(l.getQuantite());
        dto.setPrixUnitaireEstime(l.getPrixUnitaireEstime());
        dto.setMontantTotalHt(l.calculerMontantHt());
        dto.setCreatedAt(l.getCreatedAt());
        return dto;
    }
}
