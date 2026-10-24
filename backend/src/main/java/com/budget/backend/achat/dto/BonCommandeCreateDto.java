package com.budget.backend.achat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BonCommandeCreateDto {
    // CLASSIQUE (par défaut) ou CADRE_ANNUEL
    @NotBlank
    private String typeBc; // CLASSIQUE ou CADRE_ANNUEL
    @NotNull
    private UUID daId;
    @NotNull
    private UUID fournisseurId;
    @NotEmpty
    @Valid
    private List<LigneBCRequestDto> lignes;
}
