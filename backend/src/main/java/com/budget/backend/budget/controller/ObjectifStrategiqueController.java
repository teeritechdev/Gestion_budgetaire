package com.budget.backend.budget.controller;

import com.budget.backend.budget.dto.ObjectifStrategiqueRequestDto;
import com.budget.backend.budget.entity.ObjectifStrategique;
import com.budget.backend.budget.service.ObjectifStrategiqueService;
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
@RequestMapping("/api/objectifs-strategiques")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ObjectifStrategiqueController {

    @Autowired
    private ObjectifStrategiqueService service;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'os.lire')")
    public ResponseEntity<List<ObjectifStrategique>> listerOuFiltrer(
            @RequestParam(required = false) String codeOs,
            @RequestParam(required = false) String libelle,
            @RequestParam(required = false) String description) {

        if (codeOs != null || libelle != null || description != null) {
            return ResponseEntity.ok(service.filtrerOs(codeOs, libelle, description));
        }
        return ResponseEntity.ok(service.listerTous());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'os.lire')")
    public ResponseEntity<ObjectifStrategique> obtenirParId(@PathVariable UUID id) {
        return service.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'os.creer')")
    public ResponseEntity<ObjectifStrategique> creer(@RequestBody ObjectifStrategiqueRequestDto dto) {
        ObjectifStrategique os = ObjectifStrategique.builder()
                .codeOs(dto.getCodeOs())
                .libelle(dto.getLibelle())
                .description(dto.getDescription())
                .build();
        ObjectifStrategique cree = service.creer(os);
        return new ResponseEntity<>(cree, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'os.modifier')")
    public ResponseEntity<ObjectifStrategique> modifier(@PathVariable UUID id, @RequestBody ObjectifStrategiqueRequestDto dto) {
        ObjectifStrategique details = ObjectifStrategique.builder()
                .codeOs(dto.getCodeOs())
                .libelle(dto.getLibelle())
                .description(dto.getDescription())
                .build();
        ObjectifStrategique misAJour = service.modifier(id, details);
        return ResponseEntity.ok(misAJour);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'os.supprimer')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasPermission(null, 'os.exporter')")
    public ResponseEntity<byte[]> exporterCsv(
            @RequestParam(required = false) String codeOs,
            @RequestParam(required = false) String libelle,
            @RequestParam(required = false) String description) {

        String csvData = service.exporterEnCsv(codeOs, libelle, description);
        byte[] bytes = csvData.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "objectifs_strategiques_export.csv");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasPermission(null, 'os.exporter')")
    public ResponseEntity<byte[]> exporterPdf(
            @RequestParam(required = false) String codeOs,
            @RequestParam(required = false) String libelle,
            @RequestParam(required = false) String description) {

        byte[] pdfBytes = service.exporterEnPdf(codeOs, libelle, description);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "objectifs_strategiques_export.pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}