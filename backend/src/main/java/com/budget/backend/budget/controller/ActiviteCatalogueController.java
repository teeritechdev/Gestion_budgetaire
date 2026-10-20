package com.budget.backend.budget.controller;

import com.budget.backend.budget.dto.ActiviteCatalogueRequestDto;
import com.budget.backend.budget.entity.ActiviteCatalogue;
import com.budget.backend.budget.service.ActiviteCatalogueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/activites-catalogue")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ActiviteCatalogueController {

    @Autowired
    private ActiviteCatalogueService service;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'activite.lire')")
    public ResponseEntity<List<ActiviteCatalogue>> listerOuFiltrer(
            @RequestParam(required = false) String codeActivite,
            @RequestParam(required = false) String libelle,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) UUID opId) {

        if (codeActivite != null || libelle != null || description != null || opId != null) {
            return ResponseEntity.ok(service.filtrerActivites(codeActivite, libelle, description, opId));
        }
        return ResponseEntity.ok(service.listerTous());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'activite.lire')")
    public ResponseEntity<ActiviteCatalogue> obtenirParId(@PathVariable UUID id) {
        return service.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'activite.creer')")
    public ResponseEntity<ActiviteCatalogue> creer(@RequestBody ActiviteCatalogueRequestDto dto) {
        ActiviteCatalogue activite = ActiviteCatalogue.builder()
                .codeActivite(dto.getCodeActivite())
                .libelle(dto.getLibelle())
                .description(dto.getDescription())
                .build();
        ActiviteCatalogue cree = service.creer(dto.getOpId(), activite);
        return new ResponseEntity<>(cree, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'activite.modifier')")
    public ResponseEntity<ActiviteCatalogue> modifier(@PathVariable UUID id, @RequestBody ActiviteCatalogueRequestDto dto) {
        ActiviteCatalogue details = ActiviteCatalogue.builder()
                .codeActivite(dto.getCodeActivite())
                .libelle(dto.getLibelle())
                .description(dto.getDescription())
                .build();
        ActiviteCatalogue misAJour = service.modifier(id, dto.getOpId(), details);
        return ResponseEntity.ok(misAJour);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'activite.supprimer')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasPermission(null, 'activite.exporter')")
    public ResponseEntity<byte[]> exporterCsv(
            @RequestParam(required = false) String codeActivite,
            @RequestParam(required = false) String libelle,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) UUID opId) {

        String csvData = service.exporterEnCsv(codeActivite, libelle, description, opId);
        byte[] bytes = csvData.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "activites_catalogue_export.csv");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasPermission(null, 'activite.exporter')")
    public ResponseEntity<byte[]> exporterPdf(
            @RequestParam(required = false) String codeActivite,
            @RequestParam(required = false) String libelle,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) UUID opId) {

        byte[] pdfBytes = service.exporterEnPdf(codeActivite, libelle, description, opId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "activites_catalogue_export.pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}