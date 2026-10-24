package com.budget.backend.achat.dto;

import com.budget.backend.achat.entity.BonCommande;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
public class BonCommandeResponseDto {
    private UUID id;
    private String codeBc;
    private String typeBc;
    private UUID daId;
    private String codeDossier;
    private FournisseurResponseDto fournisseur;
    private BigDecimal montantTotalHt;
    private BigDecimal montantTotalTtc;
    private BigDecimal montantFactureTtc;
    private BigDecimal soldeRestantTtc;
    private String statut;
    private LocalDate dateEmission;
    private List<LigneBCResponseDto> lignes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BonCommandeResponseDto from(BonCommande b) {
        BonCommandeResponseDto dto = new BonCommandeResponseDto();
        dto.setId(b.getId());
        dto.setCodeBc(b.getCodeBc());
        dto.setTypeBc(b.getTypeBc());
        dto.setDaId(b.getDemandeAchat().getId());
        dto.setCodeDossier(b.getDemandeAchat().getCodeDossier());
        dto.setFournisseur(FournisseurResponseDto.from(b.getFournisseur()));
        dto.setMontantTotalHt(b.getMontantTotalHt());
        dto.setMontantTotalTtc(b.getMontantTotalTtc());
        dto.setMontantFactureTtc(b.getMontantFactureTtc());
        dto.setSoldeRestantTtc(b.calculerSolde());
        dto.setStatut(b.getStatut());
        dto.setDateEmission(b.getDateEmission());
        dto.setLignes(b.getLignes().stream().map(LigneBCResponseDto::from).collect(Collectors.toList()));
        dto.setCreatedAt(b.getCreatedAt());
        dto.setUpdatedAt(b.getUpdatedAt());
        return dto;
    }
}
