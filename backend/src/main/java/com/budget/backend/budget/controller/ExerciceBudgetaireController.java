package com.budget.backend.budget.controller;

import com.budget.backend.budget.dto.ExerciceBudgetaireRequestDto;
import com.budget.backend.budget.entity.ExerciceBudgetaire;
import com.budget.backend.budget.service.ExerciceBudgetaireService;
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
@RequestMapping("/api/exercices-budgetaires")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ExerciceBudgetaireController {

    @Autowired
    private ExerciceBudgetaireService service;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'exercice.lire')")
    public ResponseEntity<List<ExerciceBudgetaire>> listerOuFiltrer(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) UUID planId) {

        if (annee != null || statut != null || planId != null) {
            return ResponseEntity.ok(service.filtrerExercices(annee, statut, planId));
        }
        return ResponseEntity.ok(service.listerTous());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'exercice.lire')")
    public ResponseEntity<ExerciceBudgetaire> obtenirParId(@PathVariable UUID id) {
        return service.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'exercice.creer')")
    public ResponseEntity<ExerciceBudgetaire> creer(@RequestBody ExerciceBudgetaireRequestDto dto) {
        ExerciceBudgetaire exercice = ExerciceBudgetaire.builder()
                .annee(dto.getAnnee())
                .statut(dto.getStatut())
                .montantTotalVote(dto.getMontantTotalVote())
                .dateOuverture(dto.getDateOuverture())
                .dateFermeture(dto.getDateFermeture())
                .build();
        ExerciceBudgetaire cree = service.creer(dto.getPlanId(), exercice);
        return new ResponseEntity<>(cree, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'exercice.modifier')")
    public ResponseEntity<ExerciceBudgetaire> modifier(@PathVariable UUID id, @RequestBody ExerciceBudgetaireRequestDto dto) {
        ExerciceBudgetaire details = ExerciceBudgetaire.builder()
                .annee(dto.getAnnee())
                .statut(dto.getStatut())
                .montantTotalVote(dto.getMontantTotalVote())
                .dateOuverture(dto.getDateOuverture())
                .dateFermeture(dto.getDateFermeture())
                .build();
        ExerciceBudgetaire misAJour = service.modifier(id, dto.getPlanId(), details);
        return ResponseEntity.ok(misAJour);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'exercice.supprimer')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasPermission(null, 'exercice.exporter')")
    public ResponseEntity<byte[]> exporterCsv(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) UUID planId) {

        String csvData = service.exporterEnCsv(annee, statut, planId);
        byte[] bytes = csvData.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "exercices_budgetaires_export.csv");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasPermission(null, 'exercice.exporter')")
    public ResponseEntity<byte[]> exporterPdf(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) UUID planId) {

        byte[] pdfBytes = service.exporterEnPdf(annee, statut, planId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "exercices_budgetaires_export.pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}