package com.budget.backend.stock.dto;

import com.budget.backend.security.entity.Utilisateur;
import lombok.Data;

import java.util.UUID;

/** Résumé d'un utilisateur : n'expose jamais le hash du mot de passe. */
@Data
public class UtilisateurSummaryDto {
    private UUID id;
    private String matricule;
    private String nom;
    private String prenom;
    private String email;
    private String roleMetier;

    public static UtilisateurSummaryDto from(Utilisateur u) {
        if (u == null) return null;
        UtilisateurSummaryDto dto = new UtilisateurSummaryDto();
        dto.setId(u.getId());
        dto.setMatricule(u.getMatricule());
        dto.setNom(u.getNom());
        dto.setPrenom(u.getPrenom());
        dto.setEmail(u.getEmail());
        dto.setRoleMetier(u.getRoleMetier());
        return dto;
    }
}
