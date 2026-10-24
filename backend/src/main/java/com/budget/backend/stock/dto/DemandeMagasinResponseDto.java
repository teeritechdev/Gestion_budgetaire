package com.budget.backend.stock.dto;

import com.budget.backend.stock.entity.DemandeMagasin;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
public class DemandeMagasinResponseDto {
    private UUID id;
    private String numeroDemande;
    private UtilisateurSummaryDto demandeur;
    private UUID serviceId;
    private String entiteCode;
    private String objet;
    private String statut;
    private UtilisateurSummaryDto valideurN1;
    private LocalDateTime dateValidation;
    private UtilisateurSummaryDto magasinier;
    private LocalDateTime dateServie;
    private List<LigneDemandeMagasinResponseDto> lignes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DemandeMagasinResponseDto from(DemandeMagasin d) {
        DemandeMagasinResponseDto dto = new DemandeMagasinResponseDto();
        dto.setId(d.getId());
        dto.setNumeroDemande(d.getNumeroDemande());
        dto.setDemandeur(UtilisateurSummaryDto.from(d.getDemandeur()));
        dto.setServiceId(d.getService().getId());
        dto.setEntiteCode(d.getEntite().getCode());
        dto.setObjet(d.getObjet());
        dto.setStatut(d.getStatut());
        dto.setValideurN1(UtilisateurSummaryDto.from(d.getValideurN1()));
        dto.setDateValidation(d.getDateValidation());
        dto.setMagasinier(UtilisateurSummaryDto.from(d.getMagasinier()));
        dto.setDateServie(d.getDateServie());
        dto.setLignes(d.getLignes().stream().map(LigneDemandeMagasinResponseDto::from).collect(Collectors.toList()));
        dto.setCreatedAt(d.getCreatedAt());
        dto.setUpdatedAt(d.getUpdatedAt());
        return dto;
    }
}
