package com.budget.backend.security.service;

import com.budget.backend.security.entity.AuditLog;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * PDF du journal d'audit : synthèse (actions, utilisateurs, ressources) puis une fiche par événement
 * avec l'auteur, le poste de travail (IP, navigateur), la requête et le détail champ par champ
 * des anciennes et nouvelles valeurs.
 */
final class AuditLogPdfGenerator {

    /** Auteur d'une action, résolu à partir de utilisateur_id. */
    record Auteur(String nomComplet, String username, String matricule, String role) {}

    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DATE_EDITION = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    /** Informations techniques de la requête, affichées à part et non dans le tableau des valeurs. */
    private static final Set<String> CLES_REQUETE = Set.of("endpoint", "methodeHttp", "statutHttp");
    private static final Pattern CLE_SENSIBLE = Pattern.compile("(?i).*(mot_?de_?passe|password|motdepasse|secret|token|hash).*");

    private static final Color BORDURE = new Color(203, 213, 225);
    private static final Color FOND_ENTETE = new Color(241, 245, 249);
    private static final Color FOND_LIBELLE = new Color(248, 250, 252);
    private static final Color FOND_MODIFIE = new Color(254, 243, 199);
    private static final Color TEXTE = new Color(30, 41, 59);
    private static final Color TEXTE_DOUX = new Color(100, 116, 139);
    private static final Color VERT = new Color(21, 128, 61);

