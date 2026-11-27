package com.budget.backend.abonnement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbonnementRequestDto {

    private String codeAbonnement;

    @NotBlank(message = "L'intitulé du contrat est obligatoire")
    private String libelle;

    @NotNull(message = "Le prestataire (fournisseur) est obligatoire")
    private UUID fournisseurId;

    private UUID activiteId;

    @NotBlank(message = "La périodicité est obligatoire (MENSUEL, TRIMESTRIEL, SEMESTRIEL, ANNUEL)")
    private String periodicite;

    private Integer jourFacturation;

    @NotNull(message = "Le montant par échéance HT est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le montant HT doit être supérieur à zéro")
    private BigDecimal montantEcheanceHt;

    private BigDecimal montantEcheanceTtc;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate dateFin;

    private Boolean taciteReconduction;

    private Integer preavisJours;

    private String statut;

    private UUID responsableId;
}
