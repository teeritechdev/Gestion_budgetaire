package com.budget.backend.workflow.dto;

import com.budget.backend.workflow.entity.WorkflowDefinition;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
public class WorkflowDefinitionResponseDto {
    private UUID id;
    private String code;
    private String libelle;
    private Boolean actif;
    private List<WorkflowEtapeResponseDto> etapes;
    private LocalDateTime createdAt;

    public static WorkflowDefinitionResponseDto from(WorkflowDefinition d) {
        WorkflowDefinitionResponseDto dto = new WorkflowDefinitionResponseDto();
        dto.setId(d.getId());
        dto.setCode(d.getCode());
        dto.setLibelle(d.getLibelle());
        dto.setActif(d.getActif());
        dto.setEtapes(d.getEtapes().stream().map(WorkflowEtapeResponseDto::from).collect(Collectors.toList()));
        dto.setCreatedAt(d.getCreatedAt());
        return dto;
    }
}
