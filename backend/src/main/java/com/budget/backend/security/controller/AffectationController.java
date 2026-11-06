package com.budget.backend.security.controller;

import com.budget.backend.security.dto.AffectationRequestDto;
import com.budget.backend.security.dto.AffectationResponseDto;
import com.budget.backend.security.entity.NiveauAffectation;
import com.budget.backend.security.service.AffectationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/affectations")
public class AffectationController {
    private final AffectationService service;

    public AffectationController(AffectationService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, 'affectation.lire')")
    public List<AffectationResponseDto> lister(
            @RequestParam(required = false) UUID salarieId,
            @RequestParam(required = false) NiveauAffectation niveau,
            @RequestParam(required = false) String structureCode,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String recherche) {
        return service.lister(salarieId, niveau, structureCode, statut, recherche);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'affectation.lire')")
    public AffectationResponseDto obtenir(@PathVariable UUID id) {
        return service.obtenir(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission(null, 'affectation.creer')")
    public AffectationResponseDto creer(@Valid @RequestBody AffectationRequestDto request) {
        return service.creer(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'affectation.modifier')")
    public AffectationResponseDto modifier(@PathVariable UUID id, @Valid @RequestBody AffectationRequestDto request) {
        return service.modifier(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'affectation.supprimer')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable UUID id) {
        service.supprimer(id);
    }
}
