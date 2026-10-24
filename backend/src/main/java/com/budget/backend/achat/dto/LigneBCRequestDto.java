package com.budget.backend.achat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class LigneBCRequestDto {
    @NotNull
    private UUID ligneDaId;
    private UUID articleId; // optionnel : repris de la ligne de DA s'il est absent
    @NotBlank
    private String description;
    // Facultatifs pour un forfait / un contrat cadre (alors montantTotalHt est obligatoire)
    @Positive
    private BigDecimal quantiteCommandee;
    @Positive
    private BigDecimal prixUnitaireHt;
    @Positive
    private BigDecimal montantTotalHt;
}
