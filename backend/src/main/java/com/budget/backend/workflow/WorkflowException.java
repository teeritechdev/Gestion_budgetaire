package com.budget.backend.workflow;

/** Erreur métier du moteur de workflow (permission manquante, circuit terminé, définition invalide...). */
public class WorkflowException extends RuntimeException {
    public WorkflowException(String message) {
        super(message);
    }
}
