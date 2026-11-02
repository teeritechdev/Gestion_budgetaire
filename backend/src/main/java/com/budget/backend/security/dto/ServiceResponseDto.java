package com.budget.backend.security.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ServiceResponseDto(
        UUID id,
        String code,
        String libelle,
        UUID directionId,
        String directionCode,
        String directionLibelle,
        String structureCode,
        UUID responsableId,
        Boolean actif,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