    private static final Font TITRE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, TEXTE);
    private static final Font SOUS_TITRE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, TEXTE);
    private static final Font INFO = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, TEXTE_DOUX);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, TEXTE);
    private static final Font GRAS = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, TEXTE);
    private static final Font LIBELLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, TEXTE_DOUX);
    private static final Font PETIT = FontFactory.getFont(FontFactory.HELVETICA, 7, TEXTE_DOUX);
    private static final Font ENTETE_TABLEAU = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
    private static final Font MONO = FontFactory.getFont(FontFactory.COURIER, 7.5f, TEXTE);

    private AuditLogPdfGenerator() {
    }

    static byte[] generer(List<AuditLog> logs, Map<UUID, Auteur> auteurs) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 32, 32, 36, 40);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PiedDePage());
            document.addTitle("Journal d'audit et de traçabilité");
            document.open();

            entete(document, logs);
            synthese(document, logs, auteurs);

            Paragraph titreDetail = new Paragraph("Détail des événements", SOUS_TITRE);
            titreDetail.setSpacingBefore(16);
            titreDetail.setSpacingAfter(8);
            document.add(titreDetail);
            if (logs.isEmpty()) {
                document.add(new Paragraph("Aucun événement à afficher.", INFO));
            }
            int numero = 1;
            for (AuditLog log : logs) {
                document.add(fiche(numero++, log, auteurs));
            }

            document.close();
            return out.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Erreur lors de la génération du PDF du journal d'audit.", exception);
        }
    }

    // --- En-tête et synthèse ---

    private static void entete(Document document, List<AuditLog> logs) throws Exception {
        document.add(new Paragraph("Journal d'audit et de traçabilité", TITRE));
        List<LocalDateTime> dates = logs.stream().map(AuditLog::getCreatedAt).filter(Objects::nonNull).sorted().toList();
        String periode = dates.isEmpty() ? "—"
                : "du " + dates.get(0).format(DATE_HEURE) + " au " + dates.get(dates.size() - 1).format(DATE_HEURE);
        Paragraph info = new Paragraph("Édité le " + LocalDateTime.now().format(DATE_EDITION)
                + "   ·   " + logs.size() + " événement(s)   ·   Période couverte : " + periode, INFO);
        info.setSpacingAfter(14);
        document.add(info);
    }

    private static void synthese(Document document, List<AuditLog> logs, Map<UUID, Auteur> auteurs) throws Exception {
        Paragraph titre = new Paragraph("Synthèse", SOUS_TITRE);
        titre.setSpacingAfter(6);
        document.add(titre);

        PdfPTable blocs = new PdfPTable(3);
        blocs.setWidthPercentage(100);
        blocs.setWidths(new float[]{0.9f, 1.8f, 0.9f});
        blocs.addCell(blocSynthese("Par type d'action", compter(logs, AuditLog::getTypeAction)));
        blocs.addCell(blocUtilisateurs(logs, auteurs));
        blocs.addCell(blocSynthese("Par ressource", compter(logs, AuditLog::getNomTable)));
        document.add(blocs);
    }

    private static Map<String, Long> compter(List<AuditLog> logs, Function<AuditLog, String> cle) {
        return logs.stream()
                .collect(Collectors.groupingBy(log -> Objects.requireNonNullElse(cle.apply(log), "—"), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    /** Synthèse par utilisateur : nom, ID utilisateur et nombre d'événements. */
    private static PdfPCell blocUtilisateurs(List<AuditLog> logs, Map<UUID, Auteur> auteurs) {
        Map<String, Long> parCle = compter(logs, log -> nomAuteur(log, auteurs) + "\t"
                + (log.getUtilisateurId() == null ? "—" : log.getUtilisateurId().toString()));
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{2.2f, 3f, 0.8f});
        } catch (Exception ignored) {
            // largeurs par défaut
        }
        PdfPCell entete = new PdfPCell(new Phrase("Par utilisateur", ENTETE_TABLEAU));
        entete.setColspan(3);
        entete.setBackgroundColor(VERT);
        entete.setPadding(5);
        entete.setBorderColor(BORDURE);
        table.addCell(entete);
        for (String colonne : List.of("Utilisateur", "ID utilisateur", "Nb")) {
            table.addCell(cellule(colonne, LIBELLE, FOND_LIBELLE));
        }
        if (parCle.isEmpty()) {
            PdfPCell vide = cellule("—", NORMAL, null);
            vide.setColspan(3);
            table.addCell(vide);
        }
        parCle.forEach((cle, nombre) -> {
            String[] parties = cle.split("\t", 2);
            table.addCell(cellule(parties[0], NORMAL, null));
            table.addCell(cellule(parties[1], PETIT, null));
            PdfPCell total = cellule(String.valueOf(nombre), GRAS, null);
            total.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(total);
        });
        PdfPCell conteneur = new PdfPCell(table);
        conteneur.setBorder(Rectangle.NO_BORDER);
        conteneur.setPadding(4);
        return conteneur;
    }

    private static PdfPCell blocSynthese(String titre, Map<String, Long> valeurs) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{4f, 1f});
        } catch (Exception ignored) {
            // largeurs par défaut
        }
        PdfPCell entete = new PdfPCell(new Phrase(titre, ENTETE_TABLEAU));
        entete.setColspan(2);
        entete.setBackgroundColor(VERT);
        entete.setPadding(5);
        entete.setBorderColor(BORDURE);
        table.addCell(entete);
        if (valeurs.isEmpty()) {
            PdfPCell vide = cellule("—", NORMAL, null);
            vide.setColspan(2);
            table.addCell(vide);
        }
        valeurs.forEach((libelle, nombre) -> {
            table.addCell(cellule(libelle, NORMAL, null));
            PdfPCell total = cellule(String.valueOf(nombre), GRAS, null);
            total.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(total);
        });
        PdfPCell conteneur = new PdfPCell(table);
        conteneur.setBorder(Rectangle.NO_BORDER);
        conteneur.setPadding(4);
        return conteneur;
    }

    // --- Fiche d'un événement ---

    private static PdfPTable fiche(int numero, AuditLog log, Map<UUID, Auteur> auteurs) throws Exception {
        PdfPTable fiche = new PdfPTable(4);
        fiche.setWidthPercentage(100);
        fiche.setWidths(new float[]{1.1f, 2.4f, 1.1f, 2.4f});
        fiche.setSpacingAfter(12);
        fiche.setKeepTogether(true);

        // Bandeau : numéro, action, ressource, date
        PdfPCell bandeau = new PdfPCell();
        bandeau.setColspan(4);
        bandeau.setBackgroundColor(FOND_ENTETE);
        bandeau.setBorderColor(BORDURE);
        bandeau.setPadding(6);
        Phrase titre = new Phrase();
        titre.add(new Chunk("N° " + numero + "   ", GRAS));
        Chunk action = new Chunk("  " + Objects.requireNonNullElse(log.getTypeAction(), "—") + "  ",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE));
        action.setBackground(couleurAction(log.getTypeAction()), 1, 2, 1, 2);
        titre.add(action);
        titre.add(new Chunk("   " + Objects.requireNonNullElse(log.getNomTable(), "—"), GRAS));
        titre.add(new Chunk("   ·   " + (log.getCreatedAt() == null ? "date inconnue" : log.getCreatedAt().format(DATE_HEURE)), NORMAL));
        bandeau.addElement(titre);
        fiche.addCell(bandeau);

        Map<String, Object> nouvelles = log.getNouvellesValeurs();
        Map<String, Object> anciennes = log.getAnciennesValeurs();

        ligne(fiche, "Utilisateur", descriptionAuteur(log, auteurs), "ID utilisateur",
                log.getUtilisateurId() == null ? "— (aucun compte)" : log.getUtilisateurId().toString());
        ligne(fiche, "Adresse IP", adresseIp(log.getAdresseIp()), "ID enregistrement", log.getEnregistrementId());
        fiche.addCell(cellule("Requête", LIBELLE, FOND_LIBELLE));
        PdfPCell requete = cellule(requete(nouvelles, anciennes), NORMAL, null);
        requete.setColspan(3);
        fiche.addCell(requete);
        PdfPCell libelleUa = cellule("Navigateur / poste", LIBELLE, FOND_LIBELLE);
        fiche.addCell(libelleUa);
        PdfPCell ua = new PdfPCell();
        ua.setColspan(3);
        ua.setBorderColor(BORDURE);
        ua.setPadding(4);
        ua.addElement(new Phrase(navigateur(log.getUserAgent()), NORMAL));
        if (log.getUserAgent() != null && !log.getUserAgent().isBlank()) {
            ua.addElement(new Phrase(log.getUserAgent(), PETIT));
        }
        fiche.addCell(ua);

        // Valeurs : champ / ancienne / nouvelle
        PdfPCell conteneur = new PdfPCell(tableauValeurs(anciennes, nouvelles));
        conteneur.setColspan(4);
        conteneur.setBorderColor(BORDURE);
        conteneur.setPadding(0);
        fiche.addCell(conteneur);
        return fiche;
    }

    private static void ligne(PdfPTable table, String libelle1, String valeur1, String libelle2, String valeur2) {
        table.addCell(cellule(libelle1, LIBELLE, FOND_LIBELLE));
        table.addCell(cellule(valeur1, NORMAL, null));
        table.addCell(cellule(libelle2, LIBELLE, FOND_LIBELLE));
        table.addCell(cellule(valeur2, NORMAL, null));
    }

    private static PdfPTable tableauValeurs(Map<String, Object> anciennes, Map<String, Object> nouvelles) throws Exception {
        Map<String, String> avant = valeursLisibles(anciennes);
        Map<String, String> apres = valeursLisibles(nouvelles);
        Set<String> champs = new LinkedHashSet<>(avant.keySet());
        champs.addAll(apres.keySet());

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 2.4f, 2.4f});
        for (String entete : List.of("Champ", "Ancienne valeur", "Nouvelle valeur")) {
            PdfPCell cell = cellule(entete, ENTETE_TABLEAU, new Color(71, 85, 105));
            table.addCell(cell);
        }
        if (champs.isEmpty()) {
            PdfPCell vide = cellule("Aucune valeur enregistrée pour cet événement.", INFO, null);
            vide.setColspan(3);
            table.addCell(vide);
            return table;
        }
        for (String champ : champs) {
            String ancienne = avant.get(champ);
            String nouvelle = apres.get(champ);
            boolean modifie = ancienne != null && nouvelle != null && !ancienne.equals(nouvelle);
            Color fond = modifie ? FOND_MODIFIE : null;
            table.addCell(cellule(champ, GRAS, fond));
            table.addCell(cellule(ancienne == null ? "—" : ancienne, MONO, fond));
            table.addCell(cellule(nouvelle == null ? "—" : nouvelle, MONO, fond));
        }
        return table;
    }

    // --- Mise en forme des valeurs (partagée avec la vue détaillée, voir AuditLogService#detail) ---

    /** Données métier aplaties champ par champ (« a.b »), valeurs sensibles masquées. */
    static Map<String, String> valeursLisibles(Map<String, Object> valeurs) {
        return aplatir(donneesMetier(valeurs));
    }

    /** Les données métier sont sous « donnees » quand l'entrée vient du filtre d'API ; sinon toute la map. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> donneesMetier(Map<String, Object> valeurs) {
        if (valeurs == null) return Map.of();
        if (valeurs.get("donnees") instanceof Map<?, ?> donnees) {
            Map<String, Object> resultat = new LinkedHashMap<>((Map<String, Object>) donnees);
            valeurs.forEach((cle, valeur) -> {
                if (!"donnees".equals(cle) && !CLES_REQUETE.contains(cle)) resultat.put(cle, valeur);
            });
            return resultat;
        }
        Map<String, Object> resultat = new LinkedHashMap<>(valeurs);
        CLES_REQUETE.forEach(resultat::remove);
        return resultat;
    }

    private static Map<String, String> aplatir(Map<String, Object> valeurs) {
        Map<String, String> resultat = new LinkedHashMap<>();
        valeurs.forEach((cle, valeur) -> aplatir(cle, valeur, resultat));
        return resultat;
    }

    private static void aplatir(String cle, Object valeur, Map<String, String> resultat) {
        if (CLE_SENSIBLE.matcher(cle).matches()) {
            resultat.put(cle, "•••••• (masqué)");
        } else if (valeur instanceof Map<?, ?> map && !map.isEmpty()) {
            map.forEach((sousCle, sousValeur) -> aplatir(cle + "." + sousCle, sousValeur, resultat));
        } else if (valeur instanceof Collection<?> liste) {
            resultat.put(cle, liste.isEmpty() ? "(vide)" : liste.stream().map(String::valueOf).collect(Collectors.joining(", ")));
        } else if (valeur instanceof Boolean booleen) {
            resultat.put(cle, booleen ? "oui" : "non");
        } else {
            resultat.put(cle, valeur == null ? "(vide)" : String.valueOf(valeur));
        }
    }

    static String requete(Map<String, Object> nouvelles, Map<String, Object> anciennes) {
        Map<String, Object> source = nouvelles != null && nouvelles.containsKey("endpoint") ? nouvelles
                : anciennes != null && anciennes.containsKey("endpoint") ? anciennes : null;
        if (source == null) return "—";
        String texte = Objects.toString(source.get("methodeHttp"), "") + " " + Objects.toString(source.get("endpoint"), "");
        Object statut = source.get("statutHttp");
        return statut == null ? texte.trim() : texte.trim() + " — HTTP " + statut;
    }

    static String nomAuteur(AuditLog log, Map<UUID, Auteur> auteurs) {
        Auteur auteur = log.getUtilisateurId() == null ? null : auteurs.get(log.getUtilisateurId());
        if (auteur != null) return auteur.nomComplet() + " (" + auteur.username() + ")";
        if (log.getUtilisateurId() != null) return "Compte supprimé";
        Object username = log.getNouvellesValeurs() == null ? null : log.getNouvellesValeurs().get("username");
        return username != null ? "Non authentifié (" + username + ")" : "Système / anonyme";
    }

    private static String descriptionAuteur(AuditLog log, Map<UUID, Auteur> auteurs) {
        Auteur auteur = log.getUtilisateurId() == null ? null : auteurs.get(log.getUtilisateurId());
        if (auteur == null) {
            return nomAuteur(log, auteurs);
        }
        List<String> parties = new ArrayList<>();
        parties.add(auteur.nomComplet() + " (" + auteur.username() + ")");
        if (auteur.matricule() != null) parties.add("matricule " + auteur.matricule());
        if (auteur.role() != null) parties.add("rôle " + auteur.role());
        return String.join(" — ", parties);
    }

    private static String adresseIp(String ip) {
        if (ip == null || ip.isBlank()) return "—";
        return "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip) || "127.0.0.1".equals(ip) ? ip + " (poste local)" : ip;
    }

    /** Résumé lisible du user agent : « Edge 156 · Windows 10/11 ». */
    static String navigateur(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "Non renseigné";
        String navigateur = premier(userAgent,
                new String[]{"Edg/(\\d+)", "Edge"}, new String[]{"OPR/(\\d+)", "Opera"},
                new String[]{"Firefox/(\\d+)", "Firefox"}, new String[]{"Chrome/(\\d+)", "Chrome"},
                new String[]{"Version/(\\d+)[^ ]* .*Safari/", "Safari"}, new String[]{"PostmanRuntime/([\\d.]+)", "Postman"},
                new String[]{"curl/([\\d.]+)", "curl"});
        String systeme;
        String ua = userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("windows nt 10.0")) systeme = "Windows 10/11";
        else if (ua.contains("windows")) systeme = "Windows";
        else if (ua.contains("android")) systeme = "Android";
        else if (ua.contains("iphone") || ua.contains("ipad")) systeme = "iOS";
        else if (ua.contains("mac os x")) systeme = "macOS";
        else if (ua.contains("linux")) systeme = "Linux";
        else systeme = null;
        if (navigateur == null && systeme == null) return "Client non identifié";
        return navigateur == null ? systeme : systeme == null ? navigateur : navigateur + " · " + systeme;
    }

    private static String premier(String userAgent, String[]... regles) {
        for (String[] regle : regles) {
            Matcher matcher = Pattern.compile(regle[0]).matcher(userAgent);
            if (matcher.find()) return regle[1] + " " + matcher.group(1);
        }
        return null;
    }

    private static Color couleurAction(String typeAction) {
        String action = typeAction == null ? "" : typeAction.toUpperCase(Locale.ROOT);
        return switch (action) {
            case "INSERTION", "CREATION" -> VERT;
            case "MODIFICATION" -> new Color(180, 83, 9);
            case "SUPPRESSION" -> new Color(185, 28, 28);
            case "CONNEXION", "DECONNEXION" -> new Color(29, 78, 216);
            default -> new Color(71, 85, 105);
        };
    }

    private static PdfPCell cellule(String texte, Font font, Color fond) {
        PdfPCell cell = new PdfPCell(new Phrase(texte == null || texte.isBlank() ? "—" : texte, font));
        cell.setBorderColor(BORDURE);
        cell.setPadding(4);
        if (fond != null) cell.setBackgroundColor(fond);
        return cell;
    }

    /** Pied de page : mention de confidentialité et numéro de page. */
    private static final class PiedDePage extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            float y = document.bottom() - 18;
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new Phrase("Gestion budgétaire — Journal d'audit · Document confidentiel", PETIT), document.left(), y, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT,
                    new Phrase("Page " + writer.getPageNumber(), PETIT), document.right(), y, 0);
        }
    }
}
