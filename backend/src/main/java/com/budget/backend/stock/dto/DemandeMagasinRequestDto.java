package com.budget.backend.stock.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class DemandeMagasinRequestDto {
    // Optionnel : généré au format BS-AAAA-NNNN s'il est absent
    private String numeroDemande;
    @NotNull
    private UUID serviceId;
    // Optionnel : déduit de la direction du service ; s'il est fourni, il doit correspondre.
    private String structureCode;
    @NotBlank
    private String objet;
    @NotEmpty
    @Valid
    private List<LigneDemandeMagasinRequestDto> lignes;
}
