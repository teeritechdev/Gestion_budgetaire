package com.budget.backend.achat;

/** Erreur métier du Module 4 (statut invalide, montant incohérent, budget insuffisant...). */
public class AchatException extends RuntimeException {
    public AchatException(String message) {
        super(message);
    }
}
