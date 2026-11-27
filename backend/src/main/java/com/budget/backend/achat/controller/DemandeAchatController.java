package com.budget.backend.achat.controller;

import com.budget.backend.achat.dto.DecisionValidationDto;
import com.budget.backend.achat.dto.DemandeAchatRequestDto;
import com.budget.backend.achat.dto.DemandeAchatResponseDto;
import com.budget.backend.achat.dto.HistoriqueValidationResponseDto;
import com.budget.backend.achat.dto.VentilationImpactRequestDto;
import com.budget.backend.achat.dto.VentilationImpactResponseDto;
import com.budget.backend.achat.service.DemandeAchatService;
import com.budget.backend.common.PageResponseDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/demandes-achat")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DemandeAchatController {

    @Autowired
    private DemandeAchatService service;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'demande_achat.lire')")
    public ResponseEntity<PageResponseDto<DemandeAchatResponseDto>> lister(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) UUID demandeurId,
            @RequestParam(required = false) String typeAchat,
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) String recherche,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.filtrer(statut, demandeurId, typeAchat, annee, recherche, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'demande_achat.lire')")
    public ResponseEntity<DemandeAchatResponseDto> obtenirParId(@PathVariable UUID id) {
        return service.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'demande_achat.creer')")
    public ResponseEntity<DemandeAchatResponseDto> creer(@Valid @RequestBody DemandeAchatRequestDto dto) {
        return new ResponseEntity<>(service.creer(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'demande_achat.modifier')")
    public ResponseEntity<DemandeAchatResponseDto> modifier(@PathVariable UUID id, @Valid @RequestBody DemandeAchatRequestDto dto) {
        return ResponseEntity.ok(service.modifier(id, dto));
    }

    @PutMapping("/{id}/valider")
    @PreAuthorize("hasPermission(null, 'demande_achat.valider')")
    public ResponseEntity<DemandeAchatResponseDto> valider(@PathVariable UUID id, @Valid @RequestBody DecisionValidationDto dto) {
        return ResponseEntity.ok(service.valider(id, dto));
    }

    @GetMapping("/{id}/historique-validations")
    @PreAuthorize("hasPermission(null, 'demande_achat.lire')")
    public ResponseEntity<List<HistoriqueValidationResponseDto>> obtenirHistoriqueValidations(@PathVariable UUID id) {
        return ResponseEntity.ok(service.obtenirHistoriqueValidations(id));
    }

    @GetMapping("/{id}/ventilations")
    @PreAuthorize("hasPermission(null, 'demande_achat.lire')")
    public ResponseEntity<List<VentilationImpactResponseDto>> obtenirVentilations(@PathVariable UUID id) {
        return ResponseEntity.ok(service.obtenirVentilations(id));
    }

    @PutMapping("/{id}/ventilations")
    @PreAuthorize("hasPermission(null, 'demande_achat.modifier')")
    public ResponseEntity<List<VentilationImpactResponseDto>> enregistrerVentilations(
            @PathVariable UUID id,
            @Valid @RequestBody List<VentilationImpactRequestDto> dtos) {
        return ResponseEntity.ok(service.enregistrerVentilations(id, dtos));
    }
}

