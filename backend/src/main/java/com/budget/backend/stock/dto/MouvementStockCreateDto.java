package com.budget.backend.stock.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class MouvementStockCreateDto {
    @NotNull
    private UUID articleId;
    @NotBlank
    private String typeMouvement; // ENTREE, SORTIE, AJUSTEMENT
    // ENTREE / SORTIE : nombre d'unités (> 0). AJUSTEMENT : stock physique compté (>= 0).
    @NotNull
    private Integer quantite;
    @NotBlank
    private String referenceDocument;
    // Optionnel, ENTREE uniquement : prix unitaire d'achat pour recalculer le PUMP
    private BigDecimal prixUnitaire;
}
