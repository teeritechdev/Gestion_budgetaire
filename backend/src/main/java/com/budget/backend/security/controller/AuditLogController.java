package com.budget.backend.security.controller;

import com.budget.backend.security.entity.AuditLog;
import com.budget.backend.security.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit-logs")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'audit.lire')")
    public ResponseEntity<List<AuditLog>> listerOuFiltrer(
            @RequestParam(required = false) UUID utilisateurId,
            @RequestParam(required = false) String typeAction,
            @RequestParam(required = false) String nomTable,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        if (utilisateurId != null || typeAction != null || nomTable != null || startDate != null || endDate != null) {
            return ResponseEntity.ok(auditLogService.filtrerAuditLogs(utilisateurId, typeAction, nomTable, startDate, endDate));
        }
        return ResponseEntity.ok(auditLogService.listerTousLesLogs());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'audit.lire')")
    public ResponseEntity<AuditLog> obtenirParId(@PathVariable UUID id) {
        return auditLogService.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasPermission(null, 'audit.exporter')")
    public ResponseEntity<byte[]> exporterCsv(
            @RequestParam(required = false) UUID utilisateurId,
            @RequestParam(required = false) String typeAction,
            @RequestParam(required = false) String nomTable,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        String csvData = auditLogService.exporterEnCsv(utilisateurId, typeAction, nomTable, startDate, endDate);
        byte[] bytes = csvData.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "audit_logs_export.csv");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasPermission(null, 'audit.exporter')")
    public ResponseEntity<byte[]> exporterPdf(
            @RequestParam(required = false) UUID utilisateurId,
            @RequestParam(required = false) String typeAction,
            @RequestParam(required = false) String nomTable,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        byte[] pdfBytes = auditLogService.exporterEnPdf(utilisateurId, typeAction, nomTable, startDate, endDate);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "audit_logs_export.pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}