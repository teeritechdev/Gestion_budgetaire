package com.budget.backend.achat.controller;

import com.budget.backend.achat.dto.BonCommandeResponseDto;
import com.budget.backend.achat.dto.CreerBcDepuisDaDto;
import com.budget.backend.achat.service.BonCommandeService;
import com.budget.backend.common.PageResponseDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/bons-commande")
@CrossOrigin(origins = "*", maxAge = 3600)
public class BonCommandeController {

    @Autowired
    private BonCommandeService service;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'bon_commande.lire')")
    public ResponseEntity<PageResponseDto<BonCommandeResponseDto>> lister(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) UUID fournisseurId,
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) String recherche,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.filtrer(statut, fournisseurId, annee, recherche, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'bon_commande.lire')")
    public ResponseEntity<BonCommandeResponseDto> obtenirParId(@PathVariable UUID id) {
        return service.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/depuis-da/{daId}")
    @PreAuthorize("hasPermission(null, 'bon_commande.creer')")
    public ResponseEntity<BonCommandeResponseDto> creerDepuisDa(
            @PathVariable UUID daId,
            @Valid @RequestBody CreerBcDepuisDaDto dto) {
        return new ResponseEntity<>(service.creerDepuisDa(daId, dto), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasPermission(null, 'bon_commande.lire')")
    public ResponseEntity<byte[]> telechargerPdf(@PathVariable UUID id) {
        byte[] pdf = service.genererPdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline()
                .filename("bon_commande_" + id + ".pdf")
                .build());
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}

