package com.budget.backend.security.service;

import com.budget.backend.security.entity.AuditLog;
import com.budget.backend.security.repository.AuditLogRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    // --- Lecture ---
    public List<AuditLog> listerTousLesLogs() {
        return auditLogRepository.findAll();
    }

    public Optional<AuditLog> trouverParId(UUID id) {
        return auditLogRepository.findById(id);
    }

    // --- Filtrage multicritères avancé ---
    public List<AuditLog> filtrerAuditLogs(UUID utilisateurId, String typeAction, String nomTable, LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findAll().stream()
                .filter(log -> utilisateurId == null || (log.getUtilisateurId() != null && log.getUtilisateurId().equals(utilisateurId)))
                .filter(log -> typeAction == null || log.getTypeAction().equalsIgnoreCase(typeAction))
                .filter(log -> nomTable == null || log.getNomTable().equalsIgnoreCase(nomTable))
                .filter(log -> startDate == null || (log.getCreatedAt() != null && !log.getCreatedAt().isBefore(startDate)))
                .filter(log -> endDate == null || (log.getCreatedAt() != null && !log.getCreatedAt().isAfter(endDate)))
                .collect(Collectors.toList());
    }

    // --- Export CSV ---
    public String exporterEnCsv(UUID utilisateurId, String typeAction, String nomTable, LocalDateTime startDate, LocalDateTime endDate) {
        List<AuditLog> logs = filtrerAuditLogs(utilisateurId, typeAction, nomTable, startDate, endDate);
        StringBuilder csv = new StringBuilder("ID;UtilisateurID;TypeAction;NomTable;EnregistrementID;AdresseIP;Date\n");
        for (AuditLog log : logs) {
            csv.append(log.getId()).append(";")
                    .append(log.getUtilisateurId() != null ? log.getUtilisateurId() : "SYSTEM").append(";")
                    .append(log.getTypeAction()).append(";")
                    .append(log.getNomTable()).append(";")
                    .append(log.getEnregistrementId()).append(";")
                    .append(log.getAdresseIp() != null ? log.getAdresseIp() : "").append(";")
                    .append(log.getCreatedAt()).append("\n");
        }
        return csv.toString();
    }

    // --- Export PDF ---
    public byte[] exporterEnPdf(UUID utilisateurId, String typeAction, String nomTable, LocalDateTime startDate, LocalDateTime endDate) {
        List<AuditLog> logs = filtrerAuditLogs(utilisateurId, typeAction, nomTable, startDate, endDate);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            Document document = new Document(PageSize.A4.rotate()); // Format paysage pour le journal d'audit
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Journal Officiel d'Audit & Traçabilité - UAB", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 3f, 3f, 3f, 3f});

            table.addCell("Action");
            table.addCell("Table");
            table.addCell("ID Enregistrement");
            table.addCell("Adresse IP");
            table.addCell("Horodatage");

            for (AuditLog log : logs) {
                table.addCell(log.getTypeAction());
                table.addCell(log.getNomTable());
                table.addCell(log.getEnregistrementId());
                table.addCell(log.getAdresseIp() != null ? log.getAdresseIp() : "");
                table.addCell(log.getCreatedAt() != null ? log.getCreatedAt().toString() : "");
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF des logs d'audit : " + e.getMessage());
        }

        return out.toByteArray();
    }
}