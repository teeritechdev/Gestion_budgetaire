package com.budget.backend.budget.service;

import com.budget.backend.budget.entity.*;
import com.budget.backend.budget.repository.*;
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
public class CascadeAllocationService {

    @Autowired
    private ExerciceBudgetaireRepository exerciceRepository;
    @Autowired
    private ObjectifStrategiqueRepository osRepository;
    @Autowired
    private ObjectifOperationnelRepository opRepository;
    @Autowired
    private ActiviteCatalogueRepository activiteCatalogueRepository;

    @Autowired
    private BudgetAllocationOsRepository allocationOsRepository;
    @Autowired
    private BudgetAllocationOpRepository allocationOpRepository;
    @Autowired
    private BudgetAllocationActiviteRepository allocationActiviteRepository;

    // --- NIVEAU 1 : Exercice -> OS ---
    public List<BudgetAllocationOs> listerAllocationsOs() { return allocationOsRepository.findAll(); }
    public Optional<BudgetAllocationOs> trouverAllocationOsParId(UUID id) { return allocationOsRepository.findById(id); }

    @Transactional
    public BudgetAllocationOs allouerExerciceVersOs(UUID exerciceId, UUID osId, BigDecimal montant) {
        ExerciceBudgetaire exercice = exerciceRepository.findById(exerciceId)
                .orElseThrow(() -> new RuntimeException("Exercice budgétaire introuvable"));
        ObjectifStrategique os = osRepository.findById(osId)
                .orElseThrow(() -> new RuntimeException("Objectif stratégique introuvable"));

        BigDecimal totalActuel = allocationOsRepository.findByExerciceBudgetaireId(exerciceId)
                .stream().map(BudgetAllocationOs::getMontantAlloue).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalActuel.add(montant).compareTo(exercice.getMontantTotalVote()) > 0) {
            throw new RuntimeException("Erreur : Le cumul des allocations OS dépasse l'enveloppe globale votée de l'exercice.");
        }

