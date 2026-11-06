package com.budget.backend.security.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DirectionResponseDto(
        UUID id,
        String code,
        String libelle,
        String structureCode,
        String structureNom,
        UUID responsableId,
        Boolean actif,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
