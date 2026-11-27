package com.budget.backend.stock;

/** Erreur métier du Module 3 (stock insuffisant, statut invalide, article introuvable...). */
public class StockException extends RuntimeException {
    public StockException(String message) {
        super(message);
    }
}
