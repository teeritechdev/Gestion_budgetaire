package com.budget.backend.budget.controller;

import com.budget.backend.budget.entity.JournalMouvementsBudgetaire;
import com.budget.backend.budget.service.JournalMouvementsBudgetaireService;
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
@RequestMapping("/api/journal-mouvements")
@CrossOrigin(origins = "*", maxAge = 3600)
public class JournalMouvementsBudgetaireController {

    @Autowired
    private JournalMouvementsBudgetaireService service;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'journal.lire')")
    public ResponseEntity<List<JournalMouvementsBudgetaire>> listerOuFiltrer(
            @RequestParam(required = false) Integer anneeExercice,
            @RequestParam(required = false) UUID allocationStructureId,
            @RequestParam(required = false) String typeMouvement) {
        if (anneeExercice != null || allocationStructureId != null || typeMouvement != null) {
            return ResponseEntity.ok(service.filtrerMouvements(anneeExercice, allocationStructureId, typeMouvement));
        }
        return ResponseEntity.ok(service.listerTous());
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasPermission(null, 'journal.exporter')")
    public ResponseEntity<byte[]> exporterCsv(
            @RequestParam(required = false) Integer anneeExercice,
            @RequestParam(required = false) UUID allocationStructureId,
            @RequestParam(required = false) String typeMouvement) {
        byte[] bytes = service.exporterEnCsv(anneeExercice, allocationStructureId, typeMouvement).getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "journal_mouvements_export.csv");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasPermission(null, 'journal.exporter')")
    public ResponseEntity<byte[]> exporterPdf(
            @RequestParam(required = false) Integer anneeExercice,
            @RequestParam(required = false) UUID allocationStructureId,
            @RequestParam(required = false) String typeMouvement) {
        byte[] pdfBytes = service.exporterEnPdf(anneeExercice, allocationStructureId, typeMouvement);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "journal_mouvements_export.pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}