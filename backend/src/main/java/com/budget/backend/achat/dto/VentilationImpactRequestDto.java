package com.budget.backend.achat.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentilationImpactRequestDto {

    @NotBlank(message = "L'entité impactée est obligatoire (ex: UAB_VIE, UAB_IARDT)")
    private String entiteImpactee;

    private UUID activiteImpacteeId;

    @NotNull(message = "Le montant imputé est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le montant imputé doit être strictement positif")
    private BigDecimal montantImpute;

    @NotNull(message = "Le pourcentage appliqué est obligatoire")
    @DecimalMin(value = "0.01", message = "Le pourcentage doit être supérieur à 0")
    private BigDecimal pourcentageApplique;

    private Boolean statutBlocage;
}

