package com.budget.backend.security.controller;

import com.budget.backend.security.entity.Entite;
import com.budget.backend.security.repository.UtilisateurRepository;
import com.budget.backend.security.service.EntiteService;
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
@RequestMapping("/api/entites")
@CrossOrigin(origins = "*", maxAge = 3600)
public class EntiteController {

    @Autowired
    private EntiteService entiteService;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    private String getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) return null;
        User user = (User) authentication.getPrincipal();
        return utilisateurRepository.findByEmail(user.getUsername())
                .map(u -> u.getId().toString())
                .orElse(null);
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, 'entite.lire')")
    public ResponseEntity<List<Entite>> listerOuFiltrer(
            @RequestParam(required = false) String nom,
            @RequestParam(required = false) Boolean estCsp) {
        if (nom != null || estCsp != null) {
            return ResponseEntity.ok(entiteService.filtrerEntites(nom, estCsp));
        }
        return ResponseEntity.ok(entiteService.listerToutesLesEntites());
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasPermission(null, 'entite.lire')")
    public ResponseEntity<Entite> obtenirParCode(@PathVariable String code) {
        return entiteService.trouverParCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'entite.creer')")
    public ResponseEntity<Entite> creer(@RequestBody Entite entite, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        Entite nouvelleEntite = entiteService.creerEntite(entite, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(nouvelleEntite);
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasPermission(null, 'entite.modifier')")
    public ResponseEntity<Entite> modifier(@PathVariable String code, @RequestBody Entite entite, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        Entite entiteModifiee = entiteService.modifierEntite(code, entite, userId);
        return ResponseEntity.ok(entiteModifiee);
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("hasPermission(null, 'entite.supprimer')")
    public ResponseEntity<?> supprimer(@PathVariable String code, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        entiteService.supprimerEntite(code, userId);
        return ResponseEntity.ok(Map.of("message", "Entité supprimée avec succès."));
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasPermission(null, 'entite.exporter')")
    public ResponseEntity<byte[]> exporterCsv(
            @RequestParam(required = false) String nom,
            @RequestParam(required = false) Boolean estCsp) {
        String csvData = entiteService.exporterEnCsv(nom, estCsp);
        byte[] bytes = csvData.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "entites_export.csv");

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @PostMapping("/import/csv")
    @PreAuthorize("hasPermission(null, 'entite.importer')")
    public ResponseEntity<?> importerCsv(@RequestParam("file") MultipartFile file, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        int nbImportes = entiteService.importerCsv(file, userId);
        return ResponseEntity.ok(Map.of("message", "Importation réussie.", "elementsImportes", nbImportes));
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasPermission(null, 'entite.exporter')")
    public ResponseEntity<byte[]> exporterPdf(
            @RequestParam(required = false) String nom,
            @RequestParam(required = false) Boolean estCsp) {
        byte[] pdfBytes = entiteService.exporterEnPdf(nom, estCsp);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "entites_export.pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}