package com.budget.backend.achat.dto;

import com.budget.backend.achat.entity.BonCommande;
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
public class BonCommandeResponseDto {
    private UUID id;
    private String codeBc;
    private String typeBc;
    private UUID daId;
    private String daCodeDossier;
    private String daObjet;
    private FournisseurResponseDto fournisseur;
    private BigDecimal montantTotalHt;
    private BigDecimal montantTotalTtc;
    private BigDecimal montantFactureTtc;
    private BigDecimal soldeRestantTtc;
    private String statut;
    private LocalDate dateEmission;
    private List<LigneBonCommandeResponseDto> lignes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BonCommandeResponseDto from(BonCommande bc) {
        if (bc == null) return null;
        BigDecimal facture = bc.getMontantFactureTtc() != null ? bc.getMontantFactureTtc() : BigDecimal.ZERO;
        BigDecimal totalTtc = bc.getMontantTotalTtc() != null ? bc.getMontantTotalTtc() : BigDecimal.ZERO;
        BigDecimal solde = totalTtc.subtract(facture);

        return BonCommandeResponseDto.builder()
                .id(bc.getId())
                .codeBc(bc.getCodeBc())
                .typeBc(bc.getTypeBc())
                .daId(bc.getDemandeAchat() != null ? bc.getDemandeAchat().getId() : null)
                .daCodeDossier(bc.getDemandeAchat() != null ? bc.getDemandeAchat().getCodeDossier() : null)
                .daObjet(bc.getDemandeAchat() != null ? bc.getDemandeAchat().getObjet() : null)
                .fournisseur(FournisseurResponseDto.from(bc.getFournisseur()))
                .montantTotalHt(bc.getMontantTotalHt())
                .montantTotalTtc(bc.getMontantTotalTtc())
                .montantFactureTtc(facture)
                .soldeRestantTtc(solde)
                .statut(bc.getStatut())
                .dateEmission(bc.getDateEmission())
                .lignes(bc.getLignes() != null ? bc.getLignes().stream().map(LigneBonCommandeResponseDto::from).collect(Collectors.toList()) : List.of())
                .createdAt(bc.getCreatedAt())
                .updatedAt(bc.getUpdatedAt())
                .build();
    }
}

