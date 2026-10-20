package com.budget.backend.budget.service;

import com.budget.backend.budget.entity.BudgetAllocationActivite;
import com.budget.backend.budget.entity.BudgetAllocationStructure;
import com.budget.backend.budget.repository.BudgetAllocationActiviteRepository;
import com.budget.backend.budget.repository.BudgetAllocationStructureRepository;
import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.repository.StructureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BudgetAllocationStructureService {

    @Autowired
    private BudgetAllocationStructureRepository repository;
    @Autowired
    private BudgetAllocationActiviteRepository activiteAllocationRepository;
    @Autowired
    private StructureRepository structureRepo;

    public List<BudgetAllocationStructure> listerToutes() {
        return repository.findAll();
    }

    public Optional<BudgetAllocationStructure> trouverParId(UUID id) {
        return repository.findById(id);
    }

    @Transactional
    public BudgetAllocationStructure attribuerBudgetStructure(UUID allocationActiviteId, UUID structureId, BigDecimal montantVote) {
        BudgetAllocationActivite activiteAllocation = activiteAllocationRepository.findById(allocationActiviteId)
                .orElseThrow(() -> new RuntimeException("Allocation d'activité introuvable"));
        Structure structure = structureRepo.findById(structureId)
                .orElseThrow(() -> new RuntimeException("Structure introuvable"));

        BudgetAllocationStructure allocation = BudgetAllocationStructure.builder()
                .budgetAllocationActivite(activiteAllocation)
                .structure(structure)
                .montantVote(montantVote)
                .montantModifie(BigDecimal.ZERO)
                .montantEngage(BigDecimal.ZERO)
                .montantLiquide(BigDecimal.ZERO)
                .build();

        return repository.save(allocation);
    }

    @Transactional
    public BudgetAllocationStructure mettreAJourExecution(UUID id, BigDecimal montantEngage, BigDecimal montantLiquide) {
        BudgetAllocationStructure allocation = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Allocation structure introuvable"));

        allocation.setMontantEngage(montantEngage);
        allocation.setMontantLiquide(montantLiquide);

        return repository.save(allocation);
    }

    // Filtrage multicritères dynamique
    public List<BudgetAllocationStructure> filtrerAllocations(UUID structureId, UUID allocationActiviteId) {
        return repository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (structureId != null) predicates.add(cb.equal(root.get("structure").get("id"), structureId));
            if (allocationActiviteId != null) predicates.add(cb.equal(root.get("budgetAllocationActivite").get("id"), allocationActiviteId));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });
    }

    // Export CSV
    public String exporterEnCsv(UUID structureId, UUID allocationActiviteId) {
        List<BudgetAllocationStructure> list = filtrerAllocations(structureId, allocationActiviteId);
        StringBuilder sb = new StringBuilder("ID,Structure Code,Montant Voté,Montant Modifié,Montant Alloué,Montant Engagé,Montant Liquidé,Montant Disponible\n");
        for (BudgetAllocationStructure a : list) {
            sb.append(a.getId()).append(",")
                    .append("\"").append(a.getStructure() != null ? a.getStructure().getCode() : "").append("\",")
                    .append(a.getMontantVote()).append(",")
                    .append(a.getMontantModifie()).append(",")
                    .append(a.getMontantAlloue()).append(",")
                    .append(a.getMontantEngage()).append(",")
                    .append(a.getMontantLiquide()).append(",")
                    .append(a.getMontantDisponible()).append("\n");
        }
        return sb.toString();
    }

    // Export PDF (OpenPDF)
    public byte[] exporterEnPdf(UUID structureId, UUID allocationActiviteId) {
        List<BudgetAllocationStructure> list = filtrerAllocations(structureId, allocationActiviteId);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document doc = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new com.lowagie.text.Paragraph("Suivi de l'Exécution par Structure"));
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(6);
            table.addCell("Structure"); table.addCell("Voté"); table.addCell("Alloué");
            table.addCell("Engagé"); table.addCell("Liquidé"); table.addCell("Disponible");
            for (BudgetAllocationStructure a : list) {
                table.addCell(a.getStructure() != null ? a.getStructure().getCode() : "");
                table.addCell(a.getMontantVote() != null ? a.getMontantVote().toString() : "");
                table.addCell(a.getMontantAlloue() != null ? a.getMontantAlloue().toString() : "");
                table.addCell(a.getMontantEngage() != null ? a.getMontantEngage().toString() : "");
                table.addCell(a.getMontantLiquide() != null ? a.getMontantLiquide().toString() : "");
                table.addCell(a.getMontantDisponible() != null ? a.getMontantDisponible().toString() : "");
            }
            doc.add(table); doc.close();
            return out.toByteArray();
        } catch (Exception e) { throw new RuntimeException("Erreur PDF", e); }
    }
}