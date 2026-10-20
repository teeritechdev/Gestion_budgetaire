package com.budget.backend.budget.service;

import com.budget.backend.budget.entity.ObjectifOperationnel;
import com.budget.backend.budget.entity.ObjectifStrategique;
import com.budget.backend.budget.repository.ObjectifOperationnelRepository;
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
public class ObjectifOperationnelService {

    @Autowired
    private ObjectifOperationnelRepository repository;

    @Autowired
    private ObjectifStrategiqueRepository osRepository;

    public List<ObjectifOperationnel> listerTous() {
        return repository.findAll();
    }

    public List<ObjectifOperationnel> listerParOs(UUID osId) {
        return repository.findByObjetifStrategiqueId(osId);
    }

    public Optional<ObjectifOperationnel> trouverParId(UUID id) {
        return repository.findById(id);
    }

    @Transactional
    public ObjectifOperationnel creer(UUID osId, ObjectifOperationnel op) {
        if (repository.existsByCodeOp(op.getCodeOp())) {
            throw new RuntimeException("Un objectif opérationnel avec ce code existe déjà : " + op.getCodeOp());
        }
        ObjectifStrategique os = osRepository.findById(osId)
                .orElseThrow(() -> new RuntimeException("Objectif stratégique introuvable avec l'ID : " + osId));

        op.setObjectifStrategique(os);
        return repository.save(op);
    }

    @Transactional
    public ObjectifOperationnel modifier(UUID id, UUID osId, ObjectifOperationnel details) {
        ObjectifOperationnel op = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Objectif opérationnel introuvable avec l'ID : " + id));

        ObjectifStrategique os = osRepository.findById(osId)
                .orElseThrow(() -> new RuntimeException("Objectif stratégique introuvable avec l'ID : " + osId));

        op.setCodeOp(details.getCodeOp());
        op.setLibelle(details.getLibelle());
        op.setDescription(details.getDescription());
        op.setObjectifStrategique(os);

        return repository.save(op);
    }

    @Transactional
    public void supprimer(UUID id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Objectif opérationnel introuvable avec l'ID : " + id);
        }
        repository.deleteById(id);
    }

    // Filtrage multicritères dynamique
    public List<ObjectifOperationnel> filtrerOp(String codeOp, String libelle, String description, UUID osId) {
        return repository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (codeOp != null && !codeOp.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("codeOp")), "%" + codeOp.toLowerCase() + "%"));
            }
            if (libelle != null && !libelle.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("libelle")), "%" + libelle.toLowerCase() + "%"));
            }
            if (description != null && !description.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%"));
            }
            if (osId != null) {
                predicates.add(cb.equal(root.get("objectifStrategique").get("id"), osId));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });
    }

    // Export CSV avec filtres
    public String exporterEnCsv(String codeOp, String libelle, String description, UUID osId) {
        List<ObjectifOperationnel> list = filtrerOp(codeOp, libelle, description, osId);
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Code OP,Libellé,Description,Code OS\n");
        for (ObjectifOperationnel o : list) {
            sb.append(o.getId()).append(",")
                    .append("\"").append(o.getCodeOp()).append("\",")
                    .append("\"").append(o.getLibelle() != null ? o.getLibelle().replace("\"", "\"\"") : "").append("\",")
                    .append("\"").append(o.getDescription() != null ? o.getDescription().replace("\"", "\"\"") : "").append("\",")
                    .append("\"").append(o.getObjectifStrategique() != null ? o.getObjectifStrategique().getCodeOs() : "").append("\"\n");
        }
        return sb.toString();
    }

    // Export PDF avec filtres (OpenPDF)
    public byte[] exporterEnPdf(String codeOp, String libelle, String description, UUID osId) {
        List<ObjectifOperationnel> list = filtrerOp(codeOp, libelle, description, osId);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 16);
            document.add(new com.lowagie.text.Paragraph("Liste des Objectifs Opérationnels (OP)", titleFont));
            document.add(new com.lowagie.text.Paragraph(" "));

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(4);
            table.setWidthPercentage(100);

            table.addCell("Code OP");
            table.addCell("Libellé");
            table.addCell("Description");
            table.addCell("Code OS");

            for (ObjectifOperationnel o : list) {
                table.addCell(o.getCodeOp() != null ? o.getCodeOp() : "");
                table.addCell(o.getLibelle() != null ? o.getLibelle() : "");
                table.addCell(o.getDescription() != null ? o.getDescription() : "");
                table.addCell(o.getObjectifStrategique() != null ? o.getObjectifStrategique().getCodeOs() : "");
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des Objectifs Opérationnels", e);
        }
    }
}