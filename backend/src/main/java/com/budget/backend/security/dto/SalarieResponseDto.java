package com.budget.backend.security.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record SalarieResponseDto(
        UUID id,
        String matricule,
        String nom,
        String prenom,
        String sexe,
        String telephone,
        String email,
        String poste,
        LocalDate dateEmbauche,
        ServiceResponseDto service,
        UUID managerId,
        Boolean actif,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
