package com.budget.backend.stock.service;

import com.budget.backend.security.entity.Salarie;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.stock.entity.DemandeMagasin;
import com.budget.backend.stock.entity.LigneDemandeMagasin;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class BonDeSortiePdfGenerator {

    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");
    private static final DateTimeFormatter DATE_JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Color VERT_UAB = new Color(22, 101, 52); // #166534
    private static final Color VERT_CLAIR = new Color(240, 253, 244); // #f0fdf4
    private static final Color GRIS_FOND = new Color(248, 250, 252);
    private static final Color BORDURE = new Color(203, 213, 225);
    private static final Color TEXTE = new Color(15, 39, 64);
    private static final Color TEXTE_DOUX = new Color(100, 116, 139);

    private static final Font FONT_HEADER_ENTREPRISE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, VERT_UAB);
    private static final Font FONT_HEADER_SOUS = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXTE_DOUX);
    private static final Font FONT_TITRE_DOC = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, VERT_UAB);
    private static final Font FONT_NUM_DOC = FontFactory.getFont(FontFactory.COURIER_BOLD, 13, TEXTE);
    private static final Font FONT_SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, VERT_UAB);
    private static final Font FONT_LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, TEXTE_DOUX);
    private static final Font FONT_VALEUR = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, TEXTE);
    private static final Font FONT_ENTETE_TAB = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
    private static final Font FONT_CELLULE = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXTE);
    private static final Font FONT_CELLULE_GRAS = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, TEXTE);
    private static final Font FONT_PETIT = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, TEXTE_DOUX);

    private BonDeSortiePdfGenerator() {}

    public static byte[] generer(DemandeMagasin demande) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PageFooterHelper());
            doc.open();

            // 1. En-tête officiel
            PdfPTable entete = new PdfPTable(2);
            entete.setWidthPercentage(100);
            entete.setWidths(new float[]{65, 35});

            PdfPCell cellGauche = new PdfPCell();
            cellGauche.setBorder(Rectangle.NO_BORDER);
            cellGauche.addElement(new Paragraph("UNION DES ASSURANCES DU BURKINA (UAB)", FONT_HEADER_ENTREPRISE));
            cellGauche.addElement(new Paragraph("Direction Générale — Département Logistique & Magasin", FONT_HEADER_SOUS));
            entete.addCell(cellGauche);

            PdfPCell cellDroite = new PdfPCell();
            cellDroite.setBorder(Rectangle.NO_BORDER);
            cellDroite.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph dateP = new Paragraph("Date d'édition : " + LocalDateTime.now().format(DATE_HEURE), FONT_PETIT);
            dateP.setAlignment(Element.ALIGN_RIGHT);
            cellDroite.addElement(dateP);
            entete.addCell(cellDroite);

            doc.add(entete);
            doc.add(new Paragraph(" "));

            // 2. Cartouche Titre du Bon de Sortie
            PdfPTable cartouche = new PdfPTable(1);
            cartouche.setWidthPercentage(100);
            PdfPCell cellTitre = new PdfPCell();
            cellTitre.setBackgroundColor(VERT_CLAIR);
            cellTitre.setBorderColor(VERT_UAB);
            cellTitre.setBorderWidth(1.2f);
            cellTitre.setPadding(10);
            cellTitre.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph pTitre = new Paragraph("BON DE SORTIE MAGASIN", FONT_TITRE_DOC);
            pTitre.setAlignment(Element.ALIGN_CENTER);
            cellTitre.addElement(pTitre);

            Paragraph pNum = new Paragraph(demande.getNumeroDemande(), FONT_NUM_DOC);
            pNum.setAlignment(Element.ALIGN_CENTER);
            cellTitre.addElement(pNum);

            cartouche.addCell(cellTitre);
            doc.add(cartouche);
            doc.add(new Paragraph(" "));

            // 3. Fiche récapitulative (Demandeur, Service, Structure, Objet, Statut)
            PdfPTable infoTab = new PdfPTable(4);
            infoTab.setWidthPercentage(100);
            infoTab.setWidths(new float[]{22, 28, 22, 28});

            ajouterChamp(infoTab, "Demandeur", nomUtilisateur(demande.getDemandeur()));
            ajouterChamp(infoTab, "Structure", demande.getStructure() != null ? demande.getStructure().getLibelle() + " (" + demande.getStructure().getCode() + ")" : "—");
            ajouterChamp(infoTab, "Service", demande.getService() != null ? demande.getService().getLibelle() + " (" + demande.getService().getCode() + ")" : "—");
            ajouterChamp(infoTab, "Statut", formaterStatut(demande.getStatut()));
            ajouterChamp(infoTab, "Date demande", demande.getCreatedAt() != null ? demande.getCreatedAt().format(DATE_HEURE) : "—");
            ajouterChamp(infoTab, "Date validation N+1", demande.getDateValidation() != null ? demande.getDateValidation().format(DATE_HEURE) : "En attente");
            ajouterChamp(infoTab, "Valideur N+1", nomUtilisateur(demande.getValideurN1()));
            ajouterChamp(infoTab, "Magasinier", nomUtilisateur(demande.getMagasinier()));

            // Objet sur toute la largeur
            PdfPCell cellObjLabel = new PdfPCell(new Phrase("Objet de la demande", FONT_LABEL));
            cellObjLabel.setBackgroundColor(GRIS_FOND);
            cellObjLabel.setBorderColor(BORDURE);
            cellObjLabel.setPadding(5);
            infoTab.addCell(cellObjLabel);

            PdfPCell cellObjVal = new PdfPCell(new Phrase(demande.getObjet() != null ? demande.getObjet() : "—", FONT_VALEUR));
            cellObjVal.setColspan(3);
            cellObjVal.setBorderColor(BORDURE);
            cellObjVal.setPadding(5);
            infoTab.addCell(cellObjVal);

            doc.add(infoTab);
            doc.add(new Paragraph(" "));

            // 4. Tableau des articles demandés & servis
            Paragraph sectionArt = new Paragraph("ARTICLES ET FOURNITURES", FONT_SECTION);
            doc.add(sectionArt);
            doc.add(new Paragraph(" "));

            PdfPTable tabArticles = new PdfPTable(6);
            tabArticles.setWidthPercentage(100);
            tabArticles.setWidths(new float[]{16, 36, 14, 11, 11, 12});

            ajouterEntete(tabArticles, "Code");
            ajouterEntete(tabArticles, "Désignation");
            ajouterEntete(tabArticles, "Unité");
            ajouterEntete(tabArticles, "Qté Dem.");
            ajouterEntete(tabArticles, "Qté Serv.");
            ajouterEntete(tabArticles, "Reliquat");

            int totalDemande = 0;
            int totalServi = 0;
            int totalReliquat = 0;

            if (demande.getLignes() != null) {
                for (int i = 0; i < demande.getLignes().size(); i++) {
                    LigneDemandeMagasin l = demande.getLignes().get(i);
                    Color bg = (i % 2 == 1) ? GRIS_FOND : Color.WHITE;
                    int dem = l.getQuantiteDemande() != null ? l.getQuantiteDemande() : 0;
                    int ser = l.getQuantiteServie() != null ? l.getQuantiteServie() : 0;
                    int rel = Math.max(0, dem - ser);

                    totalDemande += dem;
                    totalServi += ser;
                    totalReliquat += rel;

                    String code = l.getArticle() != null ? l.getArticle().getCodeArticle() : "—";
                    String desig = l.getArticle() != null ? l.getArticle().getDesignation() : "—";
                    String unite = l.getArticle() != null ? l.getArticle().getUniteMesure() : "—";

                    ajouterCellule(tabArticles, code, Element.ALIGN_LEFT, bg, false);
                    ajouterCellule(tabArticles, desig, Element.ALIGN_LEFT, bg, false);
                    ajouterCellule(tabArticles, unite, Element.ALIGN_CENTER, bg, false);
                    ajouterCellule(tabArticles, String.valueOf(dem), Element.ALIGN_RIGHT, bg, false);
                    ajouterCellule(tabArticles, String.valueOf(ser), Element.ALIGN_RIGHT, bg, true);
                    ajouterCellule(tabArticles, String.valueOf(rel), Element.ALIGN_RIGHT, bg, false);
                }
            }

            // Ligne de totaux
            PdfPCell cellTotalLabel = new PdfPCell(new Phrase("TOTAL", FONT_CELLULE_GRAS));
            cellTotalLabel.setColspan(3);
            cellTotalLabel.setBackgroundColor(VERT_CLAIR);
            cellTotalLabel.setBorderColor(BORDURE);
            cellTotalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cellTotalLabel.setPadding(5);
            tabArticles.addCell(cellTotalLabel);

            ajouterCellule(tabArticles, String.valueOf(totalDemande), Element.ALIGN_RIGHT, VERT_CLAIR, true);
            ajouterCellule(tabArticles, String.valueOf(totalServi), Element.ALIGN_RIGHT, VERT_CLAIR, true);
            ajouterCellule(tabArticles, String.valueOf(totalReliquat), Element.ALIGN_RIGHT, VERT_CLAIR, true);

            doc.add(tabArticles);
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph(" "));

            // 5. Zone des Émargements / Signatures (3 colonnes)
            PdfPTable signatures = new PdfPTable(3);
            signatures.setWidthPercentage(100);
            signatures.setWidths(new float[]{33.3f, 33.3f, 33.4f});

            ajouterCadreSignature(signatures, "LE DEMANDEUR", nomUtilisateur(demande.getDemandeur()), "Date : " + (demande.getCreatedAt() != null ? demande.getCreatedAt().format(DATE_JOUR) : "___/___/______"));
            ajouterCadreSignature(signatures, "LE RESPONSABLE N+1", nomUtilisateur(demande.getValideurN1()), "Date : " + (demande.getDateValidation() != null ? demande.getDateValidation().format(DATE_JOUR) : "___/___/______"));
            ajouterCadreSignature(signatures, "LE MAGASINIER", nomUtilisateur(demande.getMagasinier()), "Date : " + (demande.getDateServie() != null ? demande.getDateServie().format(DATE_JOUR) : "___/___/______"));

            doc.add(signatures);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du bon de sortie PDF : " + e.getMessage(), e);
        }
    }

    private static void ajouterChamp(PdfPTable table, String label, String valeur) {
        PdfPCell cLabel = new PdfPCell(new Phrase(label, FONT_LABEL));
        cLabel.setBackgroundColor(GRIS_FOND);
        cLabel.setBorderColor(BORDURE);
        cLabel.setPadding(4);

        PdfPCell cVal = new PdfPCell(new Phrase(valeur != null ? valeur : "—", FONT_VALEUR));
        cVal.setBorderColor(BORDURE);
        cVal.setPadding(4);

        table.addCell(cLabel);
        table.addCell(cVal);
    }

    private static void ajouterEntete(PdfPTable table, String texte) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, FONT_ENTETE_TAB));
        cell.setBackgroundColor(VERT_UAB);
        cell.setBorderColor(VERT_UAB);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private static void ajouterCellule(PdfPTable table, String texte, int align, Color bg, boolean gras) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, gras ? FONT_CELLULE_GRAS : FONT_CELLULE));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(BORDURE);
        cell.setHorizontalAlignment(align);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private static void ajouterCadreSignature(PdfPTable table, String role, String nom, String mentionDate) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(BORDURE);
        cell.setMinimumHeight(80);
        cell.setPadding(6);

        Paragraph pRole = new Paragraph(role, FONT_SECTION);
        pRole.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pRole);

        Paragraph pNom = new Paragraph(nom != null ? nom : "(Non renseigné)", FONT_VALEUR);
        pNom.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pNom);

        Paragraph pMention = new Paragraph(mentionDate, FONT_PETIT);
        pMention.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pMention);

        Paragraph pSignature = new Paragraph("\nSignature & Cachet :", FONT_PETIT);
        cell.addElement(pSignature);

        table.addCell(cell);
    }

    private static String nomUtilisateur(Utilisateur u) {
        if (u == null) return "—";
        Salarie s = u.getSalarie();
        if (s != null) {
            String nom = s.getNom() != null ? s.getNom().toUpperCase() : "";
            String prenom = s.getPrenom() != null ? s.getPrenom() : "";
            String mat = s.getMatricule() != null ? " (" + s.getMatricule() + ")" : "";
            return (prenom + " " + nom + mat).trim();
        }
        return u.getUsername() != null ? u.getUsername() : "—";
    }

    private static String formaterStatut(String statut) {
        if (statut == null) return "—";
        return switch (statut) {
            case "SOUMIS" -> "Soumise (En attente de validation N+1)";
            case "VALIDE_N1" -> "Validée N+1 (En attente de service)";
            case "PARTIELLEMENT_SERVIE" -> "Partiellement servie";
            case "SERVIE" -> "Servie (Complète)";
            case "REJETEE" -> "Rejetée";
            default -> statut;
        };
    }

    private static class PageFooterHelper extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Phrase footer = new Phrase(
                    "UAB Assurances • Système de Gestion Budgétaire & des Stocks • Page " + writer.getPageNumber(),
                    FONT_PETIT
            );
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, footer,
                    (document.right() - document.left()) / 2 + document.leftMargin(),
                    document.bottom() - 15, 0);
        }
    }
}

