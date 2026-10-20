package com.budget.backend.budget.service;

import com.budget.backend.budget.dto.RepartitionCspRuleRequestDto;
import com.budget.backend.budget.entity.RepartitionCspRule;
import com.budget.backend.budget.repository.RepartitionCspRuleRepository;
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
public class RepartitionCspRuleService {

    @Autowired
    private RepartitionCspRuleRepository repository;

    public List<RepartitionCspRule> listerTous() {
        return repository.findAll();
    }

    public Optional<RepartitionCspRule> trouverParId(UUID id) {
        return repository.findById(id);
    }

    @Transactional
    public RepartitionCspRule creer(RepartitionCspRuleRequestDto dto) {
        RepartitionCspRule rule = RepartitionCspRule.builder()
                .activiteCspId(dto.getActiviteCspId())
                .entiteBeneficiaire(dto.getEntiteBeneficiaire())
                .activiteBeneficiaireId(dto.getActiviteBeneficiaireId())
                .cleType(dto.getCleType())
                .valeurCle(dto.getValeurCle())
                .build();

        return repository.save(rule);
    }

    @Transactional
    public RepartitionCspRule modifier(UUID id, RepartitionCspRuleRequestDto dto) {
        RepartitionCspRule rule = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Règle CSP introuvable avec l'ID : " + id));

        rule.setActiviteCspId(dto.getActiviteCspId());
        rule.setEntiteBeneficiaire(dto.getEntiteBeneficiaire());
        rule.setActiviteBeneficiaireId(dto.getActiviteBeneficiaireId());
        rule.setCleType(dto.getCleType());
        rule.setValeurCle(dto.getValeurCle());

        return repository.save(rule);
    }

    @Transactional
    public void supprimer(UUID id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Règle CSP introuvable avec l'ID : " + id);
        }
        repository.deleteById(id);
    }

    // Filtrage multicritères dynamique
    public List<RepartitionCspRule> filtrer(String entiteBeneficiaire, String cleType) {
        return repository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (entiteBeneficiaire != null && !entiteBeneficiaire.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("entiteBeneficiaire")), "%" + entiteBeneficiaire.trim().toLowerCase() + "%"));
            }
            if (cleType != null && !cleType.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("cleType")), "%" + cleType.trim().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });
    }

    // Export CSV avec filtres
    public String exporterEnCsv(String entiteBeneficiaire, String cleType) {
        List<RepartitionCspRule> list = filtrer(entiteBeneficiaire, cleType);
        StringBuilder sb = new StringBuilder();
        sb.append("ID,ID Activité CSP,Entité Bénéficiaire,ID Activité Bénéficiaire,Type Clé,Valeur Clé\n");
        for (RepartitionCspRule r : list) {
            sb.append(r.getId()).append(",")
                    .append(r.getActiviteCspId()).append(",")
                    .append("\"").append(r.getEntiteBeneficiaire()).append("\",")
                    .append(r.getActiviteBeneficiaireId()).append(",")
                    .append("\"").append(r.getCleType() != null ? r.getCleType() : "").append("\",")
                    .append(r.getValeurCle() != null ? r.getValeurCle() : BigDecimal.ZERO).append("\n");
        }
        return sb.toString();
    }

    // Export PDF avec filtres (OpenPDF)
    public byte[] exporterEnPdf(String entiteBeneficiaire, String cleType) {
        List<RepartitionCspRule> list = filtrer(entiteBeneficiaire, cleType);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 16);
            document.add(new com.lowagie.text.Paragraph("Règles de Répartition CSP", titleFont));
            document.add(new com.lowagie.text.Paragraph(" "));

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(4);
            table.setWidthPercentage(100);

            table.addCell("Entité Bénéficiaire");
            table.addCell("Type Clé");
            table.addCell("Valeur Clé");
            table.addCell("ID Activité CSP");

            for (RepartitionCspRule r : list) {
                table.addCell(r.getEntiteBeneficiaire() != null ? r.getEntiteBeneficiaire() : "");
                table.addCell(r.getCleType() != null ? r.getCleType() : "");
                table.addCell(r.getValeurCle() != null ? r.getValeurCle().toString() : "0.00");
                table.addCell(r.getActiviteCspId() != null ? r.getActiviteCspId().toString() : "");
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des règles CSP", e);
        }
    }
}