package com.budget.backend.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ServiceRequestDto(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 150) String libelle,
        @NotNull UUID directionId,
        @NotNull Boolean actif
) {}
