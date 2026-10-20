package com.budget.backend.budget.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class RepartitionCspRuleRequestDto {
    private UUID activiteCspId;
    private String entiteBeneficiaire;
    private UUID activiteBeneficiaireId;
    private String cleType; // ex: EFFECTIF, CHIFFRE_AFFAIRES
    private BigDecimal valeurCle;
}