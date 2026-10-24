package com.budget.backend.abonnement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class EcheanceUpdateDto {
    @NotBlank
    private String statut; // BC_GENERE, FACTURE_RECUE, PAYE ou ANNULE
    private String referenceFacture; // obligatoire pour FACTURE_RECUE
    private UUID bcId;               // obligatoire pour BC_GENERE
    @Positive
    private BigDecimal montantFactureTtc; // obligatoire pour FACTURE_RECUE
}
