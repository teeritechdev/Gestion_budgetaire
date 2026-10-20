package com.budget.backend.budget.controller;

import com.budget.backend.budget.dto.AllocationOpRequestDto;
import com.budget.backend.budget.entity.BudgetAllocationOp;
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
@RequestMapping("/api/allocations-op")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AllocationOpController {

    @Autowired
    private CascadeAllocationService service;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'allocation_op.lire')")
    public ResponseEntity<List<BudgetAllocationOp>> listerOuFiltrer(
            @RequestParam(required = false) UUID allocationOsId,
            @RequestParam(required = false) UUID opId) {
        if (allocationOsId != null || opId != null) {
            return ResponseEntity.ok(service.filtrerAllocationsOp(allocationOsId, opId));
        }
        return ResponseEntity.ok(service.listerAllocationsOp());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'allocation_op.creer')")
    public ResponseEntity<BudgetAllocationOp> creer(@RequestBody AllocationOpRequestDto dto) {
        BudgetAllocationOp cree = service.allouerOsVersOp(dto.getAllocationOsId(), dto.getOpId(), dto.getMontantAlloue());
        return new ResponseEntity<>(cree, HttpStatus.CREATED);
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasPermission(null, 'allocation_op.exporter')")
    public ResponseEntity<byte[]> exporterCsv(@RequestParam(required = false) UUID allocationOsId, @RequestParam(required = false) UUID opId) {
        byte[] bytes = service.exporterOpCsv(allocationOsId, opId).getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "allocation_op_export.csv");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasPermission(null, 'allocation_op.exporter')")
    public ResponseEntity<byte[]> exporterPdf(@RequestParam(required = false) UUID allocationOsId, @RequestParam(required = false) UUID opId) {
        byte[] pdfBytes = service.exporterOpPdf(allocationOsId, opId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "allocation_op_export.pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}