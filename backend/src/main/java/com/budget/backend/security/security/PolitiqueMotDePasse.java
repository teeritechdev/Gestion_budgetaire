package com.budget.backend.security.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Règles communes à tous les mots de passe (création par un administrateur, changement depuis le profil,
 * réinitialisation). Les mêmes règles sont affichées et vérifiées en direct côté front.
 */
public final class PolitiqueMotDePasse {

    public static final int LONGUEUR_MIN = 8;
    public static final int LONGUEUR_MAX = 128;

    private PolitiqueMotDePasse() {
    }

    /** Lève une erreur 400 listant les règles non respectées. */
    public static void verifier(String motDePasse, String username) {
        List<String> manquements = manquements(motDePasse, username);
        if (!manquements.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Mot de passe trop faible : " + String.join(", ", manquements) + ".");
        }
    }

    public static List<String> manquements(String motDePasse, String username) {
        List<String> manquements = new ArrayList<>();
        String valeur = motDePasse == null ? "" : motDePasse;
        if (valeur.length() < LONGUEUR_MIN) manquements.add("au moins " + LONGUEUR_MIN + " caractères");
        if (valeur.length() > LONGUEUR_MAX) manquements.add("au plus " + LONGUEUR_MAX + " caractères");
        if (!valeur.chars().anyMatch(Character::isUpperCase)) manquements.add("une lettre majuscule");
        if (!valeur.chars().anyMatch(Character::isLowerCase)) manquements.add("une lettre minuscule");
        if (!valeur.chars().anyMatch(Character::isDigit)) manquements.add("un chiffre");
        if (valeur.chars().allMatch(Character::isLetterOrDigit)) manquements.add("un caractère spécial");
        if (!valeur.equals(valeur.strip())) manquements.add("pas d'espace au début ni à la fin");
        String identifiant = identifiant(username);
        if (identifiant.length() >= 3 && valeur.toLowerCase(Locale.ROOT).contains(identifiant)) {
            manquements.add("ne pas contenir votre identifiant");
        }
        return manquements;
    }

    /** Partie significative de l'identifiant : « awa.ouedraogo@uab.bf » → « awa.ouedraogo ». */
    private static String identifiant(String username) {
        if (username == null) return "";
        String valeur = username.trim().toLowerCase(Locale.ROOT);
        int arobase = valeur.indexOf('@');
        return arobase > 0 ? valeur.substring(0, arobase) : valeur;
    }
}
