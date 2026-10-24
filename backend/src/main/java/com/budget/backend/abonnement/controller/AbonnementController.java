package com.budget.backend.abonnement.controller;

import com.budget.backend.common.PageResponseDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import com.budget.backend.abonnement.dto.AbonnementRequestDto;
import com.budget.backend.abonnement.dto.AbonnementResponseDto;
import com.budget.backend.abonnement.dto.AlertesResponseDto;
import com.budget.backend.abonnement.dto.EcheanceResponseDto;
import com.budget.backend.abonnement.service.AbonnementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
    public ResponseEntity<PageResponseDto<AbonnementResponseDto>> listerOuFiltrer(
            @RequestParam(required = false) UUID fournisseurId,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) UUID responsableId,
            @PageableDefault(size = 20, sort = "codeAbonnement") Pageable pageable) {
        return ResponseEntity.ok(service.filtrer(fournisseurId, statut, responsableId, pageable));
    }

    /** Echéances proches et préavis atteints (à traiter par les responsables). */
    @GetMapping("/alertes")
    @PreAuthorize("hasPermission(null, 'abonnement.lire')")
    public ResponseEntity<AlertesResponseDto> alertes(@RequestParam(defaultValue = "10") int jours) {
        return ResponseEntity.ok(service.alertes(Math.max(jours, 0), LocalDate.now()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'abonnement.lire')")
    public ResponseEntity<AbonnementResponseDto> obtenirParId(@PathVariable UUID id) {
        return service.trouverParId(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
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

    @GetMapping("/{id}/echeances")
    @PreAuthorize("hasPermission(null, 'abonnement.lire')")
    public ResponseEntity<PageResponseDto<EcheanceResponseDto>> echeances(
            @PathVariable UUID id,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @PageableDefault(size = 20, sort = "numEcheance") Pageable pageable) {
        return ResponseEntity.ok(service.echeances(id, statut, dateDebut, dateFin, pageable));
    }

    @PostMapping("/{id}/generer-echeances")
    @PreAuthorize("hasPermission(null, 'abonnement.generer_echeances')")
    public ResponseEntity<List<EcheanceResponseDto>> genererEcheances(@PathVariable UUID id) {
        return ResponseEntity.ok(service.genererEcheances(id));
    }

    /** Reconduction : prolonge d'un an et génère les nouvelles échéances. */
    @PostMapping("/{id}/reconduire")
    @PreAuthorize("hasPermission(null, 'abonnement.modifier')")
    public ResponseEntity<AbonnementResponseDto> reconduire(@PathVariable UUID id) {
        return ResponseEntity.ok(service.reconduire(id));
    }
}
