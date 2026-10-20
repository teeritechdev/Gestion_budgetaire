package com.budget.backend.security.service;

import com.budget.backend.security.entity.Role;
import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.entity.Utilisateur;
import com.budget.backend.security.repository.RoleRepository;
import com.budget.backend.security.repository.StructureRepository;
import com.budget.backend.security.repository.UtilisateurRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
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
public class UtilisateurService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private StructureRepository structureRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditService auditService;

    // --- CRUD : Lecture ---
    public List<Utilisateur> listerTousLesUtilisateurs() {
        return utilisateurRepository.findAll();
    }

    public Optional<Utilisateur> trouverParId(UUID id) {
        return utilisateurRepository.findById(id);
    }

    // --- CRUD : Filtrage multicritères ---
    public List<Utilisateur> filtrerUtilisateurs(UUID structureId, UUID roleId, Boolean actif, String recherche) {
        return utilisateurRepository.findAll().stream()
                .filter(u -> structureId == null || (u.getStructure() != null && u.getStructure().getId().equals(structureId)))
                .filter(u -> roleId == null || (u.getRole() != null && u.getRole().getId().equals(roleId)))
                .filter(u -> actif == null || u.getActif().equals(actif))
                .filter(u -> recherche == null ||
                        u.getNom().toLowerCase().contains(recherche.toLowerCase()) ||
                        u.getPrenom().toLowerCase().contains(recherche.toLowerCase()) ||
                        u.getEmail().toLowerCase().contains(recherche.toLowerCase()) ||
                        u.getMatricule().toLowerCase().contains(recherche.toLowerCase()))
                .collect(Collectors.toList());
    }

    // --- CRUD : Création ---
    @Transactional
    public Utilisateur creerUtilisateur(Utilisateur utilisateur, String motDePasseClair, String utilisateurIdStr) {
        if (utilisateurRepository.existsByEmail(utilisateur.getEmail())) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà : " + utilisateur.getEmail());
        }
        if (utilisateurRepository.existsByMatricule(utilisateur.getMatricule())) {
            throw new RuntimeException("Un utilisateur avec ce matricule existe déjà : " + utilisateur.getMatricule());
        }

        // Hachage sécurisé du mot de passe avec BCrypt
        if (motDePasseClair != null && !motDePasseClair.isEmpty()) {
            utilisateur.setMotDePasseHash(passwordEncoder.encode(motDePasseClair));
        } else {
            throw new RuntimeException("Le mot de passe est obligatoire pour la création d'un utilisateur.");
        }

        Utilisateur saved = utilisateurRepository.save(utilisateur);

        if (utilisateurIdStr != null) {
            auditService.logAction(
                    UUID.fromString(utilisateurIdStr),
                    "INSERTION",
                    "utilisateurs",
                    saved.getId().toString(),
                    null,
                    Map.of("matricule", saved.getMatricule(), "email", saved.getEmail())
            );
        }
        return saved;
    }

    // --- CRUD : Modification ---
    @Transactional
    public Utilisateur modifierUtilisateur(UUID id, Utilisateur nouvellesDonnees, String nouveauMotDePasse, String utilisateurIdStr) {
        Utilisateur existant = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec l'id : " + id));

        Map<String, Object> anciennesValeurs = Map.of(
                "nom", existant.getNom(),
                "prenom", existant.getPrenom(),
                "email", existant.getEmail(),
                "actif", existant.getActif()
        );

        existant.setNom(nouvellesDonnees.getNom());
        existant.setPrenom(nouvellesDonnees.getPrenom());
        existant.setEmail(nouvellesDonnees.getEmail());
        existant.setRoleMetier(nouvellesDonnees.getRoleMetier());
        existant.setStructure(nouvellesDonnees.getStructure());
        existant.setRole(nouvellesDonnees.getRole());
        existant.setManager(nouvellesDonnees.getManager());
        existant.setActif(nouvellesDonnees.getActif());

        if (nouveauMotDePasse != null && !nouveauMotDePasse.isEmpty()) {
            existant.setMotDePasseHash(passwordEncoder.encode(nouveauMotDePasse));
        }

        Utilisateur updated = utilisateurRepository.save(existant);

        if (utilisateurIdStr != null) {
            auditService.logAction(
                    UUID.fromString(utilisateurIdStr),
                    "MODIFICATION",
                    "utilisateurs",
                    id.toString(),
                    anciennesValeurs,
                    Map.of("email", updated.getEmail(), "actif", updated.getActif())
            );
        }
        return updated;
    }

    // --- CRUD : Suppression ---
    @Transactional
    public void supprimerUtilisateur(UUID id, String utilisateurIdStr) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec l'id : " + id));

        utilisateurRepository.delete(utilisateur);

        if (utilisateurIdStr != null) {
            auditService.logAction(
                    UUID.fromString(utilisateurIdStr),
                    "SUPPRESSION",
                    "utilisateurs",
                    id.toString(),
                    Map.of("matricule", utilisateur.getMatricule(), "email", utilisateur.getEmail()),
                    null
            );
        }
    }

    // --- Export CSV ---
    public String exporterEnCsv(UUID structureId, UUID roleId, Boolean actif, String recherche) {
        List<Utilisateur> utilisateurs = filtrerUtilisateurs(structureId, roleId, actif, recherche);
        StringBuilder csv = new StringBuilder("ID;Matricule;Nom;Prenom;Email;Role;Structure;Actif;Date_Creation\n");
        for (Utilisateur u : utilisateurs) {
            csv.append(u.getId()).append(";")
                    .append(u.getMatricule()).append(";")
                    .append(u.getNom()).append(";")
                    .append(u.getPrenom()).append(";")
                    .append(u.getEmail()).append(";")
                    .append(u.getRole() != null ? u.getRole().getCode() : "").append(";")
                    .append(u.getStructure() != null ? u.getStructure().getCode() : "").append(";")
                    .append(u.getActif()).append(";")
                    .append(u.getCreatedAt()).append("\n");
        }
        return csv.toString();
    }

    // --- Export PDF ---
    public byte[] exporterEnPdf(UUID structureId, UUID roleId, Boolean actif, String recherche) {
        List<Utilisateur> utilisateurs = filtrerUtilisateurs(structureId, roleId, actif, recherche);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            Document document = new Document(PageSize.A4.rotate()); // Paysage pour les infos utilisateurs
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Rapport des Comptes Utilisateurs - UAB", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 3f, 3f, 4f, 3f, 2f, 2f});

            table.addCell("Matricule");
            table.addCell("Nom");
            table.addCell("Prénom");
            table.addCell("Email");
            table.addCell("Rôle");
            table.addCell("Actif");
            table.addCell("Création");

            for (Utilisateur u : utilisateurs) {
                table.addCell(u.getMatricule());
                table.addCell(u.getNom());
                table.addCell(u.getPrenom());
                table.addCell(u.getEmail());
                table.addCell(u.getRole() != null ? u.getRole().getCode() : "");
                table.addCell(u.getActif() ? "OUI" : "NON");
                table.addCell(u.getCreatedAt() != null ? u.getCreatedAt().toString().substring(0, 10) : "");
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF utilisateurs : " + e.getMessage());
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
                if (data.length >= 7) {
                    String matricule = data[0].trim();
                    String nom = data[1].trim();
                    String prenom = data[2].trim();
                    String email = data[3].trim();
                    String mdpClair = data[4].trim();
                    String structureCode = data[5].trim();
                    String roleCode = data[6].trim();

                    if (!utilisateurRepository.existsByEmail(email) && !utilisateurRepository.existsByMatricule(matricule)) {
                        Structure structure = structureRepository.findByCode(structureCode)
                                .orElseThrow(() -> new RuntimeException("Structure introuvable pour l'import : " + structureCode));
                        Role role = roleRepository.findByCode(roleCode)
                                .orElseThrow(() -> new RuntimeException("Rôle introuvable pour l'import : " + roleCode));

                        Utilisateur u = Utilisateur.builder()
                                .matricule(matricule)
                                .nom(nom)
                                .prenom(prenom)
                                .email(email)
                                .motDePasseHash(passwordEncoder.encode(mdpClair))
                                .structure(structure)
                                .role(role)
                                .actif(true)
                                .build();

                        utilisateurRepository.save(u);
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur import CSV utilisateurs : " + e.getMessage());
        }
        return count;
    }
}