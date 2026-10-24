package com.budget.backend.abonnement.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class AbonnementRequestDto {
    // Optionnel : généré au format ABN-AAAA-NNNN s'il est absent
    private String codeAbonnement;
    @NotBlank
    private String libelle;
    @NotNull
    private UUID fournisseurId;
    @NotNull
    private UUID activiteId; // ligne budgétaire (budget_allocation_structure) imputée
    @NotBlank
    private String periodicite; // MENSUEL, TRIMESTRIEL, SEMESTRIEL, ANNUEL
    @NotNull
    @Min(1)
    @Max(31)
    private Integer jourFacturation; // 31 = dernier jour du mois
    @NotNull
    @Positive
    private BigDecimal montantEcheanceHt;
    @NotNull
    @Positive
    private BigDecimal montantEcheanceTtc;
    @NotNull
    private LocalDate dateDebut;
    @NotNull
    private LocalDate dateFin;
    private Boolean taciteReconduction;
    @PositiveOrZero
    private Integer preavisJours;
    @NotNull
    private UUID responsableId;
    // Optionnel (par défaut ACTIF à la création). BROUILLON, ACTIF, SUSPENDU ou RESILIE.
    private String statut;
}
