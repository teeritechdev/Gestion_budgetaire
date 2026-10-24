package com.budget.backend.achat.dto;

import com.budget.backend.achat.entity.DossierVentilationImpact;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class DossierVentilationResponseDto {
    private UUID id;
    private UUID daId;
    private String entiteImpactee;
    private UUID activiteImpacteeId;
    private BigDecimal montantImpute;
    private BigDecimal pourcentageApplique;
    private Boolean statutBlocage;

    public static DossierVentilationResponseDto from(DossierVentilationImpact v) {
        DossierVentilationResponseDto dto = new DossierVentilationResponseDto();
        dto.setId(v.getId());
        dto.setDaId(v.getDemandeAchat().getId());
        dto.setEntiteImpactee(v.getEntiteImpactee().getCode());
        dto.setActiviteImpacteeId(v.getActiviteImpactee() != null ? v.getActiviteImpactee().getId() : null);
        dto.setMontantImpute(v.getMontantImpute());
        dto.setPourcentageApplique(v.getPourcentageApplique());
        dto.setStatutBlocage(v.getStatutBlocage());
        return dto;
    }
}
