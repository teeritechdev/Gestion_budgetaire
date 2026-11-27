package com.budget.backend.abonnement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RapprochementFactureDto {

    @NotBlank(message = "Le numéro ou la référence de la facture fournisseur est obligatoire")
    private String referenceFacture;

    @NotNull(message = "Le montant facturé TTC est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le montant facturé doit être supérieur à zéro")
    private BigDecimal montantFactureTtc;

    private LocalDate dateFacture;

    private String commentaire;
}

