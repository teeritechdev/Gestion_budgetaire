package com.budget.backend.achat.dto;

import com.budget.backend.achat.entity.HistoriqueValidation;
import com.budget.backend.stock.dto.UtilisateurSummaryDto;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class HistoriqueValidationResponseDto {
    private UUID id;
    private UUID daId;
    private String etape;
    private UtilisateurSummaryDto valideur;
    private String decision;
    private String commentaire;
    private LocalDateTime dateDecision;

    public static HistoriqueValidationResponseDto from(HistoriqueValidation h) {
        HistoriqueValidationResponseDto dto = new HistoriqueValidationResponseDto();
        dto.setId(h.getId());
        dto.setDaId(h.getDemandeAchat().getId());
        dto.setEtape(h.getEtape());
        dto.setValideur(UtilisateurSummaryDto.from(h.getValideur()));
        dto.setDecision(h.getDecision());
        dto.setCommentaire(h.getCommentaire());
        dto.setDateDecision(h.getDateDecision());
        return dto;
    }
}
