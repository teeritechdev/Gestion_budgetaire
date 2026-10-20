package com.budget.backend.budget.controller;

import com.budget.backend.budget.dto.AllocationStructureRequestDto;
import com.budget.backend.budget.dto.ExecutionStructureUpdateRequestDto;
import com.budget.backend.budget.entity.BudgetAllocationStructure;
import com.budget.backend.budget.service.BudgetAllocationStructureService;
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
@RequestMapping("/api/allocations-structures")
@CrossOrigin(origins = "*", maxAge = 3600)
public class BudgetAllocationStructureController {

    @Autowired
    private BudgetAllocationStructureService service;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'execution.lire')")
    public ResponseEntity<List<BudgetAllocationStructure>> listerOuFiltrer(
            @RequestParam(required = false) UUID structureId,
            @RequestParam(required = false) UUID allocationActiviteId) {
        if (structureId != null || allocationActiviteId != null) {
            return ResponseEntity.ok(service.filtrerAllocations(structureId, allocationActiviteId));
        }
        return ResponseEntity.ok(service.listerToutes());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'execution.creer')")
    public ResponseEntity<BudgetAllocationStructure> attribuer(@RequestBody AllocationStructureRequestDto dto) {
        BudgetAllocationStructure cree = service.attribuerBudgetStructure(dto.getAllocationActiviteId(), dto.getStructureId(), dto.getMontantVote());
        return new ResponseEntity<>(cree, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/execution")
    @PreAuthorize("hasPermission(null, 'execution.modifier')")
    public ResponseEntity<BudgetAllocationStructure> mettreAJourExecution(
            @PathVariable UUID id, @RequestBody ExecutionStructureUpdateRequestDto dto) {
        BudgetAllocationStructure misAJour = service.mettreAJourExecution(id, dto.getMontantEngage(), dto.getMontantLiquide());
        return ResponseEntity.ok(misAJour);
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasPermission(null, 'execution.exporter')")
    public ResponseEntity<byte[]> exporterCsv(@RequestParam(required = false) UUID structureId, @RequestParam(required = false) UUID allocationActiviteId) {
        byte[] bytes = service.exporterEnCsv(structureId, allocationActiviteId).getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "execution_structure_export.csv");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasPermission(null, 'execution.exporter')")
    public ResponseEntity<byte[]> exporterPdf(@RequestParam(required = false) UUID structureId, @RequestParam(required = false) UUID allocationActiviteId) {
        byte[] pdfBytes = service.exporterEnPdf(structureId, allocationActiviteId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "execution_structure_export.pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}