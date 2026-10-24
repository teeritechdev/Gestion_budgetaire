package com.budget.backend.achat.controller;

import com.budget.backend.common.PageResponseDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import com.budget.backend.achat.dto.FournisseurRequestDto;
import com.budget.backend.achat.dto.FournisseurResponseDto;
import com.budget.backend.achat.service.FournisseurService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/fournisseurs")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FournisseurController {

    @Autowired
    private FournisseurService service;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'fournisseur.lire')")
    public ResponseEntity<PageResponseDto<FournisseurResponseDto>> listerOuFiltrer(
            @RequestParam(required = false) String raisonSociale,
            @RequestParam(required = false) String ifu,
            @RequestParam(required = false) Boolean actif,
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20, sort = "raisonSociale") Pageable pageable) {
        return ResponseEntity.ok(service.filtrer(raisonSociale, ifu, actif, query, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'fournisseur.lire')")
    public ResponseEntity<FournisseurResponseDto> obtenirParId(@PathVariable UUID id) {
        return service.trouverParId(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'fournisseur.creer')")
    public ResponseEntity<FournisseurResponseDto> creer(@Valid @RequestBody FournisseurRequestDto dto) {
        return new ResponseEntity<>(service.creer(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'fournisseur.modifier')")
    public ResponseEntity<FournisseurResponseDto> modifier(@PathVariable UUID id, @Valid @RequestBody FournisseurRequestDto dto) {
        return ResponseEntity.ok(service.modifier(id, dto));
    }
}
