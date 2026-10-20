package com.budget.backend.budget.service;

import com.budget.backend.budget.entity.JournalMouvementsBudgetaire;
import com.budget.backend.budget.repository.JournalMouvementsBudgetaireRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class JournalMouvementsBudgetaireService {

    @Autowired
    private JournalMouvementsBudgetaireRepository repository;

    public List<JournalMouvementsBudgetaire> listerTous() {
        return repository.findAll();
    }

    public List<JournalMouvementsBudgetaire> filtrerMouvements(Integer anneeExercice, UUID allocationStructureId, String typeMouvement) {
        return repository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (anneeExercice != null) predicates.add(cb.equal(root.get("anneeExercice"), anneeExercice));
            if (allocationStructureId != null) predicates.add(cb.equal(root.get("allocationStructure").get("id"), allocationStructureId));
            if (typeMouvement != null && !typeMouvement.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("typeMouvement")), "%" + typeMouvement.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });
    }

    public String exporterEnCsv(Integer anneeExercice, UUID allocationStructureId, String typeMouvement) {
        List<JournalMouvementsBudgetaire> list = filtrerMouvements(anneeExercice, allocationStructureId, typeMouvement);
        StringBuilder sb = new StringBuilder("ID,Exercice,Type Mouvement,Montant Antérieur,Variation,Nouveau Montant,Référence\n");
        for (JournalMouvementsBudgetaire j : list) {
            sb.append(j.getId()).append(",")
                    .append(j.getAnneeExercice()).append(",")
                    .append("\"").append(j.getTypeMouvement()).append("\",")
                    .append(j.getMontantAnterieur()).append(",")
                    .append(j.getVariation()).append(",")
                    .append(j.getNouveauMontant()).append(",")
                    .append("\"").append(j.getReferenceDocument() != null ? j.getReferenceDocument() : "").append("\"\n");
        }
        return sb.toString();
    }

    public byte[] exporterEnPdf(Integer anneeExercice, UUID allocationStructureId, String typeMouvement) {
        List<JournalMouvementsBudgetaire> list = filtrerMouvements(anneeExercice, allocationStructureId, typeMouvement);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document doc = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new com.lowagie.text.Paragraph("Journal des Mouvements Budgétaires (Piste d'Audit)"));
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(5);
            table.addCell("Exercice"); table.addCell("Type"); table.addCell("Variation"); table.addCell("Nouveau Montant"); table.addCell("Référence");
            for (JournalMouvementsBudgetaire j : list) {
                table.addCell(j.getAnneeExercice() != null ? j.getAnneeExercice().toString() : "");
                table.addCell(j.getTypeMouvement() != null ? j.getTypeMouvement() : "");
                table.addCell(j.getVariation() != null ? j.getVariation().toString() : "");
                table.addCell(j.getNouveauMontant() != null ? j.getNouveauMontant().toString() : "");
                table.addCell(j.getReferenceDocument() != null ? j.getReferenceDocument() : "");
            }
            doc.add(table); doc.close();
            return out.toByteArray();
        } catch (Exception e) { throw new RuntimeException("Erreur PDF", e); }
    }
}