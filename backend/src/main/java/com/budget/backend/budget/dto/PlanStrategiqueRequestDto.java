package com.budget.backend.budget.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PlanStrategiqueRequestDto {
    private String codePlan;
    private String libelle;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Boolean actif;
}