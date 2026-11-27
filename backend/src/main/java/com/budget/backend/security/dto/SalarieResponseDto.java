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
        LocalDate dateNaissance,
        String lieuNaissance,
        String numeroCnib,
        /** Affectation en cours la plus récente (null si le salarié n'est affecté nulle part). */
        AffectationResumeDto affectation,
        UUID managerId,
        Boolean actif,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
