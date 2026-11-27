package com.budget.backend.security.dto;

import com.budget.backend.security.entity.NiveauAffectation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record AffectationRequestDto(
        @NotNull(message = "Choisissez le salarié.") UUID salarieId,
        @NotNull(message = "Choisissez le niveau d'affectation.") NiveauAffectation niveau,
        /** Code de la structure, ou identifiant de la direction ou du service, selon le niveau. */
        @NotBlank(message = "Choisissez l'entité d'affectation.") @Size(max = 50) String cibleId,
        @Size(max = 100) String fonction,
        @NotNull(message = "La date de début est obligatoire.") LocalDate dateDebut,
        LocalDate dateFin,
        @Size(max = 500) String commentaire
) {}
