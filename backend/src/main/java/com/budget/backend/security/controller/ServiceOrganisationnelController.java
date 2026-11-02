package com.budget.backend.security.controller;

import com.budget.backend.security.dto.ServiceRequestDto;
import com.budget.backend.security.dto.ServiceResponseDto;
import com.budget.backend.security.service.ServiceOrganisationnelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/services")
public class ServiceOrganisationnelController {
    private final ServiceOrganisationnelService service;

    public ServiceOrganisationnelController(ServiceOrganisationnelService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, 'service.lire')")
    public List<ServiceResponseDto> lister(
            @RequestParam(required = false) UUID directionId,
            @RequestParam(required = false) String structureCode,
            @RequestParam(required = false) Boolean actif,
            @RequestParam(required = false) String recherche) {
        return service.lister(directionId, structureCode, actif, recherche);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'service.lire')")
    public ServiceResponseDto obtenir(@PathVariable UUID id) {
        return service.obtenir(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission(null, 'service.creer')")
    public ServiceResponseDto creer(@Valid @RequestBody ServiceRequestDto request) {
        return service.creer(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'service.modifier')")
    public ServiceResponseDto modifier(
            @PathVariable UUID id,
            @Valid @RequestBody ServiceRequestDto request) {
        return service.modifier(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'service.supprimer')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable UUID id) {
        service.supprimer(id);
    }
}
