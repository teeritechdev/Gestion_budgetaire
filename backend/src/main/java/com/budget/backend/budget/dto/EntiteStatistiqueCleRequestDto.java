package com.budget.backend.budget.dto;

import lombok.Data;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Data
public class EntiteStatistiqueCleRequestDto {
    @NotNull(message = "L’année est obligatoire.")
    @Min(value = 2000, message = "L’année doit être au moins 2000.")
    @Max(value = 2200, message = "L’année ne peut pas dépasser 2200.")
    private Integer anneeExercice;

    @Min(value = 1, message = "Le mois doit être compris entre 1 et 12.")
    @Max(value = 12, message = "Le mois doit être compris entre 1 et 12.")
    private Integer mois; // 1 à 12

    @NotBlank(message = "Sélectionnez une entité.")
    @Size(max = 50, message = "Le code de l’entité ne peut pas dépasser 50 caractères.")
    private String entiteCode;

    @Min(value = 0, message = "L’effectif ne peut pas être négatif.")
    private Integer effectifAgents;

    @DecimalMin(value = "0.00", message = "Le chiffre d’affaires ne peut pas être négatif.")
    @Digits(integer = 13, fraction = 2, message = "Le chiffre d’affaires accepte au maximum 13 chiffres et 2 décimales.")
    private BigDecimal chiffreAffaires;
}