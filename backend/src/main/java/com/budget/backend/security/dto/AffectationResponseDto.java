package com.budget.backend.security.dto;

import com.budget.backend.security.entity.NiveauAffectation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record AffectationResponseDto(
        UUID id,
        UUID salarieId,
        String salarieMatricule,
        String salarieNom,
        String salariePoste,
        NiveauAffectation niveau,
        String cibleId,
        String cibleCode,
        String cibleLibelle,
        /** Chemin hiérarchique lisible, ex. « UAB_VIE › DIR-FIN › SVC-COMPTA ». */
        String chemin,
        String structureCode,
        String structureNom,
        String fonction,
        LocalDate dateDebut,
        LocalDate dateFin,
        /** EN_COURS, A_VENIR ou TERMINEE (à la date du jour). */
        String statut,
        String commentaire,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
