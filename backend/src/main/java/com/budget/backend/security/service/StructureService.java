package com.budget.backend.security.service;

import com.budget.backend.common.CsvReader;
import com.budget.backend.common.CsvUtils;
import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.repository.StructureRepository;
import com.budget.backend.security.repository.DirectionRepository;
import com.budget.backend.security.repository.AffectationRepository;
import com.budget.backend.security.repository.UniteOrganisationnelleRepository;
import com.budget.backend.budget.repository.BudgetAllocationStructureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Locale;
import java.util.stream.Collectors;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;

@Service
public class StructureService {

    @Autowired
    private StructureRepository structureRepository;

    @Autowired
    private DirectionRepository directionRepository;

    @Autowired
    private UniteOrganisationnelleRepository uniteOrganisationnelleRepository;

    @Autowired
    private BudgetAllocationStructureRepository allocationRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AffectationRepository affectationRepository;

    // --- CRUD : Lecture ---
    public List<Structure> listerToutesLesStructures() {
        return structureRepository.findAll();
    }

    public Optional<Structure> trouverParCode(String code) {
        return structureRepository.findById(code);
    }

    // --- CRUD : Filtrage multicritères ---
    public List<Structure> filtrerStructures(String nom) {
        return structureRepository.findAll().stream()
                .filter(e -> nom == null || e.getNom().toLowerCase().contains(nom.toLowerCase()))
                .collect(Collectors.toList());
    }

