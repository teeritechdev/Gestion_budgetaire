package com.budget.backend.security.service;

import com.budget.backend.security.entity.JetonReinitialisation;
import com.budget.backend.security.entity.Salarie;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.JetonReinitialisationRepository;
import com.budget.backend.security.repository.UtilisateurRepository;
import com.budget.backend.security.security.LoginAttemptService;
import com.budget.backend.security.security.PolitiqueMotDePasse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Mots de passe : changement depuis le profil et « mot de passe oublié » sans service externe.
 * <p>
 * Mot de passe oublié : la personne prouve son identité avec les données de son dossier RH
 * (identifiant, matricule, N° CNIB, date de naissance). En cas de succès, un jeton à usage unique,
 * valable {@link #DUREE_JETON}, permet de choisir un nouveau mot de passe. Les tentatives sont limitées
 * par identifiant et par adresse IP, et toutes sont tracées dans le journal d'audit.
 */
@Service
public class MotDePasseService {

    static final Duration DUREE_JETON = Duration.ofMinutes(15);
    private static final String ECHEC_VERIFICATION =
            "Les informations saisies ne permettent pas de vérifier votre identité. Vérifiez-les ; si elles sont "
                    + "exactes mais que le problème persiste, contactez votre administrateur.";
    private static final String TROP_DE_TENTATIVES =
            "Trop de tentatives. Pour votre sécurité, réessayez dans quelques minutes.";

    private final UtilisateurRepository utilisateurRepository;
    private final JetonReinitialisationRepository jetonRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService tentatives;
    private final AuditService auditService;
    private final SecureRandom aleatoire = new SecureRandom();

    public MotDePasseService(UtilisateurRepository utilisateurRepository,
                             JetonReinitialisationRepository jetonRepository,
                             PasswordEncoder passwordEncoder,
                             LoginAttemptService tentatives,
                             AuditService auditService) {
        this.utilisateurRepository = utilisateurRepository;
        this.jetonRepository = jetonRepository;
        this.passwordEncoder = passwordEncoder;
        this.tentatives = tentatives;
        this.auditService = auditService;
    }

    // --- Changement depuis le profil ---

    @Transactional
    public Utilisateur changer(String username, String motDePasseActuel, String nouveauMotDePasse) {
        Utilisateur utilisateur = utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session invalide."));
        String cle = "changement:" + username;
        if (tentatives.estBloque(cle)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, TROP_DE_TENTATIVES);
        }
        if (!passwordEncoder.matches(motDePasseActuel, utilisateur.getMotDePasseHash())) {
            tentatives.echec(cle);
            auditService.logAction(utilisateur.getId(), "MOT_DE_PASSE_ECHEC", "utilisateurs",
                    utilisateur.getId().toString(), null, Map.of("motif", "mot de passe actuel incorrect"));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe actuel est incorrect.");
        }
        tentatives.succes(cle);
        if (passwordEncoder.matches(nouveauMotDePasse, utilisateur.getMotDePasseHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le nouveau mot de passe doit être différent de l'actuel.");
        }
        PolitiqueMotDePasse.verifier(nouveauMotDePasse, utilisateur.getUsername());
        appliquer(utilisateur, nouveauMotDePasse, false);
        auditService.logAction(utilisateur.getId(), "MOT_DE_PASSE_CHANGE", "utilisateurs",
                utilisateur.getId().toString(), null, Map.of("origine", "profil", "autresSessionsDeconnectees", true));
        return utilisateur;
    }

    // --- Mot de passe oublié ---

    /** Étape 1 : vérifie l'identité et renvoie un jeton de réinitialisation (en clair, une seule fois). */
    @Transactional
    public String verifierIdentite(String identifiant, String matricule, String numeroCnib, LocalDate dateNaissance,
                                   String adresseIp) {
        String cleIdentifiant = "reinitialisation:" + normaliser(identifiant);
        String cleIp = "reinitialisation-ip:" + (adresseIp == null ? "" : adresseIp);
        if (tentatives.estBloque(cleIdentifiant) || tentatives.estBloque(cleIp)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, TROP_DE_TENTATIVES);
        }

        Optional<Utilisateur> trouve = utilisateurRepository.findByUsernameIgnoreCase(identifiant.trim());
        boolean identiteValide = trouve.filter(u -> Boolean.TRUE.equals(u.getActif()))
                .map(Utilisateur::getSalarie)
                .filter(salarie -> correspond(salarie, matricule, numeroCnib, dateNaissance))
                .isPresent();

        if (!identiteValide) {
            tentatives.echec(cleIdentifiant);
            tentatives.echec(cleIp);
            auditService.logAction(trouve.map(Utilisateur::getId).orElse(null), "REINITIALISATION_ECHEC",
                    "utilisateurs", trouve.map(u -> u.getId().toString()).orElse("inconnu"), null,
                    Map.of("identifiant", identifiant.trim(), "motif", "identité non vérifiée"));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ECHEC_VERIFICATION);
        }

        Utilisateur utilisateur = trouve.get();
        tentatives.succes(cleIdentifiant);
        Instant maintenant = Instant.now();
        jetonRepository.annulerJetonsActifs(utilisateur.getId(), maintenant);
        // Ménage : les jetons expirés depuis plus de 7 jours n'ont plus d'intérêt.
        jetonRepository.supprimerExpiresAvant(maintenant.minus(Duration.ofDays(7)));

        byte[] octets = new byte[32];
        aleatoire.nextBytes(octets);
        String jeton = Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
        jetonRepository.save(JetonReinitialisation.builder()
                .utilisateurId(utilisateur.getId())
                .empreinte(empreinte(jeton))
                .creeLe(maintenant)
                .expireLe(maintenant.plus(DUREE_JETON))
                .adresseIp(adresseIp)
                .build());
        auditService.logAction(utilisateur.getId(), "REINITIALISATION_DEMANDE", "utilisateurs",
                utilisateur.getId().toString(), null,
                Map.of("identifiant", utilisateur.getUsername(), "validiteMinutes", DUREE_JETON.toMinutes()));
        return jeton;
    }

    /** Étape 2 : nouveau mot de passe avec le jeton ; toutes les sessions ouvertes sont fermées. */
    @Transactional
    public void reinitialiser(String jeton, String nouveauMotDePasse) {
        JetonReinitialisation enregistrement = jetonRepository.findByEmpreinte(empreinte(jeton))
                .filter(j -> j.getUtiliseLe() == null && j.getExpireLe().isAfter(Instant.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Ce lien de réinitialisation est invalide ou a expiré. Recommencez la vérification d'identité."));
        Utilisateur utilisateur = utilisateurRepository.findById(enregistrement.getUtilisateurId())
                .filter(u -> Boolean.TRUE.equals(u.getActif()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Ce compte n'est plus actif. Contactez votre administrateur."));

        PolitiqueMotDePasse.verifier(nouveauMotDePasse, utilisateur.getUsername());
        if (passwordEncoder.matches(nouveauMotDePasse, utilisateur.getMotDePasseHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choisissez un mot de passe différent de l'ancien.");
        }
        enregistrement.setUtiliseLe(Instant.now());
        appliquer(utilisateur, nouveauMotDePasse, false);
        tentatives.succes(utilisateur.getUsername());
        auditService.logAction(utilisateur.getId(), "REINITIALISATION_EFFECTUEE", "utilisateurs",
                utilisateur.getId().toString(), null, Map.of("origine", "mot de passe oublié", "sessionsFermees", true));
    }

    /** Mot de passe défini par un administrateur : temporaire, à changer à la prochaine connexion. */
    public void definirParAdministrateur(Utilisateur utilisateur, String motDePasse) {
        PolitiqueMotDePasse.verifier(motDePasse, utilisateur.getUsername());
        appliquer(utilisateur, motDePasse, true);
    }

    // --- Outils ---

    private void appliquer(Utilisateur utilisateur, String motDePasse, boolean temporaire) {
        utilisateur.setMotDePasseHash(passwordEncoder.encode(motDePasse));
        utilisateur.setDoitChangerMotDePasse(temporaire);
        utilisateur.setMotDePasseModifieLe(Instant.now());
        utilisateurRepository.save(utilisateur);
    }

    private static boolean correspond(Salarie salarie, String matricule, String numeroCnib, LocalDate dateNaissance) {
        return salarie != null
                && salarie.getMatricule() != null && salarie.getMatricule().equalsIgnoreCase(matricule.trim())
                && salarie.getNumeroCnib() != null
                && salarie.getNumeroCnib().equals(numeroCnib.replaceAll("\\s", "").toUpperCase(Locale.ROOT))
                && dateNaissance.equals(salarie.getDateNaissance());
    }

    private static String normaliser(String identifiant) {
        return identifiant == null ? "" : identifiant.trim().toLowerCase(Locale.ROOT);
    }

    private static String empreinte(String jeton) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(jeton.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 indisponible", exception);
        }
    }
}
