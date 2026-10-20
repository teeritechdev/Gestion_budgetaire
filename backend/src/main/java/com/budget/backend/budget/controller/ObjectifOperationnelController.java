package com.budget.backend.budget.controller;

import com.budget.backend.budget.dto.ObjectifOperationnelRequestDto;
import com.budget.backend.budget.entity.ObjectifOperationnel;
import com.budget.backend.budget.service.ObjectifOperationnelService;
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
@RequestMapping("/api/objectifs-operationnels")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ObjectifOperationnelController {

    @Autowired
    private ObjectifOperationnelService service;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'op.lire')")
    public ResponseEntity<List<ObjectifOperationnel>> listerOuFiltrer(
            @RequestParam(required = false) String codeOp,
            @RequestParam(required = false) String libelle,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) UUID osId) {

        if (codeOp != null || libelle != null || description != null || osId != null) {
            return ResponseEntity.ok(service.filtrerOp(codeOp, libelle, description, osId));
        }
        return ResponseEntity.ok(service.listerTous());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'op.lire')")
    public ResponseEntity<ObjectifOperationnel> obtenirParId(@PathVariable UUID id) {
        return service.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'op.creer')")
    public ResponseEntity<ObjectifOperationnel> creer(@RequestBody ObjectifOperationnelRequestDto dto) {
        ObjectifOperationnel op = ObjectifOperationnel.builder()
                .codeOp(dto.getCodeOp())
                .libelle(dto.getLibelle())
                .description(dto.getDescription())
                .build();
        ObjectifOperationnel cree = service.creer(dto.getOsId(), op);
        return new ResponseEntity<>(cree, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'op.modifier')")
    public ResponseEntity<ObjectifOperationnel> modifier(@PathVariable UUID id, @RequestBody ObjectifOperationnelRequestDto dto) {
        ObjectifOperationnel details = ObjectifOperationnel.builder()
                .codeOp(dto.getCodeOp())
                .libelle(dto.getLibelle())
                .description(dto.getDescription())
                .build();
        ObjectifOperationnel misAJour = service.modifier(id, dto.getOsId(), details);
        return ResponseEntity.ok(misAJour);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'op.supprimer')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasPermission(null, 'op.exporter')")
    public ResponseEntity<byte[]> exporterCsv(
            @RequestParam(required = false) String codeOp,
            @RequestParam(required = false) String libelle,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) UUID osId) {

        String csvData = service.exporterEnCsv(codeOp, libelle, description, osId);
        byte[] bytes = csvData.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "objectifs_operationnels_export.csv");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasPermission(null, 'op.exporter')")
    public ResponseEntity<byte[]> exporterPdf(
            @RequestParam(required = false) String codeOp,
            @RequestParam(required = false) String libelle,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) UUID osId) {

        byte[] pdfBytes = service.exporterEnPdf(codeOp, libelle, description, osId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "objectifs_operationnels_export.pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}