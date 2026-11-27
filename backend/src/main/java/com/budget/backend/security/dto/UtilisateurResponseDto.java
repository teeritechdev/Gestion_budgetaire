package com.budget.backend.security.dto;

import com.budget.backend.security.entity.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record UtilisateurResponseDto(
        UUID id,
        String username,
        UUID salarieId,
        String matricule,
        String nom,
        String prenom,
        String sexe,
        String telephone,
        String email,
        String poste,
        LocalDate dateEmbauche,
        AffectationResumeDto affectation,
        UUID managerId,
        Role role,
        Boolean actif,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
