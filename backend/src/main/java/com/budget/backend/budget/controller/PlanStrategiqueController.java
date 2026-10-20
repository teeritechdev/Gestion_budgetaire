package com.budget.backend.budget.controller;

import com.budget.backend.budget.dto.PlanStrategiqueRequestDto;
import com.budget.backend.budget.entity.PlanStrategique;
import com.budget.backend.budget.service.PlanStrategiqueService;
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
@RequestMapping("/api/plans-strategiques")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PlanStrategiqueController {

    @Autowired
    private PlanStrategiqueService planStrategiqueService;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'plan.lire')")
    public ResponseEntity<List<PlanStrategique>> listerOuFiltrer(
            @RequestParam(required = false) String codePlan,
            @RequestParam(required = false) String libelle,
            @RequestParam(required = false) Boolean actif) {

        if (codePlan != null || libelle != null || actif != null) {
            return ResponseEntity.ok(planStrategiqueService.filtrerPlans(codePlan, libelle, actif));
        }
        return ResponseEntity.ok(planStrategiqueService.listerTous());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'plan.lire')")
    public ResponseEntity<PlanStrategique> obtenirParId(@PathVariable UUID id) {
        return planStrategiqueService.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'plan.creer')")
    public ResponseEntity<PlanStrategique> creer(@RequestBody PlanStrategiqueRequestDto dto) {
        PlanStrategique plan = PlanStrategique.builder()
                .codePlan(dto.getCodePlan())
                .libelle(dto.getLibelle())
                .dateDebut(dto.getDateDebut())
                .dateFin(dto.getDateFin())
                .actif(dto.getActif() != null ? dto.getActif() : true)
                .build();
        PlanStrategique cree = planStrategiqueService.creer(plan);
        return new ResponseEntity<>(cree, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'plan.modifier')")
    public ResponseEntity<PlanStrategique> modifier(@PathVariable UUID id, @RequestBody PlanStrategiqueRequestDto dto) {
        PlanStrategique details = PlanStrategique.builder()
                .codePlan(dto.getCodePlan())
                .libelle(dto.getLibelle())
                .dateDebut(dto.getDateDebut())
                .dateFin(dto.getDateFin())
                .actif(dto.getActif())
                .build();
        PlanStrategique misAJour = planStrategiqueService.modifier(id, details);
        return ResponseEntity.ok(misAJour);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'plan.supprimer')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        planStrategiqueService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasPermission(null, 'plan.exporter')")
    public ResponseEntity<byte[]> exporterCsv(
            @RequestParam(required = false) String codePlan,
            @RequestParam(required = false) String libelle,
            @RequestParam(required = false) Boolean actif) {

        // Transmission des filtres multicritères à la méthode du service
        String csvData = planStrategiqueService.exporterEnCsv(codePlan, libelle, actif);
        byte[] bytes = csvData.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "plans_strategiques_export.csv");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasPermission(null, 'plan.exporter')")
    public ResponseEntity<byte[]> exporterPdf(
            @RequestParam(required = false) String codePlan,
            @RequestParam(required = false) String libelle,
            @RequestParam(required = false) Boolean actif) {

        // Si tu souhaites gérer le PDF comme dans le module 1, tu pourras ajouter la méthode correspondante dans le service
        // Pour l'instant, structure de base prête pour l'export PDF filtré
        byte[] pdfBytes = planStrategiqueService.exporterEnPdf(codePlan, libelle, actif);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "plans_strategiques_export.pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}