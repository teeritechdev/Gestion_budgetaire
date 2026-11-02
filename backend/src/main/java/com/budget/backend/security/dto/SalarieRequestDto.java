package com.budget.backend.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.UUID;

public record SalarieRequestDto(
        @NotBlank @Size(max = 50) String matricule,
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 100) String prenom,
        @NotBlank @Pattern(regexp = "FEMME|HOMME|AUTRE|NON_RENSEIGNE") String sexe,
        @Size(max = 30) String telephone,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(max = 100) String poste,
        @NotNull LocalDate dateEmbauche,
        @NotNull UUID serviceId,
        UUID managerId,
        @NotNull Boolean actif
) {}
