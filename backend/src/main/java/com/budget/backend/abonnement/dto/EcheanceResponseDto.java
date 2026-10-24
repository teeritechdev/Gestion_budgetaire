package com.budget.backend.abonnement.dto;

import com.budget.backend.abonnement.entity.EcheanceAbonnement;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class EcheanceResponseDto {
    private UUID id;
    private UUID abonnementId;
    private String codeAbonnement;
    private Integer numEcheance;
    private LocalDate dateEcheance;
    private BigDecimal montantAttenduTtc;
    private BigDecimal montantFactureTtc;
    private BigDecimal ecartTtc; // facturé - attendu ; repère les surconsommations
    private String statut;
    private UUID bcId;
    private String referenceFacture;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EcheanceResponseDto from(EcheanceAbonnement e) {
        EcheanceResponseDto dto = new EcheanceResponseDto();
        dto.setId(e.getId());
        dto.setAbonnementId(e.getAbonnement().getId());
        dto.setCodeAbonnement(e.getAbonnement().getCodeAbonnement());
        dto.setNumEcheance(e.getNumEcheance());
        dto.setDateEcheance(e.getDateEcheance());
        dto.setMontantAttenduTtc(e.getMontantAttenduTtc());
        dto.setMontantFactureTtc(e.getMontantFactureTtc());
        dto.setEcartTtc(e.ecartTtc());
        dto.setStatut(e.getStatut());
        dto.setBcId(e.getBonCommande() != null ? e.getBonCommande().getId() : null);
        dto.setReferenceFacture(e.getReferenceFacture());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        return dto;
    }
}
