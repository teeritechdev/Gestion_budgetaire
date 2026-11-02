package com.budget.backend.budget.service;

import com.budget.backend.budget.entity.ExerciceBudgetaire;
import com.budget.backend.budget.entity.PlanStrategique;
import com.budget.backend.budget.repository.ExerciceBudgetaireRepository;
import com.budget.backend.budget.repository.PlanStrategiqueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ExerciceBudgetaireService {

    @Autowired
    private ExerciceBudgetaireRepository repository;

    @Autowired
    private PlanStrategiqueRepository planRepository;

    public List<ExerciceBudgetaire> listerTous() {
        return repository.findAll();
    }

    public Optional<ExerciceBudgetaire> trouverParId(UUID id) {
        return repository.findById(id);
    }

    @Transactional
    public ExerciceBudgetaire creer(UUID planId, ExerciceBudgetaire exercice) {
        if (repository.existsByAnnee(exercice.getAnnee())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Un exercice budgétaire existe déjà pour l'année : " + exercice.getAnnee());
        }

        PlanStrategique plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Plan budgétaire introuvable avec l'ID : " + planId));

        validerAnneeDansPeriode(exercice.getAnnee(), plan);

        exercice.setPlanStrategique(plan);
        if (exercice.getStatut() == null) {
            exercice.setStatut("PREPARATION");
        }
        return repository.save(exercice);
    }

    @Transactional
    public ExerciceBudgetaire modifier(UUID id, UUID planId, ExerciceBudgetaire details) {
        ExerciceBudgetaire exercice = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Exercice budgétaire introuvable avec l'ID : " + id));

        PlanStrategique plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Plan budgétaire introuvable avec l'ID : " + planId));

        validerAnneeDansPeriode(details.getAnnee(), plan);

        exercice.setAnnee(details.getAnnee());
        exercice.setPlanStrategique(plan);
        exercice.setStatut(details.getStatut());
        exercice.setMontantTotalVote(details.getMontantTotalVote());
        exercice.setDateOuverture(details.getDateOuverture());
        exercice.setDateFermeture(details.getDateFermeture());

        return repository.save(exercice);
    }

    @Transactional
    public void supprimer(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Exercice budgétaire introuvable avec l'ID : " + id);
        }
        repository.deleteById(id);
    }

    private void validerAnneeDansPeriode(Integer annee, PlanStrategique plan) {
        if (annee == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'année de l'exercice est obligatoire.");
        }
        if (plan.getDateDebut() == null || plan.getDateFin() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le plan budgétaire sélectionné doit avoir une date de début et une date de fin.");
        }

        int anneeDebut = plan.getDateDebut().getYear();
        int anneeFin = plan.getDateFin().getYear();
        if (annee < anneeDebut || annee > anneeFin) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format(
                            "L'année de l'exercice (%d) n'est pas comprise dans la période du plan budgétaire (%d - %d). Modifiez les dates du plan ou choisissez une année comprise dans cette période.",
                            annee, anneeDebut, anneeFin));
        }
    }

    // Filtrage multicritères dynamique
    public List<ExerciceBudgetaire> filtrerExercices(Integer annee, String statut, UUID planId) {
        return repository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (annee != null) {
                predicates.add(cb.equal(root.get("annee"), annee));
            }
            if (statut != null && !statut.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("statut")), "%" + statut.toLowerCase() + "%"));
            }
            if (planId != null) {
                predicates.add(cb.equal(root.get("planStrategique").get("id"), planId));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });
    }

    // Export CSV avec filtres
    public String exporterEnCsv(Integer annee, String statut, UUID planId) {
        return exporterEnCsv(filtrerExercices(annee, statut, planId));
    }

    /** Exporte exactement la liste fournie (lignes affichées à l’écran, dans leur ordre). */
    public String exporterEnCsv(List<ExerciceBudgetaire> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Année,Statut,Montant Total Voté,Date Ouverture,Date Fermeture,Code Plan\n");
        for (ExerciceBudgetaire e : list) {
            sb.append(e.getId()).append(",")
                    .append(e.getAnnee()).append(",")
                    .append("\"").append(e.getStatut()).append("\",")
                    .append(e.getMontantTotalVote()).append(",")
                    .append(e.getDateOuverture() != null ? e.getDateOuverture() : "").append(",")
                    .append(e.getDateFermeture() != null ? e.getDateFermeture() : "").append(",")
                    .append("\"").append(e.getPlanStrategique() != null ? e.getPlanStrategique().getCodePlan() : "").append("\"\n");
        }
        return sb.toString();
    }

    // Export PDF avec filtres (OpenPDF)
    public byte[] exporterEnPdf(Integer annee, String statut, UUID planId) {
        return exporterEnPdf(filtrerExercices(annee, statut, planId));
    }

    /** Exporte exactement la liste fournie (lignes affichées à l’écran, dans leur ordre). */
    public byte[] exporterEnPdf(List<ExerciceBudgetaire> list) {

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 16);
            document.add(new com.lowagie.text.Paragraph("Liste des Exercices Budgétaires", titleFont));
            document.add(new com.lowagie.text.Paragraph(" "));

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(5);
            table.setWidthPercentage(100);

            table.addCell("Année");
            table.addCell("Statut");
            table.addCell("Montant Voté");
            table.addCell("Ouverture");
            table.addCell("Code Plan");

            for (ExerciceBudgetaire e : list) {
                table.addCell(e.getAnnee() != null ? e.getAnnee().toString() : "");
                table.addCell(e.getStatut() != null ? e.getStatut() : "");
                table.addCell(e.getMontantTotalVote() != null ? e.getMontantTotalVote().toString() : "");
                table.addCell(e.getDateOuverture() != null ? e.getDateOuverture().toString() : "");
                table.addCell(e.getPlanStrategique() != null ? e.getPlanStrategique().getCodePlan() : "");
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des exercices budgétaires", e);
        }
    }
}