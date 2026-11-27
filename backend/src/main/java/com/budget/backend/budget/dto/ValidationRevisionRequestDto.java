package com.budget.backend.budget.dto;

import lombok.Data;

/**
 * Décision sur une demande de révision. Le valideur n'est pas transmis par le client :
 * c'est toujours l'utilisateur authentifié.
 */
@Data
public class ValidationRevisionRequestDto {
    private String statut;
    private String commentaire;
}
