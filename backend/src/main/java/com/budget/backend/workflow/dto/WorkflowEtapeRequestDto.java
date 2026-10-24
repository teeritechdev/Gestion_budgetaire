package com.budget.backend.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class WorkflowEtapeRequestDto {
    @NotNull
    private Integer numEtape;
    @NotBlank
    private String codeEtape;
    @NotBlank
    private String libelle;
    @NotBlank
    private String permissionRequise;
    private BigDecimal montantMinimum;
    private BigDecimal montantMaximum;

    // Contrat du PDF : identifiants d'étapes DÉJÀ existantes. Une étape qu'on est en train de créer n'a pas encore
    // d'identifiant : pour relier les étapes d'une même définition, utiliser les champs *Code ci-dessous.
    private UUID etapeSuivanteSuccesId;
    private UUID etapeSuivanteRejetId;

    // Ajout : renvoi vers une autre étape de la même requête, par son codeEtape.
    private String etapeSuivanteSuccesCode;
    private String etapeSuivanteRejetCode;
}
