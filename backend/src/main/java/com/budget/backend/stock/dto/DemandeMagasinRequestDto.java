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
    @NotBlank
    private String entiteCode;
    @NotBlank
    private String objet;
    @NotEmpty
    @Valid
    private List<LigneDemandeMagasinRequestDto> lignes;
}
