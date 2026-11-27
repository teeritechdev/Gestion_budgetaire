package com.budget.backend.achat.dto;

import com.budget.backend.achat.entity.DossierVentilationImpact;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentilationImpactResponseDto {
    private UUID id;
    private UUID daId;
    private String entiteImpactee;
    private UUID activiteImpacteeId;
    private String activiteImpacteeCode;
    private String activiteImpacteeLibelle;
    private BigDecimal montantImpute;
    private BigDecimal pourcentageApplique;
    private Boolean statutBlocage;
    private LocalDateTime createdAt;

    public static VentilationImpactResponseDto from(DossierVentilationImpact v) {
        if (v == null) return null;
        return VentilationImpactResponseDto.builder()
                .id(v.getId())
                .daId(v.getDemandeAchat() != null ? v.getDemandeAchat().getId() : null)
                .entiteImpactee(v.getEntiteImpactee())
                .activiteImpacteeId(v.getActiviteImpactee() != null ? v.getActiviteImpactee().getId() : null)
                .activiteImpacteeCode(v.getActiviteImpactee() != null ? v.getActiviteImpactee().getCodeActivite() : null)
                .activiteImpacteeLibelle(v.getActiviteImpactee() != null ? v.getActiviteImpactee().getLibelle() : null)
                .montantImpute(v.getMontantImpute())
                .pourcentageApplique(v.getPourcentageApplique())
                .statutBlocage(v.getStatutBlocage())
                .createdAt(v.getCreatedAt())
                .build();
    }
}

