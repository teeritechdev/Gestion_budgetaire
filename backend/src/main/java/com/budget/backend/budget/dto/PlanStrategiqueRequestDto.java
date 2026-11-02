package com.budget.backend.budget.dto;

import lombok.Data;
import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class PlanStrategiqueRequestDto {
    @NotBlank(message = "Le code du plan est obligatoire.")
    private String codePlan;
    @NotBlank(message = "Le libellé du plan est obligatoire.")
    private String libelle;
    @NotNull(message = "La date de début est obligatoire.")
    private LocalDate dateDebut;
    @NotNull(message = "La date de fin est obligatoire.")
    private LocalDate dateFin;
    private Boolean actif;
}