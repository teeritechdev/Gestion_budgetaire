package com.budget.backend.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record UtilisateurRequestDto(
        @NotBlank @Size(max = 150) String username,
        UUID salarieId,
        @Size(max = 50) String matricule,
        @Size(max = 100) String nom,
        @Size(max = 100) String prenom,
        String sexe,
        @Size(max = 30) String telephone,
        @Email @Size(max = 150) String email,
        @Size(max = 100) String poste,
        LocalDate dateEmbauche,
        LocalDate dateNaissance,
        @Size(max = 100) String lieuNaissance,
        @Size(max = 20) String numeroCnib,
        UUID serviceId,
        UUID managerId,
        @NotNull UUID roleId,
        @NotNull Boolean actif
) {}
