package com.budget.backend.security.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangementMotDePasseRequest(
        @NotBlank(message = "Le mot de passe actuel est obligatoire.") String motDePasseActuel,
        @NotBlank(message = "Le nouveau mot de passe est obligatoire.") String nouveauMotDePasse
) {}
