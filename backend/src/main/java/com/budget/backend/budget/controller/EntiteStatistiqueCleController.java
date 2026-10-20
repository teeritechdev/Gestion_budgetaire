package com.budget.backend.budget.controller;

import com.budget.backend.budget.dto.EntiteStatistiqueCleRequestDto;
import com.budget.backend.budget.entity.EntiteStatistiqueCle;
import com.budget.backend.budget.service.EntiteStatistiqueCleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/statistiques-cles")
public class EntiteStatistiqueCleController {

    @Autowired
    private EntiteStatistiqueCleService service;

    @GetMapping
    public List<EntiteStatistiqueCle> listerTous() {
        return service.listerTous();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntiteStatistiqueCle> trouverParId(@PathVariable UUID id) {
        return service.trouverParId(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public EntiteStatistiqueCle creer(@RequestBody EntiteStatistiqueCleRequestDto dto) {
        return service.creer(dto);
    }

    @PutMapping("/{id}")
    public EntiteStatistiqueCle modifier(@PathVariable UUID id, @RequestBody EntiteStatistiqueCleRequestDto dto) {
        return service.modifier(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/filtrer")
    public List<EntiteStatistiqueCle> filtrer(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer mois,
            @RequestParam(required = false) String entiteCode) {
        return service.filtrer(annee, mois, entiteCode);
    }

    @GetMapping("/export/csv")
    public ResponseEntity<String> exporterCsv(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer mois,
            @RequestParam(required = false) String entiteCode) {
        String csv = service.exporterEnCsv(annee, mois, entiteCode);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=statistiques_cles.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exporterPdf(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer mois,
            @RequestParam(required = false) String entiteCode) {
        byte[] pdf = service.exporterEnPdf(annee, mois, entiteCode);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=statistiques_cles.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}