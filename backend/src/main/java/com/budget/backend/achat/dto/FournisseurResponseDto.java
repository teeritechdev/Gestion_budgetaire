package com.budget.backend.achat.dto;

import com.budget.backend.achat.entity.Fournisseur;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
        if (f == null) return null;
        return FournisseurResponseDto.builder()
                .id(f.getId())
                .codeFournisseur(f.getCodeFournisseur())
                .raisonSociale(f.getRaisonSociale())
                .ifu(f.getIfu())
                .email(f.getEmail())
                .telephone(f.getTelephone())
                .adresse(f.getAdresse())
                .actif(f.getActif())
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .build();
    }
}

