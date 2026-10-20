package com.budget.backend.security.service;

import com.budget.backend.security.entity.Permission;
import com.budget.backend.security.repository.PermissionRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PermissionService {

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private AuditService auditService;

    // --- CRUD : Lecture ---
    public List<Permission> listerToutesLesPermissions() {
        return permissionRepository.findAll();
    }

    public Optional<Permission> trouverParId(UUID id) {
        return permissionRepository.findById(id);
    }

    // --- CRUD : Filtrage multicritères ---
    public List<Permission> filtrerPermissions(String code, String libelle, String module) {
        return permissionRepository.findAll().stream()
                .filter(p -> code == null || p.getCode().toLowerCase().contains(code.toLowerCase()))
                .filter(p -> libelle == null || p.getLibelle().toLowerCase().contains(libelle.toLowerCase()))
                .filter(p -> module == null || p.getModule().equalsIgnoreCase(module))
                .collect(Collectors.toList());
    }

    // --- CRUD : Création ---
    @Transactional
    public Permission creerPermission(Permission permission, String utilisateurIdStr) {
        if (permissionRepository.findByCode(permission.getCode()).isPresent()) {
            throw new RuntimeException("Une permission avec le code '" + permission.getCode() + "' existe déjà.");
        }
        Permission saved = permissionRepository.save(permission);

        if (utilisateurIdStr != null) {
            auditService.logAction(
                    UUID.fromString(utilisateurIdStr),
                    "INSERTION",
                    "permissions",
                    saved.getId().toString(),
                    null,
                    Map.of("code", saved.getCode(), "module", saved.getModule())
            );
        }
        return saved;
    }

    // --- CRUD : Modification ---
    @Transactional
    public Permission modifierPermission(UUID id, Permission nouvellesDonnees, String utilisateurIdStr) {
        Permission existante = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission introuvable avec l'id : " + id));

        Map<String, Object> anciennesValeurs = Map.of(
                "code", existante.getCode(),
                "libelle", existante.getLibelle(),
                "module", existante.getModule()
        );

        existante.setLibelle(nouvellesDonnees.getLibelle());
        existante.setModule(nouvellesDonnees.getModule());
        Permission updated = permissionRepository.save(existante);

        if (utilisateurIdStr != null) {
            auditService.logAction(
                    UUID.fromString(utilisateurIdStr),
                    "MODIFICATION",
                    "permissions",
                    id.toString(),
                    anciennesValeurs,
                    Map.of("libelle", updated.getLibelle(), "module", updated.getModule())
            );
        }
        return updated;
    }

    // --- CRUD : Suppression ---
    @Transactional
    public void supprimerPermission(UUID id, String utilisateurIdStr) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission introuvable avec l'id : " + id));

        permissionRepository.delete(permission);

        if (utilisateurIdStr != null) {
            auditService.logAction(
                    UUID.fromString(utilisateurIdStr),
                    "SUPPRESSION",
                    "permissions",
                    id.toString(),
                    Map.of("code", permission.getCode(), "module", permission.getModule()),
                    null
            );
        }
    }

    // --- Export CSV ---
    public String exporterEnCsv(String code, String libelle, String module) {
        List<Permission> permissions = filtrerPermissions(code, libelle, module);
        StringBuilder csv = new StringBuilder("ID;Code;Libelle;Module;Date_Creation\n");
        for (Permission p : permissions) {
            csv.append(p.getId()).append(";")
                    .append(p.getCode()).append(";")
                    .append(p.getLibelle()).append(";")
                    .append(p.getModule()).append(";")
                    .append(p.getCreatedAt()).append("\n");
        }
        return csv.toString();
    }

    // --- Export PDF ---
    public byte[] exporterEnPdf(String code, String libelle, String module) {
        List<Permission> permissions = filtrerPermissions(code, libelle, module);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Catalogue des Permissions & Droits - UAB", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4f, 5f, 3f});

            table.addCell("Code");
            table.addCell("Libellé");
            table.addCell("Module");

            for (Permission p : permissions) {
                table.addCell(p.getCode());
                table.addCell(p.getLibelle());
                table.addCell(p.getModule());
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des permissions : " + e.getMessage());
        }

        return out.toByteArray();
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
                if (data.length >= 3) {
                    String code = data[0].trim();
                    String libelle = data[1].trim();
                    String module = data[2].trim();

                    if (permissionRepository.findByCode(code).isEmpty()) {
                        Permission p = Permission.builder()
                                .code(code)
                                .libelle(libelle)
                                .module(module)
                                .build();
                        permissionRepository.save(p);
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'import CSV des permissions : " + e.getMessage());
        }
        return count;
    }
}