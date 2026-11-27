package com.budget.backend.abonnement.dto;

import com.budget.backend.abonnement.entity.Abonnement;
import com.budget.backend.achat.dto.FournisseurResponseDto;
import com.budget.backend.stock.dto.UtilisateurSummaryDto;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbonnementResponseDto {
    private UUID id;
    private String codeAbonnement;
    private String libelle;
    private FournisseurResponseDto fournisseur;
    private UUID activiteId;
    private String activiteCode;
    private String activiteLibelle;
    private String periodicite;
    private Integer jourFacturation;
    private BigDecimal montantEcheanceHt;
    private BigDecimal montantEcheanceTtc;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Boolean taciteReconduction;
    private Integer preavisJours;
    private String statut;
    private UtilisateurSummaryDto responsable;
    private Integer nombreEcheances;
    private List<EcheanceAbonnementResponseDto> echeances;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AbonnementResponseDto from(Abonnement a) {
        if (a == null) return null;
        List<EcheanceAbonnementResponseDto> echeancesDto = (a.getEcheances() != null)
                ? a.getEcheances().stream().map(EcheanceAbonnementResponseDto::from).collect(Collectors.toList())
                : List.of();

        return AbonnementResponseDto.builder()
                .id(a.getId())
                .codeAbonnement(a.getCodeAbonnement())
                .libelle(a.getLibelle())
                .fournisseur(FournisseurResponseDto.from(a.getFournisseur()))
                .activiteId(a.getActivite() != null ? a.getActivite().getId() : null)
                .activiteCode(a.getActivite() != null ? a.getActivite().getCodeActivite() : null)
                .activiteLibelle(a.getActivite() != null ? a.getActivite().getLibelle() : null)
                .periodicite(a.getPeriodicite())
                .jourFacturation(a.getJourFacturation())
                .montantEcheanceHt(a.getMontantEcheanceHt())
                .montantEcheanceTtc(a.getMontantEcheanceTtc())
                .dateDebut(a.getDateDebut())
                .dateFin(a.getDateFin())
                .taciteReconduction(a.getTaciteReconduction())
                .preavisJours(a.getPreavisJours())
                .statut(a.getStatut())
                .responsable(UtilisateurSummaryDto.from(a.getResponsable()))
                .nombreEcheances(echeancesDto.size())
                .echeances(echeancesDto)
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}

