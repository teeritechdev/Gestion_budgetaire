package com.budget.backend.security.controller;

import com.budget.backend.security.dto.DirectionRequestDto;
import com.budget.backend.security.dto.DirectionResponseDto;
import com.budget.backend.security.service.DirectionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/directions")
public class DirectionController {
    private final DirectionService directionService;

    public DirectionController(DirectionService directionService) {
        this.directionService = directionService;
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, 'direction.lire')")
    public List<DirectionResponseDto> lister(
            @RequestParam(required = false) String structureCode,
            @RequestParam(required = false) Boolean actif,
            @RequestParam(required = false) String recherche) {
        return directionService.lister(structureCode, actif, recherche);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'direction.lire')")
    public DirectionResponseDto obtenir(@PathVariable UUID id) {
        return directionService.obtenir(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission(null, 'direction.creer')")
    public DirectionResponseDto creer(@Valid @RequestBody DirectionRequestDto request) {
        return directionService.creer(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'direction.modifier')")
    public DirectionResponseDto modifier(
            @PathVariable UUID id,
            @Valid @RequestBody DirectionRequestDto request) {
        return directionService.modifier(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'direction.supprimer')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable UUID id) {
        directionService.supprimer(id);
    }
}
