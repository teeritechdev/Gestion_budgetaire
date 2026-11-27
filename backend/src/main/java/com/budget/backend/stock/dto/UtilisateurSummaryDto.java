package com.budget.backend.stock.dto;

import com.budget.backend.security.entity.Utilisateur;
import lombok.Data;

import java.util.UUID;

/** Résumé d'un utilisateur : n'expose jamais le hash du mot de passe. */
@Data
public class UtilisateurSummaryDto {
    private UUID id;
    private String username;
    private String matricule;
    private String nom;
    private String prenom;
    private String email;
    private String poste;

    public static UtilisateurSummaryDto from(Utilisateur u) {
        if (u == null) return null;
        UtilisateurSummaryDto dto = new UtilisateurSummaryDto();
        dto.setId(u.getId());
        dto.setUsername(u.getUsername());
        if (u.getSalarie() != null) {
            dto.setMatricule(u.getSalarie().getMatricule());
            dto.setNom(u.getSalarie().getNom());
            dto.setPrenom(u.getSalarie().getPrenom());
            dto.setEmail(u.getSalarie().getEmail());
            dto.setPoste(u.getSalarie().getPoste());
        }
        return dto;
    }
}
