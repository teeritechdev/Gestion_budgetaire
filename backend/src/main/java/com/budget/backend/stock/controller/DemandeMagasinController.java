package com.budget.backend.stock.controller;

import com.budget.backend.common.PageResponseDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import com.budget.backend.stock.dto.DemandeMagasinRequestDto;
import com.budget.backend.stock.dto.DemandeMagasinResponseDto;
import com.budget.backend.stock.dto.DemandeMagasinServieDto;
import com.budget.backend.stock.dto.ValidationDecisionDto;
import com.budget.backend.stock.service.DemandeMagasinService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/demandes-magasin")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DemandeMagasinController {

    @Autowired
    private DemandeMagasinService service;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'demande_magasin.lire')")
    public ResponseEntity<PageResponseDto<DemandeMagasinResponseDto>> listerOuFiltrer(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) UUID demandeurId,
            @RequestParam(required = false) String entiteCode,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.filtrer(statut, demandeurId, entiteCode, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'demande_magasin.lire')")
    public ResponseEntity<DemandeMagasinResponseDto> obtenirParId(@PathVariable UUID id) {
        return service.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'demande_magasin.creer')")
    public ResponseEntity<DemandeMagasinResponseDto> creer(@Valid @RequestBody DemandeMagasinRequestDto dto) {
        return new ResponseEntity<>(service.creer(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/valider-n1")
    @PreAuthorize("hasPermission(null, 'demande_magasin.valider')")
    public ResponseEntity<DemandeMagasinResponseDto> validerN1(@PathVariable UUID id, @Valid @RequestBody ValidationDecisionDto dto) {
        return ResponseEntity.ok(service.validerN1(id, dto));
    }

    @PutMapping("/{id}/servir")
    @PreAuthorize("hasPermission(null, 'demande_magasin.servir')")
    public ResponseEntity<DemandeMagasinResponseDto> servir(@PathVariable UUID id, @Valid @RequestBody DemandeMagasinServieDto dto) {
        return ResponseEntity.ok(service.servir(id, dto));
    }
}
