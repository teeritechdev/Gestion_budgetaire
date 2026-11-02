package com.budget.backend.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DirectionRequestDto(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 150) String libelle,
        /** Obligatoire pour une direction classique ; vide pour une direction CSP (transversale). */
        @Size(max = 20) String structureCode,
        @NotNull Boolean actif,
        /** Facultatif : non renseigné = direction non CSP. */
        Boolean estCsp
) {}
