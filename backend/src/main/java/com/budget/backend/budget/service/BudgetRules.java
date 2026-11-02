package com.budget.backend.budget.service;

import com.budget.backend.budget.entity.ExerciceBudgetaire;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

/** Règles budgétaires partagées par les services de la cascade d'allocations. */
final class BudgetRules {

    static final String STATUT_EXERCICE_CLOTURE = "CLOTURE";

    private BudgetRules() {
    }

    static void verifierMontantPositif(BigDecimal montant, String libelle) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le " + libelle + " doit être strictement supérieur à zéro.");
        }
    }

    static void verifierMontantPositifOuNul(BigDecimal montant, String libelle) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le " + libelle + " doit être renseigné et positif ou nul.");
        }
    }

    /** Une fois l'exercice clôturé, plus aucune allocation ni exécution ne peut être modifiée. */
    static void verifierExerciceModifiable(ExerciceBudgetaire exercice) {
        if (exercice == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "L'allocation n'est rattachée à aucun exercice budgétaire.");
        }
        if (STATUT_EXERCICE_CLOTURE.equalsIgnoreCase(exercice.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "L'exercice " + exercice.getAnnee() + " est clôturé : les allocations ne peuvent plus être modifiées.");
        }
    }

    static BigDecimal valeur(BigDecimal montant) {
        return montant == null ? BigDecimal.ZERO : montant;
    }
}
