package com.budget.backend.achat.api;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * CONTRAT avec le Module 2 (Budget) : réservation de crédits sur une ligne budgétaire
 * (budget_allocation_structure). Le Module 4 ne dépend que de cette interface.
 *
 * Implémentation actuelle : BudgetReservationProvisoire (même package). Quand le développeur A fournit
 * la sienne, il lui suffit de l'annoter @Primary (ou de supprimer la version provisoire).
 *
 * Exigences : chaque méthode est atomique (verrou sur la ligne), écrit une ligne dans
 * journal_mouvements_budgetaires et lève une AchatException avec un message clair en cas d'échec.
 */
public interface BudgetReservationApi {

    String ENGAGEMENT_DA = "ENGAGEMENT_DA";
    String ENGAGEMENT_BC = "ENGAGEMENT_BC";
    String LIQUIDATION_FACTURE = "LIQUIDATION_FACTURE";

    /**
     * Vérifie montant_disponible >= montant puis augmente montant_engage de montant.
     * @param typeMouvement type du journal (ENGAGEMENT_DA, ENGAGEMENT_BC)
     * @throws com.budget.backend.achat.AchatException si le disponible est insuffisant ou la ligne inconnue
     */
    void engager(UUID allocationStructureId, BigDecimal montant, String typeMouvement,
                 String referenceDocument, Integer anneeExercice, UUID auteurId);

    /**
     * Diminue montant_engage de montant (rejet ou annulation d'un dossier, ajustement d'un BC).
     * @throws com.budget.backend.achat.AchatException si montant dépasse l'engagement actuel
     */
    void liberer(UUID allocationStructureId, BigDecimal montant, String typeMouvement,
                 String referenceDocument, Integer anneeExercice, UUID auteurId);

    /**
     * Liquidation d'une facture validée (Module 7, validation du Bon À Payer) : le montant facturé sort de
     * montant_engage et entre dans montant_liquide (MRD : « l'application diminue cette colonne du montant facturé
     * pour le basculer dans montant_liquide »). Le disponible ne change donc pas ; une écriture
     * LIQUIDATION_FACTURE est ajoutée au journal.
     * @throws com.budget.backend.achat.AchatException si montant dépasse l'engagement actuel de la ligne
     */
    void liquider(UUID allocationStructureId, BigDecimal montant, String referenceDocument,
                  Integer anneeExercice, UUID auteurId);

    /** Montant disponible actuel de la ligne : (vote + modifié) - (engagé + liquidé). */
    BigDecimal disponible(UUID allocationStructureId);
}
