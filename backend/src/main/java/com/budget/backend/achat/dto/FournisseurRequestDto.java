package com.budget.backend.achat.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FournisseurRequestDto {
    // Optionnel : généré au format FOURN-AAAA-NNNN s'il est absent
    private String codeFournisseur;
    @NotBlank
    private String raisonSociale;
    @NotBlank
    private String ifu;
    @Email
    private String email;
    private String telephone;
    private String adresse;
    private Boolean actif;
}
