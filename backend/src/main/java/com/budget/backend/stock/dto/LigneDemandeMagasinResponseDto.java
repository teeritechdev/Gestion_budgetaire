package com.budget.backend.stock.dto;

import com.budget.backend.stock.entity.LigneDemandeMagasin;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class LigneDemandeMagasinResponseDto {
    private UUID id;
    private ArticleResponseDto article;
    private Integer quantiteDemande;
    private Integer quantiteServie;
    private LocalDateTime createdAt;

    public static LigneDemandeMagasinResponseDto from(LigneDemandeMagasin l) {
        LigneDemandeMagasinResponseDto dto = new LigneDemandeMagasinResponseDto();
        dto.setId(l.getId());
        dto.setArticle(ArticleResponseDto.from(l.getArticle()));
        dto.setQuantiteDemande(l.getQuantiteDemande());
        dto.setQuantiteServie(l.getQuantiteServie());
        dto.setCreatedAt(l.getCreatedAt());
        return dto;
    }
}
