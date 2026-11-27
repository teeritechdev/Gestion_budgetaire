package com.budget.backend.achat.service;

import com.budget.backend.achat.entity.BonCommande;
import com.budget.backend.achat.entity.Fournisseur;
import com.budget.backend.achat.entity.LigneBonCommande;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class BonCommandePdfGenerator {

    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");
    private static final DateTimeFormatter DATE_JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Color BLEU_UAB = new Color(30, 64, 175); // #1e40af
    private static final Color BLEU_CLAIR = new Color(239, 246, 255); // #eff6ff
    private static final Color GRIS_FOND = new Color(248, 250, 252);
    private static final Color BORDURE = new Color(203, 213, 225);
    private static final Color TEXTE = new Color(15, 39, 64);
    private static final Color TEXTE_DOUX = new Color(100, 116, 139);

    private static final Font FONT_HEADER_ENTREPRISE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BLEU_UAB);
    private static final Font FONT_HEADER_SOUS = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXTE_DOUX);
    private static final Font FONT_TITRE_DOC = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, BLEU_UAB);
    private static final Font FONT_NUM_DOC = FontFactory.getFont(FontFactory.COURIER_BOLD, 13, TEXTE);
    private static final Font FONT_SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, BLEU_UAB);
    private static final Font FONT_LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, TEXTE_DOUX);
    private static final Font FONT_VALEUR = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, TEXTE);
    private static final Font FONT_ENTETE_TAB = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
    private static final Font FONT_CELLULE = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXTE);
    private static final Font FONT_CELLULE_GRAS = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, TEXTE);
    private static final Font FONT_PETIT = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, TEXTE_DOUX);

    private BonCommandePdfGenerator() {}

    public static byte[] generer(BonCommande bc) {
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
            cellGauche.addElement(new Paragraph("Direction Générale — Département Achats & Moyens Généraux", FONT_HEADER_SOUS));
            entete.addCell(cellGauche);

            PdfPCell cellDroite = new PdfPCell();
            cellDroite.setBorder(Rectangle.NO_BORDER);
            Paragraph dateP = new Paragraph("Date d'édition : " + LocalDateTime.now().format(DATE_HEURE), FONT_PETIT);
            dateP.setAlignment(Element.ALIGN_RIGHT);
            cellDroite.addElement(dateP);
            entete.addCell(cellDroite);

            doc.add(entete);
            doc.add(new Paragraph(" "));

            // 2. Cartouche Titre du Bon de Commande
            PdfPTable cartouche = new PdfPTable(1);
            cartouche.setWidthPercentage(100);
            PdfPCell cellTitre = new PdfPCell();
            cellTitre.setBackgroundColor(BLEU_CLAIR);
            cellTitre.setBorderColor(BLEU_UAB);
            cellTitre.setBorderWidth(1.2f);
            cellTitre.setPadding(9);

            Paragraph pTitre = new Paragraph("BON DE COMMANDE", FONT_TITRE_DOC);
            pTitre.setAlignment(Element.ALIGN_CENTER);
            Paragraph pNum = new Paragraph(bc.getCodeBc(), FONT_NUM_DOC);
            pNum.setAlignment(Element.ALIGN_CENTER);
            Paragraph pMeta = new Paragraph(String.format("Émis le %s  |  Type : %s  |  Statut : %s",
                    bc.getDateEmission() != null ? bc.getDateEmission().format(DATE_JOUR) : "—",
                    bc.getTypeBc(), bc.getStatut()), FONT_PETIT);
            pMeta.setAlignment(Element.ALIGN_CENTER);

            cellTitre.addElement(pTitre);
            cellTitre.addElement(pNum);
            cellTitre.addElement(pMeta);
            cartouche.addCell(cellTitre);
            doc.add(cartouche);
            doc.add(new Paragraph(" "));

            // 3. Blocs Fournisseur & Bénéficiaire (2 colonnes)
            PdfPTable blocs = new PdfPTable(2);
            blocs.setWidthPercentage(100);
            blocs.setWidths(new float[]{50, 50});

            Fournisseur f = bc.getFournisseur();
            PdfPCell blocF = new PdfPCell();
            blocF.setBorderColor(BORDURE);
            blocF.setBackgroundColor(GRIS_FOND);
            blocF.setPadding(8);
            blocF.addElement(new Paragraph("FOURNISSEUR RETENU", FONT_SECTION));
            blocF.addElement(new Paragraph("Raison sociale : " + (f != null ? f.getRaisonSociale() : "—"), FONT_CELLULE_GRAS));
            blocF.addElement(new Paragraph("Code Fournisseur : " + (f != null ? f.getCodeFournisseur() : "—"), FONT_CELLULE));
            blocF.addElement(new Paragraph("N° IFU : " + (f != null ? f.getIfu() : "—"), FONT_CELLULE_GRAS));
            blocF.addElement(new Paragraph("Téléphone : " + (f != null && f.getTelephone() != null ? f.getTelephone() : "—"), FONT_CELLULE));
            blocF.addElement(new Paragraph("E-mail : " + (f != null && f.getEmail() != null ? f.getEmail() : "—"), FONT_CELLULE));
            blocF.addElement(new Paragraph("Adresse : " + (f != null && f.getAdresse() != null ? f.getAdresse() : "—"), FONT_CELLULE));
            blocs.addCell(blocF);

            PdfPCell blocD = new PdfPCell();
            blocD.setBorderColor(BORDURE);
            blocD.setBackgroundColor(GRIS_FOND);
            blocD.setPadding(8);
            blocD.addElement(new Paragraph("RÉFÉRENCE & LIVRAISON", FONT_SECTION));
            var da = bc.getDemandeAchat();
            blocD.addElement(new Paragraph("Dossier DA d'origine : " + (da != null ? da.getCodeDossier() : "—"), FONT_CELLULE_GRAS));
            blocD.addElement(new Paragraph("Société émettrice : " + (da != null ? da.getEntiteEmettrice() : "—"), FONT_CELLULE));
            blocD.addElement(new Paragraph("Société bénéficiaire : " + (da != null ? da.getEntiteBeneficiaire() : "—"), FONT_CELLULE));
            blocD.addElement(new Paragraph("Service demandeur : " + (da != null && da.getServiceDemandeur() != null ? da.getServiceDemandeur().getLibelle() : "—"), FONT_CELLULE));
            String nomDemandeur = "—";
            if (da != null && da.getDemandeur() != null) {
                var sal = da.getDemandeur().getSalarie();
                nomDemandeur = (sal != null) ? (sal.getPrenom() + " " + sal.getNom()) : da.getDemandeur().getUsername();
            }
            blocD.addElement(new Paragraph("Demandeur : " + nomDemandeur, FONT_CELLULE));
            blocD.addElement(new Paragraph("Objet : " + (da != null ? da.getObjet() : "—"), FONT_CELLULE));
            blocs.addCell(blocD);

            doc.add(blocs);
            doc.add(new Paragraph(" "));

            // 4. Tableau des lignes commandées
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{8, 52, 12, 14, 14});

            table.addCell(creerCelluleHeader("N°", Element.ALIGN_CENTER));
            table.addCell(creerCelluleHeader("Désignation / Cahier des charges", Element.ALIGN_LEFT));
            table.addCell(creerCelluleHeader("Quantité", Element.ALIGN_RIGHT));
            table.addCell(creerCelluleHeader("P.U. HT (FCFA)", Element.ALIGN_RIGHT));
            table.addCell(creerCelluleHeader("Total HT (FCFA)", Element.ALIGN_RIGHT));

            int index = 1;
            if (bc.getLignes() != null) {
                for (LigneBonCommande l : bc.getLignes()) {
                    boolean pair = (index % 2 == 0);
                    Color bg = pair ? GRIS_FOND : Color.WHITE;

                    table.addCell(creerCellule(String.valueOf(index++), Element.ALIGN_CENTER, bg, FONT_CELLULE));
                    table.addCell(creerCellule(l.getDescription(), Element.ALIGN_LEFT, bg, FONT_CELLULE_GRAS));
                    table.addCell(creerCellule(l.getQuantiteCommandee() != null ? String.valueOf(l.getQuantiteCommandee()) : "1", Element.ALIGN_RIGHT, bg, FONT_CELLULE));
                    table.addCell(creerCellule(formaterMonnaie(l.getPrixUnitaireHt()), Element.ALIGN_RIGHT, bg, FONT_CELLULE));
                    table.addCell(creerCellule(formaterMonnaie(l.getMontantTotalHt()), Element.ALIGN_RIGHT, bg, FONT_CELLULE_GRAS));
                }
            }

            doc.add(table);
            doc.add(new Paragraph(" "));

            // 5. Récapitulatif financier
            PdfPTable recap = new PdfPTable(2);
            recap.setWidthPercentage(45);
            recap.setHorizontalAlignment(Element.ALIGN_RIGHT);
            recap.setWidths(new float[]{55, 45});

            recap.addCell(creerCelluleTotaux("Total Général HT :", Element.ALIGN_LEFT, FONT_CELLULE));
            recap.addCell(creerCelluleTotaux(formaterMonnaie(bc.getMontantTotalHt()) + " FCFA", Element.ALIGN_RIGHT, FONT_CELLULE_GRAS));

            BigDecimal tva = bc.getMontantTotalTtc() != null && bc.getMontantTotalHt() != null
                    ? bc.getMontantTotalTtc().subtract(bc.getMontantTotalHt()) : BigDecimal.ZERO;
            if (tva.compareTo(BigDecimal.ZERO) < 0) tva = BigDecimal.ZERO;

            recap.addCell(creerCelluleTotaux("TVA (18%) :", Element.ALIGN_LEFT, FONT_CELLULE));
            recap.addCell(creerCelluleTotaux(formaterMonnaie(tva) + " FCFA", Element.ALIGN_RIGHT, FONT_CELLULE));

            recap.addCell(creerCelluleTotaux("TOTAL GÉNÉRAL TTC :", Element.ALIGN_LEFT, FONT_SECTION));
            recap.addCell(creerCelluleTotaux(formaterMonnaie(bc.getMontantTotalTtc()) + " FCFA", Element.ALIGN_RIGHT, FONT_SECTION));

            doc.add(recap);
            doc.add(new Paragraph(" "));

            // 6. Cadre des signatures officielles (3 visas)
            PdfPTable signatures = new PdfPTable(3);
            signatures.setWidthPercentage(100);
            signatures.setWidths(new float[]{33, 33, 34});

            signatures.addCell(creerCelluleSignature("LE RESPONSABLE ACHATS", "Nom & Visa :"));
            signatures.addCell(creerCelluleSignature("LE CONTRÔLE BUDGÉTAIRE / DAF", "Visa d'engagement financier :"));
            signatures.addCell(creerCelluleSignature("LE FOURNISSEUR", "Bon pour accord, Date & Cachet :"));

            doc.add(signatures);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du Bon de Commande PDF : " + e.getMessage(), e);
        }
    }

    private static PdfPCell creerCelluleHeader(String texte, int align) {
        PdfPCell c = new PdfPCell(new Phrase(texte, FONT_ENTETE_TAB));
        c.setBackgroundColor(BLEU_UAB);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(6);
        c.setBorderColor(BLEU_UAB);
        return c;
    }

    private static PdfPCell creerCellule(String texte, int align, Color bg, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(texte != null ? texte : "", font));
        c.setBackgroundColor(bg);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(5);
        c.setBorderColor(BORDURE);
        return c;
    }

    private static PdfPCell creerCelluleTotaux(String texte, int align, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(texte, font));
        c.setHorizontalAlignment(align);
        c.setPadding(4);
        c.setBorderColor(BORDURE);
        c.setBackgroundColor(GRIS_FOND);
        return c;
    }

    private static PdfPCell creerCelluleSignature(String role, String sousTitre) {
        PdfPCell c = new PdfPCell();
        c.setBorderColor(BORDURE);
        c.setMinimumHeight(75);
        c.setPadding(6);
        c.addElement(new Paragraph(role, FONT_SECTION));
        c.addElement(new Paragraph(sousTitre, FONT_PETIT));
        return c;
    }

    private static String formaterMonnaie(BigDecimal valeur) {
        if (valeur == null) return "0";
        DecimalFormat df = new DecimalFormat("#,##0", new DecimalFormatSymbols(Locale.FRENCH));
        return df.format(valeur);
    }

    private static final class PageFooterHelper extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfPTable footer = new PdfPTable(2);
            footer.setTotalWidth(document.right() - document.left());
            footer.setWidthPercentage(100);
            try {
                footer.setWidths(new float[]{70, 30});
            } catch (DocumentException ignored) {}

            PdfPCell c1 = new PdfPCell(new Phrase("Document officiel UAB Assurances — Tout bon de commande doit être revêtu des cachets requis.", FONT_PETIT));
            c1.setBorder(Rectangle.TOP);
            c1.setBorderColor(BORDURE);
            c1.setPaddingTop(4);
            footer.addCell(c1);

            PdfPCell c2 = new PdfPCell(new Phrase("Page " + writer.getPageNumber(), FONT_PETIT));
            c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            c2.setBorder(Rectangle.TOP);
            c2.setBorderColor(BORDURE);
            c2.setPaddingTop(4);
            footer.addCell(c2);

            footer.writeSelectedRows(0, -1, document.left(), document.bottom() - 8, writer.getDirectContent());
        }
    }
}
