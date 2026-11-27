package com.budget.backend.achat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeAchatRequestDto {

    private String codeDossier;

    @NotBlank(message = "L'entité émettrice est obligatoire")
    @Size(max = 20)
    private String entiteEmettrice;

    @NotBlank(message = "L'entité bénéficiaire est obligatoire")
    @Size(max = 20)
    private String entiteBeneficiaire;

    @NotBlank(message = "Le type d'achat est obligatoire")
    @Size(max = 30)
    private String typeAchat;

    @NotBlank(message = "L'objet est obligatoire")
    @Size(max = 255)
    private String objet;

    private String justification;

    private UUID serviceDemandeurId;

    @NotNull(message = "L'année d'exercice est obligatoire")
    private Integer exerciceAnnee;

    private BigDecimal montantEstimeTtc;

    @NotEmpty(message = "La demande d'achat doit comporter au moins une ligne")
    @Valid
    private List<LigneDemandeAchatRequestDto> lignes;
}

