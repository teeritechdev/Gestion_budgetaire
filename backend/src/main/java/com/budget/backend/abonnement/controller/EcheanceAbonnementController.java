package com.budget.backend.abonnement.controller;

import com.budget.backend.abonnement.dto.EcheanceResponseDto;
import com.budget.backend.abonnement.dto.EcheanceUpdateDto;
import com.budget.backend.abonnement.service.EcheanceAbonnementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/echeances")
@CrossOrigin(origins = "*", maxAge = 3600)
public class EcheanceAbonnementController {

    @Autowired
    private EcheanceAbonnementService service;

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'abonnement.lire')")
    public ResponseEntity<EcheanceResponseDto> obtenirParId(@PathVariable UUID id) {
        return service.trouverParId(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'echeance.modifier')")
    public ResponseEntity<EcheanceResponseDto> modifier(@PathVariable UUID id, @Valid @RequestBody EcheanceUpdateDto dto) {
        return ResponseEntity.ok(service.modifier(id, dto));
    }
}
