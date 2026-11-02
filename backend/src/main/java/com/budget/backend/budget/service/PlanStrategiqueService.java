package com.budget.backend.budget.service;

import com.budget.backend.common.CsvUtils;
import com.budget.backend.budget.entity.PlanStrategique;
import com.budget.backend.budget.repository.PlanStrategiqueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PlanStrategiqueService {

    @Autowired
    private PlanStrategiqueRepository planStrategiqueRepository;

    public List<PlanStrategique> listerTous() {
        return planStrategiqueRepository.findAll();
    }

    public Optional<PlanStrategique> trouverParId(UUID id) {
        return planStrategiqueRepository.findById(id);
    }

    @Transactional
    public PlanStrategique creer(PlanStrategique plan) {
        if (planStrategiqueRepository.existsByCodePlan(plan.getCodePlan())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Un plan budgétaire avec ce code existe déjà : " + plan.getCodePlan());
        }
        validerPeriode(plan.getDateDebut(), plan.getDateFin());
        return planStrategiqueRepository.save(plan);
    }

    @Transactional
    public PlanStrategique modifier(UUID id, PlanStrategique details) {
        PlanStrategique plan = planStrategiqueRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Plan budgétaire introuvable avec l'ID : " + id));

        if (!plan.getCodePlan().equals(details.getCodePlan())
                && planStrategiqueRepository.existsByCodePlan(details.getCodePlan())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Un plan budgétaire avec ce code existe déjà : " + details.getCodePlan());
        }
        validerPeriode(details.getDateDebut(), details.getDateFin());

        plan.setCodePlan(details.getCodePlan());
        plan.setLibelle(details.getLibelle());
        plan.setDateDebut(details.getDateDebut());
        plan.setDateFin(details.getDateFin());
        plan.setActif(details.getActif());

        return planStrategiqueRepository.save(plan);
    }

    @Transactional
    public void supprimer(UUID id) {
        if (!planStrategiqueRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Plan budgétaire introuvable avec l'ID : " + id);
        }
        planStrategiqueRepository.deleteById(id);
    }

    private void validerPeriode(LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Les dates de début et de fin sont obligatoires.");
        }
        if (fin.isBefore(debut)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La date de fin doit être postérieure ou égale à la date de début.");
        }
    }

    // Filtrage multicritères dynamique
    public List<PlanStrategique> filtrerPlans(String codePlan, String libelle, Boolean actif) {
        return planStrategiqueRepository.findAll((root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (codePlan != null && !codePlan.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("codePlan")), "%" + codePlan.toLowerCase() + "%"));
            }
            if (libelle != null && !libelle.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("libelle")), "%" + libelle.toLowerCase() + "%"));
            }
            if (actif != null) {
                predicates.add(cb.equal(root.get("actif"), actif));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });
    }

    // Méthode pour export CSV tenant compte des filtres
    public String exporterEnCsv(String codePlan, String libelle, Boolean actif) {
        return exporterEnCsv(filtrerPlans(codePlan, libelle, actif));
    }

    /** Exporte exactement la liste fournie (lignes affichées à l’écran, dans leur ordre). */
    public String exporterEnCsv(List<PlanStrategique> plans) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Code Plan,Libellé,Date Début,Date Fin,Actif\n");
        for (PlanStrategique p : plans) {
            sb.append(CsvUtils.ligne(',', p.getId(), p.getCodePlan(), p.getLibelle(), p.getDateDebut(),
                    p.getDateFin(), p.getActif()));
        }
        return sb.toString();
    }
    public byte[] exporterEnPdf(String codePlan, String libelle, Boolean actif) {
        return exporterEnPdf(filtrerPlans(codePlan, libelle, actif));
    }

    /** Exporte exactement la liste fournie (lignes affichées à l’écran, dans leur ordre). */
    public byte[] exporterEnPdf(List<PlanStrategique> plans) {

        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            // Titre du document
            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 16);
            document.add(new com.lowagie.text.Paragraph("Liste des plans budgétaires", titleFont));
            document.add(new com.lowagie.text.Paragraph(" ")); // Espace

            // Tableau PDF (5 colonnes)
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(5);
            table.setWidthPercentage(100);

            // En-têtes du tableau
            table.addCell("Code Plan");
            table.addCell("Libellé");
            table.addCell("Date Début");
            table.addCell("Date Fin");
            table.addCell("Actif");

            // Remplissage des lignes
            for (PlanStrategique p : plans) {
                table.addCell(p.getCodePlan() != null ? p.getCodePlan() : "");
                table.addCell(p.getLibelle() != null ? p.getLibelle() : "");
                table.addCell(p.getDateDebut() != null ? p.getDateDebut().toString() : "");
                table.addCell(p.getDateFin() != null ? p.getDateFin().toString() : "");
                table.addCell(p.getActif() != null ? p.getActif().toString() : "");
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des plans budgétaires", e);
        }
    }
}