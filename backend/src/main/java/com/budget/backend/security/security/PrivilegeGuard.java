package com.budget.backend.security.security;

import com.budget.backend.security.entity.Permission;
import com.budget.backend.security.entity.Role;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.UtilisateurRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Empêche l'élévation de privilèges : un utilisateur ne peut attribuer, créer ou modifier
 * qu'un rôle dont il détient lui-même toutes les permissions. Le rôle ADMIN_SYSTEME
 * n'est gérable que par un administrateur système.
 */
@Component
public class PrivilegeGuard {

    public static final String ROLE_ADMIN_SYSTEME = "ADMIN_SYSTEME";

    private final UtilisateurRepository utilisateurRepository;

    public PrivilegeGuard(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    /** Vérifie que l'utilisateur connecté peut attribuer ce rôle ou agir sur un compte qui le possède. */
    public void verifierPeutGererRole(Role role) {
        Utilisateur acteur = acteurCourant();
        if (acteur == null || estAdministrateur(acteur) || role == null) {
            return;
        }
        if (ROLE_ADMIN_SYSTEME.equalsIgnoreCase(role.getCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seul un administrateur système peut attribuer ou gérer le rôle ADMIN_SYSTEME.");
        }
        verifierPermissionsDetenues(role.getPermissions());
    }

    /** Vérifie que l'utilisateur connecté détient toutes les permissions qu'il veut accorder. */
    public void verifierPermissionsDetenues(Collection<Permission> permissions) {
        Utilisateur acteur = acteurCourant();
        if (acteur == null || estAdministrateur(acteur) || permissions == null || permissions.isEmpty()) {
            return;
        }
        Set<String> detenues = acteur.getRole().getPermissions().stream()
                .map(Permission::getCode)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        Set<String> manquantes = permissions.stream()
                .map(Permission::getCode)
                .filter(Objects::nonNull)
                .filter(code -> !detenues.contains(code.toLowerCase()))
                .collect(Collectors.toCollection(java.util.TreeSet::new));
        if (!manquantes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous ne pouvez pas accorder des permissions que vous ne possédez pas : "
                            + String.join(", ", manquantes) + ".");
        }
    }

    /** Interdit à l'utilisateur connecté d'agir sur son propre compte pour l'action donnée. */
    public void interdireSurSoiMeme(UUID utilisateurCibleId, String message) {
        Utilisateur acteur = acteurCourant();
        if (acteur != null && acteur.getId() != null && acteur.getId().equals(utilisateurCibleId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    public UUID acteurCourantId() {
        Utilisateur acteur = acteurCourant();
        return acteur == null ? null : acteur.getId();
    }

    private Utilisateur acteurCourant() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            return null;
        }
        return utilisateurRepository.findByUsername(authentication.getName()).orElse(null);
    }

    private boolean estAdministrateur(Utilisateur utilisateur) {
        return utilisateur.getRole() != null
                && ROLE_ADMIN_SYSTEME.equalsIgnoreCase(utilisateur.getRole().getCode());
    }
}
