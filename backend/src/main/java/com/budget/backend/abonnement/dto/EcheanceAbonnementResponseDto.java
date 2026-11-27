package com.budget.backend.abonnement.dto;

import com.budget.backend.abonnement.entity.EcheanceAbonnement;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcheanceAbonnementResponseDto {
    private UUID id;
    private UUID abonnementId;
    private Integer numEcheance;
    private LocalDate dateEcheance;
    private BigDecimal montantAttenduTtc;
    private BigDecimal montantFactureTtc;
    private String statut;
    private UUID bcId;
    private String bcCode;
    private String referenceFacture;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EcheanceAbonnementResponseDto from(EcheanceAbonnement e) {
        if (e == null) return null;
        return EcheanceAbonnementResponseDto.builder()
                .id(e.getId())
                .abonnementId(e.getAbonnement() != null ? e.getAbonnement().getId() : null)
                .numEcheance(e.getNumEcheance())
                .dateEcheance(e.getDateEcheance())
                .montantAttenduTtc(e.getMontantAttenduTtc())
                .montantFactureTtc(e.getMontantFactureTtc())
                .statut(e.getStatut())
                .bcId(e.getBonCommande() != null ? e.getBonCommande().getId() : null)
                .bcCode(e.getBonCommande() != null ? e.getBonCommande().getCodeBc() : null)
                .referenceFacture(e.getReferenceFacture())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}

