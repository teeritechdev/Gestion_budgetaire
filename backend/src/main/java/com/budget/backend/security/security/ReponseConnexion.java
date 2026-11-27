package com.budget.backend.security.security;

import com.budget.backend.security.dto.JwtResponse;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.UtilisateurRepository;
import com.budget.backend.security.service.ProfilService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Réponse de connexion (jeton JWT + informations de session), à la connexion ou après un changement de mot de passe. */
@Component
public class ReponseConnexion {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final UtilisateurRepository utilisateurRepository;
    private final ProfilService profilService;

    public ReponseConnexion(JwtUtils jwtUtils, UserDetailsService userDetailsService,
                            UtilisateurRepository utilisateurRepository, ProfilService profilService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
        this.utilisateurRepository = utilisateurRepository;
        this.profilService = profilService;
    }

    @Transactional(readOnly = true)
    public JwtResponse pour(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Utilisateur utilisateur = utilisateurRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable."));
        return new JwtResponse(
                jwtUtils.generateJwtToken(authentication),
                "Bearer",
                utilisateur.getId(),
                utilisateur.getUsername(),
                utilisateur.getSalarie().getEmail(),
                utilisateur.getSalarie().getNom(),
                utilisateur.getSalarie().getPrenom(),
                utilisateur.getRole().getCode(),
                userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList(),
                Boolean.TRUE.equals(utilisateur.getDoitChangerMotDePasse()),
                profilService.avecPhoto(utilisateur));
    }

    /** Nouvelle session pour un utilisateur déjà authentifié (ex. après changement de mot de passe). */
    @Transactional(readOnly = true)
    public JwtResponse pour(String username) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return pour(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }
}
