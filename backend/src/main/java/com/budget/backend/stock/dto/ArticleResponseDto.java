package com.budget.backend.stock.dto;

import com.budget.backend.stock.entity.Article;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ArticleResponseDto {
    private UUID id;
    private String codeArticle;
    private String designation;
    private String description;
    private String uniteMesure;
    private Integer quantiteStock;
    private Integer seuilReapprovisionnement;
    private BigDecimal prixUnitaireMoyenPondere;
    private Boolean actif;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ArticleResponseDto from(Article a) {
        ArticleResponseDto dto = new ArticleResponseDto();
        dto.setId(a.getId());
        dto.setCodeArticle(a.getCodeArticle());
        dto.setDesignation(a.getDesignation());
        dto.setDescription(a.getDescription());
        dto.setUniteMesure(a.getUniteMesure());
        dto.setQuantiteStock(a.getQuantiteStock());
        dto.setSeuilReapprovisionnement(a.getSeuilReapprovisionnement());
        dto.setPrixUnitaireMoyenPondere(a.getPrixUnitaireMoyenPondere());
        dto.setActif(a.getActif());
        dto.setCreatedAt(a.getCreatedAt());
        dto.setUpdatedAt(a.getUpdatedAt());
        return dto;
    }
}
