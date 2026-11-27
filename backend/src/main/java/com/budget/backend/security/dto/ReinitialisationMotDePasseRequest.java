package com.budget.backend.security.dto;

import jakarta.validation.constraints.NotBlank;

/** Mot de passe oublié, étape 2 : nouveau mot de passe, avec le jeton reçu à l'étape 1. */
public record ReinitialisationMotDePasseRequest(
        @NotBlank(message = "Le lien de réinitialisation est invalide.") String jeton,
        @NotBlank(message = "Le nouveau mot de passe est obligatoire.") String nouveauMotDePasse
) {}
