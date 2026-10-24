package com.budget.backend.achat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class LigneDARequestDto {
    @NotNull
    private UUID activiteId; // ligne budgétaire (budget_allocation_structure) à engager
    private UUID articleId;  // optionnel : article du catalogue (Module 3)
    @NotBlank
    private String description;
    @NotBlank
    private String uniteMesure;
    @NotNull
    @Positive
    private BigDecimal quantite;
    @NotNull
    @Positive
    private BigDecimal prixUnitaireEstime;
}
