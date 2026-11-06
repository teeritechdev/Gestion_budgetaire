package com.budget.backend.security.dto;

import com.budget.backend.security.entity.NiveauAffectation;

/** Affectation en cours d'un salarié (la plus récente), telle qu'affichée dans son dossier. */
public record AffectationResumeDto(
        NiveauAffectation niveau,
        String cibleCode,
        String cibleLibelle,
        String chemin,
        String structureCode,
        String directionLibelle,
        String serviceLibelle,
        String fonction
) {}
