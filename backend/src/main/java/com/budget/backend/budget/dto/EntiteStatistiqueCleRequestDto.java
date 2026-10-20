package com.budget.backend.budget.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class EntiteStatistiqueCleRequestDto {
    private Integer anneeExercice;
    private Integer mois; // 1 à 12
    private String entiteCode;
    private Integer effectifAgents;
    private BigDecimal chiffreAffaires;
}