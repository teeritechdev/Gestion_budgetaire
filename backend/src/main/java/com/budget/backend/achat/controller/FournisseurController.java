package com.budget.backend.achat.controller;

import com.budget.backend.achat.dto.FournisseurRequestDto;
import com.budget.backend.achat.dto.FournisseurResponseDto;
import com.budget.backend.achat.entity.Fournisseur;
import com.budget.backend.achat.service.FournisseurService;
import com.budget.backend.common.PageResponseDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/fournisseurs")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FournisseurController {

    @Autowired
    private FournisseurService service;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'fournisseur.lire')")
    public ResponseEntity<PageResponseDto<FournisseurResponseDto>> lister(
            @RequestParam(required = false) String recherche,
            @RequestParam(required = false) Boolean actif,
            @PageableDefault(size = 20, sort = "raisonSociale", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(service.filtrer(recherche, actif, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'fournisseur.lire')")
    public ResponseEntity<FournisseurResponseDto> obtenirParId(@PathVariable UUID id) {
        return service.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'fournisseur.creer')")
    public ResponseEntity<FournisseurResponseDto> creer(@Valid @RequestBody FournisseurRequestDto dto) {
        return new ResponseEntity<>(service.creer(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'fournisseur.modifier')")
    public ResponseEntity<FournisseurResponseDto> modifier(@PathVariable UUID id, @Valid @RequestBody FournisseurRequestDto dto) {
        return ResponseEntity.ok(service.modifier(id, dto));
    }

    @PostMapping("/export/csv")
    @PreAuthorize("hasPermission(null, 'fournisseur.lire')")
    public ResponseEntity<byte[]> exporterCsv(@RequestBody(required = false) Map<String, List<UUID>> body) {
        List<UUID> ids = body != null ? body.get("ids") : null;
        List<Fournisseur> fournisseurs = service.listerTous(ids);
        byte[] csv = service.exporterEnCsv(fournisseurs).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"fournisseurs_" + LocalDate.now() + ".csv\"")
                .body(csv);
    }
}

