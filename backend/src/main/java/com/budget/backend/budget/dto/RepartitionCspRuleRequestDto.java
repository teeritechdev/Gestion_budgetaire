package com.budget.backend.budget.dto;

import lombok.Data;
import com.budget.backend.budget.entity.RepartitionCspCleType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class RepartitionCspRuleRequestDto {
    @NotNull
    private UUID activiteCspId;
    @NotNull
    private UUID allocationStructureId;
    @NotBlank
    private String entiteBeneficiaire;
    @NotNull
    private UUID activiteBeneficiaireId;
    @NotNull
    private RepartitionCspCleType cleType;
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal valeurCle;
}