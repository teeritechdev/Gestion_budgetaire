package com.budget.backend.workflow.api;

import com.budget.backend.security.entity.Utilisateur;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Publié (de façon synchrone, dans la même transaction) quand une décision est prise via l'API REST du moteur.
 * Le module propriétaire du dossier l'écoute pour appliquer ses effets métier (statut, historique, crédits...) ;
 * une exception levée par l'écouteur annule toute la transition.
 * Les appels directs à prochaineEtape() ne publient pas d'événement : l'appelant gère déjà ses effets.
 */
public record WorkflowTransitionEvent(String workflowCode, UUID dossierId, String etapeAvant, boolean approuve,
                                      WorkflowEngineApi.EtatWorkflow etatApres, BigDecimal montant,
                                      Utilisateur utilisateur, String commentaire) {
}
