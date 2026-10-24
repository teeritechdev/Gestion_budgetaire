package com.budget.backend.workflow.dto;

import com.budget.backend.workflow.entity.WorkflowInstance;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class WorkflowInstanceResponseDto {
    private UUID id;
    private UUID workflowId;
    private String workflowCode;
    private UUID dossierId;
    private WorkflowEtapeResponseDto etapeCourante; // null une fois le circuit terminé
    private String statutActuel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WorkflowInstanceResponseDto from(WorkflowInstance i) {
        WorkflowInstanceResponseDto dto = new WorkflowInstanceResponseDto();
        dto.setId(i.getId());
        dto.setWorkflowId(i.getWorkflow().getId());
        dto.setWorkflowCode(i.getWorkflow().getCode());
        dto.setDossierId(i.getDossierId());
        dto.setEtapeCourante(WorkflowEtapeResponseDto.from(i.getEtapeCourante()));
        dto.setStatutActuel(i.getStatutActuel());
        dto.setCreatedAt(i.getCreatedAt());
        dto.setUpdatedAt(i.getUpdatedAt());
        return dto;
    }
}
