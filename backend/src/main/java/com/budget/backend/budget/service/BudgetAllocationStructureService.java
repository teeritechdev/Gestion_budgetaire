package com.budget.backend.budget.service;

import com.budget.backend.common.CsvUtils;
import com.budget.backend.budget.entity.BudgetAllocationActivite;
import com.budget.backend.budget.entity.BudgetAllocationStructure;
import com.budget.backend.budget.entity.ExerciceBudgetaire;
import com.budget.backend.budget.repository.BudgetAllocationActiviteRepository;
import com.budget.backend.budget.repository.BudgetAllocationStructureRepository;
import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.repository.StructureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
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

    @Transactional(readOnly = true)
    public List<BudgetAllocationStructure> listerAllocationsCsp() {
        return repository.findAll().stream()
                // Provisoire : en attendant les règles de partage, toute allocation de structure peut être répartie.
                .filter(allocation -> allocation.getStructure() != null)
                .toList();
    }

    public Optional<BudgetAllocationStructure> trouverParId(UUID id) {
        return repository.findById(id);
    }

    @Transactional
    public BudgetAllocationStructure attribuerBudgetStructure(UUID allocationActiviteId, String structureCode, BigDecimal montantVote) {
        if (allocationActiviteId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'allocation d'activité est obligatoire.");
        }
        if (structureCode == null || structureCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La structure est obligatoire.");
        }
        BudgetRules.verifierMontantPositif(montantVote, "montant voté");

        // Verrou sur l'activité parente : deux attributions simultanées ne peuvent pas dépasser l'enveloppe.
        BudgetAllocationActivite activiteAllocation = activiteAllocationRepository.findByIdForUpdate(allocationActiviteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Allocation d'activité introuvable"));
        BudgetRules.verifierExerciceModifiable(exerciceDe(activiteAllocation));
        Structure structure = structureRepo.findById(structureCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Structure introuvable"));

        BigDecimal dejaAttribue = repository.findByBudgetAllocationActiviteId(allocationActiviteId).stream()
                .map(BudgetAllocationStructure::getMontantVote)
                .map(BudgetRules::valeur)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (dejaAttribue.add(montantVote).compareTo(BudgetRules.valeur(activiteAllocation.getMontantAlloue())) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Erreur : Le cumul des budgets attribués aux structures dépasse l'enveloppe allouée à cette activité.");
        }

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
        BudgetRules.verifierMontantPositifOuNul(montantEngage, "montant engagé");
        BudgetRules.verifierMontantPositifOuNul(montantLiquide, "montant liquidé");

        // Même verrou que la validation des révisions : évite d'écraser un mouvement concurrent.
        BudgetAllocationStructure allocation = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Allocation structure introuvable"));
        BudgetRules.verifierExerciceModifiable(exerciceDe(allocation.getBudgetAllocationActivite()));

        // Disponible = (voté + modifié) - (engagé + liquidé) : il ne doit jamais devenir négatif.
        BigDecimal alloue = BudgetRules.valeur(allocation.getMontantVote())
                .add(BudgetRules.valeur(allocation.getMontantModifie()));
        if (montantEngage.add(montantLiquide).compareTo(alloue) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Le cumul engagé + liquidé (" + montantEngage.add(montantLiquide)
                            + ") dépasse le montant alloué à la structure (" + alloue + ").");
        }

        allocation.setMontantEngage(montantEngage);
        allocation.setMontantLiquide(montantLiquide);

        return repository.save(allocation);
    }

    private static ExerciceBudgetaire exerciceDe(BudgetAllocationActivite activite) {
        if (activite == null || activite.getBudgetAllocationOp() == null
                || activite.getBudgetAllocationOp().getBudgetAllocationOs() == null) {
            return null;
        }
        return activite.getBudgetAllocationOp().getBudgetAllocationOs().getExerciceBudgetaire();
    }

    // Filtrage multicritères dynamique
    public List<BudgetAllocationStructure> filtrerAllocations(String structureCode, UUID allocationActiviteId) {
        return repository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (structureCode != null) predicates.add(cb.equal(root.get("structure").get("code"), structureCode));
            if (allocationActiviteId != null) predicates.add(cb.equal(root.get("budgetAllocationActivite").get("id"), allocationActiviteId));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });
    }

    // Export CSV
    public String exporterEnCsv(String structureCode, UUID allocationActiviteId) {
        return exporterEnCsv(filtrerAllocations(structureCode, allocationActiviteId));
    }

    /** Exporte exactement la liste fournie (lignes affichées à l’écran, dans leur ordre). */
    public String exporterEnCsv(List<BudgetAllocationStructure> list) {
        StringBuilder sb = new StringBuilder("ID,Structure Code,Montant Voté,Montant Modifié,Montant Alloué,Montant Engagé,Montant Liquidé,Montant Disponible\n");
        for (BudgetAllocationStructure a : list) {
            sb.append(CsvUtils.ligne(',', a.getId(), a.getStructure() != null ? a.getStructure().getCode() : "",
                    a.getMontantVote(), a.getMontantModifie(), a.getMontantAlloue(), a.getMontantEngage(),
                    a.getMontantLiquide(), a.getMontantDisponible()));
        }
        return sb.toString();
    }

    // Export PDF (OpenPDF)
    public byte[] exporterEnPdf(String structureCode, UUID allocationActiviteId) {
        return exporterEnPdf(filtrerAllocations(structureCode, allocationActiviteId));
    }

    /** Exporte exactement la liste fournie (lignes affichées à l’écran, dans leur ordre). */
    public byte[] exporterEnPdf(List<BudgetAllocationStructure> list) {
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