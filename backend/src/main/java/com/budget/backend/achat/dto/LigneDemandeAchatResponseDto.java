package com.budget.backend.achat.dto;

import com.budget.backend.achat.entity.LigneDemandeAchat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneDemandeAchatResponseDto {
    private UUID id;
    private UUID activiteId;
    private String activiteCode;
    private String activiteLibelle;
    private UUID articleId;
    private String articleCode;
    private String articleDesignation;
    private String description;
    private String uniteMesure;
    private Integer quantite;
    private BigDecimal prixUnitaireEstime;
    private BigDecimal montantTotalHt;
    private LocalDateTime createdAt;

    public static LigneDemandeAchatResponseDto from(LigneDemandeAchat l) {
        if (l == null) return null;
        return LigneDemandeAchatResponseDto.builder()
                .id(l.getId())
                .activiteId(l.getActivite() != null ? l.getActivite().getId() : null)
                .activiteCode(l.getActivite() != null ? l.getActivite().getCodeActivite() : null)
                .activiteLibelle(l.getActivite() != null ? l.getActivite().getLibelle() : null)
                .articleId(l.getArticle() != null ? l.getArticle().getId() : null)
                .articleCode(l.getArticle() != null ? l.getArticle().getCodeArticle() : null)
                .articleDesignation(l.getArticle() != null ? l.getArticle().getDesignation() : null)
                .description(l.getDescription())
                .uniteMesure(l.getUniteMesure())
                .quantite(l.getQuantite())
                .prixUnitaireEstime(l.getPrixUnitaireEstime())
                .montantTotalHt(l.getMontantTotalHt())
                .createdAt(l.getCreatedAt())
                .build();
    }
}

