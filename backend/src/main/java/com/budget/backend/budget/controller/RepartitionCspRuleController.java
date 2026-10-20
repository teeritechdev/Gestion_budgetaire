package com.budget.backend.budget.controller;

import com.budget.backend.budget.dto.RepartitionCspRuleRequestDto;
import com.budget.backend.budget.entity.RepartitionCspRule;
import com.budget.backend.budget.service.RepartitionCspRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/repartition-csp")
public class RepartitionCspRuleController {

    @Autowired
    private RepartitionCspRuleService service;

    @GetMapping
    public List<RepartitionCspRule> listerTous() {
        return service.listerTous();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RepartitionCspRule> trouverParId(@PathVariable UUID id) {
        return service.trouverParId(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public RepartitionCspRule creer(@RequestBody RepartitionCspRuleRequestDto dto) {
        return service.creer(dto);
    }

    @PutMapping("/{id}")
    public RepartitionCspRule modifier(@PathVariable UUID id, @RequestBody RepartitionCspRuleRequestDto dto) {
        return service.modifier(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/filtrer")
    public List<RepartitionCspRule> filtrer(
            @RequestParam(required = false) String entiteBeneficiaire,
            @RequestParam(required = false) String cleType) {
        return service.filtrer(entiteBeneficiaire, cleType);
    }

    @GetMapping("/export/csv")
    public ResponseEntity<String> exporterCsv(
            @RequestParam(required = false) String entiteBeneficiaire,
            @RequestParam(required = false) String cleType) {
        String csv = service.exporterEnCsv(entiteBeneficiaire, cleType);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=repartition_csp.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exporterPdf(
            @RequestParam(required = false) String entiteBeneficiaire,
            @RequestParam(required = false) String cleType) {
        byte[] pdf = service.exporterEnPdf(entiteBeneficiaire, cleType);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=repartition_csp.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}