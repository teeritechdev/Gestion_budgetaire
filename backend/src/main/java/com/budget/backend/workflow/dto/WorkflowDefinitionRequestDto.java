package com.budget.backend.workflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class WorkflowDefinitionRequestDto {
    @NotBlank
    private String code;
    @NotBlank
    private String libelle;
    private Boolean actif;
    @NotEmpty
    @Valid
    private List<WorkflowEtapeRequestDto> etapes;
}
