package com.budget.backend.achat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class DemandeAchatRequestDto {
    // Optionnel : généré au format DA-AAAA-NNNN s'il est absent
    private String codeDossier;
    @NotBlank
    private String entiteEmettrice;
    @NotBlank
    private String entiteBeneficiaire;
    @NotBlank
    private String typeAchat; // STOCKABLE, PRESTATION_SERVICE, INVESTISSEMENT_IMMO, TRAVAUX
    @NotBlank
    private String objet;
    @NotBlank
    private String justification;
    @NotNull
    private UUID directionDemandeurId;
    @NotNull
    private Integer exerciceAnnee;
    @NotEmpty
    @Valid
    private List<LigneDARequestDto> lignes;
    @Valid
    private List<DossierVentilationRequestDto> ventilations;
}
