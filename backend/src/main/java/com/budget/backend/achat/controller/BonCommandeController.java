package com.budget.backend.achat.controller;

import com.budget.backend.common.PageResponseDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import com.budget.backend.achat.dto.BonCommandeCreateDto;
import com.budget.backend.achat.dto.BonCommandeResponseDto;
import com.budget.backend.achat.service.BonCommandeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bons-commande")
@CrossOrigin(origins = "*", maxAge = 3600)
public class BonCommandeController {

    @Autowired
    private BonCommandeService service;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'bon_commande.lire')")
    public ResponseEntity<PageResponseDto<BonCommandeResponseDto>> listerOuFiltrer(
            @RequestParam(required = false) String codeBc,
            @RequestParam(required = false) UUID fournisseurId,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) UUID daId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.filtrer(codeBc, fournisseurId, statut, daId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'bon_commande.lire')")
    public ResponseEntity<BonCommandeResponseDto> obtenirParId(@PathVariable UUID id) {
        return service.trouverParId(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'bon_commande.creer')")
    public ResponseEntity<BonCommandeResponseDto> creer(@Valid @RequestBody BonCommandeCreateDto dto) {
        return new ResponseEntity<>(service.creer(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/emettre")
    @PreAuthorize("hasPermission(null, 'bon_commande.emettre')")
    public ResponseEntity<BonCommandeResponseDto> emettre(@PathVariable UUID id) {
        return ResponseEntity.ok(service.emettre(id));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasPermission(null, 'bon_commande.lire')")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        byte[] pdf = service.genererPdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "bon_commande_" + id + ".pdf");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
