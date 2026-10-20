package com.budget.backend.budget.controller;

import com.budget.backend.budget.dto.AllocationOsRequestDto;
import com.budget.backend.budget.entity.BudgetAllocationOs;
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
@RequestMapping("/api/allocations-os")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AllocationOsController {

    @Autowired
    private CascadeAllocationService service;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'allocation_os.lire')")
    public ResponseEntity<List<BudgetAllocationOs>> listerOuFiltrer(
            @RequestParam(required = false) UUID exerciceId,
            @RequestParam(required = false) UUID osId) {
        if (exerciceId != null || osId != null) {
            return ResponseEntity.ok(service.filtrerAllocationsOs(exerciceId, osId));
        }
        return ResponseEntity.ok(service.listerAllocationsOs());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'allocation_os.creer')")
    public ResponseEntity<BudgetAllocationOs> creer(@RequestBody AllocationOsRequestDto dto) {
        BudgetAllocationOs cree = service.allouerExerciceVersOs(dto.getExerciceId(), dto.getOsId(), dto.getMontantAlloue());
        return new ResponseEntity<>(cree, HttpStatus.CREATED);
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasPermission(null, 'allocation_os.exporter')")
    public ResponseEntity<byte[]> exporterCsv(@RequestParam(required = false) UUID exerciceId, @RequestParam(required = false) UUID osId) {
        byte[] bytes = service.exporterOsCsv(exerciceId, osId).getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "allocation_os_export.csv");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasPermission(null, 'allocation_os.exporter')")
    public ResponseEntity<byte[]> exporterPdf(@RequestParam(required = false) UUID exerciceId, @RequestParam(required = false) UUID osId) {
        byte[] pdfBytes = service.exporterOsPdf(exerciceId, osId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "allocation_os_export.pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}