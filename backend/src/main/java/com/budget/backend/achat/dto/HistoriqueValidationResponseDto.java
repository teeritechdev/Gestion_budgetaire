package com.budget.backend.achat.dto;

import com.budget.backend.achat.entity.HistoriqueValidation;
import com.budget.backend.stock.dto.UtilisateurSummaryDto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoriqueValidationResponseDto {
    private UUID id;
    private UUID daId;
    private UUID instanceId;
    private String etape;
    private UtilisateurSummaryDto valideur;
    private String decision;
    private String commentaire;
    private LocalDateTime dateDecision;

    public static HistoriqueValidationResponseDto from(HistoriqueValidation h) {
        if (h == null) return null;
        return HistoriqueValidationResponseDto.builder()
                .id(h.getId())
                .daId(h.getDemandeAchat() != null ? h.getDemandeAchat().getId() : null)
                .instanceId(h.getInstanceId())
                .etape(h.getEtape())
                .valideur(UtilisateurSummaryDto.from(h.getValideur()))
                .decision(h.getDecision())
                .commentaire(h.getCommentaire())
                .dateDecision(h.getDateDecision())
                .build();
    }
}

