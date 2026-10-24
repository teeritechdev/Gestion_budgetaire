package com.budget.backend.abonnement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Ce que le responsable doit traiter : échéances proches et contrats dont la date limite de préavis est atteinte. */
@Data
public class AlertesResponseDto {
    private List<EcheanceResponseDto> echeancesProches;
    private List<PreavisAlerte> preavisAtteints;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreavisAlerte {
        private UUID abonnementId;
        private String codeAbonnement;
        private String libelle;
        private LocalDate dateFin;
        private LocalDate dateLimitePreavis;
        private long joursAvantFin;
        private Boolean taciteReconduction; // si vrai, sans action le contrat sera reconduit
        private UUID responsableId;
    }
}
