package com.budget.backend.security.dto;

import jakarta.validation.Valid;

/** User payload plus a plain password that the backend hashes before persistence. */
public record UtilisateurMotDePasseRequest(@Valid UtilisateurRequestDto utilisateur, String motDePasse) {
}
