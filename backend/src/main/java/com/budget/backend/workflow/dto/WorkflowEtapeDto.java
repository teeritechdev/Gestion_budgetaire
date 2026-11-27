package com.budget.backend.workflow.dto;

import com.budget.backend.workflow.entity.WorkflowEtape;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowEtapeDto {
    private UUID id;
    private UUID workflowId;
    private Integer numEtape;
    private String codeEtape;
    private String libelle;
    private String permissionRequise;
    private String roleRequis;
    private BigDecimal montantMinimum;
    private BigDecimal montantMaximum;
    private UUID etapeSuivanteSuccesId;
    private String etapeSuivanteSuccesCode;
    private UUID etapeSuivanteRejetId;
    private String etapeSuivanteRejetCode;
    private Boolean estEtapeFinale;
    private Boolean estEtapeRejet;

    public static WorkflowEtapeDto from(WorkflowEtape e) {
        if (e == null) return null;
        return WorkflowEtapeDto.builder()
                .id(e.getId())
                .workflowId(e.getWorkflow() != null ? e.getWorkflow().getId() : null)
                .numEtape(e.getNumEtape())
                .codeEtape(e.getCodeEtape())
                .libelle(e.getLibelle())
                .permissionRequise(e.getPermissionRequise())
                .roleRequis(e.getRoleRequis())
                .montantMinimum(e.getMontantMinimum())
                .montantMaximum(e.getMontantMaximum())
                .etapeSuivanteSuccesId(e.getEtapeSuivanteSucces() != null ? e.getEtapeSuivanteSucces().getId() : null)
                .etapeSuivanteSuccesCode(e.getEtapeSuivanteSucces() != null ? e.getEtapeSuivanteSucces().getCodeEtape() : null)
                .etapeSuivanteRejetId(e.getEtapeSuivanteRejet() != null ? e.getEtapeSuivanteRejet().getId() : null)
                .etapeSuivanteRejetCode(e.getEtapeSuivanteRejet() != null ? e.getEtapeSuivanteRejet().getCodeEtape() : null)
                .estEtapeFinale(e.getEstEtapeFinale())
                .estEtapeRejet(e.getEstEtapeRejet())
                .build();
    }
}

