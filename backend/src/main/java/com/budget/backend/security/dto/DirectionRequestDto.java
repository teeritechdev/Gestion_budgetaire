package com.budget.backend.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DirectionRequestDto(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 150) String libelle,
        /** Code de la structure de rattachement. */
        @NotBlank @Size(max = 20) String structureCode,
        @NotNull Boolean actif
) {}
