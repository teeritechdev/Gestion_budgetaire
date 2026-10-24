package com.budget.backend.abonnement.dto;

import com.budget.backend.abonnement.entity.Abonnement;
import com.budget.backend.achat.dto.FournisseurResponseDto;
import com.budget.backend.stock.dto.UtilisateurSummaryDto;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AbonnementResponseDto {
    private UUID id;
    private String codeAbonnement;
    private String libelle;
    private FournisseurResponseDto fournisseur;
    private UUID activiteId;
    private String periodicite;
    private Integer jourFacturation;
    private BigDecimal montantEcheanceHt;
    private BigDecimal montantEcheanceTtc;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Boolean taciteReconduction;
    private Integer preavisJours;
    private LocalDate dateLimitePreavis;
    private String statut;
    private UtilisateurSummaryDto responsable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AbonnementResponseDto from(Abonnement a) {
        AbonnementResponseDto dto = new AbonnementResponseDto();
        dto.setId(a.getId());
        dto.setCodeAbonnement(a.getCodeAbonnement());
        dto.setLibelle(a.getLibelle());
        dto.setFournisseur(FournisseurResponseDto.from(a.getFournisseur()));
        dto.setActiviteId(a.getActivite().getId());
        dto.setPeriodicite(a.getPeriodicite());
        dto.setJourFacturation(a.getJourFacturation());
        dto.setMontantEcheanceHt(a.getMontantEcheanceHt());
        dto.setMontantEcheanceTtc(a.getMontantEcheanceTtc());
        dto.setDateDebut(a.getDateDebut());
        dto.setDateFin(a.getDateFin());
        dto.setTaciteReconduction(a.getTaciteReconduction());
        dto.setPreavisJours(a.getPreavisJours());
        dto.setDateLimitePreavis(a.dateLimitePreavis());
        dto.setStatut(a.getStatut());
        dto.setResponsable(UtilisateurSummaryDto.from(a.getResponsable()));
        dto.setCreatedAt(a.getCreatedAt());
        dto.setUpdatedAt(a.getUpdatedAt());
        return dto;
    }
}
