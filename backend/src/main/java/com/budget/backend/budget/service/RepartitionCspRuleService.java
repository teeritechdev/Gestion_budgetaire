package com.budget.backend.budget.service;

import com.budget.backend.budget.dto.RepartitionCspRuleRequestDto;
import com.budget.backend.budget.entity.RepartitionCspRule;
import com.budget.backend.budget.entity.RepartitionCspCleType;
import com.budget.backend.budget.repository.RepartitionCspRuleRepository;
import com.budget.backend.budget.entity.BudgetAllocationStructure;
import com.budget.backend.budget.repository.BudgetAllocationStructureRepository;
import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.repository.StructureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class RepartitionCspRuleService {

    @Autowired
    private RepartitionCspRuleRepository repository;

    @Autowired
    private StructureRepository entiteRepository;

    @Autowired
    private BudgetAllocationStructureRepository allocationStructureRepository;

    public List<RepartitionCspRule> listerTous() {
        return repository.findAll();
    }

    public Optional<RepartitionCspRule> trouverParId(UUID id) {
        return repository.findById(id);
    }

    @Transactional
    public RepartitionCspRule creer(RepartitionCspRuleRequestDto dto) {
        verifierBeneficiaire(validerAllocationCsp(dto), dto.getEntiteBeneficiaire());
        RepartitionCspRule rule = RepartitionCspRule.builder()
                .activiteCspId(dto.getActiviteCspId())
                .allocationStructureId(dto.getAllocationStructureId())
                .entiteBeneficiaire(dto.getEntiteBeneficiaire())
                .activiteBeneficiaireId(dto.getActiviteBeneficiaireId())
                .cleType(validerCleType(dto).name())
                .valeurCle(validerRatio(dto.getValeurCle()))
                .build();

        return repository.save(rule);
    }

    @Transactional
    public RepartitionCspRule modifier(UUID id, RepartitionCspRuleRequestDto dto) {
        verifierBeneficiaire(validerAllocationCsp(dto), dto.getEntiteBeneficiaire());
        RepartitionCspRule rule = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Règle CSP introuvable avec l'ID : " + id));

        rule.setActiviteCspId(dto.getActiviteCspId());
        rule.setAllocationStructureId(dto.getAllocationStructureId());
        rule.setEntiteBeneficiaire(dto.getEntiteBeneficiaire());
        rule.setActiviteBeneficiaireId(dto.getActiviteBeneficiaireId());
        rule.setCleType(validerCleType(dto).name());
        rule.setValeurCle(validerRatio(dto.getValeurCle()));

        return repository.save(rule);
    }

    @Transactional
    public List<RepartitionCspRule> enregistrerRepartition(List<RepartitionCspRuleRequestDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La répartition est vide.");
        }

        BigDecimal total = BigDecimal.ZERO;
        Set<String> entites = new HashSet<>();
        RepartitionCspRuleRequestDto reference = dtos.get(0);
        for (RepartitionCspRuleRequestDto dto : dtos) {
            validerCleType(dto);
            validerRatio(dto.getValeurCle());
            if (!Objects.equals(reference.getActiviteCspId(), dto.getActiviteCspId())
                    || !Objects.equals(reference.getActiviteBeneficiaireId(), dto.getActiviteBeneficiaireId())
                    || !Objects.equals(reference.getAllocationStructureId(), dto.getAllocationStructureId())
                    || reference.getCleType() != dto.getCleType()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Toutes les lignes doivent partager la même activité et le même mode de répartition."
                );
            }
            if (!entites.add(dto.getEntiteBeneficiaire())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Une entité ne peut apparaître qu’une seule fois dans une répartition."
                );
            }
            total = total.add(dto.getValeurCle());
        }
        BudgetAllocationStructure source = validerAllocationCsp(reference);
        List<Structure> entitesBeneficiaires = entiteRepository.findAllById(entites);
        if (entitesBeneficiaires.size() != entites.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Une ou plusieurs entités sélectionnées sont introuvables."
            );
        }
        entites.forEach(code -> verifierBeneficiaire(source, code));
        if (total.setScale(4, RoundingMode.HALF_UP).compareTo(BigDecimal.ONE) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                "Le total des ratios doit être égal à 1 (100 %)."
            );
        }

        Map<String, RepartitionCspRule> existingByEntity = new HashMap<>();
        for (RepartitionCspRule existing : repository.findAllByAllocationStructureIdAndActiviteBeneficiaireId(
                reference.getAllocationStructureId(), reference.getActiviteBeneficiaireId())) {
            if (!entites.contains(existing.getEntiteBeneficiaire())
                    || existingByEntity.putIfAbsent(existing.getEntiteBeneficiaire(), existing) != null) {
                repository.delete(existing);
            }
        }

        List<RepartitionCspRule> rules = new ArrayList<>();
        for (RepartitionCspRuleRequestDto dto : dtos) {
            RepartitionCspRule rule = existingByEntity.getOrDefault(
                    dto.getEntiteBeneficiaire(), new RepartitionCspRule());
            rule.setActiviteCspId(dto.getActiviteCspId());
            rule.setAllocationStructureId(dto.getAllocationStructureId());
            rule.setEntiteBeneficiaire(dto.getEntiteBeneficiaire());
            rule.setActiviteBeneficiaireId(dto.getActiviteBeneficiaireId());
            rule.setCleType(dto.getCleType().name());
            rule.setValeurCle(dto.getValeurCle());
            rules.add(repository.save(rule));
        }
        return rules;
    }

    private BigDecimal validerRatio(BigDecimal valeur) {
        if (valeur == null || valeur.signum() < 0 || valeur.compareTo(BigDecimal.ONE) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le ratio doit être compris entre 0 et 1 (0 % à 100 %)."
            );
        }
        return valeur;
    }

    private RepartitionCspCleType validerCleType(RepartitionCspRuleRequestDto dto) {
        if (dto.getCleType() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sélectionnez un type de clé de répartition."
            );
        }
        return dto.getCleType();
    }

    /** Une structure ne peut pas être bénéficiaire de la répartition de sa propre allocation. */
    private void verifierBeneficiaire(BudgetAllocationStructure source, String entiteBeneficiaire) {
        if (source.getStructure() != null && source.getStructure().getCode().equalsIgnoreCase(entiteBeneficiaire)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La structure source ne peut pas être bénéficiaire de sa propre répartition."
            );
        }
    }

    private BudgetAllocationStructure validerAllocationCsp(RepartitionCspRuleRequestDto dto) {
        if (dto.getAllocationStructureId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sélectionnez une allocation de structure CSP.");
        }
        BudgetAllocationStructure allocation = allocationStructureRepository.findById(dto.getAllocationStructureId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "L'allocation de structure sélectionnée est introuvable."));
        // Provisoire : toute allocation de structure peut servir de source.
        boolean allocationCsp = allocation.getStructure() != null;
        UUID allocationActivityId = allocation.getBudgetAllocationActivite() != null
                ? allocation.getBudgetAllocationActivite().getId()
                : null;
        if (!allocationCsp || !Objects.equals(allocationActivityId, dto.getActiviteCspId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "L'allocation sélectionnée doit être rattachée à une structure et correspondre à l'activité d'allocation source.");
        }
        return allocation;
    }

    @Transactional
    public void supprimer(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Règle CSP introuvable avec l'ID : " + id);
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
        return exporterEnCsv(filtrer(entiteBeneficiaire, cleType));
    }

    /** Exporte exactement la liste fournie (lignes affichées à l’écran, dans leur ordre). */
    public String exporterEnCsv(List<RepartitionCspRule> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Allocation structure CSP,ID Activité CSP,Entité Bénéficiaire,ID Activité Bénéficiaire,Type Clé,Ratio (0 à 1)\n");
        for (RepartitionCspRule r : list) {
            sb.append(r.getId()).append(",")
                    .append(r.getAllocationStructureId()).append(",")
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
        return exporterEnPdf(filtrer(entiteBeneficiaire, cleType));
    }

    /** Exporte exactement la liste fournie (lignes affichées à l’écran, dans leur ordre). */
    public byte[] exporterEnPdf(List<RepartitionCspRule> list) {

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 16);
            document.add(new com.lowagie.text.Paragraph("Règles de Répartition CSP", titleFont));
            document.add(new com.lowagie.text.Paragraph(" "));

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(5);
            table.setWidthPercentage(100);

            table.addCell("Entité Bénéficiaire");
            table.addCell("Allocation structure CSP");
            table.addCell("Type Clé");
            table.addCell("Ratio (0 à 1)");
            table.addCell("ID Activité CSP");

            for (RepartitionCspRule r : list) {
                table.addCell(r.getEntiteBeneficiaire() != null ? r.getEntiteBeneficiaire() : "");
                table.addCell(r.getAllocationStructureId() != null ? r.getAllocationStructureId().toString() : "");
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