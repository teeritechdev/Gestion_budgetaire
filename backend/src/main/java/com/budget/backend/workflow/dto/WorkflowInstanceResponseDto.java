package com.budget.backend.workflow.dto;

import com.budget.backend.stock.dto.UtilisateurSummaryDto;
import com.budget.backend.workflow.entity.WorkflowInstance;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowInstanceResponseDto {
    private UUID id;
    private UUID workflowId;
    private String workflowCode;
    private String workflowLibelle;
    private UUID dossierId;
    private String typeEntite;
    private String referenceDossier;
    private String objet;
    private BigDecimal montantTotal;
    private UtilisateurSummaryDto demandeur;
    private WorkflowEtapeDto etapeCourante;
    private String statutActuel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WorkflowInstanceResponseDto from(WorkflowInstance inst) {
        if (inst == null) return null;
        return WorkflowInstanceResponseDto.builder()
                .id(inst.getId())
                .workflowId(inst.getWorkflow() != null ? inst.getWorkflow().getId() : null)
                .workflowCode(inst.getWorkflow() != null ? inst.getWorkflow().getCode() : null)
                .workflowLibelle(inst.getWorkflow() != null ? inst.getWorkflow().getLibelle() : null)
                .dossierId(inst.getDossierId())
                .typeEntite(inst.getTypeEntite())
                .referenceDossier(inst.getReferenceDossier())
                .objet(inst.getObjet())
                .montantTotal(inst.getMontantTotal())
                .demandeur(inst.getDemandeur() != null ? UtilisateurSummaryDto.from(inst.getDemandeur()) : null)
                .etapeCourante(WorkflowEtapeDto.from(inst.getEtapeCourante()))
                .statutActuel(inst.getStatutActuel())
                .createdAt(inst.getCreatedAt())
                .updatedAt(inst.getUpdatedAt())
                .build();
    }
}

