package com.budget.backend.security.controller;

import com.budget.backend.security.dto.SalarieRequestDto;
import com.budget.backend.security.dto.SalarieResponseDto;
import com.budget.backend.security.service.SalarieService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/salaries")
public class SalarieController {
    private final SalarieService salarieService;

    public SalarieController(SalarieService salarieService) {
        this.salarieService = salarieService;
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, 'salarie.lire')")
    public List<SalarieResponseDto> lister(
            @RequestParam(required = false) UUID serviceId,
            @RequestParam(required = false) Boolean actif,
            @RequestParam(required = false) String recherche) {
        return salarieService.lister(serviceId, actif, recherche);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'salarie.lire')")
    public SalarieResponseDto obtenir(@PathVariable UUID id) {
        return salarieService.obtenir(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission(null, 'salarie.creer')")
    public SalarieResponseDto creer(@Valid @RequestBody SalarieRequestDto request) {
        return salarieService.creer(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'salarie.modifier')")
    public SalarieResponseDto modifier(
            @PathVariable UUID id,
            @Valid @RequestBody SalarieRequestDto request) {
        return salarieService.modifier(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasPermission(null, 'salarie.supprimer')")
    public void supprimer(@PathVariable UUID id) {
        salarieService.supprimer(id);
    }
}
