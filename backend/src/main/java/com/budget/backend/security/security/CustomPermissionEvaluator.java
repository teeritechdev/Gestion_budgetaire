package com.budget.backend.security.security;

import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return false;
        }

        User userDetails = (User) authentication.getPrincipal();
        String email = userDetails.getUsername();

        // Récupérer l'utilisateur et son rôle en base de données
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email).orElse(null);
        if (utilisateur == null || utilisateur.getRole() == null || utilisateur.getRole().getPermissions() == null) {
            return false;
        }

        String permissionDemandee = permission.toString().toUpperCase();

        // Vérifier si le rôle de l'utilisateur possède cette permission exacte dans la table role_permissions
        return utilisateur.getRole().getPermissions().stream()
                .anyMatch(p -> p.getCode().equalsIgnoreCase(permissionDemandee));
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return hasPermission(authentication, null, permission);
    }
}