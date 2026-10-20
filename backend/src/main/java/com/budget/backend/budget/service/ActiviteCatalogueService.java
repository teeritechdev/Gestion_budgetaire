package com.budget.backend.budget.service;

import com.budget.backend.budget.entity.ActiviteCatalogue;
import com.budget.backend.budget.entity.ObjectifOperationnel;
import com.budget.backend.budget.repository.ActiviteCatalogueRepository;
import com.budget.backend.budget.repository.ObjectifOperationnelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ActiviteCatalogueService {

    @Autowired
    private ActiviteCatalogueRepository repository;

    @Autowired
    private ObjectifOperationnelRepository opRepository;

    public List<ActiviteCatalogue> listerTous() {
        return repository.findAll();
    }

    public List<ActiviteCatalogue> listerParOp(UUID opId) {
        return repository.findByObjectifOperationnelId(opId);
    }

    public Optional<ActiviteCatalogue> trouverParId(UUID id) {
        return repository.findById(id);
    }

    @Transactional
    public ActiviteCatalogue creer(UUID opId, ActiviteCatalogue activite) {
        if (repository.existsByCodeActivite(activite.getCodeActivite())) {
            throw new RuntimeException("Une activité avec ce code existe déjà : " + activite.getCodeActivite());
        }
        ObjectifOperationnel op = opRepository.findById(opId)
                .orElseThrow(() -> new RuntimeException("Objectif opérationnel introuvable avec l'ID : " + opId));

        activite.setObjectifOperationnel(op);
        return repository.save(activite);
    }

    @Transactional
    public ActiviteCatalogue modifier(UUID id, UUID opId, ActiviteCatalogue details) {
        ActiviteCatalogue activite = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activité catalogue introuvable avec l'ID : " + id));

        ObjectifOperationnel op = opRepository.findById(opId)
                .orElseThrow(() -> new RuntimeException("Objectif opérationnel introuvable avec l'ID : " + opId));

        activite.setCodeActivite(details.getCodeActivite());
        activite.setLibelle(details.getLibelle());
        activite.setDescription(details.getDescription());
        activite.setObjectifOperationnel(op);

        return repository.save(activite);
    }

    @Transactional
    public void supprimer(UUID id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Activité catalogue introuvable avec l'ID : " + id);
        }
        repository.deleteById(id);
    }

    // Filtrage multicritères dynamique
    public List<ActiviteCatalogue> filtrerActivites(String codeActivite, String libelle, String description, UUID opId) {
        return repository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (codeActivite != null && !codeActivite.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("codeActivite")), "%" + codeActivite.toLowerCase() + "%"));
            }
            if (libelle != null && !libelle.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("libelle")), "%" + libelle.toLowerCase() + "%"));
            }
            if (description != null && !description.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%"));
            }
            if (opId != null) {
                predicates.add(cb.equal(root.get("objectifOperationnel").get("id"), opId));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });
    }

    // Export CSV avec filtres
    public String exporterEnCsv(String codeActivite, String libelle, String description, UUID opId) {
        List<ActiviteCatalogue> list = filtrerActivites(codeActivite, libelle, description, opId);
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Code Activité,Libellé,Description,Code OP\n");
        for (ActiviteCatalogue a : list) {
            sb.append(a.getId()).append(",")
                    .append("\"").append(a.getCodeActivite()).append("\",")
                    .append("\"").append(a.getLibelle() != null ? a.getLibelle().replace("\"", "\"\"") : "").append("\",")
                    .append("\"").append(a.getDescription() != null ? a.getDescription().replace("\"", "\"\"") : "").append("\",")
                    .append("\"").append(a.getObjectifOperationnel() != null ? a.getObjectifOperationnel().getCodeOp() : "").append("\"\n");
        }
        return sb.toString();
    }

    // Export PDF avec filtres (OpenPDF)
    public byte[] exporterEnPdf(String codeActivite, String libelle, String description, UUID opId) {
        List<ActiviteCatalogue> list = filtrerActivites(codeActivite, libelle, description, opId);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 16);
            document.add(new com.lowagie.text.Paragraph("Catalogue des Activités", titleFont));
            document.add(new com.lowagie.text.Paragraph(" "));

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(4);
            table.setWidthPercentage(100);

            table.addCell("Code Activité");
            table.addCell("Libellé");
            table.addCell("Description");
            table.addCell("Code OP");

            for (ActiviteCatalogue a : list) {
                table.addCell(a.getCodeActivite() != null ? a.getCodeActivite() : "");
                table.addCell(a.getLibelle() != null ? a.getLibelle() : "");
                table.addCell(a.getDescription() != null ? a.getDescription() : "");
                table.addCell(a.getObjectifOperationnel() != null ? a.getObjectifOperationnel().getCodeOp() : "");
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF du catalogue des activités", e);
        }
    }
}