package com.budget.backend.achat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreerBcDepuisDaDto {

    @NotNull(message = "Le fournisseur est obligatoire pour émettre un bon de commande")
    private UUID fournisseurId;

    private String typeBc;

    private BigDecimal tauxTva;
}

