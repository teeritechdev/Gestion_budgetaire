package com.budget.backend.stock.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ArticleRequestDto {
    @NotBlank
    private String codeArticle;
    @NotBlank
    private String designation;
    private String description;
    private String uniteMesure;
    @PositiveOrZero
    private Integer seuilReapprovisionnement;
    @PositiveOrZero
    private BigDecimal prixUnitaireMoyenPondere;
    private Boolean actif;
}
