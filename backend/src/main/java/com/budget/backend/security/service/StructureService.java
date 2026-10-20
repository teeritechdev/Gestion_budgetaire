package com.budget.backend.security.service;

import com.budget.backend.security.entity.Entite;
import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.repository.EntiteRepository;
import com.budget.backend.security.repository.StructureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StructureService {

    @Autowired
    private StructureRepository structureRepository;

    @Autowired
    private EntiteRepository entiteRepository;

    @Autowired
    private AuditService auditService;

    // --- CRUD : Lecture ---
    public List<Structure> listerToutesLesStructures() {
        return structureRepository.findAll();
    }

    public Optional<Structure> trouverParId(UUID id) {
        return structureRepository.findById(id);
    }

    // --- CRUD : Filtrage multicritères ---
    public List<Structure> filtrerStructures(String entiteCode, String typeStructure, Boolean actif, String libelle) {
        return structureRepository.findAll().stream()
                .filter(s -> entiteCode == null || (s.getEntite() != null && s.getEntite().getCode().equalsIgnoreCase(entiteCode)))
                .filter(s -> typeStructure == null || s.getTypeStructure().equalsIgnoreCase(typeStructure))
                .filter(s -> actif == null || s.getActif().equals(actif))
                .filter(s -> libelle == null || s.getLibelle().toLowerCase().contains(libelle.toLowerCase()))
                .collect(Collectors.toList());
    }

    // --- CRUD : Création ---
    @Transactional
    public Structure creerStructure(Structure structure, String utilisateurIdStr) {
        if (structureRepository.findByCode(structure.getCode()).isPresent()) {
            throw new RuntimeException("Une structure avec le code '" + structure.getCode() + "' existe déjà.");
        }
        Structure saved = structureRepository.save(structure);

        // Audit
        if (utilisateurIdStr != null) {
            auditService.logAction(
                    UUID.fromString(utilisateurIdStr),
                    "INSERTION",
                    "structures",
                    saved.getId().toString(),
                    null,
                    Map.of("code", saved.getCode(), "libelle", saved.getLibelle())
            );
        }
        return saved;
    }

    // --- CRUD : Modification ---
    @Transactional
    public Structure modifierStructure(UUID id, Structure nouvellesDonnees, String utilisateurIdStr) {
        Structure existante = structureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Structure introuvable avec l'id : " + id));

        Map<String, Object> anciennesValeurs = Map.of(
                "libelle", existante.getLibelle(),
                "type_structure", existante.getTypeStructure(),
                "actif", existante.getActif()
        );

        existante.setLibelle(nouvellesDonnees.getLibelle());
        existante.setTypeStructure(nouvellesDonnees.getTypeStructure());
        existante.setParent(nouvellesDonnees.getParent());
        existante.setDirection(nouvellesDonnees.getDirection());
        existante.setEntite(nouvellesDonnees.getEntite());
        existante.setResponsableId(nouvellesDonnees.getResponsableId());
        existante.setActif(nouvellesDonnees.getActif());

        Structure updated = structureRepository.save(existante);

        // Audit
        if (utilisateurIdStr != null) {
            auditService.logAction(
                    UUID.fromString(utilisateurIdStr),
                    "MODIFICATION",
                    "structures",
                    id.toString(),
                    anciennesValeurs,
                    Map.of("libelle", updated.getLibelle(), "type_structure", updated.getTypeStructure(), "actif", updated.getActif())
            );
        }
        return updated;
    }

    // --- CRUD : Suppression ---
    @Transactional
    public void supprimerStructure(UUID id, String utilisateurIdStr) {
        Structure structure = structureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Structure introuvable avec l'id : " + id));

        structureRepository.delete(structure);

        // Audit
        if (utilisateurIdStr != null) {
            auditService.logAction(
                    UUID.fromString(utilisateurIdStr),
                    "SUPPRESSION",
                    "structures",
                    id.toString(),
                    Map.of("code", structure.getCode(), "libelle", structure.getLibelle()),
                    null
            );
        }
    }

    // --- Export CSV (respectant les filtres) ---
    public String exporterEnCsv(String entiteCode, String typeStructure, Boolean actif, String libelle) {
        List<Structure> structures = filtrerStructures(entiteCode, typeStructure, actif, libelle);
        StringBuilder csv = new StringBuilder("ID;Code;Libelle;TypeStructure;EntiteCode;Actif;Date_Creation\n");
        for (Structure s : structures) {
            csv.append(s.getId()).append(";")
                    .append(s.getCode()).append(";")
                    .append(s.getLibelle()).append(";")
                    .append(s.getTypeStructure()).append(";")
                    .append(s.getEntite() != null ? s.getEntite().getCode() : "").append(";")
                    .append(s.getActif()).append(";")
                    .append(s.getCreatedAt()).append("\n");
        }
        return csv.toString();
    }

    // --- Import CSV ---
    @Transactional
    public int importerCsv(MultipartFile file, String utilisateurIdStr) {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                String[] data = line.split(";");
                if (data.length >= 5) {
                    String code = data[0].trim();
                    String libelle = data[1].trim();
                    String typeStructure = data[2].trim();
                    String entiteCode = data[3].trim();
                    Boolean actif = Boolean.parseBoolean(data[4].trim());

                    if (structureRepository.findByCode(code).isEmpty()) {
                        Entite entite = entiteRepository.findById(entiteCode)
                                .orElseThrow(() -> new RuntimeException("Entité parente introuvable pour l'import : " + entiteCode));

                        Structure s = Structure.builder()
                                .code(code)
                                .libelle(libelle)
                                .typeStructure(typeStructure)
                                .entite(entite)
                                .actif(actif)
                                .build();
                        structureRepository.save(s);
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'importation CSV des structures : " + e.getMessage());
        }
        return count;
    }
    public byte[] exporterEnPdf(String entiteCode, String typeStructure, Boolean actif, String libelle) {
        List<Structure> structures = filtrerStructures(entiteCode, typeStructure, actif, libelle);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            Document document = new Document(PageSize.A4.rotate()); // Format paysage pour plus de colonnes
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Rapport de l'Organigramme - Structures UAB", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 3f, 4f, 3f, 2f, 2f});

            table.addCell("Code");
            table.addCell("Libellé");
            table.addCell("Type");
            table.addCell("Entité");
            table.addCell("Actif");
            table.addCell("Date Création");

            for (Structure s : structures) {
                table.addCell(s.getCode());
                table.addCell(s.getLibelle());
                table.addCell(s.getTypeStructure());
                table.addCell(s.getEntite() != null ? s.getEntite().getCode() : "");
                table.addCell(s.getActif() ? "OUI" : "NON");
                table.addCell(s.getCreatedAt() != null ? s.getCreatedAt().toString() : "");
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des structures : " + e.getMessage());
        }

        return out.toByteArray();
    }
}