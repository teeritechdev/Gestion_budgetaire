package com.budget.backend.achat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FournisseurRequestDto {

    @Size(max = 50)
    private String codeFournisseur;

    @NotBlank(message = "La raison sociale est obligatoire")
    @Size(max = 200)
    private String raisonSociale;

    @NotBlank(message = "Le numéro IFU est obligatoire")
    @Size(max = 50)
    private String ifu;

    @Size(max = 150)
    private String email;

    @Size(max = 50)
    private String telephone;

    private String adresse;

    private Boolean actif;
}

