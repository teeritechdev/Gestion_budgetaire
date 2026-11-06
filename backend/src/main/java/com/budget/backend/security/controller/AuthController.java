package com.budget.backend.security.controller;

import com.budget.backend.security.dto.JwtResponse;
import com.budget.backend.security.dto.LoginRequest;
import com.budget.backend.security.dto.ReinitialisationMotDePasseRequest;
import com.budget.backend.security.dto.VerificationIdentiteRequest;
import com.budget.backend.security.security.ReponseConnexion;
import com.budget.backend.security.service.AdresseIpResolver;
import com.budget.backend.security.service.MotDePasseService;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.UtilisateurRepository;
import com.budget.backend.security.security.JwtUtils;
import com.budget.backend.security.security.LoginAttemptService;
import com.budget.backend.security.security.TokenRevocationService;
import com.budget.backend.security.service.AuditService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AuditService auditService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private TokenRevocationService tokenRevocationService;

    @Autowired
    private ReponseConnexion reponseConnexion;

    @Autowired
    private MotDePasseService motDePasseService;

    @Autowired
    private AdresseIpResolver adresseIpResolver;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        if (loginAttemptService.estBloque(username)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de tentatives de connexion échouées. Réessayez dans quelques minutes.");
        }

        // Authentification via Spring Security
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword())
            );
        } catch (AuthenticationException exception) {
            loginAttemptService.echec(username);
            UUID utilisateurId = utilisateurRepository.findByUsername(username)
                    .map(Utilisateur::getId)
                    .orElse(null);
            auditService.logAction(
                    utilisateurId,
                    "CONNEXION_ECHEC",
                    "utilisateurs",
                    utilisateurId != null ? utilisateurId.toString() : "inconnu",
                    null,
                    Map.of("username", username == null ? "" : username, "statut", "echec")
            );
            throw exception;
        }
        loginAttemptService.succes(username);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        JwtResponse reponse = reponseConnexion.pour(authentication);

        // Enregistrement de l'audit de connexion dans la base de données
        auditService.logAction(
                reponse.getId(),
                "CONNEXION",
                "utilisateurs",
                reponse.getId().toString(),
                null,
                Map.of("username", reponse.getUsername(), "statut", "succes")
        );
        return ResponseEntity.ok(reponse);
    }

    /** Mot de passe oublié, étape 1 : vérification de l'identité à partir du dossier RH. */
    @PostMapping("/mot-de-passe-oublie/verification")
    public Map<String, Object> verifierIdentite(@Valid @RequestBody VerificationIdentiteRequest request,
                                                HttpServletRequest httpRequest) {
        String jeton = motDePasseService.verifierIdentite(request.identifiant(), request.matricule(),
                request.numeroCnib(), request.dateNaissance(), adresseIpResolver.adresseClient(httpRequest));
        return Map.of("jeton", jeton, "validiteMinutes", 15);
    }

    /** Mot de passe oublié, étape 2 : choix du nouveau mot de passe. */
    @PostMapping("/mot-de-passe-oublie/reinitialisation")
    public Map<String, String> reinitialiser(@Valid @RequestBody ReinitialisationMotDePasseRequest request) {
        motDePasseService.reinitialiser(request.jeton(), request.nouveauMotDePasse());
        return Map.of("message", "Votre mot de passe a été réinitialisé. Vous pouvez vous connecter.");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletRequest request) {
        // Le token présenté est révoqué jusqu'à son expiration : il ne peut plus être réutilisé.
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            String jwt = headerAuth.substring(7);
            if (jwtUtils.validateJwtToken(jwt)) {
                Claims claims = jwtUtils.getClaims(jwt);
                tokenRevocationService.revoquer(claims.getId(), claims.getExpiration().toInstant());
            }
        }
        SecurityContextHolder.clearContext();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Déconnexion réussie avec succès !");
        return ResponseEntity.ok(response);
    }
}
