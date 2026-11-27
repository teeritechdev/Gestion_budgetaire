package com.budget.backend.abonnement.controller;

import com.budget.backend.abonnement.dto.AbonnementRequestDto;
import com.budget.backend.abonnement.dto.AbonnementResponseDto;
import com.budget.backend.abonnement.dto.EcheanceAbonnementResponseDto;
import com.budget.backend.abonnement.dto.RapprochementFactureDto;
import com.budget.backend.abonnement.service.AbonnementService;
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
@RequestMapping("/api/abonnements")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AbonnementController {

    @Autowired
    private AbonnementService service;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'abonnement.lire')")
    public ResponseEntity<PageResponseDto<AbonnementResponseDto>> lister(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) UUID fournisseurId,
            @RequestParam(required = false) UUID responsableId,
            @RequestParam(required = false) String recherche,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.filtrer(statut, fournisseurId, responsableId, recherche, pageable));
    }

    @GetMapping("/echeances")
    @PreAuthorize("hasPermission(null, 'abonnement.lire')")
    public ResponseEntity<PageResponseDto<EcheanceAbonnementResponseDto>> listerToutesEcheances(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) UUID abonnementId,
            @PageableDefault(size = 20, sort = "dateEcheance", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(service.filtrerToutesEcheances(statut, abonnementId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'abonnement.lire')")
    public ResponseEntity<AbonnementResponseDto> obtenirParId(@PathVariable UUID id) {
        return service.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'abonnement.creer')")
    public ResponseEntity<AbonnementResponseDto> creer(@Valid @RequestBody AbonnementRequestDto dto) {
        return new ResponseEntity<>(service.creer(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'abonnement.modifier')")
    public ResponseEntity<AbonnementResponseDto> modifier(@PathVariable UUID id, @Valid @RequestBody AbonnementRequestDto dto) {
        return ResponseEntity.ok(service.modifier(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'abonnement.supprimer')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/echeances")
    @PreAuthorize("hasPermission(null, 'abonnement.lire')")
    public ResponseEntity<List<EcheanceAbonnementResponseDto>> obtenirEcheances(@PathVariable UUID id) {
        return ResponseEntity.ok(service.obtenirEcheances(id));
    }

    @PostMapping("/{id}/generer-echeances")
    @PreAuthorize("hasPermission(null, 'abonnement.modifier')")
    public ResponseEntity<List<EcheanceAbonnementResponseDto>> genererEcheances(@PathVariable UUID id) {
        return ResponseEntity.ok(service.genererEcheancier(id));
    }

    @PostMapping("/{id}/echeances/{echeanceId}/generer-bc")
    @PreAuthorize("hasPermission(null, 'bon_commande.creer') or hasPermission(null, 'abonnement.modifier')")
    public ResponseEntity<EcheanceAbonnementResponseDto> genererBcPourEcheance(
            @PathVariable UUID id, @PathVariable UUID echeanceId) {
        return ResponseEntity.ok(service.genererBcPourEcheance(id, echeanceId));
    }

    @PostMapping("/{id}/echeances/{echeanceId}/rapprocher-facture")
    @PreAuthorize("hasPermission(null, 'abonnement.modifier')")
    public ResponseEntity<EcheanceAbonnementResponseDto> rapprocherFacture(
            @PathVariable UUID id, @PathVariable UUID echeanceId, @Valid @RequestBody RapprochementFactureDto dto) {
        return ResponseEntity.ok(service.rapprocherFacture(id, echeanceId, dto));
    }
}

