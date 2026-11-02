package com.budget.backend.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Lecture des fichiers CSV importés.
 * <ul>
 *   <li>séparateur « ; » ou « , » détecté sur la ligne d'en-tête, BOM UTF-8 ignoré ;</li>
 *   <li>valeurs entre guillemets (avec guillemets doublés) prises en charge, y compris sur plusieurs lignes ;</li>
 *   <li>colonnes repérées par leur <b>nom d'en-tête</b> (casse, accents et espaces ignorés) : l'ordre est libre,
 *       les colonnes supplémentaires sont ignorées et un fichier exporté peut être réimporté ;</li>
 *   <li>toute erreur est signalée avec le numéro de ligne du fichier.</li>
 * </ul>
 */
public final class CsvReader {

    private static final long TAILLE_MAX = 5L * 1024 * 1024;
    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private CsvReader() {
    }

    /** Une ligne de données, accessible par nom de colonne. */
    public static final class Ligne {
        private final int numero;
        private final Map<String, Integer> colonnes;
        private final List<String> valeurs;

        Ligne(int numero, Map<String, Integer> colonnes, List<String> valeurs) {
            this.numero = numero;
            this.colonnes = colonnes;
            this.valeurs = valeurs;
        }

        public int numero() {
            return numero;
        }

        public boolean aColonne(String... noms) {
            for (String nom : noms) {
                if (colonnes.containsKey(normaliser(nom))) {
                    return true;
                }
            }
            return false;
        }

        /** Valeur (nettoyée) de la première colonne existante parmi les noms donnés, ou null si vide. */
        public String texte(String... noms) {
            for (String nom : noms) {
                Integer index = colonnes.get(normaliser(nom));
                if (index != null && index < valeurs.size()) {
                    String valeur = valeurs.get(index).trim();
                    // Neutralisation éventuelle ajoutée à l'export (anti-formule tableur).
                    if (valeur.startsWith("'") && valeur.length() > 1 && "=+-@".indexOf(valeur.charAt(1)) >= 0) {
                        valeur = valeur.substring(1);
                    }
                    return valeur.isEmpty() ? null : valeur;
                }
            }
            return null;
        }

        public String obligatoire(String... noms) {
            String valeur = texte(noms);
            if (valeur == null) {
                throw new IllegalArgumentException("la colonne « " + noms[0] + " » est obligatoire");
            }
            return valeur;
        }

        public Integer entier(String... noms) {
            String valeur = texte(noms);
            if (valeur == null) {
                return null;
            }
            try {
                return Integer.valueOf(valeur.replace(" ", ""));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("« " + valeur + " » n'est pas un nombre entier (colonne " + noms[0] + ")");
            }
        }

        public BigDecimal decimal(String... noms) {
            String valeur = texte(noms);
            if (valeur == null) {
                return null;
            }
            String nettoye = valeur.replace(" ", "").replace(" ", "").replace(" ", "");
            if (nettoye.contains(",") && !nettoye.contains(".")) {
                nettoye = nettoye.replace(',', '.');
            }
            try {
                return new BigDecimal(nettoye);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("« " + valeur + " » n'est pas un montant valide (colonne " + noms[0] + ")");
            }
        }

        public LocalDate date(String... noms) {
            String valeur = texte(noms);
            if (valeur == null) {
                return null;
            }
            try {
                return valeur.contains("/") ? LocalDate.parse(valeur, DATE_FR) : LocalDate.parse(valeur.substring(0, Math.min(10, valeur.length())));
            } catch (DateTimeParseException exception) {
                throw new IllegalArgumentException("« " + valeur + " » n'est pas une date valide (AAAA-MM-JJ ou JJ/MM/AAAA)");
            }
        }

        public UUID uuid(String... noms) {
            String valeur = texte(noms);
            if (valeur == null) {
                return null;
            }
            try {
                return UUID.fromString(valeur);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("« " + valeur + " » n'est pas un identifiant valide (colonne " + noms[0] + ")");
            }
        }

        /** oui/non, true/false, 1/0, actif/inactif ; valeur par défaut si la cellule est vide. */
        public boolean booleen(boolean parDefaut, String... noms) {
            String valeur = texte(noms);
            if (valeur == null) {
                return parDefaut;
            }
            String v = normaliser(valeur);
            if (List.of("oui", "true", "1", "vrai", "actif", "active", "o", "yes").contains(v)) {
                return true;
            }
            if (List.of("non", "false", "0", "faux", "inactif", "inactive", "n", "no").contains(v)) {
                return false;
            }
            throw new IllegalArgumentException("« " + valeur + " » doit valoir oui ou non (colonne " + noms[0] + ")");
        }
    }

