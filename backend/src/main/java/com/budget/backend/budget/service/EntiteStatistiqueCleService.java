package com.budget.backend.budget.service;

import com.budget.backend.budget.dto.EntiteStatistiqueCleRequestDto;
import com.budget.backend.budget.entity.EntiteStatistiqueCle;
import com.budget.backend.budget.repository.EntiteStatistiqueCleRepository;
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
public class EntiteStatistiqueCleService {

    @Autowired
    private EntiteStatistiqueCleRepository repository;

    public List<EntiteStatistiqueCle> listerTous() {
        return repository.findAll();
    }

    public Optional<EntiteStatistiqueCle> trouverParId(UUID id) {
        return repository.findById(id);
    }

    @Transactional
    public EntiteStatistiqueCle creer(EntiteStatistiqueCleRequestDto dto) {
        if (dto.getMois() != null && repository.existsByAnneeExerciceAndMoisAndEntiteCode(dto.getAnneeExercice(), dto.getMois(), dto.getEntiteCode())) {
            throw new RuntimeException("Une statistique existe déjà pour cette entité, ce mois et cette année.");
        }

        EntiteStatistiqueCle statistique = EntiteStatistiqueCle.builder()
                .anneeExercice(dto.getAnneeExercice())
                .mois(dto.getMois())
                .entiteCode(dto.getEntiteCode())
                .effectifAgents(dto.getEffectifAgents())
                .chiffreAffaires(dto.getChiffreAffaires())
                .build();

        return repository.save(statistique);
    }

    @Transactional
    public EntiteStatistiqueCle modifier(UUID id, EntiteStatistiqueCleRequestDto dto) {
        EntiteStatistiqueCle statistique = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Statistique introuvable avec l'ID : " + id));

        statistique.setAnneeExercice(dto.getAnneeExercice());
        statistique.setMois(dto.getMois());
        statistique.setEntiteCode(dto.getEntiteCode());
        statistique.setEffectifAgents(dto.getEffectifAgents());
        statistique.setChiffreAffaires(dto.getChiffreAffaires());

        return repository.save(statistique);
    }

    @Transactional
    public void supprimer(UUID id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Statistique introuvable avec l'ID : " + id);
        }
        repository.deleteById(id);
    }

    // Filtrage multicritères dynamique
    public List<EntiteStatistiqueCle> filtrer(Integer annee, Integer mois, String entiteCode) {
        return repository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (annee != null) {
                predicates.add(cb.equal(root.get("anneeExercice"), annee));
            }
            if (mois != null) {
                predicates.add(cb.equal(root.get("mois"), mois));
            }
            if (entiteCode != null && !entiteCode.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("entiteCode")), "%" + entiteCode.trim().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });
    }

    // Export CSV avec filtres
    public String exporterEnCsv(Integer annee, Integer mois, String entiteCode) {
        List<EntiteStatistiqueCle> list = filtrer(annee, mois, entiteCode);
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Année,Mois,Code Entité,Effectif Agents,Chiffre d'Affaires\n");
        for (EntiteStatistiqueCle s : list) {
            sb.append(s.getId()).append(",")
                    .append(s.getAnneeExercice()).append(",")
                    .append(s.getMois() != null ? s.getMois() : "").append(",")
                    .append("\"").append(s.getEntiteCode()).append("\",")
                    .append(s.getEffectifAgents() != null ? s.getEffectifAgents() : 0).append(",")
                    .append(s.getChiffreAffaires() != null ? s.getChiffreAffaires() : BigDecimal.ZERO).append("\n");
        }
        return sb.toString();
    }

    // Export PDF avec filtres (OpenPDF)
    public byte[] exporterEnPdf(Integer annee, Integer mois, String entiteCode) {
        List<EntiteStatistiqueCle> list = filtrer(annee, mois, entiteCode);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 16);
            document.add(new com.lowagie.text.Paragraph("Clés Statistiques des Entités", titleFont));
            document.add(new com.lowagie.text.Paragraph(" "));

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(5);
            table.setWidthPercentage(100);

            table.addCell("Année");
            table.addCell("Mois");
            table.addCell("Entité");
            table.addCell("Effectif");
            table.addCell("Chiffre d'Affaires");

            for (EntiteStatistiqueCle s : list) {
                table.addCell(s.getAnneeExercice() != null ? s.getAnneeExercice().toString() : "");
                table.addCell(s.getMois() != null ? s.getMois().toString() : "");
                table.addCell(s.getEntiteCode() != null ? s.getEntiteCode() : "");
                table.addCell(s.getEffectifAgents() != null ? s.getEffectifAgents().toString() : "0");
                table.addCell(s.getChiffreAffaires() != null ? s.getChiffreAffaires().toString() : "0.00");
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des statistiques", e);
        }
    }
}