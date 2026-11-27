package com.budget.backend.workflow.dto;

import com.budget.backend.workflow.entity.WorkflowDefinition;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowDefinitionResponseDto {
    private UUID id;
    private String code;
    private String libelle;
    private String description;
    private String typeEntite;
    private Boolean actif;
    private Integer nombreEtapes;
    private List<WorkflowEtapeDto> etapes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WorkflowDefinitionResponseDto from(WorkflowDefinition def) {
        if (def == null) return null;
        var etapesDto = def.getEtapes() != null
                ? def.getEtapes().stream().map(WorkflowEtapeDto::from).toList()
                : List.<WorkflowEtapeDto>of();

        return WorkflowDefinitionResponseDto.builder()
                .id(def.getId())
                .code(def.getCode())
                .libelle(def.getLibelle())
                .description(def.getDescription())
                .typeEntite(def.getTypeEntite())
                .actif(def.getActif())
                .nombreEtapes(etapesDto.size())
                .etapes(etapesDto)
                .createdAt(def.getCreatedAt())
                .updatedAt(def.getUpdatedAt())
                .build();
    }
}

