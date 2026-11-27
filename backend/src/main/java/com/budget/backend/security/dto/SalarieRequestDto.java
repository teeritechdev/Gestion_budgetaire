package com.budget.backend.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.UUID;

public record SalarieRequestDto(
        @NotBlank @Size(max = 50) String matricule,
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 100) String prenom,
        @NotBlank @Pattern(regexp = "FEMME|HOMME", message = "Le sexe doit être HOMME ou FEMME.") String sexe,
        @Size(max = 30) String telephone,
        @Email @Size(max = 150) String email,
        @NotBlank(message = "Le poste est obligatoire.") @Size(max = 100) String poste,
        LocalDate dateEmbauche,
        @Past(message = "La date de naissance doit être antérieure à aujourd'hui.") LocalDate dateNaissance,
        @Size(max = 100) String lieuNaissance,
        @Pattern(regexp = "\\s*|B\\d{5,15}", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "Le numéro CNIB doit commencer par B suivi de chiffres (ex. B12345678).") String numeroCnib,
        UUID managerId,
        @NotNull Boolean actif
) {}
