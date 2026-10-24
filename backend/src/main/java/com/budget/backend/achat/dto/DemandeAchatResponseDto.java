package com.budget.backend.achat.dto;

import com.budget.backend.achat.entity.DemandeAchat;
import com.budget.backend.stock.dto.UtilisateurSummaryDto;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
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
    private UUID directionDemandeurId;
    private Integer exerciceAnnee;
    private List<LigneDAResponseDto> lignes;
    private List<DossierVentilationResponseDto> ventilations;
    private List<HistoriqueValidationResponseDto> historique;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DemandeAchatResponseDto from(DemandeAchat d) {
        DemandeAchatResponseDto dto = new DemandeAchatResponseDto();
        dto.setId(d.getId());
        dto.setCodeDossier(d.getCodeDossier());
        dto.setEntiteEmettrice(d.getEntiteEmettrice().getCode());
        dto.setEntiteBeneficiaire(d.getEntiteBeneficiaire().getCode());
        dto.setTypeAchat(d.getTypeAchat());
        dto.setObjet(d.getObjet());
        dto.setJustification(d.getJustification());
        dto.setMontantEstimeHt(d.getMontantEstimeHt());
        dto.setMontantEstimeTtc(d.getMontantEstimeTtc());
        dto.setStatut(d.getStatut());
        dto.setEtapeCourante(d.getEtapeCourante());
        dto.setDemandeur(UtilisateurSummaryDto.from(d.getDemandeur()));
        dto.setDirectionDemandeurId(d.getDirectionDemandeur().getId());
        dto.setExerciceAnnee(d.getExerciceAnnee());
        dto.setLignes(d.getLignes().stream().map(LigneDAResponseDto::from).collect(Collectors.toList()));
        dto.setVentilations(d.getVentilations().stream().map(DossierVentilationResponseDto::from).collect(Collectors.toList()));
        dto.setHistorique(d.getHistorique().stream().map(HistoriqueValidationResponseDto::from).collect(Collectors.toList()));
        dto.setCreatedAt(d.getCreatedAt());
        dto.setUpdatedAt(d.getUpdatedAt());
        return dto;
    }
}
