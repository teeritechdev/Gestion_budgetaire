package com.budget.backend.budget.service;

import com.budget.backend.budget.entity.ObjectifStrategique;
import com.budget.backend.budget.repository.ObjectifStrategiqueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ObjectifStrategiqueService {

    @Autowired
    private ObjectifStrategiqueRepository repository;

    public List<ObjectifStrategique> listerTous() {
        return repository.findAll();
    }

    public Optional<ObjectifStrategique> trouverParId(UUID id) {
        return repository.findById(id);
    }

    @Transactional
    public ObjectifStrategique creer(ObjectifStrategique os) {
        if (repository.existsByCodeOs(os.getCodeOs())) {
            throw new RuntimeException("Un objectif stratégique avec ce code existe déjà : " + os.getCodeOs());
        }
        return repository.save(os);
    }

    @Transactional
    public ObjectifStrategique modifier(UUID id, ObjectifStrategique details) {
        ObjectifStrategique os = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Objectif stratégique introuvable avec l'ID : " + id));
        os.setLibelle(details.getLibelle());
        os.setDescription(details.getDescription());
        return repository.save(os);
    }

    @Transactional
    public void supprimer(UUID id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Objectif stratégique introuvable avec l'ID : " + id);
        }
        repository.deleteById(id);
    }

    // Filtrage multicritères dynamique
    public List<ObjectifStrategique> filtrerOs(String codeOs, String libelle, String description) {
        return repository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (codeOs != null && !codeOs.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("codeOs")), "%" + codeOs.toLowerCase() + "%"));
            }
            if (libelle != null && !libelle.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("libelle")), "%" + libelle.toLowerCase() + "%"));
            }
            if (description != null && !description.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });
    }

    // Export CSV avec filtres
    public String exporterEnCsv(String codeOs, String libelle, String description) {
        List<ObjectifStrategique> list = filtrerOs(codeOs, libelle, description);
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Code OS,Libellé,Description\n");
        for (ObjectifStrategique o : list) {
            sb.append(o.getId()).append(",")
                    .append("\"").append(o.getCodeOs()).append("\",")
                    .append("\"").append(o.getLibelle() != null ? o.getLibelle().replace("\"", "\"\"") : "").append("\",")
                    .append("\"").append(o.getDescription() != null ? o.getDescription().replace("\"", "\"\"") : "").append("\"\n");
        }
        return sb.toString();
    }

    // Export PDF avec filtres (OpenPDF)
    public byte[] exporterEnPdf(String codeOs, String libelle, String description) {
        List<ObjectifStrategique> list = filtrerOs(codeOs, libelle, description);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 16);
            document.add(new com.lowagie.text.Paragraph("Liste des Objectifs Stratégiques (OS)", titleFont));
            document.add(new com.lowagie.text.Paragraph(" "));

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(3);
            table.setWidthPercentage(100);

            table.addCell("Code OS");
            table.addCell("Libellé");
            table.addCell("Description");

            for (ObjectifStrategique o : list) {
                table.addCell(o.getCodeOs() != null ? o.getCodeOs() : "");
                table.addCell(o.getLibelle() != null ? o.getLibelle() : "");
                table.addCell(o.getDescription() != null ? o.getDescription() : "");
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des Objectifs Stratégiques", e);
        }
    }
}