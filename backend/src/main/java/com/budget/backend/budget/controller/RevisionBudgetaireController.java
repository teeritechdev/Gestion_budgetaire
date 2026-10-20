package com.budget.backend.budget.controller;

import com.budget.backend.budget.dto.DemandeRevisionRequestDto;
import com.budget.backend.budget.dto.ValidationRevisionRequestDto;
import com.budget.backend.budget.entity.DemandeRevisionBudgetaire;
import com.budget.backend.budget.entity.LigneRevisionBudgetaire;
import com.budget.backend.budget.service.RevisionBudgetaireService;
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
@RequestMapping("/api/revisions-budgetaires")
@CrossOrigin(origins = "*", maxAge = 3600)
public class RevisionBudgetaireController {

    @Autowired
    private RevisionBudgetaireService service;

    // --- ENDPOINTS DEMANDES DE RÉVISION ---

    @GetMapping
    @PreAuthorize("hasPermission(null, 'revision.lire')")
    public ResponseEntity<List<DemandeRevisionBudgetaire>> listerOuFiltrer(
            @RequestParam(required = false) Integer anneeExercice,
            @RequestParam(required = false) String statut) {
        if (anneeExercice != null || statut != null) {
            return ResponseEntity.ok(service.filtrerDemandes(anneeExercice, statut));
        }
        return ResponseEntity.ok(service.listerToutes());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'revision.lire')")
    public ResponseEntity<DemandeRevisionBudgetaire> obtenirParId(@PathVariable UUID id) {
        return service.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'revision.creer')")
    public ResponseEntity<DemandeRevisionBudgetaire> creer(@RequestBody DemandeRevisionRequestDto dto) {
        DemandeRevisionBudgetaire demande = DemandeRevisionBudgetaire.builder()
                .numeroDemande(dto.getNumeroDemande())
                .anneeExercice(dto.getAnneeExercice())
                .demandeurId(dto.getDemandeurId())
                .motif(dto.getMotif())
                .build();
        DemandeRevisionBudgetaire cree = service.creerDemandeAvecLignes(demande, dto.getLignes());
        return new ResponseEntity<>(cree, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/valider")
    @PreAuthorize("hasPermission(null, 'revision.valider')")
    public ResponseEntity<DemandeRevisionBudgetaire> valider(
            @PathVariable UUID id, @RequestBody ValidationRevisionRequestDto dto) {
        DemandeRevisionBudgetaire valide = service.validerDemande(id, dto.getValideurId(), dto.getCommentaire());
        return ResponseEntity.ok(valide);
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasPermission(null, 'revision.exporter')")
    public ResponseEntity<byte[]> exporterCsv(
            @RequestParam(required = false) Integer anneeExercice,
            @RequestParam(required = false) String statut) {
        byte[] bytes = service.exporterEnCsv(anneeExercice, statut).getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "revisions_budgetaires_export.csv");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasPermission(null, 'revision.exporter')")
    public ResponseEntity<byte[]> exporterPdf(
            @RequestParam(required = false) Integer anneeExercice,
            @RequestParam(required = false) String statut) {
        byte[] pdfBytes = service.exporterEnPdf(anneeExercice, statut);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "revisions_budgetaires_export.pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    // --- ENDPOINTS DÉTAILS LIGNES DE RÉVISION ---

    @GetMapping("/{id}/lignes")
    @PreAuthorize("hasPermission(null, 'revision.lire')")
    public ResponseEntity<List<LigneRevisionBudgetaire>> obtenirLignesParDemande(@PathVariable UUID id) {
        return ResponseEntity.ok(service.listerLignesParDemande(id));
    }

    @GetMapping("/lignes")
    @PreAuthorize("hasPermission(null, 'revision.lire')")
    public ResponseEntity<List<LigneRevisionBudgetaire>> listerOuFiltrerLignes(
            @RequestParam(required = false) UUID demandeId,
            @RequestParam(required = false) String typeMouvement,
            @RequestParam(required = false) UUID allocationSourceId,
            @RequestParam(required = false) UUID allocationDestinationId) {
        return ResponseEntity.ok(service.filtrerLignes(demandeId, typeMouvement, allocationSourceId, allocationDestinationId));
    }

    @GetMapping("/lignes/export/csv")
    @PreAuthorize("hasPermission(null, 'revision.exporter')")
    public ResponseEntity<byte[]> exporterLignesCsv(
            @RequestParam(required = false) UUID demandeId,
            @RequestParam(required = false) String typeMouvement,
            @RequestParam(required = false) UUID allocationSourceId,
            @RequestParam(required = false) UUID allocationDestinationId) {
        byte[] bytes = service.exporterLignesCsv(demandeId, typeMouvement, allocationSourceId, allocationDestinationId).getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "lignes_revision_export.csv");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @GetMapping("/lignes/export/pdf")
    @PreAuthorize("hasPermission(null, 'revision.exporter')")
    public ResponseEntity<byte[]> exporterLignesPdf(
            @RequestParam(required = false) UUID demandeId,
            @RequestParam(required = false) String typeMouvement,
            @RequestParam(required = false) UUID allocationSourceId,
            @RequestParam(required = false) UUID allocationDestinationId) {
        byte[] pdfBytes = service.exporterLignesPdf(demandeId, typeMouvement, allocationSourceId, allocationDestinationId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "lignes_revision_export.pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}