package com.budget.backend.achat.dto;

import com.budget.backend.achat.entity.DemandeAchat;
import com.budget.backend.stock.dto.UtilisateurSummaryDto;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeAchatResponseDto {
    private UUID id;
    private String codeDossier;
    private String entiteEmettrice;
    private String entiteBeneficiaire;
    private String typeAchat;
    private String objet;
    private String justification;
    private BigDecimal montantEstimeHt;
    private BigDecimal montantEstimeTtc;
    private String statut;
    private String etapeCourante;
    private UtilisateurSummaryDto demandeur;
    private UUID serviceDemandeurId;
    private String serviceDemandeurCode;
    private String serviceDemandeurLibelle;
    private Integer exerciceAnnee;
    private List<LigneDemandeAchatResponseDto> lignes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DemandeAchatResponseDto from(DemandeAchat d) {
        if (d == null) return null;
        return DemandeAchatResponseDto.builder()
                .id(d.getId())
                .codeDossier(d.getCodeDossier())
                .entiteEmettrice(d.getEntiteEmettrice())
                .entiteBeneficiaire(d.getEntiteBeneficiaire())
                .typeAchat(d.getTypeAchat())
                .objet(d.getObjet())
                .justification(d.getJustification())
                .montantEstimeHt(d.getMontantEstimeHt())
                .montantEstimeTtc(d.getMontantEstimeTtc())
                .statut(d.getStatut())
                .etapeCourante(d.getEtapeCourante())
                .demandeur(UtilisateurSummaryDto.from(d.getDemandeur()))
                .serviceDemandeurId(d.getServiceDemandeur() != null ? d.getServiceDemandeur().getId() : null)
                .serviceDemandeurCode(d.getServiceDemandeur() != null ? d.getServiceDemandeur().getCode() : null)
                .serviceDemandeurLibelle(d.getServiceDemandeur() != null ? d.getServiceDemandeur().getLibelle() : null)
                .exerciceAnnee(d.getExerciceAnnee())
                .lignes(d.getLignes() != null ? d.getLignes().stream().map(LigneDemandeAchatResponseDto::from).collect(Collectors.toList()) : List.of())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}

