package com.budget.backend.common;

import java.util.regex.Pattern;

/**
 * Construction de lignes CSV sûres : les valeurs contenant le séparateur, des guillemets
 * ou des retours à la ligne sont entourées de guillemets (guillemets internes doublés),
 * et les valeurs interprétables comme formules par un tableur sont neutralisées.
 */
public final class CsvUtils {

    // Téléphones, montants signés... : commencent par + ou - mais ne sont pas des formules.
    private static final Pattern NUMERIQUE = Pattern.compile("[+-][0-9 .,()]*");

    private CsvUtils() {
    }

    public static String ligne(char separateur, Object... valeurs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < valeurs.length; i++) {
            if (i > 0) {
                sb.append(separateur);
            }
            sb.append(cellule(valeurs[i], separateur));
        }
        return sb.append('\n').toString();
    }

    public static String cellule(Object valeur, char separateur) {
        if (valeur == null) {
            return "";
        }
        String texte = valeur.toString();
        if (valeur instanceof CharSequence && estFormule(texte)) {
            texte = "'" + texte;
        }
        boolean aProteger = texte.indexOf(separateur) >= 0
                || texte.indexOf('"') >= 0
                || texte.indexOf('\n') >= 0
                || texte.indexOf('\r') >= 0;
        if (!aProteger) {
            return texte;
        }
        return '"' + texte.replace("\"", "\"\"") + '"';
    }

    private static boolean estFormule(String texte) {
        if (texte.isEmpty()) {
            return false;
        }
        char premier = texte.charAt(0);
        if (premier == '=' || premier == '@' || premier == '\t' || premier == '\r') {
            return true;
        }
        return (premier == '+' || premier == '-') && !NUMERIQUE.matcher(texte).matches();
    }
}
