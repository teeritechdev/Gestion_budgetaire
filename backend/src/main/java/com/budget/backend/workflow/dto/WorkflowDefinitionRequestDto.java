package com.budget.backend.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowDefinitionRequestDto {

    @NotBlank(message = "Le code unique du workflow est obligatoire")
    private String code;

    @NotBlank(message = "Le libellé du workflow est obligatoire")
    private String libelle;

    private String description;

    @NotBlank(message = "Le type d'entité est obligatoire (ex: DEMANDE_ACHAT)")
    private String typeEntite;

    private Boolean actif;

    private List<WorkflowEtapeDto> etapes;
}

