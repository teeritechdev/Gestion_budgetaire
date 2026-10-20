package com.budget.backend.budget.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class DemandeRevisionRequestDto {
    private String numeroDemande;
    private Integer anneeExercice;
    private UUID demandeurId;
    private String motif;
    private List<LigneRevisionRequestDto> lignes;
}