package com.budget.backend.abonnement;

/** Erreur métier du Module 6 (contrat invalide, transition de statut interdite, échéance déjà traitée...). */
public class AbonnementException extends RuntimeException {
    public AbonnementException(String message) {
        super(message);
    }
}
