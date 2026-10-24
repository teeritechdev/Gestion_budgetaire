package com.budget.backend.stock.dto;

import com.budget.backend.stock.entity.MouvementStock;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MouvementStockResponseDto {
    private UUID id;
    private UUID articleId;
    private String typeMouvement;
    private Integer quantite;
    private Integer stockAnterieur;
    private Integer nouveauStock;
    private String referenceDocument;
    private UtilisateurSummaryDto auteur;
    private LocalDateTime createdAt;

    public static MouvementStockResponseDto from(MouvementStock m) {
        MouvementStockResponseDto dto = new MouvementStockResponseDto();
        dto.setId(m.getId());
        dto.setArticleId(m.getArticle().getId());
        dto.setTypeMouvement(m.getTypeMouvement());
        dto.setQuantite(m.getQuantite());
        dto.setStockAnterieur(m.getStockAnterieur());
        dto.setNouveauStock(m.getNouveauStock());
        dto.setReferenceDocument(m.getReferenceDocument());
        dto.setAuteur(UtilisateurSummaryDto.from(m.getAuteur()));
        dto.setCreatedAt(m.getCreatedAt());
        return dto;
    }
}
