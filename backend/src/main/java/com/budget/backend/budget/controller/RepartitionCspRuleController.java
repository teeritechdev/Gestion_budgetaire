package com.budget.backend.budget.controller;

import com.budget.backend.budget.dto.RepartitionCspRuleRequestDto;
import com.budget.backend.budget.entity.RepartitionCspRule;
import com.budget.backend.budget.service.RepartitionCspRuleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/repartition-csp")
public class RepartitionCspRuleController {

    @Autowired
    private RepartitionCspRuleService service;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'repartition_csp.lire')")
    public List<RepartitionCspRule> listerTous() {
        return service.listerTous();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'repartition_csp.lire')")
    public ResponseEntity<RepartitionCspRule> trouverParId(@PathVariable UUID id) {
        return service.trouverParId(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'repartition_csp.creer')")
    public RepartitionCspRule creer(@Valid @RequestBody RepartitionCspRuleRequestDto dto) {
        return service.creer(dto);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasPermission(null, 'repartition_csp.creer')")
    public List<RepartitionCspRule> enregistrerRepartition(
            @Valid @RequestBody List<@Valid RepartitionCspRuleRequestDto> dtos) {
        return service.enregistrerRepartition(dtos);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'repartition_csp.modifier')")
    public RepartitionCspRule modifier(@PathVariable UUID id, @Valid @RequestBody RepartitionCspRuleRequestDto dto) {
        return service.modifier(id, dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'repartition_csp.supprimer')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/filtrer")
    @PreAuthorize("hasPermission(null, 'repartition_csp.lire')")
    public List<RepartitionCspRule> filtrer(
            @RequestParam(required = false) String entiteBeneficiaire,
            @RequestParam(required = false) String cleType) {
        return service.filtrer(entiteBeneficiaire, cleType);
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasPermission(null, 'repartition_csp.lire')")
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
    @PreAuthorize("hasPermission(null, 'repartition_csp.lire')")
    public ResponseEntity<byte[]> exporterPdf(
            @RequestParam(required = false) String entiteBeneficiaire,
            @RequestParam(required = false) String cleType) {
        byte[] pdf = service.exporterEnPdf(entiteBeneficiaire, cleType);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=repartition_csp.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> gererValidation(MethodArgumentNotValidException exception) {
        Map<String, String> erreurs = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            erreurs.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(Map.of(
                "message", "Les données de la règle de répartition sont invalides.",
                "errors", erreurs));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> gererErreurMetier(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String message = exception.getReason() == null
                ? "L’opération sur la règle de répartition a échoué."
                : exception.getReason();
        return ResponseEntity.status(status).body(Map.of("message", message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> gererConflitIntegrite() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "message",
                "La règle n’a pas pu être enregistrée car une activité, une allocation ou une entité référencée n’existe plus."));
    }
}
