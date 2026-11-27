package com.budget.backend.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Mot de passe oublié, étape 1 : la personne prouve son identité avec ses données RH. */
public record VerificationIdentiteRequest(
        @NotBlank(message = "L'identifiant est obligatoire.") String identifiant,
        @NotBlank(message = "Le matricule est obligatoire.") String matricule,
        @NotBlank(message = "Le numéro CNIB est obligatoire.") String numeroCnib,
        @NotNull(message = "La date de naissance est obligatoire.") LocalDate dateNaissance
) {}
