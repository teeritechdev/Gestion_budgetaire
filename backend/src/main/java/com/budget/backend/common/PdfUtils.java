package com.budget.backend.common;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Génération d'un PDF tabulaire simple et homogène (titre, date d'édition, tableau à en-têtes). */
public final class PdfUtils {

    private static final DateTimeFormatter HORODATAGE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private PdfUtils() {
    }

    public static byte[] tableau(String titre, List<String> entetes, List<List<Object>> lignes) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(entetes.size() > 6 ? PageSize.A4.rotate() : PageSize.A4, 28, 28, 32, 32);
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15);
            Font fontInfo = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
            Font fontEntete = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            Font fontCellule = FontFactory.getFont(FontFactory.HELVETICA, 8);

            document.add(new Paragraph(titre, fontTitre));
            document.add(new Paragraph("Édité le " + LocalDateTime.now().format(HORODATAGE)
                    + " · " + lignes.size() + " enregistrement(s)", fontInfo));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(entetes.size());
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            for (String entete : entetes) {
                PdfPCell cell = new PdfPCell(new Phrase(entete, fontEntete));
                cell.setBackgroundColor(new Color(21, 128, 61));
                cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                cell.setPadding(4);
                table.addCell(cell);
            }
            for (List<Object> ligne : lignes) {
                for (Object valeur : ligne) {
                    PdfPCell cell = new PdfPCell(new Phrase(valeur == null ? "" : valeur.toString(), fontCellule));
                    cell.setPadding(3);
                    table.addCell(cell);
                }
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Erreur lors de la génération du PDF « " + titre + " ».", exception);
        }
    }
}
