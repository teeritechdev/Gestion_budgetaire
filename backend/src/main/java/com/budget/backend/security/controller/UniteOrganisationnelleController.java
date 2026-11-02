package com.budget.backend.security.controller;

import com.budget.backend.security.entity.UniteOrganisationnelle;
import com.budget.backend.security.repository.UtilisateurRepository;
import com.budget.backend.security.service.UniteOrganisationnelleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/unites-organisationnelles")
public class UniteOrganisationnelleController {

    @Autowired
    private UniteOrganisationnelleService structureService;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    private String getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) return null;
        User user = (User) authentication.getPrincipal();
        return utilisateurRepository.findByUsername(user.getUsername())
                .map(u -> u.getId().toString())
                .orElse(null);
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, 'structure.lire')")
    public ResponseEntity<List<UniteOrganisationnelle>> listerOuFiltrer(
            @RequestParam(required = false) String entiteCode,
            @RequestParam(required = false) String typeStructure,
            @RequestParam(required = false) Boolean actif,
            @RequestParam(required = false) String libelle) {
        if (entiteCode != null || typeStructure != null || actif != null || libelle != null) {
            return ResponseEntity.ok(structureService.filtrerStructures(entiteCode, typeStructure, actif, libelle));
        }
        return ResponseEntity.ok(structureService.listerToutesLesStructures());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'structure.lire')")
    public ResponseEntity<UniteOrganisationnelle> obtenirParId(@PathVariable UUID id) {
        return structureService.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'structure.creer')")
    public ResponseEntity<UniteOrganisationnelle> creer(@RequestBody UniteOrganisationnelle structure, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        UniteOrganisationnelle nouvelleStructure = structureService.creerStructure(structure, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(nouvelleStructure);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'structure.modifier')")
    public ResponseEntity<UniteOrganisationnelle> modifier(@PathVariable UUID id, @RequestBody UniteOrganisationnelle structure, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        UniteOrganisationnelle structureModifiee = structureService.modifierStructure(id, structure, userId);
        return ResponseEntity.ok(structureModifiee);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'structure.supprimer')")
    public ResponseEntity<?> supprimer(@PathVariable UUID id, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        structureService.supprimerStructure(id, userId);
        return ResponseEntity.ok(Map.of("message", "Structure supprimée avec succès."));
    }

    // --- Export CSV ---
    @GetMapping("/export/csv")
    @PreAuthorize("hasPermission(null, 'structure.exporter')")
    public ResponseEntity<byte[]> exporterCsv(
            @RequestParam(required = false) String entiteCode,
            @RequestParam(required = false) String typeStructure,
            @RequestParam(required = false) Boolean actif,
            @RequestParam(required = false) String libelle) {
        String csvData = structureService.exporterEnCsv(entiteCode, typeStructure, actif, libelle);
        byte[] bytes = csvData.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "structures_export.csv");

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    // --- Import CSV ---
    @PostMapping("/import/csv")
    @PreAuthorize("hasPermission(null, 'structure.importer')")
    public ResponseEntity<?> importerCsv(@RequestParam("file") MultipartFile file, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        int nbImportes = structureService.importerCsv(file, userId);
        return ResponseEntity.ok(Map.of("message", "Importation des structures réussie.", "elementsImportes", nbImportes));
    }

    // --- Export PDF ---
    @GetMapping("/export/pdf")
    @PreAuthorize("hasPermission(null, 'structure.exporter')")
    public ResponseEntity<byte[]> exporterPdf(
            @RequestParam(required = false) String entiteCode,
            @RequestParam(required = false) String typeStructure,
            @RequestParam(required = false) Boolean actif,
            @RequestParam(required = false) String libelle) {
        byte[] pdfBytes = structureService.exporterEnPdf(entiteCode, typeStructure, actif, libelle);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "structures_export.pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}