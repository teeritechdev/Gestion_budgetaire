package com.budget.backend.budget.controller;

import com.budget.backend.budget.dto.EntiteStatistiqueCleRequestDto;
import com.budget.backend.budget.entity.EntiteStatistiqueCle;
import com.budget.backend.budget.service.EntiteStatistiqueCleService;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/statistiques-cles")
public class EntiteStatistiqueCleController {

    @Autowired
    private EntiteStatistiqueCleService service;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'statistique_cle.lire')")
    public List<EntiteStatistiqueCle> listerTous() {
        return service.listerTous();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'statistique_cle.lire')")
    public ResponseEntity<EntiteStatistiqueCle> trouverParId(@PathVariable UUID id) {
        return service.trouverParId(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'statistique_cle.creer')")
    public EntiteStatistiqueCle creer(@Valid @RequestBody EntiteStatistiqueCleRequestDto dto) {
        return service.creer(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'statistique_cle.modifier')")
    public EntiteStatistiqueCle modifier(@PathVariable UUID id, @Valid @RequestBody EntiteStatistiqueCleRequestDto dto) {
        return service.modifier(id, dto);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> gererValidation(MethodArgumentNotValidException exception) {
        Map<String, String> erreurs = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                erreurs.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of(
                "message", "Les données du formulaire sont invalides.",
                "errors", erreurs));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'statistique_cle.supprimer')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/filtrer")
    @PreAuthorize("hasPermission(null, 'statistique_cle.lire')")
    public List<EntiteStatistiqueCle> filtrer(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) Integer mois,
            @RequestParam(required = false) String entiteCode) {
        return service.filtrer(annee, mois, entiteCode);
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasPermission(null, 'statistique_cle.exporter')")
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
    @PreAuthorize("hasPermission(null, 'statistique_cle.exporter')")
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
