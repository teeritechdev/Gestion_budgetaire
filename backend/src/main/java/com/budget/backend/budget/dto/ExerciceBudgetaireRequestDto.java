package com.budget.backend.budget.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class ExerciceBudgetaireRequestDto {
    private Integer annee;
    private UUID planId;
    private String statut;
    private BigDecimal montantTotalVote;
    private LocalDate dateOuverture;
    private LocalDate dateFermeture;
}