        BudgetAllocationOs allocation = BudgetAllocationOs.builder()
                .exerciceBudgetaire(exercice)
                .objectifStrategique(os)
                .montantAlloue(montant)
                .build();
        return allocationOsRepository.save(allocation);
    }

    public List<BudgetAllocationOs> filtrerAllocationsOs(UUID exerciceId, UUID osId) {
        return allocationOsRepository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (exerciceId != null) predicates.add(cb.equal(root.get("exerciceBudgetaire").get("id"), exerciceId));
            if (osId != null) predicates.add(cb.equal(root.get("objectifStrategique").get("id"), osId));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });
    }

    public String exporterOsCsv(UUID exerciceId, UUID osId) {
        List<BudgetAllocationOs> list = filtrerAllocationsOs(exerciceId, osId);
        StringBuilder sb = new StringBuilder("ID,Exercice Année,OS Code,Montant Alloué\n");
        for (BudgetAllocationOs a : list) {
            sb.append(a.getId()).append(",")
                    .append(a.getExerciceBudgetaire() != null ? a.getExerciceBudgetaire().getAnnee() : "").append(",")
                    .append("\"").append(a.getObjectifStrategique() != null ? a.getObjectifStrategique().getCodeOs() : "").append("\",")
                    .append(a.getMontantAlloue()).append("\n");
        }
        return sb.toString();
    }

    public byte[] exporterOsPdf(UUID exerciceId, UUID osId) {
        List<BudgetAllocationOs> list = filtrerAllocationsOs(exerciceId, osId);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document doc = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new com.lowagie.text.Paragraph("Allocations Budgetaires - Niveau OS"));
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(3);
            table.addCell("Exercice Année"); table.addCell("Code OS"); table.addCell("Montant Alloué");
            for (BudgetAllocationOs a : list) {
                table.addCell(a.getExerciceBudgetaire() != null ? a.getExerciceBudgetaire().getAnnee().toString() : "");
                table.addCell(a.getObjectifStrategique() != null ? a.getObjectifStrategique().getCodeOs() : "");
                table.addCell(a.getMontantAlloue() != null ? a.getMontantAlloue().toString() : "");
            }
            doc.add(table); doc.close();
            return out.toByteArray();
        } catch (Exception e) { throw new RuntimeException("Erreur PDF", e); }
    }

    // --- NIVEAU 2 : OS -> OP ---
    public List<BudgetAllocationOp> listerAllocationsOp() { return allocationOpRepository.findAll(); }
    public Optional<BudgetAllocationOp> trouverAllocationOpParId(UUID id) { return allocationOpRepository.findById(id); }

    @Transactional
    public BudgetAllocationOp allouerOsVersOp(UUID allocationOsId, UUID opId, BigDecimal montant) {
        BudgetAllocationOs parentOs = allocationOsRepository.findById(allocationOsId)
                .orElseThrow(() -> new RuntimeException("Allocation OS parente introuvable"));
        ObjectifOperationnel op = opRepository.findById(opId)
                .orElseThrow(() -> new RuntimeException("Objectif opérationnel introuvable"));

        BigDecimal totalActuel = allocationOpRepository.findByBudgetAllocationOsId(allocationOsId)
                .stream().map(BudgetAllocationOp::getMontantAlloue).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalActuel.add(montant).compareTo(parentOs.getMontantAlloue()) > 0) {
            throw new RuntimeException("Erreur : Le cumul des allocations OP dépasse l'enveloppe allouée à cet OS.");
        }

        BudgetAllocationOp allocation = BudgetAllocationOp.builder()
                .budgetAllocationOs(parentOs)
                .objectifOperationnel(op)
                .montantAlloue(montant)
                .build();
        return allocationOpRepository.save(allocation);
    }

    public List<BudgetAllocationOp> filtrerAllocationsOp(UUID allocationOsId, UUID opId) {
        return allocationOpRepository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (allocationOsId != null) predicates.add(cb.equal(root.get("budgetAllocationOs").get("id"), allocationOsId));
            if (opId != null) predicates.add(cb.equal(root.get("objectifOperationnel").get("id"), opId));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });
    }

    public String exporterOpCsv(UUID allocationOsId, UUID opId) {
        List<BudgetAllocationOp> list = filtrerAllocationsOp(allocationOsId, opId);
        StringBuilder sb = new StringBuilder("ID,Allocation OS ID,OP Code,Montant Alloué\n");
        for (BudgetAllocationOp a : list) {
            sb.append(a.getId()).append(",")
                    .append(a.getBudgetAllocationOs() != null ? a.getBudgetAllocationOs().getId() : "").append(",")
                    .append("\"").append(a.getObjectifOperationnel() != null ? a.getObjectifOperationnel().getCodeOp() : "").append("\",")
                    .append(a.getMontantAlloue()).append("\n");
        }
        return sb.toString();
    }

    public byte[] exporterOpPdf(UUID allocationOsId, UUID opId) {
        List<BudgetAllocationOp> list = filtrerAllocationsOp(allocationOsId, opId);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document doc = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new com.lowagie.text.Paragraph("Allocations Budgetaires - Niveau OP"));
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(3);
            table.addCell("ID Allocation OS"); table.addCell("Code OP"); table.addCell("Montant Alloué");
            for (BudgetAllocationOp a : list) {
                table.addCell(a.getBudgetAllocationOs() != null ? a.getBudgetAllocationOs().getId().toString() : "");
                table.addCell(a.getObjectifOperationnel() != null ? a.getObjectifOperationnel().getCodeOp() : "");
                table.addCell(a.getMontantAlloue() != null ? a.getMontantAlloue().toString() : "");
            }
            doc.add(table); doc.close();
            return out.toByteArray();
        } catch (Exception e) { throw new RuntimeException("Erreur PDF", e); }
    }

    // --- NIVEAU 3 : OP -> Activité ---
    public List<BudgetAllocationActivite> listerAllocationsActivite() { return allocationActiviteRepository.findAll(); }
    public Optional<BudgetAllocationActivite> trouverAllocationActiviteParId(UUID id) { return allocationActiviteRepository.findById(id); }

    @Transactional
    public BudgetAllocationActivite allouerOpVersActivite(UUID allocationOpId, UUID activiteCatalogueId, BigDecimal montant) {
        BudgetAllocationOp parentOp = allocationOpRepository.findById(allocationOpId)
                .orElseThrow(() -> new RuntimeException("Allocation OP parente introuvable"));
        ActiviteCatalogue activite = activiteCatalogueRepository.findById(activiteCatalogueId)
                .orElseThrow(() -> new RuntimeException("Activité catalogue introuvable"));

        BigDecimal totalActuel = allocationActiviteRepository.findByBudgetAllocationOpId(allocationOpId)
                .stream().map(BudgetAllocationActivite::getMontantAlloue).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalActuel.add(montant).compareTo(parentOp.getMontantAlloue()) > 0) {
            throw new RuntimeException("Erreur : Le cumul des allocations d'activités dépasse l'enveloppe allouée à cet OP.");
        }

        BudgetAllocationActivite allocation = BudgetAllocationActivite.builder()
                .budgetAllocationOp(parentOp)
                .activiteCatalogue(activite)
                .montantAlloue(montant)
                .build();
        return allocationActiviteRepository.save(allocation);
    }

    public List<BudgetAllocationActivite> filtrerAllocationsActivite(UUID allocationOpId, UUID activiteCatalogueId) {
        return allocationActiviteRepository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (allocationOpId != null) predicates.add(cb.equal(root.get("budgetAllocationOp").get("id"), allocationOpId));
            if (activiteCatalogueId != null) predicates.add(cb.equal(root.get("activiteCatalogue").get("id"), activiteCatalogueId));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });
    }

    public String exporterActiviteCsv(UUID allocationOpId, UUID activiteCatalogueId) {
        List<BudgetAllocationActivite> list = filtrerAllocationsActivite(allocationOpId, activiteCatalogueId);
        StringBuilder sb = new StringBuilder("ID,Allocation OP ID,Activité Code,Montant Alloué\n");
        for (BudgetAllocationActivite a : list) {
            sb.append(a.getId()).append(",")
                    .append(a.getBudgetAllocationOp() != null ? a.getBudgetAllocationOp().getId() : "").append(",")
                    .append("\"").append(a.getActiviteCatalogue() != null ? a.getActiviteCatalogue().getCodeActivite() : "").append("\",")
                    .append(a.getMontantAlloue()).append("\n");
        }
        return sb.toString();
    }

    public byte[] exporterActivitePdf(UUID allocationOpId, UUID activiteCatalogueId) {
        List<BudgetAllocationActivite> list = filtrerAllocationsActivite(allocationOpId, activiteCatalogueId);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document doc = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new com.lowagie.text.Paragraph("Allocations Budgetaires - Niveau Activité"));
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(3);
            table.addCell("ID Allocation OP"); table.addCell("Code Activité"); table.addCell("Montant Alloué");
            for (BudgetAllocationActivite a : list) {
                table.addCell(a.getBudgetAllocationOp() != null ? a.getBudgetAllocationOp().getId().toString() : "");
                table.addCell(a.getActiviteCatalogue() != null ? a.getActiviteCatalogue().getCodeActivite() : "");
                table.addCell(a.getMontantAlloue() != null ? a.getMontantAlloue().toString() : "");
            }
            doc.add(table); doc.close();
            return out.toByteArray();
        } catch (Exception e) { throw new RuntimeException("Erreur PDF", e); }
    }
}