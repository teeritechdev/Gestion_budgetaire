package com.budget.backend.budget.dto;

import java.math.BigDecimal;

public record DashboardSummaryDto(
        boolean vueGlobale,
        String structureLibelle,
        String entiteNom,
        long nombreStructures,
        long nombreEntites,
        long nombreUtilisateurs,
        long nombreUtilisateursActifs,
        BigDecimal budgetAlloue,
        BigDecimal montantEngage,
        BigDecimal montantLiquide,
        long revisionsEnAttente,
        long nombreRoles,
        long nombrePermissions,
        long nombreJournauxAudit
) {
}
