package com.budget.backend.security.service;

import com.budget.backend.security.entity.Permission;
import com.budget.backend.security.entity.Role;
import com.budget.backend.security.repository.PermissionRepository;
import com.budget.backend.security.repository.RoleRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private AuditService auditService;

    // --- CRUD : Lecture ---
    public List<Role> listerTousLesRoles() {
        return roleRepository.findAll();
    }

    public Optional<Role> trouverParId(UUID id) {
        return roleRepository.findById(id);
    }

    // --- CRUD : Filtrage multicritères ---
    public List<Role> filtrerRoles(String code, String libelle) {
        return roleRepository.findAll().stream()
                .filter(r -> code == null || r.getCode().toLowerCase().contains(code.toLowerCase()))
                .filter(r -> libelle == null || r.getLibelle().toLowerCase().contains(libelle.toLowerCase()))
                .collect(Collectors.toList());
    }

    // --- CRUD : Création ---
    @Transactional
    public Role creerRole(Role role, List<UUID> permissionIds, String utilisateurIdStr) {
        if (roleRepository.findByCode(role.getCode()).isPresent()) {
            throw new RuntimeException("Un rôle avec le code '" + role.getCode() + "' existe déjà.");
        }

        if (permissionIds != null && !permissionIds.isEmpty()) {
            Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(permissionIds));
            role.setPermissions(permissions);
        }

        Role saved = roleRepository.save(role);

        if (utilisateurIdStr != null) {
            auditService.logAction(
                    UUID.fromString(utilisateurIdStr),
                    "INSERTION",
                    "roles",
                    saved.getId().toString(),
                    null,
                    Map.of("code", saved.getCode(), "libelle", saved.getLibelle())
            );
        }
        return saved;
    }

    // --- CRUD : Modification ---
    @Transactional
    public Role modifierRole(UUID id, Role nouvellesDonnees, List<UUID> permissionIds, String utilisateurIdStr) {
        Role existant = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rôle introuvable avec l'id : " + id));

        Map<String, Object> anciennesValeurs = Map.of(
                "code", existant.getCode(),
                "libelle", existant.getLibelle()
        );

        existant.setLibelle(nouvellesDonnees.getLibelle());
        existant.setDescription(nouvellesDonnees.getDescription());

        if (permissionIds != null) {
            Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(permissionIds));
            existant.setPermissions(permissions);
        }

        Role updated = roleRepository.save(existant);

        if (utilisateurIdStr != null) {
            auditService.logAction(
                    UUID.fromString(utilisateurIdStr),
                    "MODIFICATION",
                    "roles",
                    id.toString(),
                    anciennesValeurs,
                    Map.of("libelle", updated.getLibelle())
            );
        }
        return updated;
    }

    // --- CRUD : Suppression ---
    @Transactional
    public void supprimerRole(UUID id, String utilisateurIdStr) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rôle introuvable avec l'id : " + id));

        roleRepository.delete(role);

        if (utilisateurIdStr != null) {
            auditService.logAction(
                    UUID.fromString(utilisateurIdStr),
                    "SUPPRESSION",
                    "roles",
                    id.toString(),
                    Map.of("code", role.getCode(), "libelle", role.getLibelle()),
                    null
            );
        }
    }

    // --- Export CSV ---
    public String exporterEnCsv(String code, String libelle) {
        List<Role> roles = filtrerRoles(code, libelle);
        StringBuilder csv = new StringBuilder("ID;Code;Libelle;Description;Date_Creation\n");
        for (Role r : roles) {
            csv.append(r.getId()).append(";")
                    .append(r.getCode()).append(";")
                    .append(r.getLibelle()).append(";")
                    .append(r.getDescription() != null ? r.getDescription().replace(";", ",") : "").append(";")
                    .append(r.getCreatedAt()).append("\n");
        }
        return csv.toString();
    }

    // --- Export PDF ---
    public byte[] exporterEnPdf(String code, String libelle) {
        List<Role> roles = filtrerRoles(code, libelle);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Rapport des Rôles et Profils RBAC - UAB", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 4f, 5f});

            table.addCell("Code");
            table.addCell("Libellé");
            table.addCell("Description");

            for (Role r : roles) {
                table.addCell(r.getCode());
                table.addCell(r.getLibelle());
                table.addCell(r.getDescription() != null ? r.getDescription() : "");
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF des rôles : " + e.getMessage());
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
                if (data.length >= 2) {
                    String code = data[0].trim();
                    String libelle = data[1].trim();
                    String description = data.length > 2 ? data[2].trim() : "";

                    if (roleRepository.findByCode(code).isEmpty()) {
                        Role r = Role.builder()
                                .code(code)
                                .libelle(libelle)
                                .description(description)
                                .build();
                        roleRepository.save(r);
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur import CSV rôles : " + e.getMessage());
        }
        return count;
    }
}