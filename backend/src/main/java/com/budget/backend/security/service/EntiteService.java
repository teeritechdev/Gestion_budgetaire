package com.budget.backend.security.service;

import com.budget.backend.security.entity.Entite;
import com.budget.backend.security.repository.EntiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;

@Service
public class EntiteService {

    @Autowired
    private EntiteRepository entiteRepository;

    @Autowired
    private AuditService auditService;

    // --- CRUD : Lecture ---
    public List<Entite> listerToutesLesEntites() {
        return entiteRepository.findAll();
    }

    public Optional<Entite> trouverParCode(String code) {
        return entiteRepository.findById(code);
    }

    // --- CRUD : Filtrage multicritères ---
    public List<Entite> filtrerEntites(String nom, Boolean estCsp) {
        return entiteRepository.findAll().stream()
                .filter(e -> nom == null || e.getNom().toLowerCase().contains(nom.toLowerCase()))
                .filter(e -> estCsp == null || e.getEstCsP().equals(estCsp))
                .collect(Collectors.toList());
    }

    // --- CRUD : Création ---
    @Transactional
    public Entite creerEntite(Entite entite, String utilisateurIdStr) {
        // CORRECTION : entite.getCode() avec les parenthèses
        if (entiteRepository.existsByCode(entite.getCode())) {
            throw new RuntimeException("Une entité avec le code '" + entite.getCode() + "' existe déjà.");
        }
        Entite saved = entiteRepository.save(entite);

        // Audit
        if (utilisateurIdStr != null) {
            auditService.logAction(
                    UUID.fromString(utilisateurIdStr),
                    "INSERTION",
                    "entites",
                    saved.getCode(),
                    null,
                    Map.of("code", saved.getCode(), "nom", saved.getNom())
            );
        }
        return saved;
    }

    // --- CRUD : Modification ---
    @Transactional
    public Entite modifierEntite(String code, Entite nouvellesDonnees, String utilisateurIdStr) {
        Entite existante = entiteRepository.findById(code)
                .orElseThrow(() -> new RuntimeException("Entité introuvable avec le code : " + code));

        Map<String, Object> anciennesValeurs = Map.of("nom", existante.getNom(), "est_csp", existante.getEstCsP());

        existante.setNom(nouvellesDonnees.getNom());
        existante.setEstCsP(nouvellesDonnees.getEstCsP());
        Entite updated = entiteRepository.save(existante);

        // Audit
        if (utilisateurIdStr != null) {
            auditService.logAction(
                    UUID.fromString(utilisateurIdStr),
                    "MODIFICATION",
                    "entites",
                    code,
                    anciennesValeurs,
                    Map.of("nom", updated.getNom(), "est_csp", updated.getEstCsP())
            );
        }
        return updated;
    }

    // --- CRUD : Suppression ---
    @Transactional
    public void supprimerEntite(String code, String utilisateurIdStr) {
        Entite entite = entiteRepository.findById(code)
                .orElseThrow(() -> new RuntimeException("Entité introuvable avec le code : " + code));

        entiteRepository.delete(entite);

        // Audit
        if (utilisateurIdStr != null) {
            auditService.logAction(
                    UUID.fromString(utilisateurIdStr),
                    "SUPPRESSION",
                    "entites",
                    code,
                    Map.of("nom", entite.getNom()),
                    null
            );
        }
    }

    // --- Export CSV (respectant les filtres) ---
    public String exporterEnCsv(String nom, Boolean estCsp) {
        List<Entite> entites = filtrerEntites(nom, estCsp);
        StringBuilder csv = new StringBuilder("Code;Nom;Est_CSP;Date_Creation\n");
        for (Entite e : entites) {
            csv.append(e.getCode()).append(";")
                    .append(e.getNom()).append(";")
                    .append(e.getEstCsP()).append(";")
                    .append(e.getCreatedAt()).append("\n");
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
                if (isFirstLine) { // Ignorer l'en-tête
                    isFirstLine = false;
                    continue;
                }
                String[] data = line.split(";");
                if (data.length >= 3) {
                    String code = data[0].trim();
                    String nom = data[1].trim();
                    Boolean estCsp = Boolean.parseBoolean(data[2].trim());

                    if (!entiteRepository.existsByCode(code)) {
                        Entite e = Entite.builder()
                                .code(code)
                                .nom(nom)
                                .estCsP(estCsp)
                                .build();
                        entiteRepository.save(e);
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'importation du fichier CSV : " + e.getMessage());
        }
        return count;
    }
    public byte[] exporterEnPdf(String nom, Boolean estCsp) {
        List<Entite> entites = filtrerEntites(nom, estCsp);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // Titre
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Rapport des Entités - UAB", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" ")); // Saut de ligne

            // Tableau
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 4f, 2f, 3f});

            // En-têtes
            table.addCell("Code");
            table.addCell("Nom");
            table.addCell("Est CSP");
            table.addCell("Date Création");

            // Données
            for (Entite e : entites) {
                table.addCell(e.getCode());
                table.addCell(e.getNom());
                table.addCell(e.getEstCsP() ? "OUI" : "NON");
                table.addCell(e.getCreatedAt() != null ? e.getCreatedAt().toString() : "");
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des entités : " + e.getMessage());
        }

        return out.toByteArray();
    }
}