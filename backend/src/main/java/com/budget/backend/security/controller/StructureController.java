package com.budget.backend.security.controller;

import com.budget.backend.security.entity.Structure;
import com.budget.backend.security.repository.UtilisateurRepository;
import com.budget.backend.security.service.StructureService;
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

@RestController
@RequestMapping("/api/structures")
public class StructureController {

    @Autowired
    private StructureService structureService;

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
    @PreAuthorize("hasPermission(null, 'entite.lire')")
    public ResponseEntity<List<Structure>> listerOuFiltrer(
            @RequestParam(required = false) String nom) {
        if (nom != null) {
            return ResponseEntity.ok(structureService.filtrerStructures(nom));
        }
        return ResponseEntity.ok(structureService.listerToutesLesStructures());
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasPermission(null, 'entite.lire')")
    public ResponseEntity<Structure> obtenirParCode(@PathVariable String code) {
        return structureService.trouverParCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'entite.creer')")
    public ResponseEntity<Structure> creer(@RequestBody Structure structure, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        Structure nouvelleStructure = structureService.creerStructure(structure, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(nouvelleStructure);
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasPermission(null, 'entite.modifier')")
    public ResponseEntity<Structure> modifier(@PathVariable String code, @RequestBody Structure structure, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        Structure structureModifiee = structureService.modifierStructure(code, structure, userId);
        return ResponseEntity.ok(structureModifiee);
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("hasPermission(null, 'entite.supprimer')")
    public ResponseEntity<?> supprimer(@PathVariable String code, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        structureService.supprimerStructure(code, userId);
        return ResponseEntity.ok(Map.of("message", "Structure supprimée avec succès."));
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasPermission(null, 'entite.exporter')")
    public ResponseEntity<byte[]> exporterCsv(
            @RequestParam(required = false) String nom) {
        String csvData = structureService.exporterEnCsv(nom);
        byte[] bytes = csvData.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "structures_export.csv");

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @PostMapping("/import/csv")
    @PreAuthorize("hasPermission(null, 'entite.importer')")
    public ResponseEntity<?> importerCsv(@RequestParam("file") MultipartFile file, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        int nbImportes = structureService.importerCsv(file, userId);
        return ResponseEntity.ok(Map.of("message", "Importation réussie.", "elementsImportes", nbImportes));
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasPermission(null, 'entite.exporter')")
    public ResponseEntity<byte[]> exporterPdf(
            @RequestParam(required = false) String nom) {
        byte[] pdfBytes = structureService.exporterEnPdf(nom);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "structures_export.pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}