package com.budget.backend.budget.controller;

import com.budget.backend.budget.dto.AllocationActiviteRequestDto;
import com.budget.backend.budget.entity.BudgetAllocationActivite;
import com.budget.backend.budget.service.CascadeAllocationService;
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
@RequestMapping("/api/allocations-activites")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AllocationActiviteController {

    @Autowired
    private CascadeAllocationService service;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'allocation_activite.lire')")
    public ResponseEntity<List<BudgetAllocationActivite>> listerOuFiltrer(
            @RequestParam(required = false) UUID allocationOpId,
            @RequestParam(required = false) UUID activiteCatalogueId) {
        if (allocationOpId != null || activiteCatalogueId != null) {
            return ResponseEntity.ok(service.filtrerAllocationsActivite(allocationOpId, activiteCatalogueId));
        }
        return ResponseEntity.ok(service.listerAllocationsActivite());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'allocation_activite.creer')")
    public ResponseEntity<BudgetAllocationActivite> creer(@RequestBody AllocationActiviteRequestDto dto) {
        BudgetAllocationActivite cree = service.allouerOpVersActivite(dto.getAllocationOpId(), dto.getActiviteCatalogueId(), dto.getMontantAlloue());
        return new ResponseEntity<>(cree, HttpStatus.CREATED);
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasPermission(null, 'allocation_activite.exporter')")
    public ResponseEntity<byte[]> exporterCsv(@RequestParam(required = false) UUID allocationOpId, @RequestParam(required = false) UUID activiteCatalogueId) {
        byte[] bytes = service.exporterActiviteCsv(allocationOpId, activiteCatalogueId).getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "allocation_activite_export.csv");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasPermission(null, 'allocation_activite.exporter')")
    public ResponseEntity<byte[]> exporterPdf(@RequestParam(required = false) UUID allocationOpId, @RequestParam(required = false) UUID activiteCatalogueId) {
        byte[] pdfBytes = service.exporterActivitePdf(allocationOpId, activiteCatalogueId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "allocation_activite_export.pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}