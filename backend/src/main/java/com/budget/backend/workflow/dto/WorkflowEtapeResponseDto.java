package com.budget.backend.workflow.dto;

import com.budget.backend.workflow.entity.WorkflowEtape;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class WorkflowEtapeResponseDto {
    private UUID id;
    private Integer numEtape;
    private String codeEtape;
    private String libelle;
    private String permissionRequise;
    private BigDecimal montantMinimum;
    private BigDecimal montantMaximum;
    private UUID etapeSuivanteSuccesId;
    private UUID etapeSuivanteRejetId;

    public static WorkflowEtapeResponseDto from(WorkflowEtape e) {
        if (e == null) return null;
        WorkflowEtapeResponseDto dto = new WorkflowEtapeResponseDto();
        dto.setId(e.getId());
        dto.setNumEtape(e.getNumEtape());
        dto.setCodeEtape(e.getCodeEtape());
        dto.setLibelle(e.getLibelle());
        dto.setPermissionRequise(e.getPermissionRequise());
        dto.setMontantMinimum(e.getMontantMinimum());
        dto.setMontantMaximum(e.getMontantMaximum());
        dto.setEtapeSuivanteSuccesId(e.getEtapeSuivanteSucces() != null ? e.getEtapeSuivanteSucces().getId() : null);
        dto.setEtapeSuivanteRejetId(e.getEtapeSuivanteRejet() != null ? e.getEtapeSuivanteRejet().getId() : null);
        return dto;
    }
}
