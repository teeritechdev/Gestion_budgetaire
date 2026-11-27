package com.budget.backend.security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private UUID id;
    private String username;
    private String email;
    private String nom;
    private String prenom;
    private String role;
    private List<String> permissions;
    /** Mot de passe temporaire : l'utilisateur doit le changer avant d'accéder à l'application. */
    private boolean doitChangerMotDePasse;
    private boolean avecPhoto;
}