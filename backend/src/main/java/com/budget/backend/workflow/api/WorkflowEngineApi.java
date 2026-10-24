package com.budget.backend.workflow.api;

import com.budget.backend.security.entity.Utilisateur;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Interface du moteur de workflow appelée par les modules métier (PDF §3.2 : Module 4 / 5 / 7 -> Engine Workflow,
 * WorkflowEngineApi.prochaineEtape()) pour évaluer les seuils de montant et les privilèges requis.
 *
 * Le moteur conserve l'état de chaque dossier dans workflow_instances ; l'appelant n'a qu'à lui donner
 * le code du workflow, l'identifiant du dossier et son montant.
 */
public interface WorkflowEngineApi {

    /** Code du workflow des demandes d'achat (Module 4), créé au démarrage s'il n'existe pas. */
    String WORKFLOW_DA = "WORKFLOW_DA";

    String STATUT_EN_ATTENTE = "EN_ATTENTE_VALIDATION";
    String STATUT_APPROUVE = "APPROUVE";
    String STATUT_REJETE = "REJETE";
    String STATUT_ANNULE = "ANNULE";

    /**
     * Etat d'un dossier : code de l'étape où il se trouve (null si le circuit est terminé) et statut.
     * REJETE avec une étape non nulle : le dossier est dans le circuit d'exception (ex. annulation / libération
     * des crédits) ; REJETE sans étape ou ANNULE : fin de circuit sur un échec ; APPROUVE : fin de circuit réussie.
     */
    record EtatWorkflow(String etapeCourante, String statut) {
        public boolean termine() {
            return etapeCourante == null;
        }
    }

    /** Crée le suivi du dossier sur sa première étape applicable au montant. */
    EtatWorkflow demarrer(String workflowCode, UUID dossierId, BigDecimal montant);

    /**
     * Applique la décision (approuver ou rejeter) de l'utilisateur sur l'étape courante du dossier et renvoie
     * l'état suivant : étape suivante en cas de succès (les étapes dont la plage de montants ne couvre pas le dossier
     * sont sautées) ou étape de rejet.
     * @throws com.budget.backend.workflow.WorkflowException si l'utilisateur n'a pas la permission requise par l'étape,
     *         si le dossier est inconnu ou si son circuit est terminé
     */
    EtatWorkflow prochaineEtape(String workflowCode, UUID dossierId, BigDecimal montant, boolean approuve, Utilisateur utilisateur);
}
