package com.budget.backend.workflow.api;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Permet au moteur de connaître le montant d'un dossier quand il est appelé directement par l'API REST
 * (POST /workflows/instances/execute), sans passer par le module métier. Chaque module qui possède un
 * workflow (DA, PV, BAP...) fournit son implémentation ; le moteur n'a aucune dépendance sur les tables métier.
 */
public interface DossierMontantProvider {

    boolean supporte(String workflowCode);

    Optional<BigDecimal> montant(UUID dossierId);
}
