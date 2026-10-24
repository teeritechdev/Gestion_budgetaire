package com.budget.backend.achat.dto;

import com.budget.backend.achat.entity.Fournisseur;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class FournisseurResponseDto {
    private UUID id;
    private String codeFournisseur;
    private String raisonSociale;
    private String ifu;
    private String email;
    private String telephone;
    private String adresse;
    private Boolean actif;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FournisseurResponseDto from(Fournisseur f) {
        FournisseurResponseDto dto = new FournisseurResponseDto();
        dto.setId(f.getId());
        dto.setCodeFournisseur(f.getCodeFournisseur());
        dto.setRaisonSociale(f.getRaisonSociale());
        dto.setIfu(f.getIfu());
        dto.setEmail(f.getEmail());
        dto.setTelephone(f.getTelephone());
        dto.setAdresse(f.getAdresse());
        dto.setActif(f.getActif());
        dto.setCreatedAt(f.getCreatedAt());
        dto.setUpdatedAt(f.getUpdatedAt());
        return dto;
    }
}