    // --- CRUD : Création ---
    @Transactional
    public Structure creerStructure(Structure structure, String utilisateurIdStr) {
        if (structure == null || structure.getCode() == null || structure.getCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le code de la structure est obligatoire.");
        }
        if (structure.getNom() == null || structure.getNom().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom de la structure est obligatoire.");
        }
        structure.setCode(structure.getCode().trim().toUpperCase(Locale.ROOT));
        structure.setNom(structure.getNom().trim());
        if (structure.getCode().length() > 20) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Le code de la structure ne peut pas dépasser 20 caractères.");
        }
        if (structure.getNom().length() > 150) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Le nom de la structure ne peut pas dépasser 150 caractères.");
        }
        if (structureRepository.existsByCode(structure.getCode())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Une structure avec le code '" + structure.getCode() + "' existe déjà.");
        }
        Structure saved = structureRepository.save(structure);

        // Audit
        if (utilisateurIdStr != null) {
            auditService.logAction(
                    UUID.fromString(utilisateurIdStr),
                    "INSERTION",
                    "structures",
                    saved.getCode(),
                    null,
                    Map.of("code", saved.getCode(), "nom", saved.getNom())
            );
        }
        return saved;
    }

    // --- CRUD : Modification ---
    @Transactional
    public Structure modifierStructure(String code, Structure nouvellesDonnees, String utilisateurIdStr) {
        Structure existante = structureRepository.findById(code)
                .orElseThrow(() -> new RuntimeException("Structure introuvable avec le code : " + code));

        Map<String, Object> anciennesValeurs = Map.of("nom", existante.getNom());

        existante.setNom(nouvellesDonnees.getNom());
        existante.setTelephone(nouvellesDonnees.getTelephone());
        existante.setAdresse(nouvellesDonnees.getAdresse());
        existante.setEmail(nouvellesDonnees.getEmail());
        existante.setVille(nouvellesDonnees.getVille());
        existante.setPays(nouvellesDonnees.getPays());
        existante.setSigle(nouvellesDonnees.getSigle());
        existante.setSecteurActivite(nouvellesDonnees.getSecteurActivite());
        existante.setFormeJuridique(nouvellesDonnees.getFormeJuridique());
        existante.setNumeroFiscal(nouvellesDonnees.getNumeroFiscal());
        existante.setSiteWeb(nouvellesDonnees.getSiteWeb());
        Structure updated = structureRepository.save(existante);

        // Audit
        if (utilisateurIdStr != null) {
            auditService.logAction(
                    UUID.fromString(utilisateurIdStr),
                    "MODIFICATION",
                    "structures",
                    code,
                    anciennesValeurs,
                    Map.of("nom", updated.getNom())
            );
        }
        return updated;
    }

    // --- CRUD : Suppression ---
    @Transactional
    public void supprimerStructure(String code, String utilisateurIdStr) {
        Structure structure = structureRepository.findById(code)
                .orElseThrow(() -> new RuntimeException("Structure introuvable avec le code : " + code));

        if (directionRepository.existsByStructure_Code(code)
                || !uniteOrganisationnelleRepository.findByStructureCode(code).isEmpty()
                || allocationRepository.existsByStructure_Code(code)
                || affectationRepository.existsByStructure_Code(code)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cette Structure ne peut pas être supprimée tant que des directions, unités historiques, allocations ou salariés y sont rattachés.");
        }

        structureRepository.delete(structure);

        // Audit
        if (utilisateurIdStr != null) {
            auditService.logAction(
                    UUID.fromString(utilisateurIdStr),
                    "SUPPRESSION",
                    "structures",
                    code,
                    Map.of("nom", structure.getNom()),
                    null
            );
        }
    }

    // --- Export CSV (respectant les filtres) ---
    public String exporterEnCsv(String nom) {
        return exporterEnCsv(filtrerStructures(nom));
    }

    /** Exporte exactement la liste fournie (lignes affichées à l’écran, dans leur ordre). */
    public String exporterEnCsv(List<Structure> structures) {
        StringBuilder csv = new StringBuilder("Code;Nom;Telephone;Adresse;Email;Ville;Pays;Sigle;Secteur_Activite;Forme_Juridique;Numero_Fiscal;Site_Web;Date_Creation\n");
        for (Structure e : structures) {
            csv.append(CsvUtils.ligne(';', e.getCode(), e.getNom(), e.getTelephone(), e.getAdresse(),
                    e.getEmail(), e.getVille(), e.getPays(), e.getSigle(), e.getSecteurActivite(),
                    e.getFormeJuridique(), e.getNumeroFiscal(), e.getSiteWeb(), e.getCreatedAt()));
        }
        return csv.toString();
    }

    // --- Import CSV ---
    @Transactional
    public int importerCsv(MultipartFile file, String utilisateurIdStr) {
        // Colonnes repérées par leur nom (format de l'export) ; les codes déjà existants sont ignorés.
        int[] crees = {0};
        CsvReader.lire(file, List.of("code", "nom"), ligne -> {
            String code = ligne.obligatoire("code").toUpperCase();
            if (structureRepository.existsByCode(code)) {
                return;
            }
            structureRepository.save(Structure.builder()
                    .code(code)
                    .nom(ligne.obligatoire("nom"))
                    .telephone(ligne.texte("telephone"))
                    .adresse(ligne.texte("adresse"))
                    .email(ligne.texte("email"))
                    .ville(ligne.texte("ville"))
                    .pays(ligne.texte("pays"))
                    .sigle(ligne.texte("sigle"))
                    .secteurActivite(ligne.texte("secteur_activite"))
                    .formeJuridique(ligne.texte("forme_juridique"))
                    .numeroFiscal(ligne.texte("numero_fiscal"))
                    .siteWeb(ligne.texte("site_web"))
                    .build());
            crees[0]++;
        });
        int count = crees[0];
        return count;
    }
    public byte[] exporterEnPdf(String nom) {
        return exporterEnPdf(filtrerStructures(nom));
    }

    /** Exporte exactement la liste fournie (lignes affichées à l’écran, dans leur ordre). */
    public byte[] exporterEnPdf(List<Structure> entites) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // Titre
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Rapport des Structures - UAB", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" ")); // Saut de ligne

            // Tableau
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 4f, 3f, 3f});

            // En-têtes
            table.addCell("Code");
            table.addCell("Nom");
            table.addCell("Ville");
            table.addCell("Date Création");

            // Données
            for (Structure e : entites) {
                table.addCell(e.getCode());
                table.addCell(e.getNom());
                table.addCell(e.getVille() != null ? e.getVille() : "");
                table.addCell(e.getCreatedAt() != null ? e.getCreatedAt().toString() : "");
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des entités : " + e.getMessage());
        }

        return out.toByteArray();
    }

    private String csvValue(String[] data, int index) {
        return data.length > index && !data[index].isBlank() ? data[index].trim() : null;
    }
}