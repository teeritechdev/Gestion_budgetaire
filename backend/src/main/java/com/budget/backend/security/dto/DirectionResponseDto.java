package com.budget.backend.security.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DirectionResponseDto(
        UUID id,
        String code,
        String libelle,
        String structureCode,
        String structureNom,
        java.util.UUID responsableId,
        Boolean actif,
        Boolean estCsp,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