    /**
     * Lit le fichier et appelle {@code traitement} pour chaque ligne de données.
     * Une exception levée par le traitement est renvoyée en 400 avec le numéro de ligne.
     *
     * @param colonnesRequises noms de colonnes devant figurer dans l'en-tête (chaque élément peut proposer
     *                         des alternatives séparées par « | »)
     * @return le nombre de lignes traitées
     */
    public static int lire(MultipartFile fichier, List<String> colonnesRequises, Consumer<Ligne> traitement) {
        List<List<String>> enregistrements = parser(contenu(fichier));
        if (enregistrements.isEmpty()) {
            throw erreur("Le fichier est vide : la première ligne doit contenir les noms de colonnes.");
        }
        Map<String, Integer> colonnes = new HashMap<>();
        List<String> entete = enregistrements.get(0);
        for (int i = 0; i < entete.size(); i++) {
            colonnes.putIfAbsent(normaliser(entete.get(i)), i);
        }
        List<String> manquantes = new ArrayList<>();
        for (String requise : colonnesRequises) {
            boolean presente = false;
            for (String alternative : requise.split("\\|")) {
                presente |= colonnes.containsKey(normaliser(alternative));
            }
            if (!presente) {
                manquantes.add(requise.split("\\|")[0]);
            }
        }
        if (!manquantes.isEmpty()) {
            throw erreur("Colonne(s) manquante(s) dans l'en-tête : " + String.join(", ", manquantes)
                    + ". Téléchargez le modèle pour connaître le format attendu.");
        }

        int traitees = 0;
        for (int i = 1; i < enregistrements.size(); i++) {
            List<String> valeurs = enregistrements.get(i);
            if (valeurs.stream().allMatch(v -> v.isBlank())) {
                continue;
            }
            Ligne ligne = new Ligne(i + 1, colonnes, valeurs);
            try {
                traitement.accept(ligne);
            } catch (ResponseStatusException exception) {
                throw new ResponseStatusException(exception.getStatusCode(),
                        "Ligne " + ligne.numero() + " : " + (exception.getReason() == null ? "valeur invalide" : exception.getReason()));
            } catch (IllegalArgumentException | IllegalStateException exception) {
                throw erreur("Ligne " + ligne.numero() + " : " + exception.getMessage());
            } catch (RuntimeException exception) {
                String message = exception.getMessage() == null ? "erreur inattendue" : exception.getMessage();
                throw erreur("Ligne " + ligne.numero() + " : " + message);
            }
            traitees++;
        }
        if (traitees == 0) {
            throw erreur("Le fichier ne contient aucune ligne de données.");
        }
        return traitees;
    }

    static String normaliser(String nom) {
        String sansAccents = Normalizer.normalize(nom == null ? "" : nom, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sansAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private static String contenu(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw erreur("Sélectionnez un fichier CSV à importer.");
        }
        if (fichier.getSize() > TAILLE_MAX) {
            throw erreur("Le fichier dépasse la taille maximale autorisée (5 Mo).");
        }
        try {
            String texte = new String(fichier.getBytes(), StandardCharsets.UTF_8);
            return texte.startsWith("﻿") ? texte.substring(1) : texte;
        } catch (IOException exception) {
            throw erreur("Le fichier n'a pas pu être lu.");
        }
    }

    /** Découpe le texte en enregistrements, en respectant les guillemets. */
    static List<List<String>> parser(String texte) {
        int finEntete = texte.indexOf('\n');
        String entete = finEntete < 0 ? texte : texte.substring(0, finEntete);
        char separateur = compter(entete, ';') >= compter(entete, ',') ? ';' : ',';

        List<List<String>> enregistrements = new ArrayList<>();
        List<String> courant = new ArrayList<>();
        StringBuilder cellule = new StringBuilder();
        boolean entreGuillemets = false;
        for (int i = 0; i < texte.length(); i++) {
            char c = texte.charAt(i);
            if (entreGuillemets) {
                if (c == '"') {
                    if (i + 1 < texte.length() && texte.charAt(i + 1) == '"') {
                        cellule.append('"');
                        i++;
                    } else {
                        entreGuillemets = false;
                    }
                } else {
                    cellule.append(c);
                }
            } else if (c == '"') {
                entreGuillemets = true;
            } else if (c == separateur) {
                courant.add(cellule.toString());
                cellule.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < texte.length() && texte.charAt(i + 1) == '\n') {
                    i++;
                }
                courant.add(cellule.toString());
                cellule.setLength(0);
                enregistrements.add(courant);
                courant = new ArrayList<>();
            } else {
                cellule.append(c);
            }
        }
        if (cellule.length() > 0 || !courant.isEmpty()) {
            courant.add(cellule.toString());
            enregistrements.add(courant);
        }
        return enregistrements;
    }

    private static int compter(String texte, char c) {
        int n = 0;
        for (int i = 0; i < texte.length(); i++) {
            if (texte.charAt(i) == c) {
                n++;
            }
        }
        return n;
    }

    private static ResponseStatusException erreur(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
