package com.budget.backend.security.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Profil de l'utilisateur connecté (lecture seule : l'identité est gérée par les RH). */
public record ProfilDto(
        UUID id,
        String username,
        String nom,
        String prenom,
        String sexe,
        String email,
        String telephone,
        String matricule,
        String poste,
        LocalDate dateEmbauche,
        String serviceLibelle,
        String directionLibelle,
        String structureCode,
        String roleCode,
        String roleLibelle,
        LocalDateTime compteCreeLe,
        Instant motDePasseModifieLe,
        boolean doitChangerMotDePasse,
        boolean avecPhoto,
        Instant photoModifieeLe
) {}
